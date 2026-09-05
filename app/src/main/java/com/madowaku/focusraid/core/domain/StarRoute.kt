package com.madowaku.focusraid.core.domain

object StarRoute {
    const val BEACONS_PER_ROUTE = 5
    private const val MINUTES_PER_CHECKPOINT = 25

    fun targetCheckpoint(totalFocusMinutes: Int): Int =
        totalFocusMinutes.coerceAtLeast(0) / MINUTES_PER_CHECKPOINT + 1

    fun reachedCheckpoint(totalFocusMinutes: Int): Int {
        val minutes = totalFocusMinutes.coerceAtLeast(0)
        if (minutes == 0) return 0
        return (minutes - 1) / MINUTES_PER_CHECKPOINT + 1
    }

    fun litBeacons(progress: Float): Int =
        (progress.coerceIn(0f, 1f) * BEACONS_PER_ROUTE)
            .toInt()
            .coerceIn(0, BEACONS_PER_ROUTE)
}
