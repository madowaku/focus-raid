package com.madowaku.focusraid.walking

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WalkingExpeditionTest {
    private val date = LocalDate.of(2026, 10, 24)
    private val now = Instant.parse("2026-10-24T05:00:00Z")
    private val tokyo = ZoneId.of("Asia/Tokyo")

    @Test fun thresholdsAreIndependentDailyDiscoveries() {
        listOf(0L to 0, 999L to 0, 1_000L to 1, 2_999L to 1, 3_000L to 2, 4_999L to 2, 5_000L to 3, 50_000L to 3).forEach { (steps, count) ->
            val journal = WalkingJournal().record(date, steps)
            assertEquals(count, journal.days.getValue(date).checkpoints.size)
            assertEquals(steps, journal.days.getValue(date).steps)
        }
    }

    @Test fun repeatedReadsAndDownwardCorrectionsNeverDuplicateOrRevokeDiscoveries() {
        var journal = WalkingJournal().record(date, 5_000)
        repeat(20) { journal = journal.record(date, 5_000) }
        journal = journal.record(date, 900)
        assertEquals(setOf(1_000, 3_000, 5_000), journal.days.getValue(date).checkpoints)
        assertEquals(900L, journal.days.getValue(date).steps)
        journal = journal.record(date, null)
        assertNull(journal.days.getValue(date).steps)
        assertEquals(3, journal.days.getValue(date).checkpoints.size)
    }

    @Test fun missingDataIsNotZeroAndDoesNotCreateDiscoveries() {
        val missing = WalkingJournal().record(date, null).days.getValue(date)
        assertNull(missing.steps)
        assertTrue(missing.checkpoints.isEmpty())
        assertEquals(0L, WalkingJournal().record(date, 0).days.getValue(date).steps)
    }

    @Test fun nextDayAndLateDataHaveSeparateStableKeys() {
        val journal = WalkingJournal().record(date, 999).record(date.plusDays(1), 3_000).record(date, 5_000)
        assertEquals(5, journal.days.values.sumOf { it.checkpoints.size })
        assertNotEquals(WalkingExpedition.discovery(date, 1_000), WalkingExpedition.discovery(date.plusDays(1), 1_000))
    }

    @Test fun catchUpIncludesYesterdayButNotBeforeOptInOrOverThirtyDays() {
        val journal = WalkingJournal().enable(now, tokyo)
        assertEquals(listOf(date, date.plusDays(1)), journal.datesToRead(now.plusSeconds(86_400)))
        val later = journal.datesToRead(now.plusSeconds(86_400 * 50))
        assertEquals(30, later.size)
        assertEquals(date.plusDays(21), later.first())
        assertEquals(date.plusDays(50), later.last())
        assertTrue(journal.datesToRead(now.minusSeconds(86_400)).isEmpty())
    }

    @Test fun timezoneChangeAndReenableKeepOriginalCalendarAndLedger() {
        val original = WalkingJournal().enable(now, tokyo).record(date, 5_000)
        val reopened = original.copy(enabled = false).enable(now.plusSeconds(86_400), ZoneId.of("America/Los_Angeles"))
        assertEquals("Asia/Tokyo", reopened.zone)
        assertEquals(original.firstDay, reopened.firstDay)
        assertEquals(original.days, reopened.days)
        assertEquals(date.plusDays(1), reopened.datesToRead(now.plusSeconds(86_400)).last())
    }

    @Test fun daylightSavingUsesRealCalendarBoundaries() {
        val zone = ZoneId.of("America/New_York")
        val spring = LocalDate.of(2026, 3, 8)
        val fall = LocalDate.of(2026, 11, 1)
        fun hours(day: LocalDate) = java.time.Duration.between(day.atStartOfDay(zone), day.plusDays(1).atStartOfDay(zone)).toHours()
        assertEquals(23, hours(spring))
        assertEquals(25, hours(fall))
    }

    @Test fun fakeIsExplicitAndCanRepresentUnavailableRevokedAndMissing() = runTest {
        val fake = FakeStepsRepository()
        assertNull(fake.read(now.minusSeconds(60), now))
        fake.permission = false
        assertFalse(fake.hasPermission())
        fake.status = StepsAvailability.UNAVAILABLE
        assertEquals(StepsAvailability.UNAVAILABLE, fake.availability())
    }
}
