package com.madowaku.focusraid

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.madowaku.focusraid.data.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.Assert.*
import java.util.UUID

/** Opt-in real SDK/Room/callable path. The runner must seed a demo-only world first. */
class WorldwideEmulatorTest {
    @Test fun twoAnonymousInstallationsConvergeAndLostResponseReplaysFromRoom() = runBlocking {
        assumeTrue("Requires Firebase emulators", InstrumentationRegistry.getArguments().getString("worldEmulator") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val project = "demo-focus-raid"
        val apps = mutableListOf<FirebaseApp>()
        fun repository(): FirebaseWorldRepository {
            val app = FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
                .setProjectId(project).setApplicationId("1:123456789:android:emulator")
                .setApiKey("AIza" + "0".repeat(35)).build(), "qa-${UUID.randomUUID()}")
            apps += app
            val auth = FirebaseAuth.getInstance(app).apply { useEmulator("10.0.2.2", 9099) }
            val firestore = FirebaseFirestore.getInstance(app).apply { useEmulator("10.0.2.2", 8080) }
            val functions = FirebaseFunctions.getInstance(app, "us-central1").apply { useEmulator("10.0.2.2", 5001) }
            return FirebaseWorldRepository(auth, firestore, functions,
                context.getSharedPreferences(app.name, 0))
        }
        val a = repository(); val b = repository()
        val name = "emulator-outbox-${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, FocusRaidDatabase::class.java, name).build()
        try {
            a.refresh(); b.refresh()
            assertEquals(WorldSyncStatus.LIVE, a.syncStatus.value)
            assertEquals(a.world.value, b.world.value)
            val baseline = a.world.value
            val now = System.currentTimeMillis()
            val row = WorldContribution(UUID.randomUUID().toString(), null, 5, 5, now,
                startedAtEpochMillis = now - 300_000)
            db.contributionDao().insert(row)
            assertTrue(ContributionDelivery(db.contributionDao()) { error("offline transport") }.drain())
            db.close(); db = Room.databaseBuilder(context, FocusRaidDatabase::class.java, name).build()
            // Use the actual callable, then deliberately lose its successful response.
            var serverReply: ContributionReceipt? = null
            var transportFailure: Throwable? = null
            assertTrue(ContributionDelivery(db.contributionDao()) {
                try { serverReply = a.submit(it) } catch (e: Throwable) { transportFailure = e; throw e }
                error("response lost")
            }.drain())
            assertNull("Callable must actually complete before simulating response loss: $transportFailure", transportFailure)
            assertEquals(ContributionStatus.ACCEPTED, serverReply?.status)
            b.refresh()
            assertEquals(baseline.totalFocusMinutes + 5, b.world.value.totalFocusMinutes)
            assertFalse(ContributionDelivery(db.contributionDao(), a).drain())
            assertEquals("ALREADY_COUNTED", db.contributionDao().observeAll().first().single().status)
            val receiptB = b.submit(row.copy(sessionId = UUID.randomUUID().toString(), generation = baseline.generation))
            assertEquals(ContributionStatus.ACCEPTED, receiptB.status)
            a.refresh(); b.refresh()
            assertEquals(a.world.value, b.world.value)
            assertEquals(baseline.totalFocusMinutes + 10, a.world.value.totalFocusMinutes)
            assertEquals(baseline.bossHp - 10, a.world.value.bossHp)
            assertEquals(baseline.raidParticipants + 2, a.world.value.raidParticipants)
        } finally {
            db.close(); context.deleteDatabase(name)
            apps.forEach { FirebaseFirestore.getInstance(it).terminate(); it.delete() }
        }
    }
}
