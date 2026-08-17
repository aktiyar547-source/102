package com.middleeastcontainer.data.identity

import com.middleeastcontainer.data.session.SecurePrefs
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Replacement for the legacy IMEI (Q4). Generates a stable UUID on first run and
 * persists it in encrypted storage; sent in the same `IMEInum` wire field.
 */
@Singleton
class InstallIdProvider @Inject constructor(private val securePrefs: SecurePrefs) {
    val deviceId: String
        get() = securePrefs.prefs.getString(KEY, null) ?: UUID.randomUUID().toString().also {
            securePrefs.prefs.edit().putString(KEY, it).apply()
        }

    private companion object { const val KEY = "install_uuid" }
}
