package com.middleeastcontainer.ui.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.core.common.Constants
import com.middleeastcontainer.domain.model.Container
import com.middleeastcontainer.domain.repository.ContainerRepository
import com.middleeastcontainer.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeleteViewModel @Inject constructor(
    private val repository: ContainerRepository,
) : ViewModel() {

    // Legacy Delete only lists already-uploaded (Status1='Done') containers.
    val state = repository.observeAll()
        .map { list -> list.filter { it.uploadStatus == Constants.STATUS_DONE } }
        .map { if (it.isEmpty()) UiState.Empty else UiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun delete(names: Set<String>) = viewModelScope.launch {
        names.forEach { repository.delete(it) }
    }
}
