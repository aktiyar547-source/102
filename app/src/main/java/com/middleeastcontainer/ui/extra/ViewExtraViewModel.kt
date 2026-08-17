package com.middleeastcontainer.ui.extra

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.domain.model.ExtraImage
import com.middleeastcontainer.domain.repository.ExtraImageRepository
import com.middleeastcontainer.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExtraItem(val extra: ExtraImage, val absolutePath: String?)

@HiltViewModel
class ViewExtraViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val extraRepository: ExtraImageRepository,
    private val fileStore: ImageFileStore,
) : ViewModel() {

    val container: String = savedStateHandle["container"] ?: ""

    private val _state = MutableStateFlow<UiState<List<ExtraItem>>>(UiState.Loading)
    val state = _state.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        _state.value = UiState.Loading
        val items = extraRepository.forContainer(container).map {
            ExtraItem(it, it.imagePath?.let { p -> fileStore.absoluteFor(p).path })
        }
        _state.value = if (items.isEmpty()) UiState.Empty else UiState.Content(items)
    }
}
