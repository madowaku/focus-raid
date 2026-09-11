package com.madowaku.focusraid

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.madowaku.focusraid.core.model.*
import com.madowaku.focusraid.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import java.util.UUID

class ContributionPersistenceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun migrateV1PreservesHistoryAndAddsDurableOutbox() = runBlocking {
        val name = "migration-${UUID.randomUUID()}.db"
        val old = context.openOrCreateDatabase(name, 0, null)
        old.execSQL("CREATE TABLE focus_sessions (sessionId TEXT NOT NULL PRIMARY KEY, completedAtEpochMillis INTEGER NOT NULL, plannedMinutes INTEGER NOT NULL, creditedMinutes INTEGER NOT NULL, expedition TEXT NOT NULL, outcome TEXT NOT NULL, damage INTEGER NOT NULL, rarity TEXT, discovery TEXT)")
        old.execSQL("INSERT INTO focus_sessions VALUES ('old', 100, 25, 25, 'TOWER', 'COMPLETED', 25, NULL, NULL)")
        old.version = 1; old.close()
        val db = Room.databaseBuilder(context, FocusRaidDatabase::class.java, name)
            .addMigrations(FocusRaidDatabase.MIGRATION_1_2).build()
        try {
            assertEquals("old", db.focusSessionDao().observeAll().first().single().sessionId)
            assertTrue(db.contributionDao().observeAll().first().isEmpty()) // no historical backfill
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun journalAtomicOutboxAndOfflineReplaySurviveRepositoryRecreation() = runBlocking {
        val name = "queue-${UUID.randomUUID()}.db"
        val id = UUID.randomUUID().toString()
        val entry = SessionHistoryEntry(id, 2_000_000, 25, 25, Expedition.TOWER, SessionOutcome.COMPLETED,
            25, null, null, null, 500_000)
        val store = SessionPreferences(context)
        store.saveReady()
        store.saveRunning(25, Expedition.TOWER, 2_000_000, id, null, 500_000)
        store.savePaused(1_000_000)
        store.saveRunning(25, Expedition.TOWER, 3_000_000, id, "must-not-rebind", 600_000)
        assertNull(store.session.first().raidGeneration)
        assertEquals(500_000, store.session.first().startedAtEpochMillis)
        store.stageFinishedSession(entry)
        assertEquals(entry, SessionPreferences(context).session.first().finishedEntry)
        var db = Room.databaseBuilder(context, FocusRaidDatabase::class.java, name).build()
        try {
            val history = RoomSessionHistoryRepository(db.focusSessionDao()) { error("enqueue unavailable") }
            coroutineScope { repeat(8) { launch { history.record(entry) } } }
            assertEquals(1, db.focusSessionDao().observeAll().first().size)
            assertEquals("PENDING", db.contributionDao().observeAll().first().single().status)
            assertTrue(ContributionDelivery(db.contributionDao()) { error("offline") }.drain())
            db.close()
            db = Room.databaseBuilder(context, FocusRaidDatabase::class.java, name).build()
            assertEquals("OFFLINE", db.contributionDao().observeAll().first().single().status)
            assertFalse(ContributionDelivery(db.contributionDao()) {
                assertEquals(id, it.sessionId); assertNull(it.generation)
                ContributionReceipt(ContributionStatus.ACCEPTED, 25)
            }.drain())
            RoomSessionHistoryRepository(db.focusSessionDao()).record(entry)
            assertEquals("ACCEPTED", db.contributionDao().observeAll().first().single().status)
            val abort = entry.copy(sessionId = "abort", outcome = SessionOutcome.ABORTED, creditedMinutes = 2)
            RoomSessionHistoryRepository(db.focusSessionDao()).record(abort)
            assertEquals(1, db.contributionDao().observeAll().first().size)
        } finally { db.close(); store.saveReady(); context.deleteDatabase(name) }
    }
}
