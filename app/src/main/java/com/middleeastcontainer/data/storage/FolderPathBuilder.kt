package com.middleeastcontainer.data.storage

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Builds the folder layout under /OCR2.
 *
 *     <section>/<yyyy>/<MM>/<yyyy-MM-dd>/<group>/<group> <date> [HH-mm-ss].jpg
 *
 * Dated folders keep a yard's worth of photos navigable by hand — someone
 * looking for last Tuesday's count can find it without the app.
 */
object FolderPathBuilder {

    private val YEAR = SimpleDateFormat("yyyy", Locale.US)
    private val MONTH = SimpleDateFormat("MM", Locale.US)
    private val DAY = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val TIME = SimpleDateFormat("HH-mm-ss", Locale.US)

    /** Strips anything that could break out of the folder or upset a filesystem. */
    /** Exposed so callers can match a folder written earlier. */
    fun safeName(value: String): String = safe(value)

    private fun safe(value: String): String =
        value.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").take(48)
            .ifBlank { "unknown" }

    fun relativeDir(section: String, group: String, at: Date = Date()): String =
        "${safe(section)}/${YEAR.format(at)}/${MONTH.format(at)}/${DAY.format(at)}/${safe(group)}"

    fun captureFileName(group: String, at: Date = Date()): String =
        "${safe(group)} ${DAY.format(at)} [${TIME.format(at)}].jpg"

    /** Kept for callers that still pass a Calendar. */
    fun relativeContainerDir(container: String, calendar: Calendar): String =
        relativeDir("Inspection", container, calendar.time)
}
