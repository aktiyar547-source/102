package com.middleeastcontainer.data.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.middleeastcontainer.core.common.DispatcherProvider
import com.middleeastcontainer.domain.model.Sighting
import com.middleeastcontainer.domain.model.Sweep
import com.middleeastcontainer.domain.model.UnreadUnit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Where the sheet landed, so it can be opened or shared straight away. */
data class ExportResult(val uri: Uri, val fileName: String, val rows: Int)

/**
 * Writes a sweep to a spreadsheet on the phone.
 *
 * Entirely offline: a yard often has no signal, and the count is wanted the
 * moment the walk ends rather than whenever a server becomes reachable.
 *
 * The file goes to the shared Downloads collection through MediaStore, which
 * needs no storage permission on Android 10 and above, and puts it somewhere the
 * inspector can actually find — app-scoped storage would be invisible to them.
 */
@Singleton
class SweepExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {

    suspend fun export(
        sweep: Sweep,
        sightings: List<Sighting>,
        unread: List<UnreadUnit> = emptyList(),
    ): ExportResult =
        withContext(dispatchers.io) {
            val rows = buildRows(sweep, sightings, unread)
            val fileName = fileName(sweep)

            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, XLSX_MIME)
                // Marked pending while writing, so nothing can open a half-file.
                put(MediaStore.Downloads.IS_PENDING, 1)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create the file in Downloads")

            try {
                resolver.openOutputStream(uri)?.use { out ->
                    XlsxWriter.write(out, SHEET_NAME, rows)
                } ?: error("Could not open the file for writing")
            } catch (e: Exception) {
                // Leaving a pending entry behind would clutter Downloads invisibly.
                runCatching { resolver.delete(uri, null, null) }
                throw e
            }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)

            Timber.i("Exported %s (%d units)", fileName, sightings.size)
            ExportResult(uri, fileName, sightings.size)
        }

    private fun buildRows(
        sweep: Sweep,
        sightings: List<Sighting>,
        unread: List<UnreadUnit>,
    ): List<List<XlsxWriter.Cell>> {
        val rows = mutableListOf<List<XlsxWriter.Cell>>()
        rows += HEADERS.map { XlsxWriter.text(it) }
        var n = 0
        sightings.sortedBy { it.containerNumber }.forEach { s ->
            rows += listOf(
                XlsxWriter.number((++n).toLong()),
                XlsxWriter.text(s.containerNumber),
                XlsxWriter.text(sweep.zone),
                XlsxWriter.text(sweep.startedBy),
                XlsxWriter.text(s.seenAt),
                XlsxWriter.text(if (s.fromOcr) "Scanned" else "Typed"),
            )
        }
        // Gaps belong in the sheet. A count that quietly omits what it could not
        // read is worse than one that says so, because nobody goes looking.
        unread.forEach { u ->
            rows += listOf(
                XlsxWriter.number((++n).toLong()),
                XlsxWriter.text(if (u.partial.isNotBlank()) "${u.partial}… (unread)" else "UNREAD"),
                XlsxWriter.text(sweep.zone),
                XlsxWriter.text(sweep.startedBy),
                XlsxWriter.text(u.seenAt),
                XlsxWriter.text("Needs check ${u.tag}"),
            )
        }
        return rows
    }

    /** Zone and date in the name, so a Downloads folder of these stays navigable. */
    private fun fileName(sweep: Sweep): String {
        val zone = sweep.zone.ifBlank { "Yard" }
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .take(24)
        val stamp = sweep.startedAt.replace(Regex("[^0-9]"), "").take(14)
        return "MECRC_inventory_${zone}_$stamp.xlsx"
    }

    private companion object {
        const val SHEET_NAME = "Inventory"
        const val XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        val HEADERS = listOf("#", "Container No", "Zone", "Counted by", "Seen at", "Source")
    }
}
