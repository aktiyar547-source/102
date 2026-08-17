package com.middleeastcontainer.core.common

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Injectable clock so time-dependent logic (dates, retention) is deterministic in tests. */
interface Clock {
    fun now(): Date
    fun calendar(): Calendar
}

/** Legacy-exact date/time formats, centralized (no scattered SimpleDateFormat). */
object DateFormats {
    private fun fmt(p: String) = SimpleDateFormat(p, Locale.US)
    fun displayDate(d: Date): String = fmt("dd-MMMM-yyyy").format(d)   // Container.Date
    fun createdDate(d: Date): String = fmt("yyyy-MM-dd").format(d)      // CreatedDate
    fun timestamp(d: Date): String = fmt("yyyy-MM-dd HH:mm:ss").format(d) // EImages.Time / watermark

    /** yyyy-MM-dd for `now` minus [days] — the retention cutoff. */
    fun createdDateMinusDays(cal: Calendar, days: Long): String {
        val c = cal.clone() as Calendar
        c.add(Calendar.DAY_OF_YEAR, -days.toInt())
        return createdDate(c.time)
    }
}
