package com.middleeastcontainer.data.repository

import com.middleeastcontainer.core.common.Clock
import com.middleeastcontainer.core.common.Constants
import com.middleeastcontainer.core.common.DateFormats
import com.middleeastcontainer.core.common.DispatcherProvider
import com.middleeastcontainer.data.database.dao.ContainerDao
import com.middleeastcontainer.data.database.dao.ExtraImageDao
import com.middleeastcontainer.data.database.entity.EImagesEntity
import com.middleeastcontainer.domain.model.ExtraImage
import com.middleeastcontainer.domain.repository.ExtraImageRepository
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ExtraImageRepositoryImpl @Inject constructor(
    private val extraImageDao: ExtraImageDao,
    private val containerDao: ContainerDao,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
) : ExtraImageRepository {

    override suspend fun forContainer(containerName: String): List<ExtraImage> =
        withContext(dispatchers.io) {
            extraImageDao.forContainer(containerName).map {
                ExtraImage(it.Id, it.Name, it.Image, it.Remarks, it.Time, it.Status, it.Type)
            }
        }

    /** Legacy addextraimages: insert EImages(Status='Upload') + set Container.Status='Upload'. */
    override suspend fun add(
        containerName: String,
        imageRelativePath: String,
        remark: String,
        category: String,
    ): Unit = withContext(dispatchers.io) {
        val now = clock.now()
        extraImageDao.insert(
            EImagesEntity(
                Name = containerName,
                Image = imageRelativePath,
                Remarks = remark,
                Time = DateFormats.timestamp(now),
                Status = Constants.STATUS_PENDING,
                Type = category,
                CreatedDate = DateFormats.createdDate(now),
            )
        )
        containerDao.updateStatus(containerName, Constants.STATUS_PENDING)
    }

    override suspend fun pendingCount(containerName: String): Int =
        withContext(dispatchers.io) { extraImageDao.pendingCount(containerName) }
}
