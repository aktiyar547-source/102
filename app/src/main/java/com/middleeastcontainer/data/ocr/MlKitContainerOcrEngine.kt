package com.middleeastcontainer.data.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.middleeastcontainer.core.common.DispatcherProvider
import com.middleeastcontainer.domain.ocr.ContainerOcrEngine
import com.middleeastcontainer.domain.ocr.DetectedNumber
import com.middleeastcontainer.domain.ocr.FrameBox
import com.middleeastcontainer.domain.ocr.FrameReading
import com.middleeastcontainer.domain.ocr.UnreadRegion
import com.middleeastcontainer.domain.usecase.ValidateContainerNumberUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Container-number OCR built around one fact: the ISO 6346 check digit tells us
 * whether a candidate is real. That means the engine can afford to be generous —
 * assemble every plausible string it can, and let validation throw away the rest.
 * Tare weights, max-gross figures and CSC plates cannot pass a check digit.
 *
 * Three problems it solves that a plain `text` read does not:
 *
 * 1. **Vertical numbers.** On many doors the number runs top to bottom, so ML Kit
 *    returns each character as its own line and the flat text is unusable. Here
 *    the element bounding boxes are used to re-stack characters into a column.
 * 2. **Rotated photos.** The image is retried at 90 and 270 degrees, because a
 *    vertically-painted number is simply a horizontal one to a turned camera.
 * 3. **Letter/digit confusion.** Positions 0-3 must be letters and 4-10 digits,
 *    so an O read in a digit position can only have been a 0. Knowing the shape
 *    lets us repair the classic O/0, I/1, S/5, B/8, Z/2 misreads.
 */
class MlKitContainerOcrEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val validate: ValidateContainerNumberUseCase,
    private val dispatchers: DispatcherProvider,
) : ContainerOcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Scanning one container wants speed: the first valid number is the answer,
     * so the rotated passes can be abandoned the moment anything reads.
     */
    override suspend fun recognizeContainerNumber(absolutePath: String): String? =
        scan(absolutePath, enoughToStop = 1).firstOrNull()?.number

    /**
     * A sweep wants thoroughness — every unit in the frame — so it keeps going
     * until the upright pass looks convincingly complete.
     */
    override suspend fun readFrame(absolutePath: String): FrameReading =
        scan(absolutePath, enoughToStop = UPRIGHT_CONFIDENT)

    private suspend fun scan(absolutePath: String, enoughToStop: Int): FrameReading =
        withContext(dispatchers.default) {
            val file = File(absolutePath)
            if (!file.exists()) {
                Timber.w("OCR: no such file %s", absolutePath)
                return@withContext FrameReading(emptyList(), emptyList())
            }

            val source = decodeForOcr(absolutePath)
                ?: return@withContext FrameReading(emptyList(), emptyList())
            val found = LinkedHashMap<String, DetectedNumber>()
            val unread = LinkedHashMap<String, UnreadRegion>()

            try {
                for (rotation in ROTATIONS) {
                    val bitmap = if (rotation == 0) source else source.rotated(rotation)
                    try {
                        val text = recognise(bitmap) ?: continue
                        collectFrom(text, rotation, source.width, source.height, found, unread)
                    } finally {
                        if (bitmap !== source) bitmap.recycle()
                    }
                    // The rotated passes exist for vertically painted numbers. If
                    // the upright pass already read several, the stack is upright
                    // and turning the image again is close to a second of wasted
                    // work on every frame of a sweep.
                    if (rotation == 0 && found.size >= enoughToStop) break
                }
            } finally {
                source.recycle()
            }

            // A region that turned out to be readable on another rotation is no
            // longer a miss.
            val stillUnread = unread.values.filterNot { region ->
                found.values.any { it.box.overlaps(region.box) }
            }

            Timber.d(
                "OCR: %d confirmed, %d unreadable in %s",
                found.size, stillUnread.size, file.name,
            )
            FrameReading(found.values.toList(), stillUnread)
        }

    /** Two boxes describing the same physical stencil. */
    private fun FrameBox.overlaps(other: FrameBox): Boolean {
        val ix = minOf(right, other.right) - maxOf(left, other.left)
        val iy = minOf(bottom, other.bottom) - maxOf(top, other.top)
        if (ix <= 0 || iy <= 0) return false
        val inter = ix * iy
        val mine = (right - left) * (bottom - top)
        return mine > 0f && inter / mine > 0.4f
    }

    // ---------------------------------------------------------------- OCR ----

    private suspend fun recognise(bitmap: Bitmap): Text? =
        suspendCancellableCoroutine { cont ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener {
                    Timber.w(it, "OCR failed")
                    cont.resume(null)
                }
        }

    /**
     * Harvests candidates three ways, because a container number can reach ML Kit
     * as a whole line, as a line split in two, or as a column of single characters.
     */
    private fun collectFrom(
        text: Text,
        rotation: Int,
        srcW: Int,
        srcH: Int,
        into: MutableMap<String, DetectedNumber>,
        unread: MutableMap<String, UnreadRegion>,
    ) {
        for (block in text.textBlocks) {
            val lines = block.lines

            // 1. Each line on its own — the ordinary horizontal case.
            for (line in lines) {
                accept(line.text, line.boundingBox, rotation, srcW, srcH, into, unread)
            }

            // 2. Consecutive lines joined — the prefix and serial often split.
            for (i in 0 until lines.size - 1) {
                val a = lines[i]
                val b = lines[i + 1]
                accept(a.text + b.text, union(a.boundingBox, b.boundingBox),
                    rotation, srcW, srcH, into, unread)
            }

            // 3. Characters stacked into a column — the vertical case.
            for (column in verticalRuns(lines)) {
                val joined = column.joinToString("") { it.text }
                val box = column.mapNotNull { it.boundingBox }
                    .reduceOrNull { acc, r -> union(acc, r)!! }
                accept(joined, box, rotation, srcW, srcH, into, unread)
            }
        }
    }

    /**
     * Groups lines that sit above one another in a narrow column.
     *
     * A vertically painted number arrives as several one-character lines whose
     * horizontal centres nearly coincide; that alignment is what distinguishes
     * them from unrelated text elsewhere in the frame.
     */
    private fun verticalRuns(lines: List<Text.Line>): List<List<Text.Line>> {
        val boxed = lines.filter { it.boundingBox != null && it.text.length <= SHORT_LINE }
            .sortedBy { it.boundingBox!!.top }
        if (boxed.size < MIN_COLUMN) return emptyList()

        val runs = mutableListOf<MutableList<Text.Line>>()
        for (line in boxed) {
            val box = line.boundingBox!!
            val centreX = box.centerX()
            val run = runs.lastOrNull { current ->
                val last = current.last().boundingBox!!
                val tolerance = maxOf(last.width(), box.width()) * COLUMN_TOLERANCE
                kotlin.math.abs(last.centerX() - centreX) <= tolerance &&
                    box.top >= last.top &&
                    box.top - last.bottom <= last.height() * COLUMN_GAP
            }
            if (run != null) run.add(line) else runs.add(mutableListOf(line))
        }
        return runs.filter { it.size >= MIN_COLUMN }
    }

    // --------------------------------------------------------- candidates ----

    private fun accept(
        raw: String,
        box: Rect?,
        rotation: Int,
        srcW: Int,
        srcH: Int,
        into: MutableMap<String, DetectedNumber>,
        unread: MutableMap<String, UnreadRegion>,
    ) {
        val cleaned = raw.uppercase()
            .map { LOOKALIKES[it] ?: it }
            .filter { it in 'A'..'Z' || it in '0'..'9' }
            .joinToString("")

        // Shorter than a full number, but shaped like one: a stencil obscured by
        // rust, glare or the edge of the frame. Worth flagging rather than
        // dropping — it is the difference between "six here" and "ten here and I
        // could read six".
        if (cleaned.length < LENGTH) {
            if (NEAR_MISS.matches(cleaned) && box != null) {
                val mapped = mapBack(box, rotation, srcW, srcH)
                unread.putIfAbsent(cleaned, UnreadRegion(cleaned, mapped.toFrameBox(srcW, srcH)))
            }
            return
        }

        // Slide an 11-character window: the number is often embedded in a longer run.
        for (start in 0..cleaned.length - LENGTH) {
            val window = cleaned.substring(start, start + LENGTH)
            val repaired = repairConfusions(window)
            if (validate(repaired) !is ValidateContainerNumberUseCase.Result.Valid) {
                // Right shape, wrong check digit — a genuine container the
                // camera could not read cleanly.
                if (NEAR_MISS.matches(repaired) && box != null) {
                    val mapped = mapBack(box, rotation, srcW, srcH)
                    unread.putIfAbsent(
                        repaired,
                        UnreadRegion(repaired, mapped.toFrameBox(srcW, srcH)),
                    )
                }
                continue
            }
            if (into.containsKey(repaired)) return
            val mapped = box?.let { mapBack(it, rotation, srcW, srcH) }
            into[repaired] = DetectedNumber(
                number = repaired,
                box = mapped?.toFrameBox(srcW, srcH) ?: FrameBox(0f, 0f, 0f, 0f),
            )
            // Whatever partial text pointed at the same stencil is now resolved.
            unread.keys.removeAll { it.length < LENGTH && repaired.startsWith(it.take(4)) }
            return
        }
    }

    /**
     * Repairs misreads using the format itself: the first four characters are
     * always letters and the last seven always digits, so a 0 among the letters
     * can only have been an O.
     */
    private fun repairConfusions(window: String): String {
        val out = StringBuilder(LENGTH)
        window.forEachIndexed { i, c ->
            out.append(if (i < OWNER_LEN) TO_LETTER[c] ?: c else TO_DIGIT[c] ?: c)
        }
        return out.toString()
    }

    // ------------------------------------------------------------ bitmaps ----

    /**
     * Decodes at a resolution OCR can actually work with.
     *
     * A distant number may be only a few pixels tall; downsampling to save memory
     * is exactly what makes it unreadable, so the ceiling here is deliberately
     * high.
     */
    private fun decodeForOcr(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0) return null

        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_OCR_EDGE) {
            sample *= 2
        }
        return BitmapFactory.decodeFile(
            path,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    private fun Bitmap.rotated(degrees: Int): Bitmap {
        val m = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, m, true)
    }

    /** Returns a box found in a rotated image to coordinates in the original. */
    private fun mapBack(box: Rect, rotation: Int, srcW: Int, srcH: Int): Rect = when (rotation) {
        90 -> Rect(box.top, srcH - box.right, box.bottom, srcH - box.left)
        270 -> Rect(srcW - box.bottom, box.left, srcW - box.top, box.right)
        else -> box
    }

    private fun Rect.toFrameBox(srcW: Int, srcH: Int) = FrameBox(
        left = left / srcW.toFloat(),
        top = top / srcH.toFloat(),
        right = right / srcW.toFloat(),
        bottom = bottom / srcH.toFloat(),
    )

    private fun union(a: Rect?, b: Rect?): Rect? = when {
        a == null -> b
        b == null -> a
        else -> Rect(minOf(a.left, b.left), minOf(a.top, b.top),
            maxOf(a.right, b.right), maxOf(a.bottom, b.bottom))
    }

    private companion object {
        const val LENGTH = 11

        /**
         * The shape of a container number even when damaged: owner letters then
         * serial digits. Tare weights, capacity plates and door text do not
         * match it, so they never become false alarms.
         */
        val NEAR_MISS = Regex("^[A-Z]{3,4}[0-9]{5,7}$")
        const val OWNER_LEN = 4

        /** Upright first, then the two turns that make a vertical number horizontal. */
        val ROTATIONS = intArrayOf(0, 90, 270)
        /**
         * Numbers read upright that make the rotated passes not worth their cost.
         *
         * One match is not enough — a stack photographed with vertical stencils
         * can still yield a single horizontal number from a placard.
         */
        const val UPRIGHT_CONFIDENT = 2

        /** Long enough for a distant number to survive; small enough to decode safely. */
        const val MAX_OCR_EDGE = 2400

        const val SHORT_LINE = 4          // a column element is a char or two
        const val MIN_COLUMN = 4          // fewer than this is not a stacked number
        const val COLUMN_TOLERANCE = 0.9  // horizontal drift allowed within a column
        const val COLUMN_GAP = 2.0        // vertical gap allowed, in element heights

        val TO_LETTER = mapOf('0' to 'O', '1' to 'I', '5' to 'S', '8' to 'B', '2' to 'Z')
        val TO_DIGIT = mapOf('O' to '0', 'Q' to '0', 'D' to '0', 'I' to '1', 'L' to '1',
            'S' to '5', 'B' to '8', 'Z' to '2', 'G' to '6')

        /**
         * Cyrillic and Greek letters that are visually identical to Latin ones.
         * The recogniser occasionally emits these on weathered stencils, and they
         * fail `isLetterOrDigit` filtering silently — the string simply never
         * matches, with nothing to indicate why.
         */
        val LOOKALIKES = mapOf(
            'З' to '3', 'О' to 'O', 'Ο' to 'O', 'А' to 'A', 'Α' to 'A',
            'В' to 'B', 'Β' to 'B', 'С' to 'C', 'Ϲ' to 'C', 'Е' to 'E',
            'Ε' to 'E', 'Н' to 'H', 'Η' to 'H', 'К' to 'K', 'Κ' to 'K',
            'М' to 'M', 'Μ' to 'M', 'Р' to 'P', 'Ρ' to 'P', 'Т' to 'T',
            'Τ' to 'T', 'Х' to 'X', 'Χ' to 'X', 'У' to 'Y', 'Υ' to 'Y',
            'І' to 'I', 'Ι' to 'I', 'Ѕ' to 'S', 'б' to '6', 'Ч' to '4',
        )
    }
}
