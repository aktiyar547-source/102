package com.middleeastcontainer.domain.repository

import com.middleeastcontainer.domain.model.Session
import kotlinx.coroutines.flow.Flow

/** Login state + identity. Legacy stored these in SharedPreferences("MyPref"). */
interface SessionRepository {
    fun observeSession(): Flow<Session>
    suspend fun currentSession(): Session
    /** Persists username + marks logged in. Device id is generated once and reused. */
    suspend fun login(username: String)
    suspend fun updateUsername(username: String)
    suspend fun logout()
    /** The stable device identifier sent in the wire `IMEInum` field. */
    suspend fun deviceId(): String
}
