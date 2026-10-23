package com.madowaku.focusraid.walking

import androidx.test.platform.app.InstrumentationRegistry
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class WalkingStorageTest {
    @Test fun concurrentReplaysAndReopeningKeepSingleDiscoveryPerCheckpoint() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storage = WalkingStorage(context)
        val now = Instant.parse("2026-10-24T05:00:00Z")
        val day = now.atZone(ZoneId.of("Asia/Tokyo")).toLocalDate()
        storage.update { WalkingJournal().enable(now, ZoneId.of("Asia/Tokyo")) }
        (1..10).map { async { storage.update { it.record(day, 5_000) } } }.awaitAll()
        val reopened = WalkingStorage(context).read()
        assertEquals(setOf(1_000, 3_000, 5_000), reopened.days.getValue(day).checkpoints)
        storage.update { it.record(day, null) }
        assertNull(WalkingStorage(context).read().days.getValue(day).steps)
        assertEquals(3, WalkingStorage(context).read().days.getValue(day).checkpoints.size)
        storage.update { WalkingJournal() }
        Unit
    }
}
