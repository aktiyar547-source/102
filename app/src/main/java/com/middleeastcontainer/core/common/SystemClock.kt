package com.middleeastcontainer.core.common

import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class SystemClock @Inject constructor() : Clock {
    override fun now(): Date = Date()
    override fun calendar(): Calendar = Calendar.getInstance()
}
