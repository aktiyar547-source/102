package com.middleeastcontainer.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.middleeastcontainer.data.database.entity.CImagesEntity
import com.middleeastcontainer.data.database.entity.RemarksEntity
import com.middleeastcontainer.data.database.entity.TagEntity

/**
 * The three parallel side tables. Side columns are updated by name via a
 * parameterized whitelist in the repository (never by concatenating the side
 * into SQL), so the legacy dynamic-column update is reproduced safely (L6/L7).
 */
@Dao
interface SideTablesDao {

    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCImages(e: CImagesEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertRemarks(e: RemarksEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTag(e: TagEntity): Long

    @Query("SELECT * FROM CImages WHERE Name = :name LIMIT 1")
    suspend fun cImages(name: String): CImagesEntity?

    @Query("SELECT * FROM Remarks WHERE Name = :name LIMIT 1")
    suspend fun remarks(name: String): RemarksEntity?

    @Query("SELECT * FROM Tag WHERE Name = :name LIMIT 1")
    suspend fun tags(name: String): TagEntity?

    @androidx.room.Update suspend fun updateCImages(e: CImagesEntity)
    @androidx.room.Update suspend fun updateRemarks(e: RemarksEntity)
    @androidx.room.Update suspend fun updateTag(e: TagEntity)

    @Query("DELETE FROM CImages WHERE Name = :name") suspend fun deleteCImages(name: String)
    @Query("DELETE FROM Remarks WHERE Name = :name") suspend fun deleteRemarks(name: String)
    @Query("DELETE FROM Tag WHERE Name = :name") suspend fun deleteTag(name: String)
}
