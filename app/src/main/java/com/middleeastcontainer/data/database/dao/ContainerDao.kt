package com.middleeastcontainer.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.middleeastcontainer.data.database.entity.ContainerEntity
import kotlinx.coroutines.flow.Flow

/** All access is parameterized — eliminates the legacy SQL-injection surface (L6). */
@Dao
interface ContainerDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ContainerEntity): Long

    @Query("SELECT * FROM Container ORDER BY Id")
    fun observeAll(): Flow<List<ContainerEntity>>

    @Query("SELECT * FROM Container WHERE Name = :name LIMIT 1")
    suspend fun findByName(name: String): ContainerEntity?

    @Query("SELECT * FROM Container WHERE Status1 = :status")
    suspend fun byUploadStatus(status: String): List<ContainerEntity>

    @Query("UPDATE Container SET Type = :type WHERE Name = :name")
    suspend fun updateType(name: String, type: String)

    @Query("UPDATE Container SET Status = :status WHERE Name = :name")
    suspend fun updateStatus(name: String, status: String)

    @Query("UPDATE Container SET Status = 'Done', Status1 = 'Done' WHERE Name = :name")
    suspend fun markDone(name: String)

    @Query("DELETE FROM Container WHERE Name = :name")
    suspend fun deleteByName(name: String)

    /** Q7 housekeeping: only uploaded inspections strictly older than the cutoff. */
    @Query("DELETE FROM Container WHERE Status1 = 'Done' AND CreatedDate < :cutoff")
    suspend fun purgeUploadedBefore(cutoff: String)

    @Query("SELECT Name FROM Container WHERE Status1 = 'Done' AND CreatedDate < :cutoff")
    suspend fun namesUploadedBefore(cutoff: String): List<String>
}
