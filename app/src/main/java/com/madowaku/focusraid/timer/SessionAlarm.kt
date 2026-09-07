package com.madowaku.focusraid.timer

interface SessionAlarm {
    fun schedule(endEpochMillis: Long)
    fun cancel()
}
