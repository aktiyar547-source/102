package com.middleeastcontainer.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.middleeastcontainer.domain.repository.UploadRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

/**
 * Durable upload of one container: main inspection data, then its pending extra
 * images, then mark Done. Any network failure -> Result.retry() (exponential
 * backoff), so work survives connectivity loss and process death (NFR-4).
 *
 * Publishes a human-readable step so the Upload screen can show live progress.
 */
@HiltWorker
class UploadContainerWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val uploadRepository: UploadRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val name = inputData.getString(KEY_CONTAINER) ?: return Result.failure()
        Timber.d("Uploading container %s (attempt %d)", name, runAttemptCount + 1)

        setProgress(workDataOf(KEY_STEP to STEP_PHOTOS))
        val dataOk = uploadRepository.uploadContainer(name)
        if (!dataOk) {
            Timber.w("Container data upload failed for %s - will retry", name)
            return Result.retry()
        }

        setProgress(workDataOf(KEY_STEP to STEP_EXTRAS))
        val extrasOk = uploadRepository.uploadExtraImages(name)
        if (!extrasOk) {
            Timber.w("Extra image upload failed for %s - will retry", name)
            return Result.retry()
        }

        uploadRepository.markContainerDone(name)
        Timber.i("Upload complete for %s", name)
        return Result.success()
    }

    companion object {
        const val KEY_CONTAINER = "container_name"
        const val KEY_STEP = "step"
        const val STEP_PHOTOS = "Uploading photos"
        const val STEP_EXTRAS = "Uploading extra images"

        fun uniqueName(container: String) = "upload_container_$container"
    }
}
