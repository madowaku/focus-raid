package com.madowaku.focusraid.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.focusRaidDataStore by preferencesDataStore(name = "focus_raid")

data class PersistedSession(
    val selectedMinutes: Int = 25,
    val expedition: Expedition = Expedition.TOWER,
    val phase: SessionPhase = SessionPhase.READY,
    val endEpochMillis: Long = 0L,
    val pausedRemainingMillis: Long = 0L,
    val totalFocusMinutes: Int = 645,
    val systemAccessEducationSeen: Boolean = false,
)

class SessionPreferences(private val context: Context) {
    private object Keys {
        val selectedMinutes = intPreferencesKey("selected_minutes")
        val expedition = stringPreferencesKey("expedition")
        val phase = stringPreferencesKey("phase")
        val endEpochMillis = longPreferencesKey("end_epoch_millis")
        val pausedRemainingMillis = longPreferencesKey("paused_remaining_millis")
        val totalFocusMinutes = intPreferencesKey("total_focus_minutes")
        val systemAccessEducationSeen = booleanPreferencesKey("system_access_education_seen")
    }

    val session: Flow<PersistedSession> = context.focusRaidDataStore.data.map { prefs ->
        PersistedSession(
            selectedMinutes = prefs[Keys.selectedMinutes] ?: 25,
            expedition = prefs[Keys.expedition]
                ?.let { runCatching { Expedition.valueOf(it) }.getOrNull() }
                ?: Expedition.TOWER,
            phase = prefs[Keys.phase]
                ?.let { runCatching { SessionPhase.valueOf(it) }.getOrNull() }
                ?: SessionPhase.READY,
            endEpochMillis = prefs[Keys.endEpochMillis] ?: 0L,
            pausedRemainingMillis = prefs[Keys.pausedRemainingMillis] ?: 0L,
            totalFocusMinutes = prefs[Keys.totalFocusMinutes] ?: 645,
            systemAccessEducationSeen = prefs[Keys.systemAccessEducationSeen] ?: false,
        )
    }

    suspend fun setSelectedMinutes(minutes: Int) {
        context.focusRaidDataStore.edit { it[Keys.selectedMinutes] = minutes }
    }

    suspend fun setExpedition(expedition: Expedition) {
        context.focusRaidDataStore.edit { it[Keys.expedition] = expedition.name }
    }

    suspend fun saveRunning(minutes: Int, expedition: Expedition, endEpochMillis: Long) {
        context.focusRaidDataStore.edit {
            it[Keys.selectedMinutes] = minutes
            it[Keys.expedition] = expedition.name
            it[Keys.phase] = SessionPhase.RUNNING.name
            it[Keys.endEpochMillis] = endEpochMillis
            it[Keys.pausedRemainingMillis] = 0L
        }
    }

    suspend fun savePaused(remainingMillis: Long) {
        context.focusRaidDataStore.edit {
            it[Keys.phase] = SessionPhase.PAUSED.name
            it[Keys.endEpochMillis] = 0L
            it[Keys.pausedRemainingMillis] = remainingMillis
        }
    }

    suspend fun saveReady() {
        context.focusRaidDataStore.edit {
            it[Keys.phase] = SessionPhase.READY.name
            it[Keys.endEpochMillis] = 0L
            it[Keys.pausedRemainingMillis] = 0L
        }
    }

    suspend fun addFocusMinutes(minutes: Int) {
        context.focusRaidDataStore.edit {
            val current = it[Keys.totalFocusMinutes] ?: 645
            it[Keys.totalFocusMinutes] = current + minutes.coerceAtLeast(0)
        }
    }

    suspend fun markSystemAccessEducationSeen() {
        context.focusRaidDataStore.edit {
            it[Keys.systemAccessEducationSeen] = true
        }
    }
}
