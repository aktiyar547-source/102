package com.middleeastcontainer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.middleeastcontainer.core.common.DispatcherProvider
import com.middleeastcontainer.data.network.ServerConfigRepository
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.domain.repository.SessionRepository
import com.middleeastcontainer.domain.usecase.UpdateUsernameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import javax.inject.Inject

data class SettingsUiState(
    val username: String = "",
    /** True when photos are going to the visible /OCR2 folder. */
    val hasPhotoFolderAccess: Boolean = false,
    val photoFolderPath: String = "",
    val deviceId: String = "",
    val serverUrl: String = "",
    val error: String? = null,
    val testing: Boolean = false,
    val testResult: String? = null,
    val testOk: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val session: SessionRepository,
    private val updateUsername: UpdateUsernameUseCase,
    private val serverConfig: ServerConfigRepository,
    private val fileStore: ImageFileStore,
    private val httpClient: OkHttpClient,
    private val dispatchers: DispatcherProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val s = session.currentSession()
            _state.value = SettingsUiState(
                username = s.username.orEmpty(),
                deviceId = session.deviceId(),
                serverUrl = serverConfig.baseUrl,
                hasPhotoFolderAccess = fileStore.hasRootAccess,
                photoFolderPath = fileStore.sharedRootPath(),
            )
        }
    }

    /** Re-read on resume, since the permission is granted outside the app. */
    fun refreshPhotoFolderAccess() {
        _state.value = _state.value.copy(
            hasPhotoFolderAccess = fileStore.hasRootAccess,
            photoFolderPath = fileStore.sharedRootPath(),
        )
    }

    fun onUsernameChange(v: String) {
        _state.value = _state.value.copy(username = v, error = null)
    }

    fun onServerUrlChange(v: String) {
        _state.value = _state.value.copy(serverUrl = v, testResult = null)
    }

    fun resetServerUrl() {
        serverConfig.resetToDefault()
        _state.value = _state.value.copy(serverUrl = serverConfig.defaultBaseUrl, testResult = null)
    }

    /** Saves both the username and the server address. */
    fun save(onSuccess: () -> Unit) = viewModelScope.launch {
        val url = _state.value.serverUrl.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            _state.value = _state.value.copy(error = "Server URL must start with http:// or https://")
            return@launch
        }
        serverConfig.baseUrl = url
        if (updateUsername(_state.value.username)) {
            onSuccess()
        } else {
            _state.value = _state.value.copy(error = "Enter username")
        }
    }

    /**
     * Checks the server is reachable. Any HTTP reply - even 404 - proves the host
     * resolves and is accepting connections, which is what we need to know.
     */
    fun testConnection() = viewModelScope.launch {
        val url = _state.value.serverUrl.trim()
        _state.value = _state.value.copy(testing = true, testResult = null)

        val result = withContext(dispatchers.io) {
            runCatching {
                val request = Request.Builder().url(url).build()
                // Bypass the host-rewriting interceptor: test exactly what was typed.
                val plain = OkHttpClient.Builder().build()
                plain.newCall(request).execute().use { it.code }
            }
        }

        _state.value = result.fold(
            onSuccess = { code ->
                _state.value.copy(
                    testing = false,
                    testOk = true,
                    testResult = "Server reachable (HTTP $code)",
                )
            },
            onFailure = { e ->
                Timber.w(e, "Connection test failed")
                _state.value.copy(
                    testing = false,
                    testOk = false,
                    testResult = "Cannot reach server: ${e.message ?: e::class.simpleName}",
                )
            },
        )
    }
}
