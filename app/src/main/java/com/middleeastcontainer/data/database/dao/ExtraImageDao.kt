package com.middleeastcontainer.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.middleeastcontainer.data.database.entity.EImagesEntity

@Dao
interface ExtraImageDao {

    @Insert suspend fun insert(e: EImagesEntity): Long

    @Query("SELECT * FROM EImages WHERE Name = :name")
    suspend fun forContainer(name: String): List<EImagesEntity>

    @Query("SELECT * FROM EImages WHERE Name = :name AND Status = :status")
    suspend fun forContainerWithStatus(name: String, status: String): List<EImagesEntity>

    @Query("SELECT COUNT(*) FROM EImages WHERE Name = :name AND Status = 'Upload'")
    suspend fun pendingCount(name: String): Int

    @Query("UPDATE EImages SET Status = :status WHERE Name = :name AND Time = :time")
    suspend fun markStatus(name: String, status: String, time: String)

    @Query("DELETE FROM EImages WHERE Name = :name AND Status = 'Done'")
    suspend fun deleteDoneForContainer(name: String)
}
