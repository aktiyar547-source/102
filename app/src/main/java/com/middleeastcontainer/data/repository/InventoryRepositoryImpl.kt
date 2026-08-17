package com.middleeastcontainer.data.repository

import com.middleeastcontainer.core.common.Clock
import com.middleeastcontainer.core.common.DateFormats
import com.middleeastcontainer.core.common.DispatcherProvider
import com.middleeastcontainer.core.common.AppConfig
import com.middleeastcontainer.data.database.dao.InventoryDao
import com.middleeastcontainer.data.network.MecrcApi
import com.middleeastcontainer.data.network.dto.SweepPayload
import com.middleeastcontainer.data.network.dto.SweepUnit
import com.middleeastcontainer.data.network.mapper.ImageEncoder
import com.middleeastcontainer.data.storage.ImageFileStore
import com.middleeastcontainer.data.database.entity.SightingEntity
import com.middleeastcontainer.data.database.entity.SweepEntity
import com.middleeastcontainer.data.database.entity.UnreadEntity
import com.middleeastcontainer.domain.model.Sighting
import com.middleeastcontainer.domain.model.Sweep
import com.middleeastcontainer.domain.model.UnreadUnit
import com.middleeastcontainer.domain.repository.InventoryRepository
import com.middleeastcontainer.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

class InventoryRepositoryImpl @Inject constructor(
    private val dao: InventoryDao,
    private val api: MecrcApi,
    private val imageEncoder: ImageEncoder,
    private val fileStore: ImageFileStore,
    private val session: SessionRepository,
    private val clock: Clock,
    private val dispatchers: DispatcherProvider,
) : InventoryRepository {

    override suspend fun startSweep(zone: String): Long = withContext(dispatchers.io) {
        val id = dao.insertSweep(
            SweepEntity(
                Zone = zone.trim().ifBlank { "Yard" },
                StartedBy = session.currentSession().username.orEmpty(),
                StartedAt = DateFormats.timestamp(clock.now()),
            )
        )
        Timber.i("Sweep %d started in zone '%s'", id, zone)
        id
    }

    override suspend fun sweep(id: Long): Sweep? = withContext(dispatchers.io) {
        dao.sweep(id)?.toDomain(
            units = dao.countSightings(id),
            photos = dao.countPhotos(id),
        )
    }

    override fun observeSweeps(): Flow<List<Sweep>> =
        dao.observeSweeps().map { list -> list.map { it.toDomain() } }

    override fun observeSightings(sweepId: Long): Flow<List<Sighting>> =
        dao.observeSightings(sweepId).map { list -> list.map { it.toDomain() } }

    override suspend fun sightingsOnce(sweepId: Long): List<Sighting> =
        withContext(dispatchers.io) { dao.sightings(sweepId).map { it.toDomain() } }

    override suspend fun unreadOnce(sweepId: Long): List<UnreadUnit> =
        withContext(dispatchers.io) { dao.unread(sweepId).map { it.toDomain() } }

    override suspend fun addSightings(
        sweepId: Long,
        numbers: List<String>,
        photoRelativePath: String?,
        fromOcr: Boolean,
    ): Int = withContext(dispatchers.io) {
        val seenAt = DateFormats.timestamp(clock.now())
        var added = 0
        for (number in numbers.map { it.trim().uppercase() }.filter { it.isNotBlank() }) {
            // The unique index does the deduplication; IGNORE returns -1 when the
            // unit was already counted in this sweep.
            val rowId = dao.insertSighting(
                SightingEntity(
                    SweepId = sweepId,
                    ContainerNumber = number,
                    PhotoPath = photoRelativePath,
                    SeenAt = seenAt,
                    FromOcr = fromOcr,
                )
            )
            if (rowId != -1L) added++
        }
        // A second angle on the same stack often reads what the first could not,
        // so a new number quietly closes an outstanding gap.
        if (added > 0) {
            repeat(added.coerceAtMost(dao.countUnresolved(sweepId))) {
                dao.resolveOldestPending(sweepId, RESOLVED_BY_LATER_FRAME)
            }
        }
        Timber.d("Sweep %d: %d of %d numbers were new", sweepId, added, numbers.size)
        added
    }



    /**
     * Identifying a gap must also count it — otherwise the unit is marked
     * resolved while the sweep total still says it was never there.
     */


    override fun observeUnread(sweepId: Long): Flow<List<UnreadUnit>> =
        dao.observeUnread(sweepId).map { list -> list.map { it.toDomain() } }

    override suspend fun addUnread(
        sweepId: Long,
        regions: List<Pair<String, FloatArray>>,
        photoRelativePath: String?,
    ): List<String> = withContext(dispatchers.io) {
        val seenAt = DateFormats.timestamp(clock.now())
        // Tags continue across the whole sweep, so A1 always means the same unit
        // however many frames have been taken since.
        var next = dao.unreadIssued(sweepId)
        val issued = mutableListOf<String>()
        for ((partial, box) in regions) {
            val tag = tagFor(next++)
            dao.insertUnread(
                UnreadEntity(
                    SweepId = sweepId,
                    Tag = tag,
                    Partial = partial,
                    PhotoPath = photoRelativePath,
                    BoxLeft = box[0], BoxTop = box[1],
                    BoxRight = box[2], BoxBottom = box[3],
                    SeenAt = seenAt,
                )
            )
            issued += tag
        }
        if (issued.isNotEmpty()) {
            Timber.i("Sweep %d: %d unit(s) need a closer look: %s",
                sweepId, issued.size, issued.joinToString(","))
        }
        issued
    }

    override suspend fun resolveUnread(id: Long, number: String): Unit =
        withContext(dispatchers.io) { dao.resolveUnread(id, number.trim().uppercase()) }

    override suspend fun dismissUnread(id: Long): Unit =
        withContext(dispatchers.io) { dao.deleteUnread(id) }

    /** A1, A2 … A9, B1 … short enough to read off a screen in daylight. */
    private fun tagFor(index: Int): String =
        "${'A' + (index / 9) % 26}${index % 9 + 1}"

    override suspend fun correctSighting(id: Long, number: String): Unit =
        withContext(dispatchers.io) {
            dao.correctSighting(id, number.trim().uppercase())
        }

    override suspend fun removeSighting(id: Long): Unit = withContext(dispatchers.io) {
        dao.deleteSighting(id)
    }

    override suspend fun finishSweep(sweepId: Long): Unit = withContext(dispatchers.io) {
        dao.finishSweep(sweepId, DateFormats.timestamp(clock.now()))
        Timber.i("Sweep %d finished with %d units", sweepId, dao.countSightings(sweepId))
    }

    override suspend fun deleteSweep(sweepId: Long): Unit = withContext(dispatchers.io) {
        // Collect the paths first: deleting the sweep cascades the sightings
        // away, and with them any record of which files to remove.
        val paths = dao.sightings(sweepId).mapNotNull { it.PhotoPath }.distinct()
        dao.deleteSweep(sweepId)
        fileStore.deleteAll(paths)
        Timber.i("Sweep %d deleted with %d photo(s)", sweepId, paths.size)
    }

    override suspend fun uploadSweep(sweepId: Long): Boolean = withContext(dispatchers.io) {
        val sweep = dao.sweep(sweepId) ?: return@withContext false
        val units = dao.sightings(sweepId)
        if (units.isEmpty()) {
            // An empty sweep is still a fact worth recording — the zone was walked.
            Timber.i("Sweep %d has no units; uploading the sweep record only", sweepId)
        }

        // Encode each distinct frame once. A photo of a stack backs several
        // units, so both the encoding work and the bytes on the wire would
        // otherwise be repeated per container.
        val distinctPhotos = units.mapNotNull { it.PhotoPath }.distinct()
        val keyFor = distinctPhotos.withIndex().associate { (i, path) -> path to "p$i" }
        val photos = distinctPhotos.associate { path ->
            keyFor.getValue(path) to imageEncoder.forExtraImage(
                fileStore.absoluteFor(path).path
            )
        }

        val payload = SweepPayload(
            deviceId = session.deviceId(),
            zone = sweep.Zone,
            userName = sweep.StartedBy,
            startedAt = sweep.StartedAt,
            finishedAt = sweep.FinishedAt,
            photos = photos,
            units = units.map { s ->
                SweepUnit(
                    containerNo = s.ContainerNumber,
                    seenAt = s.SeenAt,
                    fromOcr = s.FromOcr,
                    // One frame often carries several units, so the same photo is
                    // encoded per unit. The server writes one file each, which
                    // keeps every row independently reviewable.
                    photoRef = s.PhotoPath?.let { keyFor[it] },
                )
            },
        )

        val response = runCatching { api.uploadSweep(payload) }.getOrNull()
        val ok = response?.isSuccessful == true
        if (ok) {
            dao.markSightingsDone(sweepId)
            dao.setSweepStatus(sweepId, "Done")
            Timber.i(
                "Sweep %d uploaded: %d units from %d photo(s)",
                sweepId, units.size, photos.size,
            )
        } else {
            Timber.w("Sweep %d upload failed: HTTP %s", sweepId, response?.code())
        }
        ok
    }


    private fun SweepEntity.toDomain(units: Int = 0, photos: Int = 0) = Sweep(
        id = Id,
        zone = Zone,
        startedBy = StartedBy,
        startedAt = StartedAt,
        finishedAt = FinishedAt,
        unitCount = units,
        photoCount = photos,
    )

    private companion object {
        /** A frame realistically holds a handful of units, not fifty. */
        const val MAX_PENDING_PER_FRAME = 20

        /** Marker for a gap closed by a later frame rather than by hand. */
        const val RESOLVED_BY_LATER_FRAME = "read in a later photo"
    }

    private fun UnreadEntity.toDomain() = UnreadUnit(
        id = Id,
        sweepId = SweepId,
        tag = Tag,
        partial = Partial,
        photoPath = PhotoPath,
        left = BoxLeft, top = BoxTop, right = BoxRight, bottom = BoxBottom,
        seenAt = SeenAt,
    )

    private fun SightingEntity.toDomain() = Sighting(
        id = Id,
        sweepId = SweepId,
        containerNumber = ContainerNumber,
        photoPath = PhotoPath,
        seenAt = SeenAt,
        fromOcr = FromOcr,
    )
}
