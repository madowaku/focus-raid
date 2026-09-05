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
import com.madowaku.focusraid.data.SessionPreferences
import com.madowaku.focusraid.data.WorldRepository
import com.madowaku.focusraid.data.WorldSyncStatus
import com.madowaku.focusraid.timer.FocusAlarmScheduler
import java.util.UUID
import kotlin.math.floor
import kotlin.math.max
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class FocusUiState(
    val phase: SessionPhase = SessionPhase.READY,
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
    val selectedFootprintPresetId: String? = null,
    val footprintPosted: Boolean = false,
    val sessionHistory: List<SessionHistoryEntry> = emptyList(),
) {
    val progress: Float
        get() = if (durationSeconds <= 0) 0f
        else (1f - remainingSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
}

class FocusViewModel(
    private val preferences: SessionPreferences,
    private val worldRepository: WorldRepository,
    private val sessionHistoryRepository: SessionHistoryRepository,
    private val alarmScheduler: FocusAlarmScheduler,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        FocusUiState(
            world = worldRepository.snapshot(),
            worldSyncStatus = worldRepository.syncStatus.value,
        ),
    )
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var endEpochMillis: Long = 0L
    private var activeSessionId: String? = null
    private var tickerJob: Job? = null
    private var sessionPersistenceJob: Job? = null

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
        viewModelScope.launch {
            restore()
            refreshWorldIfQuiet()
        }
    }

    fun selectMinutes(minutes: Int) {
        if (_uiState.value.phase != SessionPhase.READY) return
        val safe = minutes.coerceIn(5, 180)
        _uiState.value = _uiState.value.copy(
            selectedMinutes = safe,
            remainingSeconds = safe * 60,
            durationSeconds = safe * 60,
        )
        viewModelScope.launch { preferences.setSelectedMinutes(safe) }
    }

    fun selectExpedition(expedition: Expedition) {
        if (_uiState.value.phase != SessionPhase.READY) return
        _uiState.value = _uiState.value.copy(expedition = expedition)
        viewModelScope.launch { preferences.setExpedition(expedition) }
    }

    fun markSystemAccessEducationSeen() {
        if (_uiState.value.systemAccessEducationSeen) return
        _uiState.value = _uiState.value.copy(systemAccessEducationSeen = true
        )
        viewModelScope.launch { preferences.markSystemAccessEducationSeen() }
    }

    fun selectFootprintPreset(presetId: String) {
        val current = _uiState.value
        if (current.phase != SessionPhase.COMPLETED || current.footprintPosted) return
        if (FootprintPresets.byId(presetId) == null) return
        _uiState.value = current.copy(selectedFootprintPresetId = presetId)
    }

    fun leaveFootprint() {
        val current = _uiState.value
        if (current.phase != SessionPhase.COMPLETED || current.footprintPosted) return
        val presetId = current.selectedFootprintPresetId ?: return
        val footprint = worldRepository.leaveFootprint(
            expedition = current.expedition,
            checkpoint = checkpointFor(current),
            presetId = presetId,
        ) ?: return

        _uiState.value = current.copy(
            footprints = (listOf(footprint) + current.footprints).take(3),
            footprintPosted = true,
        )
    }

    fun start() {
        if (_uiState.value.phase != SessionPhase.READY) return
        val seconds = _uiState.value.selectedMinutes * 60
        val sessionId = UUID.randomUUID().toString()
        activeSessionId = sessionId
        endEpochMillis = nowMillis() + seconds * 1000L
        _uiState.value = _uiState.value.copy(
            phase = SessionPhase.RUNNING,
            remainingSeconds = seconds,
            durationSeconds = seconds,
            reward = null,
            companionEvolution = null,
            footprints = emptyList(),
            selectedFootprintPresetId = null,
            footprintPosted = false,
        )
        alarmScheduler.schedule(endEpochMillis)
        persistSession {
            preferences.saveRunning(
                minutes = _uiState.value.selectedMinutes,
                expedition = _uiState.value.expedition,
                endEpochMillis = endEpochMillis,
                sessionId = sessionId,
            )
        }
        startTicker()
    }

    fun pause() {
        if (_uiState.value.phase != SessionPhase.RUNNING) return
        val remainingMillis = max(0L, endEpochMillis - nowMillis())
        alarmScheduler.cancel()
        tickerJob?.cancel()
        _uiState.value = _uiState.value.copy(
            phase = SessionPhase.PAUSED,
            remainingSeconds = ((remainingMillis + 999L) / 1000L).toInt(),
        )
        persistSession { preferences.savePaused(remainingMillis) }
    }

    fun resume() {
        if (_uiState.value.phase != SessionPhase.PAUSED) return
        val remainingMillis = _uiState.value.remainingSeconds * 1000L
        val sessionId = activeSessionId ?: UUID.randomUUID().toString().also { activeSessionId = it }
        endEpochMillis = nowMillis() + remainingMillis
        _uiState.value = _uiState.value.copy(phase = SessionPhase.RUNNING)
        alarmScheduler.schedule(endEpochMillis)
        persistSession {
            preferences.saveRunning(
                minutes = _uiState.value.selectedMinutes,
                expedition = _uiState.value.expedition,
                endEpochMillis = endEpochMillis,
                sessionId = sessionId,
            )
        }
        startTicker()
    }

    fun finishEarly() {
        if (_uiState.value.phase !in setOf(SessionPhase.RUNNING, SessionPhase.PAUSED)) return
        val elapsedSeconds = (_uiState.value.durationSeconds - _uiState.value.remainingSeconds)
            .coerceAtLeast(0)
        val creditedMinutes = floor(elapsedSeconds / 60.0).toInt()
        finishSession(creditedMinutes, SessionPhase.ABORTED)
    }

    fun resetAfterResult() {
        if (_uiState.value.phase !in setOf(SessionPhase.COMPLETED, SessionPhase.ABORTED)) return
        resetResultState()
        viewModelScope.launch { refreshWorldIfQuiet() }
    }

    fun startAgain() {
        if (_uiState.value.phase != SessionPhase.COMPLETED) return
        resetResultState()
        start()
    }

    private fun resetResultState() {
        val selected = _uiState.value.selectedMinutes
        _uiState.value = _uiState.value.copy(
            phase = SessionPhase.READY,
            remainingSeconds = selected * 60,
            durationSeconds = selected * 60,
            reward = null,
            companionEvolution = null,
            footprints = emptyList(),
            selectedFootprintPresetId = null,
            footprintPosted = false,
        )
    }

    private suspend fun restore() {
        val saved = preferences.session.first()
        val durationSeconds = saved.selectedMinutes * 60
        val base = _uiState.value.copy(
            selectedMinutes = saved.selectedMinutes,
            expedition = saved.expedition,
            durationSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            reward = null,
            companionEvolution = null,
            totalFocusMinutes = saved.totalFocusMinutes,
            world = worldRepository.snapshot(),
            systemAccessEducationSeen = saved.systemAccessEducationSeen,
        )

        when (saved.phase) {
            SessionPhase.RUNNING -> {
                activeSessionId = saved.sessionId ?: "legacy-${saved.endEpochMillis}"
                if (saved.endEpochMillis <= nowMillis()) {
                    _uiState.value = base
                    complete(saved.selectedMinutes)
                } else {
                    endEpochMillis = saved.endEpochMillis
                    val remaining = ((saved.endEpochMillis - nowMillis() + 999L) / 1000L).toInt()
                    _uiState.value = base.copy(
                        phase = SessionPhase.RUNNING,
                        remainingSeconds = remaining,
                    )
                    alarmScheduler.schedule(saved.endEpochMillis)
                    startTicker()
                }
            }

            SessionPhase.PAUSED -> {
                activeSessionId = saved.sessionId
                    ?: "legacy-paused-${saved.totalFocusMinutes}-${saved.pausedRemainingMillis}"
                val remaining = ((saved.pausedRemainingMillis + 999L) / 1000L).toInt()
                _uiState.value = base.copy(
                    phase = SessionPhase.PAUSED,
                    remainingSeconds = remaining.coerceAtLeast(0),
                )
            }

            else -> {
                activeSessionId = null
                _uiState.value = base.copy(phase = SessionPhase.READY)
                preferences.saveReady()
            }
        }
    }

    private suspend fun refreshWorldIfQuiet() {
        if (_uiState.value.phase in setOf(SessionPhase.RUNNING, SessionPhase.PAUSED)) return
        worldRepository.refresh()
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (_uiState.value.phase == SessionPhase.RUNNING) {
                val remainingMillis = endEpochMillis - nowMillis()
                if (remainingMillis <= 0L) {
                    _uiState.value = _uiState.value.copy(remainingSeconds = 0)
                    complete(_uiState.value.selectedMinutes)
                    break
                }
                _uiState.value = _uiState.value.copy(
                    remainingSeconds = ((remainingMillis + 999L) / 1000L).toInt(),
                )
                delay(250L)
            }
        }
    }

    private fun complete(creditedMinutes: Int) {
        finishSession(creditedMinutes, SessionPhase.COMPLETED)
    }

    private fun finishSession(creditedMinutes: Int, phase: SessionPhase) {
        alarmScheduler.cancel()
        tickerJob?.cancel()

        val before = _uiState.value
        val reward = FocusRules.resolveSession(
            creditedMinutes = creditedMinutes,
            expedition = before.expedition,
            discoveryProgressMinutes = before.totalFocusMinutes % 25,
        )
        val updatedTotalFocusMinutes = before.totalFocusMinutes + reward.creditedMinutes
        val evolution = CompanionGrowth.evolutionBetween(
            beforeMinutes = before.totalFocusMinutes,
            afterMinutes = updatedTotalFocusMinutes,
        )
        val reachedNewStarRouteCheckpoint = before.expedition != Expedition.STAR_ROUTE ||
            StarRoute.reachedCheckpoint(updatedTotalFocusMinutes) >
            StarRoute.reachedCheckpoint(before.totalFocusMinutes)
        val nearbyFootprints = if (phase == SessionPhase.COMPLETED && reachedNewStarRouteCheckpoint) {
            val checkpoint = if (before.expedition == Expedition.STAR_ROUTE) {
                StarRoute.reachedCheckpoint(updatedTotalFocusMinutes)
            } else {
                checkpointFor(before)
            }
            worldRepository.footprints(
                expedition = before.expedition,
                checkpoint = checkpoint,
            )
        } else {
            emptyList()
        }
        val sessionId = activeSessionId ?: UUID.randomUUID().toString()
        val completedAtEpochMillis = nowMillis()
        activeSessionId = null

        _uiState.value = before.copy(
            phase = phase,
            remainingSeconds = 0,
            reward = reward,
            companionEvolution = evolution,
            totalFocusMinutes = updatedTotalFocusMinutes,
            footprints = nearbyFootprints,
            selectedFootprintPresetId = null,
            footprintPosted = false,
        )

        persistSession {
            if (reward.creditedMinutes > 0) {
                sessionHistoryRepository.record(
                    SessionHistoryEntry(
                        sessionId = sessionId,
                        completedAtEpochMillis = completedAtEpochMillis,
                        plannedMinutes = before.selectedMinutes,
                        creditedMinutes = reward.creditedMinutes,
                        expedition = before.expedition,
                        outcome = if (phase == SessionPhase.COMPLETED) {
                            SessionOutcome.COMPLETED
                        } else {
                            SessionOutcome.ABORTED
                        },
                        damage = reward.personalDamage,
                        rarity = reward.rarity,
                        discovery = reward.discovery,
                    ),
                )
            }
            preferences.commitFinishedSession(
                sessionId = sessionId,
                creditedMinutes = reward.creditedMinutes,
            )
        }
    }

    private fun persistSession(block: suspend () -> Unit) {
        val previous = sessionPersistenceJob
        sessionPersistenceJob = viewModelScope.launch {
            previous?.join()
            block()
        }
    }

    private fun checkpointFor(state: FocusUiState): Int = when (state.expedition) {
        Expedition.TOWER -> state.world.towerFloor
        Expedition.ABYSS -> state.world.abyssDepth
        Expedition.STAR_ROUTE -> StarRoute.reachedCheckpoint(state.totalFocusMinutes)
    }

    class Factory(
        private val preferences: SessionPreferences,
        private val worldRepository: WorldRepository,
        private val sessionHistoryRepository: SessionHistoryRepository,
        private val alarmScheduler: FocusAlarmScheduler,
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
