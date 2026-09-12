@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
package com.madowaku.focusraid.ui

import androidx.lifecycle.ViewModelStore
import com.madowaku.focusraid.core.model.*
import com.madowaku.focusraid.data.*
import com.madowaku.focusraid.timer.SessionAlarm
import com.madowaku.focusraid.audio.Sfx
import com.madowaku.focusraid.audio.SfxPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class FocusViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val models = mutableListOf<ViewModelStore>()
    private var now = 1_000_000L
    private val store = MemorySessionStore()
    private val history = MemoryHistory()
    private val sounds = mutableListOf<Sfx>()
    private val audio = object : SfxPlayer {
        override fun play(sound: Sfx, eventId: String?, variant: Int, volume: Float, pitch: Float) {
            sounds += sound
        }
    }
    private val alarm = object : SessionAlarm {
        var end = 0L
        override fun schedule(endEpochMillis: Long) { end = endEpochMillis }
        override fun cancel() { end = 0 }
    }
    private fun scenario(block: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { block() } finally { models.forEach { it.clear() } }
    }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun teardown() { models.forEach { it.clear() }; Dispatchers.resetMain() }
    private fun vm(world: WorldRepository = FakeWorldRepository()): FocusViewModel =
        FocusViewModel(store, world, history, alarm, { now }, sfx = audio).also {
            models += ViewModelStore().apply { put("focus", it) }
        }

    @Test fun `audio start and completion happen once even with repeated commands and collectors`() = scenario {
        val model = vm(); runCurrent()
        model.start(); model.start(); runCurrent()
        assertEquals(listOf(Sfx.FOCUS_START), sounds)
        model.uiState.first(); model.uiState.first()
        now += 1_500_000; advanceTimeBy(250); runCurrent()
        model.finishEarly(); model.uiState.first(); advanceTimeBy(1_000); runCurrent()
        assertEquals(listOf(Sfx.FOCUS_START, Sfx.FOCUS_COMPLETE), sounds)
        models.first().clear()
        vm(); runCurrent()
        assertEquals(1, sounds.count { it == Sfx.FOCUS_COMPLETE })
    }

    @Test fun `aborted session never plays completion and restoration never plays start`() = scenario {
        val model = vm(); runCurrent(); model.start(); runCurrent()
        now += 60_000; model.finishEarly(); runCurrent()
        assertEquals(listOf(Sfx.FOCUS_START), sounds)
        models.first().clear(); vm(); runCurrent()
        assertEquals(listOf(Sfx.FOCUS_START), sounds)
    }

    @Test fun `raid audio is staggered and duplicate strike cannot replay it`() = scenario {
        val model = vm(); runCurrent(); model.start(); runCurrent()
        now += 1_500_000; advanceTimeBy(250); runCurrent(); sounds.clear()
        model.raidStrike(3); model.raidStrike(3); runCurrent()
        assertEquals(listOf(Sfx.RAID_HIT_SELF), sounds)
        advanceTimeBy(519); runCurrent(); assertEquals(1, sounds.size)
        advanceTimeBy(1_300); runCurrent()
        assertEquals(3, sounds.count { it == Sfx.RAID_HIT_OTHER })
        assertEquals(1, sounds.count { it == Sfx.RAID_HIT_SELF })
    }

    @Test fun `raid with no other contributions has no imaginary other hits`() = scenario {
        val model = vm(); runCurrent(); model.start(); runCurrent()
        now += 1_500_000; advanceTimeBy(250); runCurrent(); sounds.clear()
        model.raidStrike(0); advanceTimeBy(2_000); runCurrent()
        assertEquals(listOf(Sfx.RAID_HIT_SELF), sounds)
    }

    @Test fun `raid victory audio is claimed once per completed result`() = scenario {
        val model = vm(); runCurrent(); model.start(); runCurrent()
        now += 1_500_000; advanceTimeBy(250); runCurrent(); sounds.clear()
        model.raidVictory(); model.raidVictory(); runCurrent()
        assertEquals(listOf(Sfx.RAID_VICTORY), sounds)
    }

    @Test fun `result confirmation sound is one shot under rapid repeated CTA taps`() = scenario {
        val model = vm(); runCurrent(); model.start(); runCurrent()
        now += 60_000; model.finishEarly(); runCurrent(); sounds.clear()

        model.confirmResultAndReset()
        model.confirmResultAndReset()
        runCurrent()

        assertEquals(listOf(Sfx.UI_CONFIRM), sounds)
        assertEquals(SessionPhase.READY, model.uiState.value.phase)
    }

    @Test fun `generation and start time freeze across pause process restart and world rotation`() = scenario {
        val snapshot = MutableStateFlow(WorldSnapshot(generation = "N"))
        val world = object : WorldRepository by FakeWorldRepository() { override val world = snapshot }
        val first = vm(world); runCurrent(); first.start(); runCurrent()
        assertEquals("N", store.value.raidGeneration)
        val started = now
        now += 60_000; first.pause(); runCurrent()
        models.first().clear()
        snapshot.value = snapshot.value.copy(generation = "N-plus-1")
        val restored = vm(world); runCurrent(); restored.resume(); runCurrent()
        now += 1_440_000; advanceTimeBy(250); runCurrent()
        val entry = history.rows.value.single()
        assertEquals("N", entry.raidGeneration)
        assertEquals(started, entry.startedAtEpochMillis)
        assertEquals(25, entry.creditedMinutes)
    }

    @Test fun `first offline completion preserves unbound identity and start time for safe server resolution`() = scenario {
        val first = vm(); runCurrent(); first.start(); runCurrent()
        val started = now
        models.first().clear()
        val restored = vm(); runCurrent()
        now += 1_500_000; advanceTimeBy(250); runCurrent()
        val entry = history.rows.value.single()
        assertNull(entry.raidGeneration)
        assertEquals(started, entry.startedAtEpochMillis)
        assertEquals(SessionPhase.COMPLETED, restored.uiState.value.phase)
        assertEquals(25, store.value.totalFocusMinutes)
    }

    @Test fun `upgrade recovers previously written abort row instead of rerolling full completion`() = scenario {
        val row = SessionHistoryEntry("old", now - 1000, 25, 2, Expedition.TOWER, SessionOutcome.ABORTED, 2, null, null)
        history.record(row)
        store.session.value = PersistedSession(phase = SessionPhase.RUNNING, sessionId = "old", endEpochMillis = now - 1)
        val vm = vm(); runCurrent()
        assertEquals(SessionPhase.ABORTED, vm.uiState.value.phase)
        assertEquals(2, vm.uiState.value.totalFocusMinutes)
        assertEquals(row, history.rows.value.single())
    }

    @Test fun `companion selection honors unlock and survives recreation`() = scenario {
        val vm = vm(); runCurrent()
        vm.selectCompanion(com.madowaku.focusraid.core.domain.CompanionIdentity.MIKO); runCurrent()
        assertEquals(com.madowaku.focusraid.core.domain.CompanionIdentity.RAG, store.value.companion)
        models.first().clear()
        store.session.value = store.value.copy(totalFocusMinutes = 75)
        val unlocked = vm(); runCurrent()
        unlocked.selectCompanion(com.madowaku.focusraid.core.domain.CompanionIdentity.MIKO); runCurrent()
        models.last().clear()
        val restored = vm(); runCurrent()
        assertEquals(com.madowaku.focusraid.core.domain.CompanionIdentity.MIKO, restored.uiState.value.companion)
        assertEquals(75, restored.uiState.value.totalFocusMinutes)
    }

    @Test fun `legacy paused identity migration never passes through running`() = scenario {
        store.session.value = PersistedSession(phase = SessionPhase.PAUSED, pausedRemainingMillis = 73_251)
        val vm = vm(); runCurrent()
        assertEquals(SessionPhase.PAUSED, store.value.phase)
        assertEquals(73_251, store.value.pausedRemainingMillis)
        assertEquals(0, store.value.totalFocusMinutes)
        vm.resume(); runCurrent()
        assertEquals(now + 73_251, store.value.endEpochMillis)
    }

    @Test fun `start is not acknowledged and alarm is not scheduled until durable write`() = scenario {
        val vm = vm(); runCurrent()
        val gate = CompletableDeferred<Unit>(); store.beforeRunning = { gate.await() }
        vm.start(); runCurrent()
        assertTrue(vm.uiState.value.saving)
        assertEquals(SessionPhase.READY, vm.uiState.value.phase)
        assertEquals(0L, alarm.end)
        vm.start() // double tap must not enqueue another start
        gate.complete(Unit); runCurrent()
        assertEquals(SessionPhase.RUNNING, vm.uiState.value.phase)
        assertEquals(store.value.endEpochMillis, alarm.end)
        assertNotNull(store.value.sessionId)
        models.first().clear()
        val recovered = vm(); runCurrent()
        assertEquals(SessionPhase.RUNNING, recovered.uiState.value.phase)
        assertEquals(1500, recovered.uiState.value.remainingSeconds)
    }

    @Test fun `pause and process death preserve subsecond duration without accumulated rounding`() = scenario {
        val vm = vm(); runCurrent(); vm.start(); runCurrent()
        now += 1250; vm.pause(); runCurrent()
        assertEquals(1_498_750L, store.value.pausedRemainingMillis)
        models.first().clear()
        val restored = vm(); runCurrent(); now += 50_000
        restored.resume(); runCurrent()
        assertEquals(now + 1_498_750, store.value.endEpochMillis)
        repeat(5) { now += 125; restored.pause(); runCurrent(); restored.resume(); runCurrent() }
        assertEquals(now + 1_498_125, store.value.endEpochMillis)
    }

    @Test fun `abort journal replays exact minutes and loot after Room failure and process death`() = scenario {
        val vm = vm(); runCurrent(); vm.start(); runCurrent(); now += 125_001
        history.fail = true
        vm.finishEarly(); runCurrent()
        val frozen = checkNotNull(store.value.finishedEntry)
        assertEquals(2, frozen.creditedMinutes)
        assertEquals(SessionOutcome.ABORTED, frozen.outcome)
        assertNotNull(vm.uiState.value.persistenceError)
        assertNotEquals(SessionPhase.ABORTED, vm.uiState.value.phase)
        models.first().clear(); now += 9_000_000; history.fail = false
        val restored = vm(); runCurrent()
        assertEquals(SessionPhase.ABORTED, restored.uiState.value.phase)
        assertEquals(frozen, history.rows.value.single())
        assertEquals(2, store.value.totalFocusMinutes)
        models.last().clear()
        val again = vm(); runCurrent()
        assertEquals(SessionPhase.ABORTED, again.uiState.value.phase)
        assertEquals(2, again.uiState.value.totalFocusMinutes)
        assertEquals(1, history.rows.value.size)
    }

    @Test fun `crash after Room insertion before credit reconciles once`() = scenario {
        val vm = vm(); runCurrent(); vm.start(); runCurrent()
        store.failCommit = true; now += 1_500_000
        advanceTimeBy(250); runCurrent()
        assertEquals(1, history.rows.value.size)
        assertEquals(0, store.value.totalFocusMinutes)
        models.first().clear(); store.failCommit = false
        val restored = vm(); runCurrent()
        assertEquals(25, restored.uiState.value.totalFocusMinutes)
        assertEquals(1, history.rows.value.size)
        restored.finishEarly(); restored.pause(); runCurrent()
        assertEquals(25, store.value.totalFocusMinutes)
    }

    @Test fun `expiry races with pause and early exit give a single completed reward`() = scenario {
        val vm = vm(); runCurrent(); vm.start(); runCurrent(); now += 1_500_000
        vm.pause(); vm.finishEarly(); advanceTimeBy(250); runCurrent()
        assertEquals(SessionPhase.COMPLETED, vm.uiState.value.phase)
        assertEquals(25, store.value.totalFocusMinutes)
        assertEquals(SessionOutcome.COMPLETED, history.rows.value.single().outcome)
        models.first().clear(); val restored = vm(); runCurrent()
        assertEquals(25, restored.uiState.value.totalFocusMinutes)
        restored.resetAfterResult(); runCurrent()
        assertNull(store.value.finishedEntry)
    }

    @Test fun `storage failure can retry without duplicate result or new identity`() = scenario {
        val vm = vm(); runCurrent(); vm.start(); runCurrent(); now += 60_500
        store.failCommit = true; vm.finishEarly(); runCurrent()
        val id = history.rows.value.single().sessionId
        store.failCommit = false; vm.retryPersistence(); runCurrent()
        assertNull(vm.uiState.value.persistenceError)
        assertEquals(id, vm.uiState.value.resultSessionId)
        assertEquals(1, store.value.totalFocusMinutes)
        assertEquals(1, history.rows.value.size)
    }

    @Test fun `late footprint write cannot resurrect victory over a new running session`() = scenario {
        val response = CompletableDeferred<Footprint?>()
        val world = object : WorldRepository by FakeWorldRepository() {
            override suspend fun leaveFootprint(expedition: Expedition, checkpoint: Int, presetId: String): Footprint? =
                withContext(NonCancellable) { response.await() }
        }
        val vm = vm(world); runCurrent(); vm.start(); runCurrent(); now += 1_500_000
        advanceTimeBy(250); runCurrent()
        vm.selectFootprintPreset("made_it"); vm.leaveFootprint(); runCurrent()
        assertTrue(vm.uiState.value.footprintPosting)
        vm.startAgain(); runCurrent()
        response.complete(Footprint(Expedition.TOWER, 4281, "made_it", "x", "late", "now")); runCurrent()
        assertEquals(SessionPhase.RUNNING, vm.uiState.value.phase)
        assertFalse(vm.uiState.value.footprintPosted)
        assertEquals(25, vm.uiState.value.totalFocusMinutes)
    }

    @Test fun `footprint failure is explicit and retryable without changing local completion`() = scenario {
        var offline = true
        val world = object : WorldRepository by FakeWorldRepository() {
            override suspend fun footprints(expedition: Expedition, checkpoint: Int, limit: Int): List<Footprint> {
                if (offline) error("permission denied private details")
                return emptyList()
            }
        }
        val vm = vm(world); runCurrent(); vm.start(); runCurrent(); now += 1_500_000
        advanceTimeBy(250); runCurrent()
        assertNotNull(vm.uiState.value.footprintLoadError)
        assertFalse(vm.uiState.value.footprintLoadError!!.contains("permission"))
        offline = false; vm.retryFootprints(); runCurrent()
        assertNull(vm.uiState.value.footprintLoadError)
        assertEquals(25, store.value.totalFocusMinutes)
    }
}

private class MemoryHistory : SessionHistoryRepository {
    val rows = MutableStateFlow<List<SessionHistoryEntry>>(emptyList())
    var fail = false
    override val recentSessions = rows
    override suspend fun record(entry: SessionHistoryEntry) {
        if (fail) error("disk full")
        if (rows.value.none { it.sessionId == entry.sessionId }) rows.value += entry
    }
}

private class MemorySessionStore : SessionStore {
    override val session = MutableStateFlow(PersistedSession())
    val value get() = session.value
    var beforeRunning: suspend () -> Unit = {}
    var failCommit = false
    override suspend fun ensureSessionIdentity(sessionId: String) { if (value.sessionId == null) session.value = value.copy(sessionId = sessionId) }
    override suspend fun setCompanion(identity: com.madowaku.focusraid.core.domain.CompanionIdentity) { session.value = value.copy(companion = identity) }
    override suspend fun setSelectedMinutes(minutes: Int) { session.value = value.copy(selectedMinutes = minutes) }
    override suspend fun setExpedition(expedition: Expedition) { session.value = value.copy(expedition = expedition) }
    override suspend fun saveRunning(minutes: Int, expedition: Expedition, endEpochMillis: Long, sessionId: String, raidGeneration: String?, startedAtEpochMillis: Long) {
        beforeRunning()
        session.value = value.copy(selectedMinutes = minutes, expedition = expedition, endEpochMillis = endEpochMillis,
            raidGeneration = if (value.sessionId == sessionId) value.raidGeneration else raidGeneration,
            startedAtEpochMillis = if (value.sessionId == sessionId) value.startedAtEpochMillis else startedAtEpochMillis,
            phase = SessionPhase.RUNNING, sessionId = sessionId, finishedEntry = null)
    }
    override suspend fun savePaused(remainingMillis: Long) {
        session.value = value.copy(phase = SessionPhase.PAUSED, endEpochMillis = 0, pausedRemainingMillis = remainingMillis)
    }
    override suspend fun saveReady() { session.value = value.copy(phase = SessionPhase.READY, sessionId = null, finishedEntry = null) }
    override suspend fun stageFinishedSession(entry: SessionHistoryEntry) {
        check(value.sessionId == entry.sessionId)
        if (value.finishedEntry == null) session.value = value.copy(finishedEntry = entry)
    }
    override suspend fun commitFinishedSession(sessionId: String, creditedMinutes: Int) {
        if (failCommit) error("disk full")
        if (value.sessionId != sessionId) return
        session.value = value.copy(phase = SessionPhase.READY, sessionId = null, totalFocusMinutes = value.totalFocusMinutes + creditedMinutes)
    }
    override suspend fun markSystemAccessEducationSeen() { session.value = value.copy(systemAccessEducationSeen = true) }
}
