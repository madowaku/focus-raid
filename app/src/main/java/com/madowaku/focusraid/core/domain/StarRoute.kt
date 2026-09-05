package com.madowaku.focusraid.core.domain

object StarRoute {
    const val BEACONS_PER_ROUTE = 5
    const val MINUTES_PER_CHECKPOINT = 25

    fun reachedCheckpoint(totalFocusMinutes: Int): Int =
        totalFocusMinutes.coerceAtLeast(0) / MINUTES_PER_CHECKPOINT

    fun targetCheckpoint(totalFocusMinutes: Int): Int =
        reachedCheckpoint(totalFocusMinutes) + 1

    fun minutesUntilTarget(totalFocusMinutes: Int): Int {
        val progressMinutes = totalFocusMinutes.coerceAtLeast(0) % MINUTES_PER_CHECKPOINT
        return MINUTES_PER_CHECKPOINT - progressMinutes
    }

    fun litBeacons(progress: Float): Int =
        (progress.coerceIn(0f, 1f) * BEACONS_PER_ROUTE)
            .toInt()
            .coerceIn(0, BEACONS_PER_ROUTE)
}
