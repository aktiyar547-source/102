package com.middleeastcontainer.data.camera

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Allocates the temp files the in-app camera writes into, and prunes stale ones. */
@Singleton
class CaptureFileProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val captureDir: File
        get() = File(context.getExternalFilesDir(null), "captures").apply { mkdirs() }

    fun newCaptureFile(): File = File(captureDir, "capture_${System.currentTimeMillis()}.jpg")

    /**
     * Fixed scratch path for the container-number scan.
     *
     * The scan is read by OCR and thrown away, so it needs no unique name — and
     * a known location lets the OCR screen pick the photo up on resume instead
     * of threading a result back through navigation.
     */
    fun ocrScratchFile(): File = File(captureDir, "ocr_scan.jpg")

    /**
     * Deletes camera temp files left behind by a process that died mid-save.
     *
     * A capture lands here first and is removed once filed against a container, so
     * anything still present after an hour is an orphan — and at full camera
     * resolution each one is several megabytes.
     */
    fun pruneStaleCaptures(olderThanMillis: Long = STALE_AFTER_MS) {
        val cutoff = System.currentTimeMillis() - olderThanMillis
        runCatching {
            captureDir.listFiles()?.forEach { f ->
                if (f.isFile && f.lastModified() < cutoff) f.delete()
            }
        }
    }

    private companion object {
        const val STALE_AFTER_MS = 60L * 60L * 1000L
    }
}
