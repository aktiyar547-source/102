package com.middleeastcontainer.data.network.mapper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.middleeastcontainer.core.common.AppConfig
import timber.log.Timber
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Encodes captured photos as Base64 JPEG for the two legacy endpoints.
 *
 * The app is JPEG-only: photos are captured, stored and uploaded as JPEG. This
 * gives far better detail per byte than the legacy PNG path, and keeps payloads
 * well under the server's limits.
 *
 * SIZING MATTERS. The legacy app ran on ~5MP phones and downsampled 8x, so the
 * server only ever received small images. A modern 12MP photo sent at full size
 * would be ~23 MB per container and exceed PHP's default 8 MB post_max_size, so
 * the long edge is capped by [AppConfig.uploadImageMaxEdge].
 *
 * Field names, ordering and Base64 flags are unchanged, so the wire contract is
 * preserved - only the image bytes differ.
 */
class ImageEncoder @Inject constructor(
    private val config: AppConfig,
) {

    /** /container/test - one of these per side, all in a single POST. */
    fun forTestPayload(path: String): String =
        encode(path, maxEdge = config.uploadImageMaxEdge, label = "side")

    /** /container/extra_images - one image per POST. */
    fun forExtraImage(path: String): String =
        encode(path, maxEdge = config.uploadImageMaxEdge, label = "extra")

    private fun encode(path: String, maxEdge: Int, label: String): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            Timber.w("Could not read image: %s", path)
            return ""
        }

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        val decoded = BitmapFactory.decodeFile(path, opts) ?: return ""
        val scaled = scaleToMaxEdge(decoded, maxEdge)

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
        val bytes = out.toByteArray()
        val encoded = Base64.encodeToString(bytes, Base64.DEFAULT)

        Timber.d(
            "encode[%s] %dx%d -> %dx%d, JPEG %d KB, base64 %d KB",
            label, bounds.outWidth, bounds.outHeight, scaled.width, scaled.height,
            bytes.size / 1024, encoded.length / 1024,
        )

        if (scaled != decoded) scaled.recycle()
        decoded.recycle()
        return encoded
    }

    /** Power-of-two prescale so we never decode a huge bitmap into memory. */
    private fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxEdge && h / 2 >= maxEdge) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    /** Exact scale so the long edge equals [maxEdge] (never upscales). */
    private fun scaleToMaxEdge(src: Bitmap, maxEdge: Int): Bitmap {
        val longEdge = maxOf(src.width, src.height)
        if (longEdge <= maxEdge) return src
        val ratio = maxEdge.toFloat() / longEdge
        val w = (src.width * ratio).toInt().coerceAtLeast(1)
        val h = (src.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(src, w, h, true)
    }

    private companion object {
        /** Visually near-lossless for inspection detail, ~10x smaller than PNG. */
        const val JPEG_QUALITY = 85
    }
}
