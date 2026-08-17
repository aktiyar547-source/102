package com.middleeastcontainer.domain.repository

import com.middleeastcontainer.domain.model.Sighting
import com.middleeastcontainer.domain.model.Sweep
import com.middleeastcontainer.domain.model.UnreadUnit
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {

    suspend fun startSweep(zone: String): Long

    suspend fun sweep(id: Long): Sweep?

    fun observeSweeps(): Flow<List<Sweep>>

    fun observeSightings(sweepId: Long): Flow<List<Sighting>>

    /** One-shot read, for exporting a finished sweep. */
    suspend fun sightingsOnce(sweepId: Long): List<Sighting>

    /** One-shot read of outstanding gaps, for the export. */
    suspend fun unreadOnce(sweepId: Long): List<UnreadUnit>

    /**
     * Records numbers read from one photo.
     *
     * @return how many were new. Re-seeing a unit is normal — the same container
     *   appears in overlapping frames — so duplicates are ignored rather than
     *   treated as errors, and the count reflects only what was actually added.
     */
    suspend fun addSightings(
        sweepId: Long,
        numbers: List<String>,
        photoRelativePath: String?,
        fromOcr: Boolean,
    ): Int

    /** Containers seen but not read, still awaiting a closer photo. */
    fun observeUnread(sweepId: Long): Flow<List<UnreadUnit>>

    /**
     * Records regions the camera could not read.
     *
     * @return the tags issued, so they can be shown against the frame.
     */
    suspend fun addUnread(
        sweepId: Long,
        regions: List<Pair<String, FloatArray>>,
        photoRelativePath: String?,
    ): List<String>

    /** Marks an unread unit resolved once its number has been captured. */
    suspend fun resolveUnread(id: Long, number: String)

    /** Drops an unread record — the unit was a duplicate or is not there. */
    suspend fun dismissUnread(id: Long)

    suspend fun correctSighting(id: Long, number: String)

    // ------------------------------------------------------- pending units --

    
    /** Identifies a pending unit, which also counts it. */

    /** Drops a pending unit — a miscount, or the container has gone. */

    suspend fun removeSighting(id: Long)

    suspend fun finishSweep(sweepId: Long)

    suspend fun deleteSweep(sweepId: Long)

    /** Sends a finished sweep. @return true when the server accepted it. */
    suspend fun uploadSweep(sweepId: Long): Boolean
}
