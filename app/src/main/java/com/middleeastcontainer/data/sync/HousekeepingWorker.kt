package com.middleeastcontainer.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.middleeastcontainer.domain.usecase.PurgeOldUploadedUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Periodic Q7 purge of old, already-uploaded inspections. */
@HiltWorker
class HousekeepingWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val purgeOldUploaded: PurgeOldUploadedUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            purgeOldUploaded()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object { const val UNIQUE_NAME = "housekeeping_purge" }
}
