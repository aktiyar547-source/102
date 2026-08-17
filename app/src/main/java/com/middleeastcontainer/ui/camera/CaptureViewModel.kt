package com.middleeastcontainer.ui.camera

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.core.common.Constants
import com.middleeastcontainer.data.camera.CaptureFileProvider
import com.middleeastcontainer.data.camera.WatermarkUtil
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.domain.model.Side
import com.middleeastcontainer.domain.repository.ExtraImageRepository
import com.middleeastcontainer.domain.repository.SideCaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

data class CaptureUiState(
    /** Single-shot mode only: a save is in flight and the screen is about to close. */
    val saving: Boolean = false,
    /** Single-shot mode only: stored, the screen may leave. */
    val saved: Boolean = false,
    val error: String? = null,
    /** Photos taken since the camera opened. */
    val shotCount: Int = 0,
    /** Photos captured but not yet written to storage. */
    val pending: Int = 0,
    /** Most recent stored photo, shown as a confirmation thumbnail. */
    val lastThumbnail: String? = null,
    /** Done was pressed and the queue is draining. */
    val finishing: Boolean = false,
)

/**
 * Stores photos as they are taken.
 *
 * Two behaviours share one screen:
 *
 * - A **named side** is one deliberate shot. The save completes before the screen
 *   closes, because the ViewModel dies with the destination and an in-flight
 *   coroutine would be cancelled with it.
 * - **Extras** are unlimited. The shutter hands each photo to a queue and re-arms
 *   immediately, so shooting is never gated on watermarking and encoding.
 *
 * The queue has a single consumer on purpose. Watermarking holds a decoded bitmap
 * of roughly 12 MB; running twenty of those at once because someone tapped quickly
 * would exhaust the heap. Serialising keeps peak memory flat no matter how fast
 * the shutter is pressed.
 */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sideRepository: SideCaptureRepository,
    private val extraRepository: ExtraImageRepository,
    private val watermark: WatermarkUtil,
    private val captureFiles: CaptureFileProvider,
    private val fileStore: ImageFileStore,
) : ViewModel() {

    val container: String = savedStateHandle["container"] ?: ""
    private val target: String = savedStateHandle["target"] ?: EXTRA

    private val side: Side? = Side.entries.firstOrNull { it.dbName == target }
    private val isOcrScan: Boolean = target == OCR

    /** Unlimited shooting: anything that is not a named side or a number scan. */
    val isBurst: Boolean = side == null && !isOcrScan

    /**
     * Whether to favour image quality over shutter speed.
     *
     * A scan or a single named side is one deliberate shot where detail decides
     * whether OCR can read a distant number. Unlimited shooting is the opposite:
     * the inspector is working through a container quickly, and a slower shutter
     * would be felt on every tap.
     */
    val prefersQuality: Boolean = !isBurst

    val containerLabel: String = if (isOcrScan) "New inspection" else container

    val targetLabel: String = when {
        isOcrScan -> "Container number"
        side != null -> side.label
        else -> "Photos"
    }

    private val _state = MutableStateFlow(CaptureUiState())
    val state = _state.asStateFlow()

    private val saveQueue = Channel<File>(Channel.UNLIMITED)

    init {
        // Clear temp files from any previous session killed mid-save.
        captureFiles.pruneStaleCaptures()

        // One consumer, so saves are serialised however fast photos arrive.
        viewModelScope.launch {
            for (file in saveQueue) {
                store(file)
                _state.update { it.copy(pending = (it.pending - 1).coerceAtLeast(0)) }
            }
        }
    }

    fun newCaptureFile(): File =
        if (isOcrScan) captureFiles.ocrScratchFile() else captureFiles.newCaptureFile()

    fun onCameraError(message: String) {
        Timber.w("Capture failed: %s", message)
        _state.update { it.copy(error = message, saving = false) }
    }

    /**
     * A photo has landed on disk from the camera.
     *
     * In burst mode this returns at once so the shutter can fire again; the photo
     * is watermarked and filed behind the scenes.
     */
    fun onPhotoTaken(file: File) {
        // A scan is read for its text and discarded — not watermarked or filed.
        if (isOcrScan) {
            _state.update { it.copy(saved = true) }
            return
        }

        if (isBurst) {
            _state.update {
                it.copy(shotCount = it.shotCount + 1, pending = it.pending + 1, error = null)
            }
            saveQueue.trySend(file)
            return
        }

        // Named side: finish writing before the screen closes.
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            store(file)
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    /**
     * Done pressed. If photos are still being written, wait — this ViewModel is
     * destroyed when the screen closes, which would cancel them mid-write.
     */
    fun onFinish(onDone: () -> Unit) {
        if (_state.value.pending == 0) {
            onDone()
            return
        }
        _state.update { it.copy(finishing = true) }
        viewModelScope.launch {
            while (_state.value.pending > 0) {
                delay(50)
            }
            onDone()
        }
    }

    override fun onCleared() {
        saveQueue.close()
        super.onCleared()
    }

    private suspend fun store(file: File) {
        runCatching {
            watermark.applyTimestampWatermark(file, container)
            val relativePath = fileStore.importCapture(
                Constants.INSPECTION_DIR, container, file,
            )
            if (side != null) {
                sideRepository.saveSide(container, side, relativePath, "")
            } else {
                extraRepository.add(container, relativePath, "", "")
            }
            fileStore.absoluteFor(relativePath).path
        }.onSuccess { stored ->
            _state.update { it.copy(lastThumbnail = stored) }
        }.onFailure { e ->
            Timber.e(e, "Could not store the photo")
            _state.update { it.copy(error = e.message ?: "Could not save the photo") }
        }
    }

    companion object {
        /** Route value for unlimited shooting against a container. */
        const val EXTRA = "__extra__"

        /** Route value for the container-number scan on the New Project screen. */
        const val OCR = "__ocr__"
    }
}
