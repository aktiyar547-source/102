package com.middleeastcontainer.ui.dimension

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.domain.model.Side
import com.middleeastcontainer.domain.repository.SideCaptureRepository
import com.middleeastcontainer.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One row of the side grid, with an absolute path so the thumbnail can render. */
data class SideRow(
    val side: Side,
    val absolutePath: String?,
    val remark: String?,
    val captured: Boolean,
)

@HiltViewModel
class DimensionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sideRepository: SideCaptureRepository,
    private val fileStore: ImageFileStore,
) : ViewModel() {

    val container: String = savedStateHandle["container"] ?: ""
    val type: String = savedStateHandle["type"] ?: ""

    private val _state = MutableStateFlow<UiState<List<SideRow>>>(UiState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = UiState.Loading
            val rows = sideRepository.sidesFor(container).map { capture ->
                SideRow(
                    side = capture.side,
                    absolutePath = capture.imagePath
                        ?.takeIf { it.isNotBlank() }
                        ?.let { fileStore.absoluteFor(it).path },
                    remark = capture.remark,
                    captured = capture.captured,
                )
            }
            _state.value = if (rows.isEmpty()) UiState.Empty else UiState.Content(rows)
        }
    }
}
