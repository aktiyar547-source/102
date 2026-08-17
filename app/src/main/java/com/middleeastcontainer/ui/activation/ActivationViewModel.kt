package com.middleeastcontainer.ui.activation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.core.common.DispatcherProvider
import com.middleeastcontainer.domain.activation.ActivationGate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ActivationUiState(
    val code: String = "",
    val checking: Boolean = false,
    val error: String? = null,
    val attempts: Int = 0,
)

@HiltViewModel
class ActivationViewModel @Inject constructor(
    private val gate: ActivationGate,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(ActivationUiState())
    val state = _state.asStateFlow()

    val alreadyActivated: Boolean get() = gate.isActivated

    fun onCodeChange(value: String) {
        _state.value = _state.value.copy(code = value, error = null)
    }

    fun submit(onActivated: () -> Unit) {
        val code = _state.value.code
        if (code.isBlank()) {
            _state.value = _state.value.copy(error = "Enter the activation code")
            return
        }
        _state.value = _state.value.copy(checking = true, error = null)
        viewModelScope.launch {
            // 120k PBKDF2 rounds takes long enough to block the frame, so it runs
            // off the main thread — the same cost that makes guessing slow.
            val ok = withContext(dispatchers.default) { gate.activate(code) }
            if (ok) {
                onActivated()
            } else {
                _state.value = _state.value.copy(
                    checking = false,
                    code = "",
                    attempts = _state.value.attempts + 1,
                    error = "That code is not correct.",
                )
            }
        }
    }
}
