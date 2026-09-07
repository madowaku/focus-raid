package com.madowaku.focusraid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madowaku.focusraid.core.domain.CompanionEvolution
import com.madowaku.focusraid.core.domain.CompanionGrowth
import com.madowaku.focusraid.core.domain.FocusActivitySummaries
import com.madowaku.focusraid.core.domain.FocusRules
import com.madowaku.focusraid.core.domain.StarRoute
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Footprint
import com.madowaku.focusraid.core.model.FootprintPresets
import com.madowaku.focusraid.core.model.SessionHistoryEntry
import com.madowaku.focusraid.core.model.SessionOutcome
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.SessionReward
import com.madowaku.focusraid.core.model.WorldSnapshot
import com.madowaku.focusraid.data.SessionHistoryRepository
import com.madowaku.focusraid.data.SessionStore
import com.madowaku.focusraid.data.WorldRepository
import com.madowaku.focusraid.data.WorldSyncStatus
import com.madowaku.focusraid.timer.SessionAlarm
import java.util.UUID
import kotlin.math.floor
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class FocusUiState(
    val companion: com.madowaku.focusraid.core.domain.CompanionIdentity = com.madowaku.focusraid.core.domain.CompanionIdentity.RAG,
    val phase: SessionPhase = SessionPhase.READY,
    val initialized: Boolean = true,
    val saving: Boolean = false,
    val persistenceError: String? = null,
    val resultSessionId: String? = null,
    val footprintLoadError: String? = null,
    val selectedMinutes: Int = 25,
    val expedition: Expedition = Expedition.TOWER,
    val remainingSeconds: Int = 25 * 60,
    val durationSeconds: Int = 25 * 60,
    val reward: SessionReward? = null,
    val companionEvolution: CompanionEvolution? = null,
    val totalFocusMinutes: Int = 0,
    val todayFocusMinutes: Int = 0,
    val streakDays: Int = 0,
    val world: WorldSnapshot = WorldSnapshot(),
    val worldSyncStatus: WorldSyncStatus = WorldSyncStatus.LOCAL_PREVIEW,
    val systemAccessEducationSeen: Boolean = false,
    val footprints: List<Footprint> = emptyList(),
    val footprintsLoading: Boolean = false,
    val selectedFootprintPresetId: String? = null,
    val footprintPosting: Boolean = false,
    val footprintPosted: Boolean = false,
    val footprintPostError: String? = null,
    val sessionHistory: List<SessionHistoryEntry> = emptyList(),
) {
    val progress: Float
        get() = if (durationSeconds <= 0) 0f
        else (1f - remainingSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
}

class FocusViewModel(
    private val preferences: SessionStore,
    private val worldRepository: WorldRepository,
    private val sessionHistoryRepository: SessionHistoryRepository,
    private val alarmScheduler: SessionAlarm,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        FocusUiState(
            initialized = false,
            world = worldRepository.snapshot(),
            worldSyncStatus = worldRepository.syncStatus.value,
        ),
    )
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var endEpochMillis: Long = 0L
    private var activeSessionId: String? = null
    private var tickerJob: Job? = null
    private var pausedRemainingMillis = 0L
    private var retryCommand: (suspend () -> Unit)? = null
    private var footprintJob: Job? = null

    init {
        viewModelScope.launch {
            sessionHistoryRepository.recentSessions.collect { entries ->
                val activity = FocusActivitySummaries.from(
                    entries = entries,
                    nowEpochMillis = nowMillis(),
                )
                _uiState.value = _uiState.value.copy(
                    sessionHistory = entries,
                    todayFocusMinutes = activity.todayMinutes,
                    streakDays = activity.currentStreakDays,
                )
            }
        }
        viewModelScope.launch {
            worldRepository.syncStatus.collect { status ->
                _uiState.value = _uiState.value.copy(worldSyncStatus = status)
            }
        }
        viewModelScope.launch {
            worldRepository.world.collect { world ->
                if (_uiState.value.phase !in setOf(SessionPhase.RUNNING, SessionPhase.PAUSED)) {
                    _uiState.value = _uiState.value.copy(world = world)
                }
            }
        }
        transition {
            restore()
            viewModelScope.launch { refreshWorldIfQuiet() }
        }
    }

    fun selectCompanion(identity: com.madowaku.focusraid.core.domain.CompanionIdentity) {
        if (!canChange(SessionPhase.READY) || !com.madowaku.focusraid.core.domain.AdventureCollection.canSelect(identity, _uiState.value.totalFocusMinutes)) return
        transition {
            preferences.setCompanion(identity)
            _uiState.value = _uiState.value.copy(companion = identity)
        }
    }

    fun selectMinutes(minutes: Int) {
        if (!canChange(SessionPhase.READY)) return
        val safe = minutes.coerceIn(5, 180)
        transition {
            preferences.setSelectedMinutes(safe)
            _uiState.value = _uiState.value.copy(selectedMinutes = safe, remainingSeconds = safe * 60, durationSeconds = safe * 60)
        }
    }

    fun selectExpedition(expedition: Expedition) {
        if (!canChange(SessionPhase.READY)) return
        transition {
            preferences.setExpedition(expedition)
            _uiState.value = _uiState.value.copy(expedition = expedition)
        }
    }

    fun markSystemAccessEducationSeen() {
        viewModelScope.launch {
            try {
                preferences.markSystemAccessEducationSeen()
                _uiState.value = _uiState.value.copy(systemAccessEducationSeen = true)
            } catch (e: CancellationException) { throw e } catch (_: Exception) { /* Optional education only. */ }
        }
    }

    fun selectFootprintPreset(presetId: String) {
        val current = _uiState.value
        if (
            current.phase != SessionPhase.COMPLETED ||
            current.footprintPosted ||
            current.footprintPosting
        ) {
            return
        }
        if (FootprintPresets.byId(presetId) == null) return
        _uiState.value = current.copy(
            selectedFootprintPresetId = presetId,
            footprintPostError = null,
        )
    }

    fun leaveFootprint() {
        val current = _uiState.value
        if (current.phase != SessionPhase.COMPLETED || current.footprintPosted || current.footprintPosting) return
        val presetId = current.selectedFootprintPresetId ?: return
        val resultId = current.resultSessionId ?: return
        val expedition = current.expedition
        val checkpoint = checkpointFor(current)
        _uiState.value = current.copy(footprintPosting = true, footprintPostError = null)
        footprintJob?.cancel()
        footprintJob = viewModelScope.launch {
            val footprint = try {
                withTimeout(12_000) { worldRepository.leaveFootprint(expedition, checkpoint, presetId) }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) { null
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { null }
            if (!isCurrentResult(resultId)) return@launch
            val latest = _uiState.value
            _uiState.value = latest.copy(
                footprints = if (footprint == null) latest.footprints else (listOf(footprint) + latest.footprints).distinct().take(3),
                footprintsLoading = false,
                footprintPosting = false,
                footprintPosted = footprint != null,
                footprintPostError = if (footprint == null) "足跡を送信できませんでした。通信を確認してもう一度お試しください。" else null,
            )
        }
    }

    fun retryFootprints() {
        val state = _uiState.value
        if (state.phase == SessionPhase.COMPLETED && !state.footprintPosting) {
            loadNearbyFootprints(state.expedition, checkpointFor(state))
        }
    }

    fun start() {
        if (!canChange(SessionPhase.READY)) return
        val before = _uiState.value
        val sessionId = UUID.randomUUID().toString()
        transition { startPersisted(before, sessionId) }
    }

    private suspend fun startPersisted(before: FocusUiState, sessionId: String) {
        val seconds = before.selectedMinutes * 60
        val end = nowMillis() + seconds * 1000L
        preferences.saveRunning(before.selectedMinutes, before.expedition, end, sessionId)
        activeSessionId = sessionId
        endEpochMillis = end
        footprintJob?.cancel()
        _uiState.value = _uiState.value.copy(
            phase = SessionPhase.RUNNING, remainingSeconds = seconds, durationSeconds = seconds,
            reward = null, companionEvolution = null, resultSessionId = null, footprints = emptyList(),
            footprintsLoading = false, footprintLoadError = null, selectedFootprintPresetId = null,
            footprintPosting = false, footprintPosted = false, footprintPostError = null,
        )
        scheduleSafely(end)
        startTicker()
    }

    fun pause() {
        if (!canChange(SessionPhase.RUNNING)) return
        val remaining = (endEpochMillis - nowMillis()).coerceAtLeast(0)
        if (remaining == 0L) { complete(_uiState.value.selectedMinutes); return }
        transition {
            preferences.savePaused(remaining)
            pausedRemainingMillis = remaining
            alarmScheduler.cancel()
            tickerJob?.cancel()
            _uiState.value = _uiState.value.copy(phase = SessionPhase.PAUSED, remainingSeconds = secondsCeiling(remaining))
        }
    }

    fun resume() {
        if (!canChange(SessionPhase.PAUSED)) return
        val before = _uiState.value
        val id = checkNotNull(activeSessionId)
        transition {
            val end = nowMillis() + pausedRemainingMillis
            preferences.saveRunning(before.selectedMinutes, before.expedition, end, id)
            endEpochMillis = end
            _uiState.value = _uiState.value.copy(phase = SessionPhase.RUNNING)
            scheduleSafely(end)
            startTicker()
        }
    }

    fun finishEarly() {
        if (!canChange(SessionPhase.RUNNING, SessionPhase.PAUSED)) return
        val state = _uiState.value
        val remaining = if (state.phase == SessionPhase.PAUSED) pausedRemainingMillis else (endEpochMillis - nowMillis()).coerceAtLeast(0)
        if (remaining == 0L) { complete(state.selectedMinutes); return }
        val credited = ((state.durationSeconds * 1000L - remaining).coerceAtLeast(0) / 60_000).toInt()
        finishSession(credited, SessionPhase.ABORTED)
    }

    fun resetAfterResult() {
        if (!canChange(SessionPhase.COMPLETED, SessionPhase.ABORTED)) return
        transition {
            preferences.saveReady()
            resetResultState()
            viewModelScope.launch { refreshWorldIfQuiet() }
        }
    }

    fun startAgain() {
        if (!canChange(SessionPhase.COMPLETED)) return
        val before = _uiState.value
        val id = UUID.randomUUID().toString()
        transition { startPersisted(before, id) }
    }

    private fun resetResultState() {
        footprintJob?.cancel()
        val selected = _uiState.value.selectedMinutes
        _uiState.value = _uiState.value.copy(
            phase = SessionPhase.READY, remainingSeconds = selected * 60, durationSeconds = selected * 60,
            reward = null, companionEvolution = null, resultSessionId = null, footprints = emptyList(),
            footprintsLoading = false, footprintLoadError = null, selectedFootprintPresetId = null,
            footprintPosting = false, footprintPosted = false, footprintPostError = null,
        )
    }

    private suspend fun restore() {
        val saved = preferences.session.first()
        activeSessionId = saved.sessionId
        endEpochMillis = saved.endEpochMillis
        pausedRemainingMillis = saved.pausedRemainingMillis
        _uiState.value = _uiState.value.copy(
            initialized = true, companion = saved.companion, selectedMinutes = saved.selectedMinutes, expedition = saved.expedition,
            durationSeconds = saved.selectedMinutes * 60, remainingSeconds = saved.selectedMinutes * 60,
            totalFocusMinutes = saved.totalFocusMinutes, systemAccessEducationSeen = saved.systemAccessEducationSeen,
        )
        if (saved.finishedEntry != null) {
            reconcileFinished(saved.finishedEntry)
            return
        }
        when (saved.phase) {
            SessionPhase.RUNNING, SessionPhase.PAUSED -> {
                val id = saved.sessionId ?: "legacy-${saved.endEpochMillis}-${saved.pausedRemainingMillis}"
                activeSessionId = id
                if (saved.sessionId == null) {
                    preferences.ensureSessionIdentity(id)
                }
                // Upgrade recovery: older builds may have written Room before their credit commit.
                val recorded = sessionHistoryRepository.recentSessions.first().firstOrNull { it.sessionId == id }
                if (recorded != null) {
                    preferences.stageFinishedSession(recorded)
                    reconcileFinished(recorded)
                    return
                }
                val remaining = if (saved.phase == SessionPhase.RUNNING) (saved.endEpochMillis - nowMillis()).coerceAtLeast(0) else saved.pausedRemainingMillis
                _uiState.value = _uiState.value.copy(phase = saved.phase, remainingSeconds = secondsCeiling(remaining))
                if (remaining <= 0L) {
                    val entry = createEntry(saved.selectedMinutes, SessionPhase.COMPLETED)
                    preferences.stageFinishedSession(entry)
                    reconcileFinished(entry)
                } else if (saved.phase == SessionPhase.RUNNING) {
                    scheduleSafely(saved.endEpochMillis)
                    startTicker()
                }
            }
            else -> _uiState.value = _uiState.value.copy(phase = SessionPhase.READY)
        }
    }

    private suspend fun refreshWorldIfQuiet() {
        if (_uiState.value.phase in setOf(SessionPhase.RUNNING, SessionPhase.PAUSED)) return
        try { withTimeout(12_000) { worldRepository.refresh() } }
        catch (_: kotlinx.coroutines.TimeoutCancellationException) { }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { /* Shared world availability cannot block local focus. */ }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (_uiState.value.phase == SessionPhase.RUNNING) {
                val remainingMillis = endEpochMillis - nowMillis()
                if (remainingMillis <= 0L) {
                    _uiState.value = _uiState.value.copy(remainingSeconds = 0)
                    complete(_uiState.value.selectedMinutes)
                    delay(250L)
                    continue
                }
                _uiState.value = _uiState.value.copy(
                    remainingSeconds = ((remainingMillis + 999L) / 1000L).toInt(),
                )
                delay(250L)
            }
        }
    }

    private fun complete(creditedMinutes: Int) {
        if (!canChange(SessionPhase.RUNNING, SessionPhase.PAUSED)) return
        finishSession(creditedMinutes, SessionPhase.COMPLETED)
    }

    private fun createEntry(creditedMinutes: Int, phase: SessionPhase): SessionHistoryEntry {
        val before = _uiState.value
        val reward = FocusRules.resolveSession(creditedMinutes, before.expedition, before.totalFocusMinutes % 25)
        return SessionHistoryEntry(
            checkNotNull(activeSessionId), nowMillis(), before.selectedMinutes, reward.creditedMinutes,
            before.expedition, if (phase == SessionPhase.COMPLETED) SessionOutcome.COMPLETED else SessionOutcome.ABORTED,
            reward.personalDamage, reward.rarity, reward.discovery,
        )
    }

    private fun finishSession(creditedMinutes: Int, phase: SessionPhase) {
        val entry = createEntry(creditedMinutes, phase)
        transition {
            preferences.stageFinishedSession(entry)
            reconcileFinished(checkNotNull(preferences.session.first().finishedEntry))
        }
    }

    private suspend fun reconcileFinished(entry: SessionHistoryEntry) {
        // Room's primary key ignores replays. The journal freezes aborted minutes and random loot.
        if (entry.creditedMinutes > 0) sessionHistoryRepository.record(entry)
        preferences.commitFinishedSession(entry.sessionId, entry.creditedMinutes)
        val saved = preferences.session.first()
        val total = saved.totalFocusMinutes
        val beforeTotal = (total - entry.creditedMinutes).coerceAtLeast(0)
        val phase = if (entry.outcome == SessionOutcome.COMPLETED) SessionPhase.COMPLETED else SessionPhase.ABORTED
        val reward = SessionReward(entry.creditedMinutes, entry.damage, entry.creditedMinutes, entry.creditedMinutes / 25,
            entry.rarity, entry.discovery, when (entry.rarity) {
                com.madowaku.focusraid.core.model.Rarity.COMMON -> 1
                com.madowaku.focusraid.core.model.Rarity.RARE -> 2
                com.madowaku.focusraid.core.model.Rarity.EPIC -> 5
                com.madowaku.focusraid.core.model.Rarity.LEGENDARY -> 10
                null -> 0
            })
        tickerJob?.cancel()
        alarmScheduler.cancel()
        activeSessionId = null
        _uiState.value = _uiState.value.copy(
            phase = phase, remainingSeconds = 0, reward = reward, totalFocusMinutes = total,
            companionEvolution = CompanionGrowth.evolutionBetween(beforeTotal, total),
            resultSessionId = entry.sessionId, footprintLoadError = null,
        )
        if (phase == SessionPhase.COMPLETED && (entry.expedition != Expedition.STAR_ROUTE ||
                StarRoute.reachedCheckpoint(total) > StarRoute.reachedCheckpoint(beforeTotal))) {
            loadNearbyFootprints(entry.expedition, checkpointFor(_uiState.value))
        }
    }

    private fun isCurrentResult(id: String) = _uiState.value.phase == SessionPhase.COMPLETED && _uiState.value.resultSessionId == id

    private fun loadNearbyFootprints(expedition: Expedition, checkpoint: Int) {
        val id = _uiState.value.resultSessionId ?: return
        _uiState.value = _uiState.value.copy(footprintsLoading = true, footprintLoadError = null)
        footprintJob?.cancel()
        footprintJob = viewModelScope.launch {
            val nearby = try {
                withTimeout(12_000) { worldRepository.footprints(expedition, checkpoint) }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) { null
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { null }
            if (!isCurrentResult(id)) return@launch
            _uiState.value = _uiState.value.copy(
                footprints = nearby ?: emptyList(), footprintsLoading = false,
                footprintLoadError = if (nearby == null) "足跡を読み込めませんでした。通信を確認して再試行できます。" else null,
            )
        }
    }

    private fun canChange(vararg phases: SessionPhase): Boolean = _uiState.value.let {
        it.initialized && !it.saving && it.persistenceError == null && it.phase in phases
    }

    fun retryPersistence() {
        if (_uiState.value.saving) return
        retryCommand?.let { transition(it) }
    }

    private fun transition(block: suspend () -> Unit) {
        if (_uiState.value.saving) return
        _uiState.value = _uiState.value.copy(saving = true, persistenceError = null)
        retryCommand = block
        viewModelScope.launch {
            try {
                block()
                retryCommand = null
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(persistenceError = "端末に記録を保存できませんでした。空き容量を確認して再試行してください。")
            } finally {
                _uiState.value = _uiState.value.copy(saving = false)
            }
        }
    }

    private fun scheduleSafely(end: Long) {
        // Timer recovery remains authoritative even on devices that deny alarm scheduling.
        runCatching { alarmScheduler.schedule(end) }
    }

    private fun secondsCeiling(millis: Long) = ((millis.coerceAtLeast(0) + 999) / 1000).toInt()

    private fun checkpointFor(state: FocusUiState): Int = when (state.expedition) {
        Expedition.TOWER -> state.world.towerFloor
        Expedition.ABYSS -> state.world.abyssDepth
        Expedition.STAR_ROUTE -> StarRoute.reachedCheckpoint(state.totalFocusMinutes)
    }

    class Factory(
        private val preferences: SessionStore,
        private val worldRepository: WorldRepository,
        private val sessionHistoryRepository: SessionHistoryRepository,
        private val alarmScheduler: SessionAlarm,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(
                preferences = preferences,
                worldRepository = worldRepository,
                sessionHistoryRepository = sessionHistoryRepository,
                alarmScheduler = alarmScheduler,
            ) as T
        }
    }
}
