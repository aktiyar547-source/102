package com.middleeastcontainer.domain.ocr

/**
 * A box in the frame, as fractions of the image (0..1).
 *
 * Fractions rather than pixels because OCR decodes at whatever sample size fits
 * memory and the frame is resized before storage — pixel coordinates would be
 * meaningless by the time anything drew them.
 */
data class FrameBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    val isValid: Boolean get() = right > left && bottom > top
    val centreY: Float get() = (top + bottom) / 2f
    val centreX: Float get() = (left + right) / 2f
}

/** A container number read and confirmed by its ISO 6346 check digit. */
data class DetectedNumber(
    val number: String,
    val box: FrameBox,
)

/**
 * Text shaped like a container number that could not be confirmed.
 *
 * Rust, glare, a partly obscured stencil or a unit half out of frame all produce
 * this. It matters because it is the difference between "there are six containers
 * here" and "there are ten here and I could only read six" — the second is what
 * sends someone back to look.
 *
 * [partial] is whatever was legible, which is often enough to find the unit: a
 * prefix narrows it to one operator.
 */
data class UnreadRegion(
    val partial: String,
    val box: FrameBox,
)

/** Everything one frame yielded. */
data class FrameReading(
    val confirmed: List<DetectedNumber>,
    val unread: List<UnreadRegion>,
) {
    val total: Int get() = confirmed.size + unread.size
}

interface ContainerOcrEngine {

    /** Best single candidate, or null. Used when scanning one container. */
    suspend fun recognizeContainerNumber(absolutePath: String): String?

    /**
     * Everything in the frame: numbers confirmed by check digit, and regions
     * that look like a container number but could not be confirmed.
     */
    suspend fun readFrame(absolutePath: String): FrameReading
}
