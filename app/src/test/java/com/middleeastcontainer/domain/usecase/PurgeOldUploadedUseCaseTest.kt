package com.middleeastcontainer.domain.usecase

import com.middleeastcontainer.core.common.Clock
import com.middleeastcontainer.domain.model.Container
import com.middleeastcontainer.domain.repository.ContainerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar

class PurgeOldUploadedUseCaseTest {

    private class FixedClock(private val cal: Calendar) : Clock {
        override fun now(): Date = cal.time
        override fun calendar(): Calendar = cal.clone() as Calendar
    }

    private class RecordingRepo : ContainerRepository {
        var purgedCutoff: String? = null
        override fun observeAll(): Flow<List<Container>> = emptyFlow()
        override suspend fun get(name: String): Container? = null
        override suspend fun create(name: String, type: String) {}
        override suspend fun updateType(name: String, type: String) {}
        override suspend fun delete(name: String) {}
        override suspend fun purgeUploadedBefore(cutoffDate: String) { purgedCutoff = cutoffDate }
    }

    @Test
    fun `computes cutoff as now minus retention days in yyyy-MM-dd`() = runTest {
        // 2026-07-25 -> minus 7 days -> 2026-07-18
        val clock = FixedClock(GregorianCalendar(2026, Calendar.JULY, 25))
        val repo = RecordingRepo()

        PurgeOldUploadedUseCase(repo, clock).invoke(retentionDays = 7)

        assertEquals("2026-07-18", repo.purgedCutoff)
    }

    @Test
    fun `cutoff rolls across month boundary`() = runTest {
        val clock = FixedClock(GregorianCalendar(2026, Calendar.MARCH, 3))
        val repo = RecordingRepo()

        PurgeOldUploadedUseCase(repo, clock).invoke(retentionDays = 7)

        assertEquals("2026-02-24", repo.purgedCutoff)
    }
}
