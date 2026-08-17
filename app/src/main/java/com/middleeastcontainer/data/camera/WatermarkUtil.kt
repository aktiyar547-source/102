package com.middleeastcontainer.data.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.media.ExifInterface
import com.middleeastcontainer.core.common.Clock
import com.middleeastcontainer.core.common.DateFormats
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * Burns the container number and capture time into the photo itself, so a
 * printed or forwarded image still proves what it shows and when.
 *
 * Also EXIF-orients the shot and re-encodes it as JPEG.
 */
class WatermarkUtil @Inject constructor(private val clock: Clock) {

    /**
     * Processes [source] in place and returns the same file.
     *
     * [containerName] appears alongside the timestamp; pass null for a photo not
     * yet tied to a container.
     */
    fun applyTimestampWatermark(
        source: File,
        containerName: String? = null,
        sampleSize: Int = STORAGE_SAMPLE_SIZE,
    ): File {
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        var bitmap = BitmapFactory.decodeFile(source.path, opts) ?: return source
        bitmap = orientToExif(bitmap, source.path)
        bitmap = scaleToMaxEdge(bitmap, STORAGE_MAX_EDGE)

        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)

        // Scale with the image. A fixed size is illegible on a large photo and
        // overwhelming on a small one.
        val textSize = mutable.width / 30f
        val pad = textSize * 0.5f
        val bandHeight = textSize * 2.4f

        // Dark band so the text stays readable over a light or dark container.
        canvas.drawRect(
            0f,
            mutable.height - bandHeight,
            mutable.width.toFloat(),
            mutable.height.toFloat(),
            Paint().apply { color = Color.argb(150, 0, 0, 0) },
        )

        val stamp = DateFormats.timestamp(clock.now())
        val baseline = mutable.height - bandHeight + textSize + pad * 0.6f

        if (!containerName.isNullOrBlank()) {
            val idPaint = Paint().apply {
                color = Color.WHITE
                this.textSize = textSize
                isAntiAlias = true
                isFakeBoldText = true
            }
            canvas.drawText(containerName, pad, baseline, idPaint)
        }

        // Timestamp right-aligned, so a long container number cannot collide.
        val timePaint = Paint().apply {
            color = Color.WHITE
            this.textSize = textSize * 0.85f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(stamp, mutable.width - pad, baseline, timePaint)

        // Stored as JPEG: a fraction of PNG's size on the device, and the same
        // format that is uploaded, so no re-encoding between formats is needed.
        FileOutputStream(source).use {
            mutable.compress(Bitmap.CompressFormat.JPEG, STORAGE_JPEG_QUALITY, it)
        }
        if (mutable != bitmap) bitmap.recycle()
        mutable.recycle()
        return source
    }

    /** Exact scale to a target long edge. Never upscales. */
    private fun scaleToMaxEdge(src: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = maxOf(src.width, src.height)
        if (longEdge <= maxEdge) return src
        val ratio = maxEdge.toFloat() / longEdge
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, w, h, true)
        if (scaled != src) src.recycle()
        return scaled
    }

    private fun orientToExif(bitmap: Bitmap, path: String): Bitmap {
        val rotate = when (
            ExifInterface(path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotate == 0f) return bitmap
        val m = Matrix().apply { preRotate(rotate) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, false)
    }

    private companion object {
        /**
         * Measured, not guessed: on a noisy photograph q90 -> q85 costs about
         * 23% of the file for a difference that is not visible, and the bulk of
         * the saving comes from resolution rather than quality.
         */
        const val STORAGE_JPEG_QUALITY = 85

        /**
         * Long edge of the stored photo.
         *
         * Must stay ABOVE the upload cap (UPLOAD_IMAGE_MAX_EDGE, 1280px) —
         * storing smaller would throw away detail at capture time that no upload
         * setting could recover. 1600px clears it with margin while cutting the
         * file to roughly a third of a 2016px original: about 380 KB instead of
         * 1 MB, so an eleven-side inspection is 4 MB rather than 11 MB.
         */
        const val STORAGE_MAX_EDGE = 1600

        /** Prescale ceiling: never decode a full 12MP frame just to shrink it. */
        const val STORAGE_SAMPLE_SIZE = 2
    }
}
