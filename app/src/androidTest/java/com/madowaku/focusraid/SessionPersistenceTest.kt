package com.madowaku.focusraid

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.madowaku.focusraid.core.model.*
import com.madowaku.focusraid.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SessionPersistenceTest {
    @Test fun journalRoomAndCreditAreReplaySafe() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SessionPreferences(context)
        store.setCompanion(com.madowaku.focusraid.core.domain.CompanionIdentity.LUNE)
        assertEquals(com.madowaku.focusraid.core.domain.CompanionIdentity.LUNE, store.session.first().companion)
        store.setCompanion(com.madowaku.focusraid.core.domain.CompanionIdentity.RAG)
        store.saveReady()
        val before = store.session.first().totalFocusMinutes
        val id = UUID.randomUUID().toString()
        val entry = SessionHistoryEntry(id, 1000, 25, 2, Expedition.TOWER, SessionOutcome.ABORTED, 2, Rarity.RARE, "古代の鍵")
        val db = FocusRaidDatabase.create(context)
        try {
            val history = RoomSessionHistoryRepository(db.focusSessionDao())
            store.saveRunning(25, Expedition.TOWER, System.currentTimeMillis() + 1_500_000, id)
            store.savePaused(1_374_751)
            assertEquals(1_374_751, store.session.first().pausedRemainingMillis)
            store.stageFinishedSession(entry)
            // Real DataStore codec roundtrip and independent repository reconstruction.
            assertEquals(entry, SessionPreferences(context).session.first().finishedEntry)
            coroutineScope { repeat(8) { launch { history.record(entry) } } }
            coroutineScope { repeat(8) { launch { store.commitFinishedSession(id, 2) } } }
            assertEquals(before + 2, store.session.first().totalFocusMinutes)
            assertEquals(1, history.recentSessions.first().count { it.sessionId == id })
            assertEquals(entry, history.recentSessions.first().single { it.sessionId == id })
            assertEquals(entry, store.session.first().finishedEntry)
            store.saveRunning(25, Expedition.ABYSS, 999_999, "new-$id")
            store.commitFinishedSession(id, 2) // Delayed old completion cannot clear the new timer.
            assertEquals("new-$id", store.session.first().sessionId)
            assertEquals(before + 2, store.session.first().totalFocusMinutes)
            store.saveReady()
            store.savePaused(42_123)
            store.ensureSessionIdentity("legacy-paused")
            assertEquals(SessionPhase.PAUSED, store.session.first().phase)
            assertEquals(42_123, store.session.first().pausedRemainingMillis)
            assertEquals(0, store.session.first().endEpochMillis)
        } finally { store.saveReady(); db.close() }
    }
}
