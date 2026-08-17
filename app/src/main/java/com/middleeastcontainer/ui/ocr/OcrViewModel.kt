package com.middleeastcontainer.ui.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.data.camera.CaptureFileProvider
import com.middleeastcontainer.domain.model.ContainerType
import com.middleeastcontainer.domain.ocr.ContainerOcrEngine
import com.middleeastcontainer.domain.usecase.CreateContainerUseCase
import com.middleeastcontainer.domain.usecase.ValidateContainerNumberUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OcrUiState(
    val containerNumber: String = "",
    val types: List<String> = ContainerType.wireValues,
    val selectedType: String = ContainerType.wireValues.first(),
    val recognizing: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class OcrViewModel @Inject constructor(
    private val ocrEngine: ContainerOcrEngine,
    private val createContainer: CreateContainerUseCase,
    private val captureFiles: CaptureFileProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(OcrUiState())
    val state = _state.asStateFlow()

    init {
        // If the app died mid-scan, a scratch photo survives. Reading it now
        // would prefill the *previous* container's number onto a new inspection,
        // so discard anything left over before this screen starts listening.
        captureFiles.ocrScratchFile().delete()
    }

    fun onNumberChange(v: String) { _state.value = _state.value.copy(containerNumber = v.uppercase(), error = null) }
    fun onTypeChange(v: String) { _state.value = _state.value.copy(selectedType = v) }

    /**
     * Called when the screen resumes. If the camera left a scan behind, read the
     * container number from it and delete it — the scan itself is not kept, only
     * the number it yielded.
     */
    fun collectPendingScan() {
        val scan = captureFiles.ocrScratchFile()
        if (!scan.exists()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(recognizing = true)
            val candidate = ocrEngine.recognizeContainerNumber(scan.path)
            scan.delete()
            _state.value = _state.value.copy(
                recognizing = false,
                containerNumber = candidate ?: _state.value.containerNumber,
                error = if (candidate == null) "No container number found — type it below." else null,
            )
        }
    }

    fun save(onCreated: (container: String, type: String) -> Unit) {
        viewModelScope.launch {
            val name = _state.value.containerNumber.trim()
            when (val outcome = createContainer(name, _state.value.selectedType)) {
                CreateContainerUseCase.Outcome.Created -> onCreated(name, _state.value.selectedType)
                is CreateContainerUseCase.Outcome.InvalidNumber ->
                    _state.value = _state.value.copy(error = reasonText(outcome.reason))
                CreateContainerUseCase.Outcome.Duplicate ->
                    _state.value = _state.value.copy(error = "Project name should be unique")
            }
        }
    }

    private fun reasonText(reason: ValidateContainerNumberUseCase.Reason): String = "Enter Correct Container No"
}
