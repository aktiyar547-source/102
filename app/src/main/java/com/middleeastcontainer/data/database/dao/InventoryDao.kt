package com.middleeastcontainer.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.middleeastcontainer.data.database.entity.SightingEntity
import com.middleeastcontainer.data.database.entity.SweepEntity
import com.middleeastcontainer.data.database.entity.UnreadEntity
import kotlinx.coroutines.flow.Flow

/** All access is parameterized, as elsewhere in the app. */
@Dao
interface InventoryDao {

    // ------------------------------------------------------------- sweeps ----

    @Insert
    suspend fun insertSweep(sweep: SweepEntity): Long

    @Query("SELECT * FROM Sweep WHERE Id = :id")
    suspend fun sweep(id: Long): SweepEntity?

    @Query("SELECT * FROM Sweep ORDER BY Id DESC LIMIT 200")
    fun observeSweeps(): Flow<List<SweepEntity>>

    @Query("UPDATE Sweep SET FinishedAt = :finishedAt WHERE Id = :id")
    suspend fun finishSweep(id: Long, finishedAt: String)

    @Query("UPDATE Sweep SET Status = :status WHERE Id = :id")
    suspend fun setSweepStatus(id: Long, status: String)

    @Query("DELETE FROM Sweep WHERE Id = :id")
    suspend fun deleteSweep(id: Long)

    // ---------------------------------------------------------- sightings ----

    /**
     * IGNORE, not ABORT: re-seeing a unit is normal and expected, so a duplicate
     * is a no-op rather than an error the caller has to catch.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSighting(sighting: SightingEntity): Long

    @Query("SELECT * FROM Sighting WHERE SweepId = :sweepId ORDER BY Id")
    suspend fun sightings(sweepId: Long): List<SightingEntity>

    @Query("SELECT * FROM Sighting WHERE SweepId = :sweepId ORDER BY Id")
    fun observeSightings(sweepId: Long): Flow<List<SightingEntity>>

    @Query("SELECT COUNT(*) FROM Sighting WHERE SweepId = :sweepId")
    suspend fun countSightings(sweepId: Long): Int

    @Query(
        "SELECT COUNT(DISTINCT PhotoPath) FROM Sighting" +
            " WHERE SweepId = :sweepId AND PhotoPath IS NOT NULL"
    )
    suspend fun countPhotos(sweepId: Long): Int

    @Query("DELETE FROM Sighting WHERE Id = :id")
    suspend fun deleteSighting(id: Long)

    @Query("UPDATE Sighting SET ContainerNumber = :number, FromOcr = 0 WHERE Id = :id")
    suspend fun correctSighting(id: Long, number: String)

    @Query("UPDATE Sighting SET Status = 'Done' WHERE SweepId = :sweepId")
    suspend fun markSightingsDone(sweepId: Long)

    // ------------------------------------------------------------- unread ----

    @Insert
    suspend fun insertUnread(unread: UnreadEntity): Long

    /** Still needing attention, oldest first. */
    @Query("SELECT * FROM Unread WHERE SweepId = :sweepId AND ResolvedBy IS NULL ORDER BY Id")
    fun observeUnread(sweepId: Long): Flow<List<UnreadEntity>>

    @Query("SELECT * FROM Unread WHERE SweepId = :sweepId AND ResolvedBy IS NULL ORDER BY Id")
    suspend fun unread(sweepId: Long): List<UnreadEntity>

    @Query("SELECT COUNT(*) FROM Unread WHERE SweepId = :sweepId AND ResolvedBy IS NULL")
    suspend fun countUnread(sweepId: Long): Int

    @Query("UPDATE Unread SET ResolvedBy = :number WHERE Id = :id")
    suspend fun resolveUnread(id: Long, number: String)

    @Query("DELETE FROM Unread WHERE Id = :id")
    suspend fun deleteUnread(id: Long)

    /** How many tags have been issued in this sweep, so the next one continues. */
    @Query("SELECT COUNT(*) FROM Unread WHERE SweepId = :sweepId")
    suspend fun unreadIssued(sweepId: Long): Int
}
