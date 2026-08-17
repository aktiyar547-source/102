package com.middleeastcontainer.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Renders an image from an absolute path, decoded off the main thread.
 *
 * Reads the produced state via `.value` rather than a `by` delegate: the delegate
 * form needs an extra runtime import and adds nothing here, so this is the simpler
 * and more portable form.
 */
@Composable
fun FileImage(
    absolutePath: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val bitmapState = produceState<ImageBitmap?>(initialValue = null, key1 = absolutePath) {
        value = loadBitmap(absolutePath)
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Fit,
        )
    }
}

/** Decodes a downsampled bitmap on the IO dispatcher; null if missing or unreadable. */
private suspend fun loadBitmap(absolutePath: String?): ImageBitmap? {
    if (absolutePath == null) {
        return null
    }
    return withContext(Dispatchers.IO) {
        val file = File(absolutePath)
        if (!file.exists()) {
            null
        } else {
            runCatching {
                val options = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeFile(absolutePath, options)?.asImageBitmap()
            }.getOrNull()
        }
    }
}
