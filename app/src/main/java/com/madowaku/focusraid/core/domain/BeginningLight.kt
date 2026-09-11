package com.madowaku.focusraid.core.domain

data class BeginningLightStatus(
    val lit: Boolean,
    val hatched: Boolean,
    val totalMinutes: Int,
    val hatchRemainingMinutes: Int,
    val hatchProgress: Float,
)

object BeginningLight {
    const val LIGHT_THRESHOLD_MINUTES = 25
    const val HATCH_THRESHOLD_MINUTES = 75

    fun from(totalFocusMinutes: Int): BeginningLightStatus {
        val total = totalFocusMinutes.coerceAtLeast(0)
        return BeginningLightStatus(
            lit = total >= LIGHT_THRESHOLD_MINUTES,
            hatched = total >= HATCH_THRESHOLD_MINUTES,
            totalMinutes = total,
            hatchRemainingMinutes = (HATCH_THRESHOLD_MINUTES - total).coerceAtLeast(0),
            hatchProgress = total
                .coerceIn(0, HATCH_THRESHOLD_MINUTES)
                .toFloat() / HATCH_THRESHOLD_MINUTES.toFloat(),
        )
    }
}
