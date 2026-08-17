package com.middleeastcontainer.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.middleeastcontainer.data.database.entity.ImagesEntity

@Dao
interface ImagesDao {
    @Insert suspend fun insert(e: ImagesEntity): Long

    @Query("SELECT * FROM Images WHERE C_Num = :name AND Status = 'Upload'")
    suspend fun pendingForContainer(name: String): List<ImagesEntity>

    @Query("UPDATE Images SET Status = :status WHERE C_Num = :name")
    suspend fun markStatus(name: String, status: String)
}
