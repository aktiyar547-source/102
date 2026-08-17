package com.middleeastcontainer.data.storage

import android.content.Context
import android.os.Build
import android.os.Environment
import com.middleeastcontainer.core.common.Constants
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Where inspection and inventory photos live on the phone.
 *
 * Preferred layout, at the top of shared storage so it is obvious over USB or in
 * any file manager:
 *
 *     /OCR2/Inspection/<yyyy>/<MM>/<yyyy-MM-dd>/<container>/
 *     /OCR2/Inventory/<yyyy>/<MM>/<yyyy-MM-dd>/<zone>/
 *
 * Android has not allowed apps to write to the storage root since API 29, so this
 * needs the All-files-access permission. That is granted once in Settings, and is
 * available here because the app is distributed directly rather than through the
 * Play Store.
 *
 * Without it, photos fall back to the app's private directory. Everything keeps
 * working — the files are simply buried under Android/data and awkward to reach.
 */
@Singleton
class ImageFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** True when photos can be written to the visible /OCR2 folder. */
    val hasRootAccess: Boolean
        get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager()

    /** Root actually in use, which may be the private fallback. */
    val root: File
        get() = if (hasRootAccess) sharedRoot() else privateRoot()

    /** Where the visible folder would be, whether or not it is reachable yet. */
    fun sharedRootPath(): String =
        File(Environment.getExternalStorageDirectory(), Constants.IMAGE_ROOT_DIR).path

    private fun sharedRoot(): File =
        File(Environment.getExternalStorageDirectory(), Constants.IMAGE_ROOT_DIR)
            .apply { if (!exists()) mkdirs() }

    private fun privateRoot(): File =
        File(context.getExternalFilesDir(null), Constants.IMAGE_ROOT_DIR)
            .apply { if (!exists()) mkdirs() }

    /**
     * Resolves a stored path.
     *
     * Photos captured before the all-files permission was granted live under the
     * private root, and their database rows are relative to that. Once the
     * permission is granted the root changes, so resolving only against the
     * current root would make every earlier photo silently disappear. Both are
     * checked, newest location first.
     */
    fun absoluteFor(relativePath: String): File {
        val primary = File(root, relativePath)
        if (primary.exists()) return primary
        val fallback = File(otherRoot(), relativePath)
        return if (fallback.exists()) fallback else primary
    }

    /** The root not currently in use — where older photos may still sit. */
    private fun otherRoot(): File = if (hasRootAccess) privateRoot() else sharedRoot()

    /**
     * Allocates a new photo path.
     *
     * @param section [Constants.INSPECTION_DIR] or [Constants.INVENTORY_DIR]
     * @param group container number, or zone for a sweep
     * @return the RELATIVE path stored in the database. Relative on purpose: the
     *   absolute root changes when the permission is granted, and rows written
     *   before that must not break.
     */
    fun newCaptureFile(section: String, group: String): Pair<String, File> {
        val relDir = FolderPathBuilder.relativeDir(section, group)
        val fileName = FolderPathBuilder.captureFileName(group)
        File(root, relDir).apply { if (!exists()) mkdirs() }
        val relPath = "$relDir/$fileName"
        return relPath to File(root, relPath)
    }

    /**
     * Recursively deletes one group's photos, across every date folder and both
     * roots.
     *
     * Folders are dated, so looking only under today's date would miss anything
     * captured earlier — the database row would go and the files would remain on
     * the phone forever.
     */
    fun deleteGroupDir(section: String, group: String) {
        val target = FolderPathBuilder.safeName(group)
        for (base in listOf(root, otherRoot())) {
            val sectionDir = File(base, FolderPathBuilder.safeName(section))
            if (!sectionDir.isDirectory) continue
            // <section>/<yyyy>/<MM>/<yyyy-MM-dd>/<group>
            sectionDir.listFiles()?.forEach { year ->
                year.listFiles()?.forEach { month ->
                    month.listFiles()?.forEach { day ->
                        File(day, target).takeIf { it.isDirectory }?.deleteRecursively()
                    }
                }
            }
        }
    }

    /** Deletes every photo belonging to one group, by relative path. */
    fun deleteAll(relativePaths: List<String>) {
        relativePaths.forEach { deleteRelative(it) }
    }

    /** Deletes one stored image by its DB-relative path. Silent if already gone. */
    fun deleteRelative(relativePath: String) {
        runCatching { File(root, relativePath).delete() }
        runCatching { File(otherRoot(), relativePath).delete() }
    }

    /** Converts an absolute path under the store back to the RELATIVE path kept in the DB. */
    fun relativeOf(absolutePath: String): String {
        val rootPath = root.path
        return if (absolutePath.startsWith(rootPath)) {
            absolutePath.removePrefix(rootPath).trimStart('/')
        } else {
            absolutePath
        }
    }

    /**
     * Moves a freshly captured (already watermarked) file into place and returns
     * the RELATIVE path stored in the DB. The source is removed.
     */
    fun importCapture(section: String, group: String, source: File): String {
        val (relPath, target) = newCaptureFile(section, group)
        source.copyTo(target, overwrite = true)
        source.delete()
        Timber.d("Stored %s", relPath)
        return relPath
    }
}
