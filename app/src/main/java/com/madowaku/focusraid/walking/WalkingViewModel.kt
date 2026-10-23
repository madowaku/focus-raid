package com.madowaku.focusraid.walking

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZoneId

enum class WalkingStatus { LOADING, DISABLED, READY, PERMISSION_REQUIRED, UPDATE_REQUIRED, UNAVAILABLE, ERROR }
data class WalkingUiState(
    val journal: WalkingJournal = WalkingJournal(),
    val status: WalkingStatus = WalkingStatus.LOADING,
    val checkedAt: Instant? = null,
)

class WalkingViewModel(application: Application) : AndroidViewModel(application) {
    private val storage = WalkingStorage(application)
    private val steps: StepsRepository = HealthConnectStepsRepository(application)
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(WalkingUiState())
    val state = mutableState.asStateFlow()
    private var readJob: Job? = null

    fun cancelRead() { readJob?.cancel() }
    fun refresh() {
        if (readJob?.isActive == true) return
        readJob = viewModelScope.launch { mutex.withLock { synchronize() } }
    }
    fun enable(onEnabled: () -> Unit) {
        cancelRead()
        viewModelScope.launch { mutex.withLock {
        try {
            val journal = storage.update { it.enable(Instant.now(), ZoneId.systemDefault()) }
            mutableState.value = WalkingUiState(journal, WalkingStatus.PERMISSION_REQUIRED)
            onEnabled()
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutableState.value = mutableState.value.copy(status = WalkingStatus.ERROR) }
    } } }
    fun disable() { cancelRead(); viewModelScope.launch { mutex.withLock {
        try {
            val journal = storage.update { it.copy(enabled = false) }
            mutableState.value = WalkingUiState(journal, WalkingStatus.DISABLED)
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { mutableState.value = mutableState.value.copy(status = WalkingStatus.ERROR) }
    } } }

    private suspend fun synchronize() {
        try {
            var journal = storage.read()
            val available = steps.availability()
            val status = when {
                !journal.enabled -> WalkingStatus.DISABLED
                available == StepsAvailability.UNAVAILABLE -> WalkingStatus.UNAVAILABLE
                available == StepsAvailability.UPDATE_REQUIRED -> WalkingStatus.UPDATE_REQUIRED
                !steps.hasPermission() -> WalkingStatus.PERMISSION_REQUIRED
                else -> WalkingStatus.READY
            }
            mutableState.value = WalkingUiState(journal, if (status == WalkingStatus.READY) WalkingStatus.LOADING else status)
            if (status != WalkingStatus.READY) return
            val now = Instant.now()
            val zone = ZoneId.of(journal.zone)
            val readings = withTimeout(30_000) {
                journal.datesToRead(now).associateWith { date ->
                    val start = date.atStartOfDay(zone).toInstant()
                    val end = minOf(date.plusDays(1).atStartOfDay(zone).toInstant(), now)
                    if (start < end) steps.read(start, end) else null
                }
            }
            // All readings and earned checkpoints commit atomically; replay is a set union.
            journal = storage.update { current ->
                readings.entries.fold(current) { result, (date, count) -> result.record(date, count) }
            }
            mutableState.value = WalkingUiState(journal, WalkingStatus.READY, now)
        } catch (_: kotlinx.coroutines.TimeoutCancellationException) {
            mutableState.value = mutableState.value.copy(status = WalkingStatus.ERROR)
        } catch (e: CancellationException) { throw e }
        catch (_: SecurityException) {
            mutableState.value = mutableState.value.copy(status = WalkingStatus.PERMISSION_REQUIRED)
        } catch (_: Exception) {
            mutableState.value = mutableState.value.copy(status = WalkingStatus.ERROR)
        }
    }
}
