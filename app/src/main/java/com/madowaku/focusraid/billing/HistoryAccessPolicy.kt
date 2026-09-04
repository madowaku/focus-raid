package com.madowaku.focusraid.billing

import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionHistoryEntry
import com.madowaku.focusraid.core.model.SessionOutcome
import java.time.Instant
import java.time.ZoneId

object HistoryAccessPolicy {
    const val FREE_HISTORY_DAYS = 7L

    fun visibleEntries(
        entries: List<SessionHistoryEntry>,
        accessLevel: AccessLevel,
        nowEpochMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<SessionHistoryEntry> {
        if (FeatureAccess.canUse(ProFeature.FULL_HISTORY, accessLevel)) return entries

        val today = Instant.ofEpochMilli(nowEpochMillis)
            .atZone(zoneId)
            .toLocalDate()
        val cutoffEpochMillis = today
            .minusDays(FREE_HISTORY_DAYS - 1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        return entries.filter { entry ->
            entry.completedAtEpochMillis in cutoffEpochMillis..nowEpochMillis
        }
    }

    fun detailedStats(entries: List<SessionHistoryEntry>): DetailedHistoryStats {
        if (entries.isEmpty()) return DetailedHistoryStats()

        val completed = entries.count { it.outcome == SessionOutcome.COMPLETED }
        val aborted = entries.count { it.outcome == SessionOutcome.ABORTED }
        val totalMinutes = entries.sumOf { it.creditedMinutes }
        val averageMinutes = totalMinutes.toDouble() / entries.size.toDouble()
        val longestMinutes = entries.maxOfOrNull { it.creditedMinutes } ?: 0
        val towerMinutes = entries
            .filter { it.expedition == Expedition.TOWER }
            .sumOf { it.creditedMinutes }
        val abyssMinutes = entries
            .filter { it.expedition == Expedition.ABYSS }
            .sumOf { it.creditedMinutes }
        val completionRatePercent = if (entries.isEmpty()) {
            0
        } else {
            ((completed.toDouble() / entries.size.toDouble()) * 100.0).toInt()
        }

        return DetailedHistoryStats(
            sessionCount = entries.size,
            completedCount = completed,
            abortedCount = aborted,
            completionRatePercent = completionRatePercent,
            averageMinutes = averageMinutes,
            longestMinutes = longestMinutes,
            towerMinutes = towerMinutes,
            abyssMinutes = abyssMinutes,
        )
    }
}

data class DetailedHistoryStats(
    val sessionCount: Int = 0,
    val completedCount: Int = 0,
    val abortedCount: Int = 0,
    val completionRatePercent: Int = 0,
    val averageMinutes: Double = 0.0,
    val longestMinutes: Int = 0,
    val towerMinutes: Int = 0,
    val abyssMinutes: Int = 0,
)
