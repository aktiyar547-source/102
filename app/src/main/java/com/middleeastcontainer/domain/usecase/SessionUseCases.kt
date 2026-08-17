package com.middleeastcontainer.domain.usecase

import com.middleeastcontainer.domain.model.Session
import com.middleeastcontainer.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(private val repo: SessionRepository) {
    /** Legacy required only a non-empty username. */
    suspend operator fun invoke(username: String): Boolean {
        val u = username.trim()
        if (u.isEmpty()) return false
        repo.login(u)
        return true
    }
}

class ObserveSessionUseCase @Inject constructor(private val repo: SessionRepository) {
    operator fun invoke(): Flow<Session> = repo.observeSession()
}

class UpdateUsernameUseCase @Inject constructor(private val repo: SessionRepository) {
    suspend operator fun invoke(username: String): Boolean {
        val u = username.trim()
        if (u.isEmpty()) return false
        repo.updateUsername(u)
        return true
    }
}
