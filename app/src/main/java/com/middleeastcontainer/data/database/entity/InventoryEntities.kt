package com.middleeastcontainer.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "Sweep")
data class SweepEntity(
    @PrimaryKey(autoGenerate = true) val Id: Long = 0,
    val Zone: String,
    val StartedBy: String,
    val StartedAt: String,
    val FinishedAt: String? = null,
    /** Upload lifecycle, matching the inspection tables: Upload -> Done. */
    val Status: String = "Upload",
)

/**
 * One container seen in one sweep.
 *
 * The unique index on (SweepId, ContainerNumber) is the deduplication: photographing
 * the same unit from two angles inserts once, so the running count stays honest
 * without the caller having to check first.
 */
/**
 * A container the camera saw but could not read.
 *
 * Recorded rather than discarded so a sweep can say "ten here, four unread"
 * instead of silently reporting six. [Tag] is a short label shown on the photo,
 * so an inspector can match the box on screen to the unit in front of them.
 */
@Entity(
    tableName = "Unread",
    foreignKeys = [
        ForeignKey(
            entity = SweepEntity::class,
            parentColumns = ["Id"],
            childColumns = ["SweepId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = ["SweepId"])],
)
data class UnreadEntity(
    @PrimaryKey(autoGenerate = true) val Id: Long = 0,
    val SweepId: Long,
    /** Short human label, e.g. "A3" — matches the box drawn on the photo. */
    val Tag: String,
    /** Whatever was legible, often enough to narrow the unit down. */
    val Partial: String,
    val PhotoPath: String?,
    /** Box within the frame, stored as fractions so it survives resizing. */
    val BoxLeft: Float,
    val BoxTop: Float,
    val BoxRight: Float,
    val BoxBottom: Float,
    val SeenAt: String,
    /** Set when the unit is later read successfully; the record then clears. */
    val ResolvedBy: String? = null,
)

@Entity(
    tableName = "Sighting",
    foreignKeys = [
        ForeignKey(
            entity = SweepEntity::class,
            parentColumns = ["Id"],
            childColumns = ["SweepId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["SweepId", "ContainerNumber"], unique = true),
        Index(value = ["SweepId"]),
    ],
)
data class SightingEntity(
    @PrimaryKey(autoGenerate = true) val Id: Long = 0,
    val SweepId: Long,
    val ContainerNumber: String,
    val PhotoPath: String?,
    val SeenAt: String,
    val FromOcr: Boolean = true,
    val Status: String = "Upload",
)


