package com.madowaku.focusraid.billing

import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionHistoryEntry
import com.madowaku.focusraid.core.model.SessionOutcome
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryAccessPolicyTest {
    private val zone = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 9, 5)
    private val now = today.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `Free sees today plus previous six calendar days while Pro sees all`() {
        val entries = listOf(
            entry("today", today, 25),
            entry("day6", today.minusDays(6), 25),
            entry("day7", today.minusDays(7), 25),
        )

        val free = HistoryAccessPolicy.visibleEntries(entries, AccessLevel.FREE, now, zone)
        val pro = HistoryAccessPolicy.visibleEntries(entries, AccessLevel.PRO, now, zone)

        assertEquals(listOf("today", "day6"), free.map { it.sessionId })
        assertEquals(listOf("today", "day6", "day7"), pro.map { it.sessionId })
    }

    @Test
    fun `detailed stats summarize all supplied sessions`() {
        val entries = listOf(
            entry("tower-complete", today, 25, Expedition.TOWER, SessionOutcome.COMPLETED),
            entry("abyss-complete", today, 45, Expedition.ABYSS, SessionOutcome.COMPLETED),
            entry("star-complete", today, 30, Expedition.STAR_ROUTE, SessionOutcome.COMPLETED),
            entry("tower-abort", today, 10, Expedition.TOWER, SessionOutcome.ABORTED),
        )

        val stats = HistoryAccessPolicy.detailedStats(entries)

        assertEquals(4, stats.sessionCount)
        assertEquals(3, stats.completedCount)
        assertEquals(1, stats.abortedCount)
        assertEquals(75, stats.completionRatePercent)
        assertEquals(110.0 / 4.0, stats.averageMinutes, 0.001)
        assertEquals(45, stats.longestMinutes)
        assertEquals(35, stats.towerMinutes)
        assertEquals(45, stats.abyssMinutes)
        assertEquals(30, stats.starRouteMinutes)
    }

    private fun entry(
        id: String,
        date: LocalDate,
        minutes: Int,
        expedition: Expedition = Expedition.TOWER,
        outcome: SessionOutcome = SessionOutcome.COMPLETED,
    ): SessionHistoryEntry = SessionHistoryEntry(
        sessionId = id,
        completedAtEpochMillis = date.atTime(10, 0).atZone(zone).toInstant().toEpochMilli(),
        plannedMinutes = minutes,
        creditedMinutes = minutes,
        expedition = expedition,
        outcome = outcome,
        damage = minutes,
        rarity = null,
        discovery = null,
    )
}
