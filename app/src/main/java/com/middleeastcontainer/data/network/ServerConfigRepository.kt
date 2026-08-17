package com.middleeastcontainer.data.network

import com.middleeastcontainer.core.common.AppConfig
import com.middleeastcontainer.data.session.SecurePrefs
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The backend base URL, changeable at runtime from Settings.
 *
 * Defaults to the value baked into the build flavor, but an inspector or admin can
 * point the app at a different server without a rebuild - essential while the real
 * host is still being confirmed.
 */
@Singleton
class ServerConfigRepository @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val appConfig: AppConfig,
) {
    /** Full base URL, always ending in '/'. */
    var baseUrl: String
        get() = securePrefs.prefs.getString(KEY_BASE_URL, null)?.takeIf { it.isNotBlank() }
            ?: appConfig.mainBaseUrl
        set(value) {
            val normalised = value.trim().let { if (it.endsWith("/")) it else "$it/" }
            securePrefs.prefs.edit().putString(KEY_BASE_URL, normalised).apply()
        }

    /** Restores the URL compiled into this build flavor. */
    fun resetToDefault() {
        securePrefs.prefs.edit().remove(KEY_BASE_URL).apply()
    }

    val defaultBaseUrl: String get() = appConfig.mainBaseUrl

    private companion object {
        const val KEY_BASE_URL = "server_base_url"
    }
}
