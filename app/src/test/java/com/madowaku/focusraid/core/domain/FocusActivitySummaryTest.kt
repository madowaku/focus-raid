package com.madowaku.focusraid.core.domain

import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionHistoryEntry
import com.madowaku.focusraid.core.model.SessionOutcome
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusActivitySummaryTest {
    private val zone = ZoneId.of("Asia/Tokyo")

    @Test
    fun `today sessions aggregate minutes and consecutive days form streak`() {
        val now = epoch(2026, 9, 4, 20, 0)
        val entries = listOf(
            entry("today-1", epoch(2026, 9, 4, 9, 0), 25),
            entry("today-2", epoch(2026, 9, 4, 18, 0), 45),
            entry("yesterday", epoch(2026, 9, 3, 12, 0), 25),
            entry("two-days", epoch(2026, 9, 2, 12, 0), 15),
            entry("old-gap", epoch(2026, 8, 30, 12, 0), 25),
        )

        val summary = FocusActivitySummaries.from(entries, now, zone)

        assertEquals(70, summary.todayMinutes)
        assertEquals(3, summary.currentStreakDays)
        assertEquals(4, summary.activeDays)
    }

    @Test
    fun `yesterday keeps streak alive until current day ends`() {
        val now = epoch(2026, 9, 4, 8, 0)
        val entries = listOf(
            entry("yesterday", epoch(2026, 9, 3, 23, 0), 25),
            entry("two-days", epoch(2026, 9, 2, 23, 0), 25),
        )

        val summary = FocusActivitySummaries.from(entries, now, zone)

        assertEquals(0, summary.todayMinutes)
        assertEquals(2, summary.currentStreakDays)
    }

    @Test
    fun `full inactive day resets streak`() {
        val now = epoch(2026, 9, 4, 8, 0)
        val entries = listOf(
            entry("two-days", epoch(2026, 9, 2, 23, 0), 25),
            entry("three-days", epoch(2026, 9, 1, 23, 0), 25),
        )

        val summary = FocusActivitySummaries.from(entries, now, zone)

        assertEquals(0, summary.currentStreakDays)
    }

    @Test
    fun `zero credited sessions do not count as active days`() {
        val now = epoch(2026, 9, 4, 12, 0)
        val entries = listOf(entry("zero", epoch(2026, 9, 4, 10, 0), 0))

        val summary = FocusActivitySummaries.from(entries, now, zone)

        assertEquals(0, summary.todayMinutes)
        assertEquals(0, summary.currentStreakDays)
        assertEquals(0, summary.activeDays)
    }

    private fun entry(id: String, completedAt: Long, minutes: Int) = SessionHistoryEntry(
        sessionId = id,
        completedAtEpochMillis = completedAt,
        plannedMinutes = 25,
        creditedMinutes = minutes,
        expedition = Expedition.TOWER,
        outcome = SessionOutcome.COMPLETED,
        damage = minutes,
        rarity = null,
        discovery = null,
    )

    private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
}
