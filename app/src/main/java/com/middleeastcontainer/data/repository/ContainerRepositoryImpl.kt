package com.middleeastcontainer.data.repository

import com.middleeastcontainer.core.common.Clock
import com.middleeastcontainer.core.common.Constants
import com.middleeastcontainer.core.common.DateFormats
import com.middleeastcontainer.core.common.DispatcherProvider
import com.middleeastcontainer.data.database.dao.ContainerDao
import com.middleeastcontainer.data.database.dao.ExtraImageDao
import com.middleeastcontainer.data.database.dao.SideTablesDao
import com.middleeastcontainer.data.database.entity.CImagesEntity
import com.middleeastcontainer.data.database.entity.ContainerEntity
import com.middleeastcontainer.data.database.entity.RemarksEntity
import com.middleeastcontainer.data.database.entity.TagEntity
import com.middleeastcontainer.data.storage.FolderPathBuilder
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.domain.model.Container
import com.middleeastcontainer.domain.repository.ContainerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ContainerRepositoryImpl @Inject constructor(
    private val containerDao: ContainerDao,
    private val sideTablesDao: SideTablesDao,
    private val extraImageDao: ExtraImageDao,
    private val fileStore: ImageFileStore,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
) : ContainerRepository {

    override fun observeAll(): Flow<List<Container>> =
        containerDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun get(name: String): Container? = withContext(dispatchers.io) {
        containerDao.findByName(name)?.toDomain()
    }

    /** Legacy insertCData: Container + empty CImages/Remarks/Tag rows, Status1='Upload'. */
    override suspend fun create(name: String, type: String): Unit = withContext(dispatchers.io) {
        val now = clock.now()
        val created = DateFormats.createdDate(now)
        containerDao.insert(
            ContainerEntity(
                Name = name,
                Type = type,
                Date = DateFormats.displayDate(now),
                Status = Constants.STATUS_PENDING,
                Username = null,
                IMEInum = null,
                Status1 = Constants.STATUS_PENDING,
                CreatedDate = created,
            )
        )
        sideTablesDao.insertCImages(CImagesEntity(Name = name, CreatedDate = created))
        sideTablesDao.insertRemarks(RemarksEntity(Name = name, CreatedDate = created))
        sideTablesDao.insertTag(TagEntity(Name = name, CreatedDate = created))
        Unit
    }

    override suspend fun updateType(name: String, type: String): Unit = withContext(dispatchers.io) {
        containerDao.updateType(name, type)
    }

    override suspend fun delete(name: String): Unit = withContext(dispatchers.io) {
        // Remove DB rows (parity with legacy DeleteContainer) ...
        extraImageDao.deleteDoneForContainer(name)
        containerDao.deleteByName(name)
        sideTablesDao.deleteCImages(name)
        sideTablesDao.deleteRemarks(name)
        sideTablesDao.deleteTag(name)
        // ... and the on-disk capture folder for today's date path (parity with DeleteImages).
        fileStore.deleteGroupDir(Constants.INSPECTION_DIR, name)
    }

    override suspend fun purgeUploadedBefore(cutoffDate: String): Unit = withContext(dispatchers.io) {
        val names = containerDao.namesUploadedBefore(cutoffDate)
        names.forEach { name ->
            sideTablesDao.deleteCImages(name)
            sideTablesDao.deleteRemarks(name)
            sideTablesDao.deleteTag(name)
            extraImageDao.deleteDoneForContainer(name)
        }
        containerDao.purgeUploadedBefore(cutoffDate)
    }

    private suspend fun ContainerEntity.toDomain(): Container = Container(
        name = Name,
        type = Type,
        date = Date,
        status = Status,
        username = Username,
        deviceId = IMEInum,
        uploadStatus = Status1,
        createdDate = CreatedDate,
        extraImageCount = extraImageDao.forContainer(Name).size,
    )
}
