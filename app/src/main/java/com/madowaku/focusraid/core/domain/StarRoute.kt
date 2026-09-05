package com.madowaku.focusraid.core.domain

object StarRoute {
    const val BEACONS_PER_ROUTE = 5
    private const val MINUTES_PER_CHECKPOINT = 25

    fun checkpoint(totalFocusMinutes: Int): Int =
        totalFocusMinutes.coerceAtLeast(0) / MINUTES_PER_CHECKPOINT + 1

    fun litBeacons(progress: Float): Int =
        (progress.coerceIn(0f, 1f) * BEACONS_PER_ROUTE)
            .toInt()
            .coerceIn(0, BEACONS_PER_ROUTE)
}
