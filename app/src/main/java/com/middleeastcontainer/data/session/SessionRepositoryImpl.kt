package com.middleeastcontainer.data.session

import com.middleeastcontainer.core.common.Constants
import com.middleeastcontainer.data.identity.InstallIdProvider
import com.middleeastcontainer.domain.model.Session
import com.middleeastcontainer.domain.repository.SessionRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepositoryImpl @Inject constructor(
    private val securePrefs: SecurePrefs,
    private val installIdProvider: InstallIdProvider,
) : SessionRepository {

    private val prefs get() = securePrefs.prefs

    override fun observeSession(): Flow<Session> = callbackFlow {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(readSession())
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(readSession())
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override suspend fun currentSession(): Session = readSession()

    override suspend fun login(username: String) {
        prefs.edit()
            .putBoolean(Constants.KEY_LOGGED_IN, true)
            .putString(Constants.KEY_USERNAME, username)
            .putString(Constants.KEY_DEVICE_ID, installIdProvider.deviceId)
            .apply()
    }

    override suspend fun updateUsername(username: String) {
        prefs.edit().putString(Constants.KEY_USERNAME, username).apply()
    }

    override suspend fun logout() {
        prefs.edit().putBoolean(Constants.KEY_LOGGED_IN, false).apply()
    }

    override suspend fun deviceId(): String = installIdProvider.deviceId

    private fun readSession() = Session(
        loggedIn = prefs.getBoolean(Constants.KEY_LOGGED_IN, false),
        username = prefs.getString(Constants.KEY_USERNAME, null),
        deviceId = prefs.getString(Constants.KEY_DEVICE_ID, null),
    )
}
