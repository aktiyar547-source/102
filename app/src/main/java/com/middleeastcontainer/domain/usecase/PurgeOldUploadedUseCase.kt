package com.middleeastcontainer.domain.usecase

import com.middleeastcontainer.core.common.Clock
import com.middleeastcontainer.core.common.Constants
import com.middleeastcontainer.core.common.DateFormats
import com.middleeastcontainer.domain.repository.ContainerRepository
import javax.inject.Inject

/**
 * Q7 housekeeping. Unlike legacy (which deleted anything exactly 2 days old,
 * losing un-uploaded work), this purges ONLY uploaded inspections strictly older
 * than [Constants.RETENTION_DAYS]. Never deletes pending work.
 */
class PurgeOldUploadedUseCase @Inject constructor(
    private val repository: ContainerRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(retentionDays: Long = Constants.RETENTION_DAYS) {
        val cutoff = DateFormats.createdDateMinusDays(clock.calendar(), retentionDays)
        repository.purgeUploadedBefore(cutoff)
    }
}
