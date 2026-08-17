package com.middleeastcontainer.data.repository

import com.middleeastcontainer.core.common.AppConfig
import com.middleeastcontainer.core.common.Constants
import com.middleeastcontainer.core.common.DispatcherProvider
import com.middleeastcontainer.data.database.SideColumnMapper
import com.middleeastcontainer.data.database.dao.ContainerDao
import com.middleeastcontainer.data.database.dao.ExtraImageDao
import com.middleeastcontainer.data.database.dao.SideTablesDao
import com.middleeastcontainer.data.network.MecrcApi
import com.middleeastcontainer.data.network.mapper.ContainerPayloadBuilder
import com.middleeastcontainer.data.network.mapper.ImageEncoder
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.domain.model.Side
import com.middleeastcontainer.domain.repository.SessionRepository
import com.middleeastcontainer.domain.repository.UploadRepository
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Performs the actual network uploads, reproducing the frozen legacy payloads.
 * Absolute image paths are resolved from the stored relative paths only at encode
 * time (L7). All failures are swallowed into a boolean so workers can retry.
 */
class UploadRepositoryImpl @Inject constructor(
    private val api: MecrcApi,
    private val containerDao: ContainerDao,
    private val sideTablesDao: SideTablesDao,
    private val extraImageDao: ExtraImageDao,
    private val fileStore: ImageFileStore,
    private val encoder: ImageEncoder,
    private val session: SessionRepository,
    private val config: AppConfig,
    private val dispatchers: DispatcherProvider,
) : UploadRepository {

    override suspend fun uploadContainer(containerName: String): Boolean =
        withContext(dispatchers.io) {
            runCatching {
                val container = containerDao.findByName(containerName) ?: return@runCatching false
                val images = sideTablesDao.cImages(containerName)
                val remarks = sideTablesDao.remarks(containerName)

                val sidePaths = Side.entries.associateWith { side ->
                    SideColumnMapper.imageOf(images, side)?.let { rel ->
                        fileStore.absoluteFor(rel).path
                    }
                }
                val sideRemarks = Side.entries.associateWith { side ->
                    SideColumnMapper.remarkOf(remarks, side)
                }

                val fields = ContainerPayloadBuilder.build(
                    ContainerPayloadBuilder.Input(
                        deviceId = container.IMEInum ?: session.deviceId(),
                        containerName = container.Name,
                        userName = container.Username ?: session.currentSession().username.orEmpty(),
                        containerType = container.Type,
                        sideImagePaths = sidePaths,
                        sideRemarks = sideRemarks,
                        includeUnderFloor = config.includeUnderFloorInTestPayload,
                    ),
                    encodeImage = { encoder.forTestPayload(it) },
                )

                // Diagnose payload size before sending - PHP's default post_max_size
                // is 8 MB and an oversized POST fails with HTTP 413 or is truncated.
                val payloadBytes = fields.entries.sumOf { it.key.length + it.value.length }.toLong()
                val payloadMb = payloadBytes / 1_048_576.0
                Timber.i(
                    "Uploading %s: %d fields, ~%.1f MB payload",
                    containerName, fields.size, payloadMb,
                )
                if (payloadMb > 8) {
                    Timber.w(
                        "Payload for %s is ~%.1f MB - many servers reject over 8 MB. " +
                            "Lower UPLOAD_IMAGE_MAX_EDGE in app/build.gradle.kts.",
                        containerName, payloadMb,
                    )
                }

                val response = api.uploadContainer(fields)
                if (!response.isSuccessful) {
                    Timber.w(
                        "Upload of %s rejected: HTTP %d %s",
                        containerName, response.code(), response.message(),
                    )
                }
                response.isSuccessful
            }.getOrElse { e ->
                Timber.w(e, "uploadContainer failed for %s", containerName)
                false
            }
        }

    override suspend fun uploadExtraImages(containerName: String): Boolean =
        withContext(dispatchers.io) {
            runCatching {
                val pending = extraImageDao.forContainerWithStatus(containerName, Constants.STATUS_PENDING)
                if (pending.isEmpty()) return@runCatching true
                val deviceId = session.deviceId()
                val userName = session.currentSession().username.orEmpty()
                var allOk = true
                for (extra in pending) {
                    val absPath = extra.Image?.let { fileStore.absoluteFor(it).path }.orEmpty()
                    val response = api.uploadExtraImage(
                        imeiNum = deviceId,
                        containerNo = extra.Name,
                        userName = userName,
                        pictureTime = extra.Time.orEmpty(),
                        eRemarks = extra.Remarks.orEmpty(),
                        type = extra.Type.orEmpty(),
                        extraImageBase64 = if (absPath.isEmpty()) "" else encoder.forExtraImage(absPath),
                    )
                    if (response.isSuccessful) {
                        extra.Time?.let { extraImageDao.markStatus(extra.Name, Constants.STATUS_DONE, it) }
                    } else {
                        Timber.w(
                            "Extra image for %s rejected: HTTP %d",
                            extra.Name, response.code(),
                        )
                        allOk = false
                    }
                }
                allOk
            }.getOrElse { e ->
                Timber.w(e, "uploadExtraImages failed for %s", containerName)
                false
            }
        }

    override suspend fun markContainerDone(containerName: String): Unit =
        withContext(dispatchers.io) { containerDao.markDone(containerName) }
}
