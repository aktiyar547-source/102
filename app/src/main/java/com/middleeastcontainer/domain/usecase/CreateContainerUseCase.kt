package com.middleeastcontainer.domain.usecase

import com.middleeastcontainer.domain.repository.ContainerRepository
import javax.inject.Inject

/**
 * Validates the container number (ISO 6346) and, if valid and unique, creates the
 * local inspection. Returns a typed outcome the UI maps to legacy toasts/errors.
 */
class CreateContainerUseCase @Inject constructor(
    private val validate: ValidateContainerNumberUseCase,
    private val repository: ContainerRepository,
) {
    sealed interface Outcome {
        data object Created : Outcome
        data class InvalidNumber(val reason: ValidateContainerNumberUseCase.Reason) : Outcome
        data object Duplicate : Outcome
    }

    suspend operator fun invoke(name: String, type: String): Outcome {
        when (val v = validate(name)) {
            is ValidateContainerNumberUseCase.Result.Invalid -> return Outcome.InvalidNumber(v.reason)
            ValidateContainerNumberUseCase.Result.Valid -> Unit
        }
        if (repository.get(name) != null) return Outcome.Duplicate
        return try {
            repository.create(name, type)
            Outcome.Created
        } catch (e: Exception) {
            Outcome.Duplicate // UNIQUE constraint race -> treat as duplicate (legacy parity)
        }
    }
}
