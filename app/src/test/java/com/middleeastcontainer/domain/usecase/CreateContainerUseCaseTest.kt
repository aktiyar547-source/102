package com.middleeastcontainer.domain.usecase

import com.middleeastcontainer.domain.model.Container
import com.middleeastcontainer.domain.repository.ContainerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateContainerUseCaseTest {

    private class FakeRepo(private val existing: Set<String> = emptySet()) : ContainerRepository {
        var createdName: String? = null
        override fun observeAll(): Flow<List<Container>> = emptyFlow()
        override suspend fun get(name: String): Container? =
            if (name in existing) Container(name, "Standard 20", "", "Upload", null, null, "Upload", "") else null
        override suspend fun create(name: String, type: String) { createdName = name }
        override suspend fun updateType(name: String, type: String) {}
        override suspend fun delete(name: String) {}
        override suspend fun purgeUploadedBefore(cutoffDate: String) {}
    }

    private val validate = ValidateContainerNumberUseCase()

    @Test
    fun `creates a valid unique container`() = runTest {
        val repo = FakeRepo()
        val outcome = CreateContainerUseCase(validate, repo).invoke("CSQU3054383", "Standard 20")
        assertEquals(CreateContainerUseCase.Outcome.Created, outcome)
        assertEquals("CSQU3054383", repo.createdName)
    }

    @Test
    fun `rejects an invalid container number without touching the repo`() = runTest {
        val repo = FakeRepo()
        val outcome = CreateContainerUseCase(validate, repo).invoke("CSQU3054384", "Standard 20")
        assertTrue(outcome is CreateContainerUseCase.Outcome.InvalidNumber)
        assertEquals(null, repo.createdName)
    }

    @Test
    fun `reports duplicate for an existing container`() = runTest {
        val repo = FakeRepo(existing = setOf("CSQU3054383"))
        val outcome = CreateContainerUseCase(validate, repo).invoke("CSQU3054383", "Standard 20")
        assertEquals(CreateContainerUseCase.Outcome.Duplicate, outcome)
    }
}
