package com.middleeastcontainer.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.middleeastcontainer.domain.repository.InventoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Durable upload of one finished sweep.
 *
 * Same guarantee as an inspection upload: a sweep survives loss of signal and
 * process death, and retries on its own. A count that reached nobody is a walk
 * around the yard wasted.
 */
@HiltWorker
class UploadSweepWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val inventoryRepository: InventoryRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val sweepId = inputData.getLong(KEY_SWEEP_ID, -1L)
        if (sweepId <= 0) return Result.failure()

        Timber.d("Uploading sweep %d (attempt %d)", sweepId, runAttemptCount + 1)
        return if (inventoryRepository.uploadSweep(sweepId)) {
            Result.success()
        } else {
            Timber.w("Sweep %d upload failed — will retry", sweepId)
            Result.retry()
        }
    }

    companion object {
        const val KEY_SWEEP_ID = "sweep_id"
        fun uniqueName(sweepId: Long) = "upload_sweep_$sweepId"
    }
}
