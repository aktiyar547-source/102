package com.middleeastcontainer.ui.inventory

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.core.common.Constants
import com.middleeastcontainer.data.camera.CaptureFileProvider
import com.middleeastcontainer.data.sync.UploadScheduler
import com.middleeastcontainer.data.camera.WatermarkUtil
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.domain.model.Sighting
import com.middleeastcontainer.domain.model.UnreadUnit
import com.middleeastcontainer.domain.ocr.ContainerOcrEngine
import com.middleeastcontainer.domain.ocr.DetectedNumber
import com.middleeastcontainer.domain.ocr.FrameReading
import com.middleeastcontainer.domain.ocr.UnreadRegion
import com.middleeastcontainer.domain.repository.InventoryRepository
import com.middleeastcontainer.domain.usecase.ValidateContainerNumberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/** Numbers read from the frame just taken, awaiting confirmation. */
data class PendingShot(
    val photoAbsolutePath: String,
    val photoRelativePath: String,
    val detected: List<DetectedNumber>,
    val unread: List<UnreadRegion> = emptyList(),
)

data class SweepUiState(
    val scanning: Boolean = false,
    /** Set only when the frame needs a decision — nothing read, or a review. */
    val pending: PendingShot? = null,
    val message: String? = null,
    /** The frame just accepted, shown as a strip that can be undone. */
    val lastShot: AcceptedShot? = null,
)

/**
 * A frame that counted itself.
 *
 * Kept visible after the fact rather than confirmed beforehand: the ISO 6346
 * check digit rejects about 96% of single-character misreads, so stopping the
 * inspector on every frame buys little and costs a tap forty times a sweep.
 * What it must not do is hide what happened, hence the undo.
 */
data class AcceptedShot(
    val photoAbsolutePath: String,
    val photoRelativePath: String,
    val added: List<String>,
    val duplicates: List<String>,
    val detected: List<DetectedNumber>,
)

/**
 * Drives one yard sweep.
 *
 * Every frame is passed through multi-number OCR, and each candidate is validated
 * by its ISO 6346 check digit — which is what makes a photo of a stack usable, as
 * tare weights, max-gross figures and CSC plates cannot pass it.
 *
 * Detections are shown for confirmation rather than counted silently. The app is
 * choosing what goes on the manifest, and a misread number is worse than a missed
 * one: a wrong unit reconciles against nothing and sends someone looking for a
 * container that was never there.
 */
@HiltViewModel
class SweepViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: InventoryRepository,
    private val ocr: ContainerOcrEngine,
    private val validate: ValidateContainerNumberUseCase,
    private val watermark: WatermarkUtil,
    private val captureFiles: CaptureFileProvider,
    private val fileStore: ImageFileStore,
    private val scheduler: UploadScheduler,
) : ViewModel() {

    val sweepId: Long = savedStateHandle.get<String>("sweepId")?.toLongOrNull() ?: 0L

    private val _state = MutableStateFlow(SweepUiState())
    val state = _state.asStateFlow()

    /** Live list of units counted so far — visible while walking, not just at the end. */
    val sightings = repository.observeSightings(sweepId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Named at the start of the sweep; stamped into every photo and the export. */
    private val _zone = MutableStateFlow("")
    val zone = _zone.asStateFlow()

    /** Units the camera saw but could not read — the sweep is not done until empty. */
    val unread = repository.observeUnread(sweepId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Containers seen but unread. A sweep with gaps should say so. */

    init {
        viewModelScope.launch {
            _zone.value = repository.sweep(sweepId)?.zone.orEmpty()
        }
    }

    fun newCaptureFile(): File = captureFiles.newCaptureFile()

    fun onCameraError(message: String) {
        _state.update { it.copy(scanning = false, message = message) }
    }

    /**
     * Reads every container number in the frame.
     *
     * The photo is filed straight away: it is the evidence behind each row, and
     * keeping it only until confirmation would lose it if the app died first.
     */
    fun onPhotoTaken(file: File) {
        _state.update { it.copy(scanning = true, message = null, lastShot = null) }
        viewModelScope.launch {
            runCatching {
                // Read the numbers BEFORE the photo is watermarked and resized.
                // Watermarking downsizes to the storage edge and burns text into
                // the frame; doing it first would hand OCR a smaller image than
                // the camera captured, which is precisely the detail a distant
                // number in a stack depends on.
                val reading = ocr.readFrame(file.path)

                watermark.applyTimestampWatermark(file, _zone.value)
                val relative = fileStore.importCapture(
                    Constants.INVENTORY_DIR,
                    _zone.value.ifBlank { "Yard" },
                    file,
                )
                PendingShot(
                    photoAbsolutePath = fileStore.absoluteFor(relative).path,
                    photoRelativePath = relative,
                    detected = reading.confirmed,
                    unread = reading.unread,
                )
            }.onSuccess { shot ->
                Timber.d("Sweep %d: %d number(s) in frame", sweepId, shot.detected.size)
                if (shot.detected.isEmpty() && shot.unread.isEmpty()) {
                    // Nothing read is the one case worth stopping for — the
                    // inspector needs to move closer or type it.
                    _state.update {
                        it.copy(
                            scanning = false,
                            pending = shot,
                            message = null,
                        )
                    }
                } else {
                    accept(shot)
                }
            }.onFailure { e ->
                Timber.e(e, "Sweep capture failed")
                _state.update {
                    it.copy(scanning = false, message = e.message ?: "Could not read the photo")
                }
            }
        }
    }

    /** Counts a frame and keeps it on screen so it can be undone. */
    private fun accept(shot: PendingShot) {
        viewModelScope.launch {
            val already = sightings.value.map { it.containerNumber }.toSet()
            val numbers = shot.detected.map { it.number }
            val duplicates = numbers.filter { it in already }
            repository.addSightings(sweepId, numbers, shot.photoRelativePath, true)

            // Anything the camera saw but could not read is recorded and tagged,
            // so the sweep reports "ten here, four unread" rather than six.
            val tags = repository.addUnread(
                sweepId,
                shot.unread.map { r ->
                    r.partial to floatArrayOf(r.box.left, r.box.top, r.box.right, r.box.bottom)
                },
                shot.photoRelativePath,
            )

            // A number read now may be one flagged earlier from another angle.
            resolveMatching(numbers)
            _state.update {
                it.copy(
                    scanning = false,
                    pending = null,
                    message = null,
                    lastShot = AcceptedShot(
                        photoAbsolutePath = shot.photoAbsolutePath,
                        photoRelativePath = shot.photoRelativePath,
                        added = numbers.filterNot { n -> n in already },
                        duplicates = duplicates,
                        detected = shot.detected,
                        needsAttention = tags,
                    ),
                )
            }
        }
    }

    /** Opens the last frame for review — to correct a misread or add a missed unit. */
    fun reviewLastShot() {
        val last = _state.value.lastShot ?: return
        _state.update {
            it.copy(
                pending = PendingShot(last.photoAbsolutePath, "", last.detected),
                lastShot = null,
            )
        }
    }

    /** Removes everything the last frame added. */
    fun undoLastShot() {
        val last = _state.value.lastShot ?: return
        viewModelScope.launch {
            val current = sightings.value
            last.added.forEach { number ->
                current.firstOrNull { it.containerNumber == number }
                    ?.let { repository.removeSighting(it.id) }
            }
            _state.update { it.copy(lastShot = null) }
        }
    }

    fun dismissLastShot() {
        _state.update { it.copy(lastShot = null) }
    }

    /**
     * Records that the last frame held units the camera could not read.
     *
     * Only the inspector can know this — OCR reports what it read, not what was
     * there — so it is a deliberate tap, kept to one gesture so it does not
     * reintroduce the confirmation step on every frame.
     */
    fun noteMissed(count: Int) {
        val last = _state.value.lastShot ?: return
        viewModelScope.launch {
            repository.notePending(sweepId, count, last.photoRelativePath)
            _state.update { it.copy(lastShot = null) }
        }
    }



    /** Accepts an edited set of numbers from the review sheet. */
    fun confirm(numbers: List<String>) {
        val shot = _state.value.pending ?: return
        viewModelScope.launch {
            val photo = shot.photoRelativePath.ifBlank { null }
            val added = repository.addSightings(sweepId, numbers, photo, true)
            _state.update {
                it.copy(
                    pending = null,
                    message = when {
                        numbers.isEmpty() -> null
                        added == 0 -> "Already counted"
                        else -> null
                    },
                )
            }
        }
    }

    fun discardShot() {
        _state.update { it.copy(pending = null, message = null) }
    }

    /**
     * Clears any outstanding flag whose partial text matches a number just read.
     *
     * An inspector walking closer to photograph A3 will usually capture it in a
     * frame of its own; without this the flag would linger after the work was
     * already done.
     */
    private suspend fun resolveMatching(numbers: List<String>) {
        if (numbers.isEmpty()) return
        for (pending in unread.value) {
            val match = numbers.firstOrNull { n ->
                pending.partial.length >= 4 && n.startsWith(pending.partial.take(4))
            }
            if (match != null) repository.resolveUnread(pending.id, match)
        }
    }

    /**
     * Clears a flagged unit by typing its number.
     *
     * Rust and glare defeat the camera far more often than they defeat a person
     * standing in front of the container, so this is frequently faster than
     * walking back for another photograph.
     */
    fun resolveUnread(item: UnreadUnit, number: String, onInvalid: () -> Unit) {
        val cleaned = number.trim().uppercase()
        if (validate(cleaned) !is ValidateContainerNumberUseCase.Result.Valid) {
            onInvalid()
            return
        }
        viewModelScope.launch {
            repository.addSightings(sweepId, listOf(cleaned), item.photoPath, false)
            repository.resolveUnread(item.id, cleaned)
        }
    }

    fun dismissUnread(item: UnreadUnit) {
        viewModelScope.launch { repository.dismissUnread(item.id) }
    }

    /** Adds a unit the camera could not read — unreachable, damaged or painted over. */
    fun addManually(number: String, onInvalid: () -> Unit) {
        val cleaned = number.trim().uppercase()
        if (validate(cleaned) !is ValidateContainerNumberUseCase.Result.Valid) {
            onInvalid()
            return
        }
        viewModelScope.launch {
            val added = repository.addSightings(sweepId, listOf(cleaned), null, false)
            resolveMatching(listOf(cleaned))
            _state.update {
                it.copy(message = if (added == 0) "Already counted" else null)
            }
        }
    }

    fun remove(sighting: Sighting) {
        viewModelScope.launch { repository.removeSighting(sighting.id) }
    }

    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.finishSweep(sweepId)
            // Queued rather than sent: it must survive leaving the yard.
            scheduler.enqueueSweepUpload(sweepId)
            onDone()
        }
    }


}
