package com.middleeastcontainer.domain.model

/**
 * A yard count.
 *
 * Inventory inverts the inspection model. An inspection is one container and many
 * photos; a sweep is one photo and many containers, because units stand in stacks
 * and a single frame can carry ten numbers.
 */
data class Sweep(
    val id: Long = 0,
    val zone: String,
    val startedBy: String,
    val startedAt: String,
    /** Null while the sweep is still running. */
    val finishedAt: String? = null,
    val unitCount: Int = 0,
    val photoCount: Int = 0,
)

/**
 * One container seen during a sweep.
 *
 * Kept distinct from the photo that produced it: the same unit is often visible in
 * two frames from different angles, and the count must not double.
 */
data class Sighting(
    val id: Long = 0,
    val sweepId: Long,
    val containerNumber: String,
    /** Relative path of the photo it was read from — the evidence for this row. */
    val photoPath: String?,
    val seenAt: String,
    /** True when OCR read it; false when an inspector typed or corrected it. */
    val fromOcr: Boolean,
)

/**
 * A container seen but not read.
 *
 * Carries its own short [tag] and the box it occupied, so the inspector can be
 * shown exactly which unit on the stack still needs a closer look.
 */
data class UnreadUnit(
    val id: Long = 0,
    val sweepId: Long,
    val tag: String,
    val partial: String,
    val photoPath: String?,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val seenAt: String,
)

/** Progress shown while walking, so a bad sweep is obvious before it ends. */
data class SweepProgress(
    val sweep: Sweep,
    val units: List<String>,
    val photos: Int,
) {
    val unitCount: Int get() = units.size
}


