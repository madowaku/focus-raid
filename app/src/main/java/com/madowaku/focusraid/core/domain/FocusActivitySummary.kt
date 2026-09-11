package com.madowaku.focusraid.core.domain

import com.madowaku.focusraid.core.model.SessionHistoryEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Local activity facts derived only from completed focus history.
 *
 * Streak semantics intentionally allow today's session to be missing without immediately
 * destroying yesterday's streak. If the latest active day is yesterday, the streak remains
 * alive for the current day and becomes zero only after a full inactive day has passed.
 */
data class FocusActivitySummary(
    val todayMinutes: Int,
    val currentStreakDays: Int,
    val activeDays: Int,
)

object FocusActivitySummaries {
    fun from(
        entries: List<SessionHistoryEntry>,
        nowEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): FocusActivitySummary {
        val today = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId).toLocalDate()
        val minutesByDay = entries
            .asSequence()
            .filter { it.creditedMinutes > 0 }
            .groupBy { it.completedAtEpochMillis.toLocalDate(zoneId) }
            .mapValues { (_, sessions) -> sessions.sumOf { it.creditedMinutes } }

        val activeDays = minutesByDay.keys
        val latestAllowedStart = when {
            today in activeDays -> today
            today.minusDays(1) in activeDays -> today.minusDays(1)
            else -> null
        }

        var streak = 0
        var cursor = latestAllowedStart
        while (cursor != null && cursor in activeDays) {
            streak += 1
            cursor = cursor.minusDays(1)
        }

        return FocusActivitySummary(
            todayMinutes = minutesByDay[today] ?: 0,
            currentStreakDays = streak,
            activeDays = activeDays.size,
        )
    }

    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
}
