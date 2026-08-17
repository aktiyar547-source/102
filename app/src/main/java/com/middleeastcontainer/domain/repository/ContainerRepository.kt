package com.middleeastcontainer.domain.repository

import com.middleeastcontainer.domain.model.Container
import kotlinx.coroutines.flow.Flow

interface ContainerRepository {
    fun observeAll(): Flow<List<Container>>
    suspend fun get(name: String): Container?

    /** Creates the container + empty side/remark/tag rows (legacy insertCData). */
    suspend fun create(name: String, type: String)

    suspend fun updateType(name: String, type: String)
    suspend fun delete(name: String)

    /** Q7 housekeeping: purge uploaded inspections older than [cutoffDate] (yyyy-MM-dd). */
    suspend fun purgeUploadedBefore(cutoffDate: String)
}
