package com.middleeastcontainer.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Enqueues durable upload + housekeeping work, and exposes live progress. */
@Singleton
class UploadScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val workManager get() = WorkManager.getInstance(context)

    private val onlineConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * One durable upload per container; re-enqueueing the same container is
     * idempotent. Workers run one at a time (see MecrcApp's executor), so
     * selecting 20 containers queues them rather than flooding memory.
     */
    fun enqueueContainerUpload(containerName: String) {
        val request = OneTimeWorkRequestBuilder<UploadContainerWorker>()
            .setInputData(
                Data.Builder()
                    .putString(UploadContainerWorker.KEY_CONTAINER, containerName)
                    .build()
            )
            .setConstraints(onlineConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_UPLOAD)
            .addTag(containerTag(containerName))
            .build()

        workManager.enqueueUniqueWork(
            UploadContainerWorker.uniqueName(containerName),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Live state of every queued/running/finished upload. */
    fun observeUploads(): Flow<List<WorkInfo>> = workManager.getWorkInfosByTagFlow(TAG_UPLOAD)

    /** Queues a finished sweep. Runs behind any inspection uploads already waiting. */
    fun enqueueSweepUpload(sweepId: Long) {
        val request = OneTimeWorkRequestBuilder<UploadSweepWorker>()
            .setInputData(
                Data.Builder().putLong(UploadSweepWorker.KEY_SWEEP_ID, sweepId).build()
            )
            .setConstraints(onlineConstraint)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag(TAG_UPLOAD)
            .build()

        workManager.enqueueUniqueWork(
            UploadSweepWorker.uniqueName(sweepId),
            ExistingWorkPolicy.KEEP,
            request,
        )
    }

    /** Daily housekeeping purge (Q7). */
    fun ensureHousekeepingScheduled() {
        val request = PeriodicWorkRequestBuilder<HousekeepingWorker>(1, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            HousekeepingWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    companion object {
        const val TAG_UPLOAD = "mecrc_upload"
        private const val CONTAINER_TAG_PREFIX = "container:"

        fun containerTag(name: String) = CONTAINER_TAG_PREFIX + name

        /** Recovers the container name from a WorkInfo's tags. */
        fun containerOf(info: WorkInfo): String? =
            info.tags.firstOrNull { it.startsWith(CONTAINER_TAG_PREFIX) }
                ?.removePrefix(CONTAINER_TAG_PREFIX)
    }
}
