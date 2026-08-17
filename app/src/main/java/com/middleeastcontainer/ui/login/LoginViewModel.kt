package com.middleeastcontainer.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.domain.repository.SessionRepository
import com.middleeastcontainer.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val deviceId: String = "",
    val alreadyLoggedIn: Boolean = false,
    val usernameError: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: SessionRepository,
    private val login: LoginUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val current = session.currentSession()
            _state.value = _state.value.copy(
                deviceId = session.deviceId(),
                username = current.username.orEmpty(),
                alreadyLoggedIn = current.loggedIn,
            )
        }
    }

    fun onUsernameChange(value: String) {
        _state.value = _state.value.copy(username = value, usernameError = null)
    }

    /** @return true if login succeeded and the caller should navigate onward. */
    fun save(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (login(_state.value.username)) onSuccess()
            else _state.value = _state.value.copy(usernameError = "Enter username")
        }
    }
}
