package com.middleeastcontainer.ui.inventory

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.nativeCanvas
import com.middleeastcontainer.domain.ocr.DetectedNumber
import com.middleeastcontainer.domain.ocr.FrameBox
import com.middleeastcontainer.domain.ocr.UnreadRegion
import com.middleeastcontainer.ui.theme.BrandGold
import com.middleeastcontainer.ui.theme.VerifiedGreen as ReadGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The captured frame with each recognised number boxed.
 *
 * This is the point of keeping the OCR bounding boxes: on a stack of eight
 * containers, a bare list of five numbers does not tell you which three were
 * missed. Boxes on the photo make the gap obvious at a glance.
 */
@Composable
fun DetectionOverlay(
    photoAbsolutePath: String,
    detections: List<DetectedNumber>,
    unread: List<UnreadRegion> = emptyList(),
    unreadTags: List<String> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val bitmapState = produceState<ImageBitmap?>(null, photoAbsolutePath) {
        value = withContext(Dispatchers.IO) {
            val f = File(photoAbsolutePath)
            if (!f.exists()) null
            else runCatching {
                val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                BitmapFactory.decodeFile(photoAbsolutePath, opts)?.asImageBitmap()
            }.getOrNull()
        }
    }
    val bitmap = bitmapState.value ?: return

    Box(modifier) {
        androidx.compose.foundation.Image(
            bitmap = bitmap,
            contentDescription = "Captured frame",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit,
        )

        Canvas(Modifier.fillMaxSize()) {
            // The image is letterboxed by ContentScale.Fit, so boxes must be
            // mapped through the same fit — not simply scaled to the canvas.
            val scale = minOf(
                size.width / bitmap.width.toFloat(),
                size.height / bitmap.height.toFloat(),
            )
            val drawnW = bitmap.width * scale
            val drawnH = bitmap.height * scale
            val offsetX = (size.width - drawnW) / 2f
            val offsetY = (size.height - drawnH) / 2f

            // Detections are fractions of the image, so they map straight onto
            // the drawn area whatever decode or resize happened in between.
            detections.forEach { d ->
                if (!d.hasBox) return@forEach
                val l = offsetX + d.left * drawnW
                val t = offsetY + d.top * drawnH
                val r = offsetX + d.right * drawnW
                val b = offsetY + d.bottom * drawnH
                drawRect(
                    color = BrandGold,
                    topLeft = Offset(l, t),
                    size = Size(r - l, b - t),
                    style = Stroke(width = 3f),
                )
                drawRect(
                    color = Color(0x22F2A33C),
                    topLeft = Offset(l, t),
                    size = Size(r - l, b - t),
                )
            }
        }
    }
}
