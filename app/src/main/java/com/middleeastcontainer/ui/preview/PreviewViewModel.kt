package com.middleeastcontainer.ui.preview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.domain.model.Container
import com.middleeastcontainer.domain.model.ContainerType
import com.middleeastcontainer.domain.repository.ContainerRepository
import com.middleeastcontainer.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val repository: ContainerRepository,
) : ViewModel() {

    val types: List<String> = ContainerType.wireValues

    val state = repository.observeAll()
        .map { list -> if (list.isEmpty()) UiState.Empty else UiState.Content(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun updateType(container: String, type: String) = viewModelScope.launch {
        repository.updateType(container, type)
    }
}
