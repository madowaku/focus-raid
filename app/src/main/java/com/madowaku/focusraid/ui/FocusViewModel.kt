package com.madowaku.focusraid.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.madowaku.focusraid.core.domain.FocusRules
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.SessionReward
import com.madowaku.focusraid.core.model.WorldSnapshot
import com.madowaku.focusraid.data.SessionPreferences
import com.madowaku.focusraid.data.WorldRepository
import com.madowaku.focusraid.timer.FocusAlarmScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max

data class FocusUiState(
    val phase: SessionPhase = SessionPhase.READY,
    val selectedMinutes: Int = 25,
    val expedition: Expedition = Expedition.TOWER,
    val remainingSeconds: Int = 25 * 60,
    val durationSeconds: Int = 25 * 60,
    val reward: SessionReward? = null,
    val totalFocusMinutes: Int = 645,
    val streakDays: Int = 12,
    val world: WorldSnapshot = WorldSnapshot(),
) {
    val progress: Float
        get() = if (durationSeconds <= 0) 0f
        else (1f - remainingSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
}

class FocusViewModel(
    private val preferences: SessionPreferences,
    private val worldRepository: WorldRepository,
    private val alarmScheduler: FocusAlarmScheduler,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FocusUiState(world = worldRepository.snapshot()))
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var endEpochMillis: Long = 0L
    private var tickerJob: Job? = null

    init {
        viewModelScope.launch {
            restore()
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

    fun start() {
        if (_uiState.value.phase != SessionPhase.READY) return
        val seconds = _uiState.value.selectedMinutes * 60
        endEpochMillis = nowMillis() + seconds * 1000L
        _uiState.value = _uiState.value.copy(
            phase = SessionPhase.RUNNING,
            remainingSeconds = seconds,
            durationSeconds = seconds,
            reward = null,
        )
        alarmScheduler.schedule(endEpochMillis)
        viewModelScope.launch {
            preferences.saveRunning(
                minutes = _uiState.value.selectedMinutes,
                expedition = _uiState.value.expedition,
                endEpochMillis = endEpochMillis,
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
        viewModelScope.launch { preferences.savePaused(remainingMillis) }
    }

    fun resume() {
        if (_uiState.value.phase != SessionPhase.PAUSED) return
        val remainingMillis = _uiState.value.remainingSeconds * 1000L
        endEpochMillis = nowMillis() + remainingMillis
        _uiState.value = _uiState.value.copy(phase = SessionPhase.RUNNING)
        alarmScheduler.schedule(endEpochMillis)
        viewModelScope.launch {
            preferences.saveRunning(
                minutes = _uiState.value.selectedMinutes,
                expedition = _uiState.value.expedition,
                endEpochMillis = endEpochMillis,
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
        val selected = _uiState.value.selectedMinutes
        _uiState.value = _uiState.value.copy(
            phase = SessionPhase.READY,
            remainingSeconds = selected * 60,
            durationSeconds = selected * 60,
            reward = null,
        )
    }

    fun startAgain() {
        if (_uiState.value.phase != SessionPhase.COMPLETED) return
        resetAfterResult()
        start()
    }

    private suspend fun restore() {
        val saved = preferences.session.first()
        val durationSeconds = saved.selectedMinutes * 60
        val base = _uiState.value.copy(
            selectedMinutes = saved.selectedMinutes,
            expedition = saved.expedition,
            durationSeconds = durationSeconds,
            remainingSeconds = durationSeconds,
            totalFocusMinutes = saved.totalFocusMinutes,
            world = worldRepository.snapshot(),
        )

        when (saved.phase) {
            SessionPhase.RUNNING -> {
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
                val remaining = ((saved.pausedRemainingMillis + 999L) / 1000L).toInt()
                _uiState.value = base.copy(
                    phase = SessionPhase.PAUSED,
                    remainingSeconds = remaining.coerceAtLeast(0),
                )
            }
            else -> {
                _uiState.value = base.copy(phase = SessionPhase.READY)
                preferences.saveReady()
            }
        }
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

        _uiState.value = before.copy(
            phase = phase,
            remainingSeconds = 0,
            reward = reward,
            totalFocusMinutes = before.totalFocusMinutes + reward.creditedMinutes,
        )

        viewModelScope.launch {
            preferences.addFocusMinutes(reward.creditedMinutes)
            preferences.saveReady()
        }
    }

    class Factory(
        private val preferences: SessionPreferences,
        private val worldRepository: WorldRepository,
        private val alarmScheduler: FocusAlarmScheduler,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FocusViewModel(preferences, worldRepository, alarmScheduler) as T
        }
    }
}