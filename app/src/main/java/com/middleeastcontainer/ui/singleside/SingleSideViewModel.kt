package com.middleeastcontainer.ui.singleside

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.domain.model.Side
import com.middleeastcontainer.domain.repository.SideCaptureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SingleSideUiState(
    val remark: String = "",
    val previewPath: String? = null,
    val saving: Boolean = false,
    val message: String? = null,
)

/**
 * Remark editor for one side, reached by long-pressing a row in the grid.
 *
 * Capture lives in the camera screen, so this only reads the existing photo and
 * writes the note. A remark can be saved with no photo present — "side against a
 * wall, could not reach" is exactly the kind of thing worth recording.
 */
@HiltViewModel
class SingleSideViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sideRepository: SideCaptureRepository,
    private val fileStore: ImageFileStore,
) : ViewModel() {

    val container: String = savedStateHandle["container"] ?: ""
    private val sideDbName: String = savedStateHandle["side"] ?: ""
    val side: Side = Side.entries.first { it.dbName == sideDbName }

    private val _state = MutableStateFlow(SingleSideUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = sideRepository.sidesFor(container).firstOrNull { it.side == side }
            _state.value = _state.value.copy(
                remark = existing?.remark.orEmpty(),
                previewPath = existing?.imagePath
                    ?.takeIf { it.isNotBlank() }
                    ?.let { fileStore.absoluteFor(it).path },
            )
        }
    }

    fun onRemarkChange(v: String) {
        _state.value = _state.value.copy(remark = v, message = null)
    }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            // Remark only: leaves the photo and the captured flag untouched.
            sideRepository.saveRemark(container, side, _state.value.remark)
            _state.value = _state.value.copy(saving = false)
            onDone()
        }
    }
}
