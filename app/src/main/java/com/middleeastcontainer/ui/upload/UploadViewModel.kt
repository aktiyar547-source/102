package com.middleeastcontainer.ui.upload

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.middleeastcontainer.data.sync.UploadContainerWorker
import com.middleeastcontainer.data.sync.UploadScheduler
import com.middleeastcontainer.domain.model.Container
import com.middleeastcontainer.domain.repository.ContainerRepository
import com.middleeastcontainer.domain.usecase.EnqueueUploadUseCase
import com.middleeastcontainer.ui.components.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Per-container upload status shown next to each row. */
enum class UploadStatus { NONE, QUEUED, RUNNING, DONE, FAILED }

data class UploadRow(
    val container: Container,
    val status: UploadStatus,
    val step: String?,
)

data class UploadSummary(
    val rows: List<UploadRow>,
    val queued: Int,
    val running: Int,
    val done: Int,
    val failed: Int,
) {
    val activeTotal: Int get() = queued + running
}

@HiltViewModel
class UploadViewModel @Inject constructor(
    repository: ContainerRepository,
    private val scheduler: UploadScheduler,
    private val enqueueUpload: EnqueueUploadUseCase,
) : ViewModel() {

    val state = combine(
        repository.observeAll(),
        scheduler.observeUploads(),
    ) { containers, workInfos ->
        val byContainer = workInfos.mapNotNull { info ->
            UploadScheduler.containerOf(info)?.let { it to info }
        }.toMap()

        val rows = containers.map { container ->
            val info = byContainer[container.name]
            UploadRow(
                container = container,
                status = info.toStatus(),
                step = info?.progress?.getString(UploadContainerWorker.KEY_STEP),
            )
        }

        UiState.Content(
            UploadSummary(
                rows = rows,
                queued = rows.count { it.status == UploadStatus.QUEUED },
                running = rows.count { it.status == UploadStatus.RUNNING },
                done = rows.count { it.status == UploadStatus.DONE },
                failed = rows.count { it.status == UploadStatus.FAILED },
            )
        ) as UiState<UploadSummary>
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    /** Queues durable uploads; they run one at a time to protect memory. */
    fun upload(names: Set<String>) = enqueueUpload(names.toList())

    private fun WorkInfo?.toStatus(): UploadStatus = when (this?.state) {
        null -> UploadStatus.NONE
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> UploadStatus.QUEUED
        WorkInfo.State.RUNNING -> UploadStatus.RUNNING
        WorkInfo.State.SUCCEEDED -> UploadStatus.DONE
        WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> UploadStatus.FAILED
    }
}
