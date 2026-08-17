package com.middleeastcontainer.data.repository

import com.middleeastcontainer.core.common.DispatcherProvider
import com.middleeastcontainer.data.database.SideColumnMapper
import com.middleeastcontainer.data.database.dao.SideTablesDao
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.domain.model.Side
import com.middleeastcontainer.domain.model.SideCapture
import com.middleeastcontainer.domain.repository.SideCaptureRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reproduces legacy updateimages: writes the image path into CImages.<side>, the
 * remark into Remarks.<side>, and Tag.<side>='Capture' — all via safe entity copies
 * (SideColumnMapper), never dynamic SQL (L6/L7).
 */
class SideCaptureRepositoryImpl @Inject constructor(
    private val sideTablesDao: SideTablesDao,
    private val fileStore: ImageFileStore,
    private val dispatchers: DispatcherProvider,
) : SideCaptureRepository {

    override suspend fun saveRemark(
        containerName: String,
        side: Side,
        remark: String,
    ): Unit = withContext(dispatchers.io) {
        sideTablesDao.remarks(containerName)?.let {
            sideTablesDao.updateRemarks(SideColumnMapper.withRemark(it, side, remark))
        }
        Unit
    }

    override suspend fun sidesFor(containerName: String): List<SideCapture> =
        withContext(dispatchers.io) {
            val images = sideTablesDao.cImages(containerName)
            val remarks = sideTablesDao.remarks(containerName)
            val tags = sideTablesDao.tags(containerName)
            Side.gridOrder.map { side ->
                val path = SideColumnMapper.imageOf(images, side)
                SideCapture(
                    side = side,
                    imagePath = path,
                    remark = SideColumnMapper.remarkOf(remarks, side),
                    captured = SideColumnMapper.tagOf(tags, side) == TAG_CAPTURED,
                )
            }
        }

    override suspend fun saveSide(
        containerName: String,
        side: Side,
        imageRelativePath: String,
        remark: String,
    ): Unit = withContext(dispatchers.io) {
        sideTablesDao.cImages(containerName)?.let {
            // Retaking a side writes a new timestamped file. Without removing the
            // previous one it lingers unreferenced until the whole container is
            // purged — and inspectors retake shots often.
            val previous = SideColumnMapper.imageOf(it, side)
            if (!previous.isNullOrBlank() && previous != imageRelativePath) {
                fileStore.deleteRelative(previous)
            }
            sideTablesDao.updateCImages(SideColumnMapper.withImage(it, side, imageRelativePath))
        }
        sideTablesDao.remarks(containerName)?.let {
            sideTablesDao.updateRemarks(SideColumnMapper.withRemark(it, side, remark))
        }
        sideTablesDao.tags(containerName)?.let {
            sideTablesDao.updateTag(SideColumnMapper.withTag(it, side, TAG_CAPTURED))
        }
        Unit
    }

    private companion object { const val TAG_CAPTURED = "Capture" }
}
