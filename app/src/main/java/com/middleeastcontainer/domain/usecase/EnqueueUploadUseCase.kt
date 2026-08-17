package com.middleeastcontainer.domain.usecase

import com.middleeastcontainer.data.sync.UploadScheduler
import javax.inject.Inject

/** Enqueues durable uploads for the selected containers (mirrors the Upload screen action). */
class EnqueueUploadUseCase @Inject constructor(
    private val scheduler: UploadScheduler,
) {
    operator fun invoke(containerNames: List<String>) {
        containerNames.forEach { scheduler.enqueueContainerUpload(it) }
    }
}
