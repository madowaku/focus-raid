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
import com.madowaku.focusraid.core.model.SessionHistoryEntry
import com.madowaku.focusraid.core.model.SessionOutcome
import com.madowaku.focusraid.core.model.Rarity
import org.json.JSONObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.focusRaidDataStore by preferencesDataStore(name = "focus_raid")

data class PersistedSession(
    val raidGeneration: String? = null,
    val startedAtEpochMillis: Long = 0,
    val companion: com.madowaku.focusraid.core.domain.CompanionIdentity = com.madowaku.focusraid.core.domain.CompanionIdentity.RAG,
    val selectedMinutes: Int = 25,
    val expedition: Expedition = Expedition.TOWER,
    val phase: SessionPhase = SessionPhase.READY,
    val endEpochMillis: Long = 0L,
    val pausedRemainingMillis: Long = 0L,
    val totalFocusMinutes: Int = 0,
    val systemAccessEducationSeen: Boolean = false,
    val sessionId: String? = null,
    val finishedEntry: SessionHistoryEntry? = null,
    val firstRunVersion: Int = 0,
)

class SessionPreferences(private val context: Context) : SessionStore {
    private object Keys {
        val raidGeneration = stringPreferencesKey("raid_generation")
        val startedAt = longPreferencesKey("session_started_at")
        val companion = stringPreferencesKey("companion_identity")
        val selectedMinutes = intPreferencesKey("selected_minutes")
        val expedition = stringPreferencesKey("expedition")
        val phase = stringPreferencesKey("phase")
        val endEpochMillis = longPreferencesKey("end_epoch_millis")
        val pausedRemainingMillis = longPreferencesKey("paused_remaining_millis")
        val totalFocusMinutes = intPreferencesKey("total_focus_minutes")
        val systemAccessEducationSeen = booleanPreferencesKey("system_access_education_seen")
        val firstRunVersion = intPreferencesKey("first_run_version")
        val sessionId = stringPreferencesKey("session_id")
        val lastCreditedSessionId = stringPreferencesKey("last_credited_session_id")
        val finishedEntry = stringPreferencesKey("finished_entry")
    }

    override val session: Flow<PersistedSession> = context.focusRaidDataStore.data.map { prefs ->
        PersistedSession(
            raidGeneration = prefs[Keys.raidGeneration],
            startedAtEpochMillis = prefs[Keys.startedAt] ?: 0,
            companion = prefs[Keys.companion]?.let { runCatching { com.madowaku.focusraid.core.domain.CompanionIdentity.valueOf(it) }.getOrNull() } ?: com.madowaku.focusraid.core.domain.CompanionIdentity.RAG,
            selectedMinutes = prefs[Keys.selectedMinutes] ?: 25,
            expedition = prefs[Keys.expedition]
                ?.let { runCatching { Expedition.valueOf(it) }.getOrNull() }
                ?: Expedition.TOWER,
            phase = prefs[Keys.phase]
                ?.let { runCatching { SessionPhase.valueOf(it) }.getOrNull() }
                ?: SessionPhase.READY,
            endEpochMillis = prefs[Keys.endEpochMillis] ?: 0L,
            pausedRemainingMillis = prefs[Keys.pausedRemainingMillis] ?: 0L,
            totalFocusMinutes = prefs[Keys.totalFocusMinutes] ?: 0,
            systemAccessEducationSeen = prefs[Keys.systemAccessEducationSeen] ?: false,
            firstRunVersion = prefs[Keys.firstRunVersion] ?: 0,
            sessionId = prefs[Keys.sessionId],
            finishedEntry = prefs[Keys.finishedEntry]?.let(::decodeEntry),
        )
    }

    override suspend fun ensureSessionIdentity(sessionId: String) {
        context.focusRaidDataStore.edit { if (it[Keys.sessionId] == null) it[Keys.sessionId] = sessionId }
    }

    override suspend fun setCompanion(identity: com.madowaku.focusraid.core.domain.CompanionIdentity) {
        context.focusRaidDataStore.edit { it[Keys.companion] = identity.name }
    }

    override suspend fun setSelectedMinutes(minutes: Int) {
        context.focusRaidDataStore.edit { it[Keys.selectedMinutes] = minutes }
    }

    override suspend fun setExpedition(expedition: Expedition) {
        context.focusRaidDataStore.edit { it[Keys.expedition] = expedition.name }
    }

    override suspend fun saveRunning(
        minutes: Int,
        expedition: Expedition,
        endEpochMillis: Long,
        sessionId: String,
        raidGeneration: String?,
        startedAtEpochMillis: Long,
    ) {
        context.focusRaidDataStore.edit {
            if (it[Keys.sessionId] != sessionId) {
                it[Keys.startedAt] = startedAtEpochMillis
                if (raidGeneration == null) it.remove(Keys.raidGeneration)
                else it[Keys.raidGeneration] = raidGeneration
            }
            it[Keys.selectedMinutes] = minutes
            it[Keys.expedition] = expedition.name
            it[Keys.phase] = SessionPhase.RUNNING.name
            it[Keys.endEpochMillis] = endEpochMillis
            it[Keys.pausedRemainingMillis] = 0L
            it[Keys.sessionId] = sessionId
            it.remove(Keys.finishedEntry)
        }
    }

    override suspend fun savePaused(remainingMillis: Long) {
        context.focusRaidDataStore.edit {
            it[Keys.phase] = SessionPhase.PAUSED.name
            it[Keys.endEpochMillis] = 0L
            it[Keys.pausedRemainingMillis] = remainingMillis
        }
    }

    override suspend fun saveReady() {
        context.focusRaidDataStore.edit {
            it[Keys.phase] = SessionPhase.READY.name
            it[Keys.endEpochMillis] = 0L
            it[Keys.pausedRemainingMillis] = 0L
            it.remove(Keys.sessionId)
            it.remove(Keys.finishedEntry)
        }
    }

    override suspend fun stageFinishedSession(entry: SessionHistoryEntry) {
        context.focusRaidDataStore.edit {
            check(it[Keys.sessionId] == entry.sessionId || it[Keys.finishedEntry]?.let(::decodeEntry)?.sessionId == entry.sessionId) { "Session changed before completion" }
            // Journal the exact outcome/drop BEFORE Room. Replay cannot reroll or turn an abort
            // into a full completion if the process dies between the two durable stores.
            if (it[Keys.finishedEntry] == null) it[Keys.finishedEntry] = encodeEntry(entry)
        }
    }

    override suspend fun commitFinishedSession(sessionId: String, creditedMinutes: Int) {
        context.focusRaidDataStore.edit {
            // A delayed completion must never clear or credit a different, newer session.
            if (it[Keys.sessionId] != sessionId) return@edit
            if (it[Keys.lastCreditedSessionId] != sessionId) {
                val current = it[Keys.totalFocusMinutes] ?: 0
                it[Keys.totalFocusMinutes] = current + creditedMinutes.coerceAtLeast(0)
                it[Keys.lastCreditedSessionId] = sessionId
            }
            it[Keys.phase] = SessionPhase.READY.name
            it[Keys.endEpochMillis] = 0L
            it[Keys.pausedRemainingMillis] = 0L
            it.remove(Keys.sessionId)
        }
    }

    override suspend fun markSystemAccessEducationSeen() {
        context.focusRaidDataStore.edit {
            it[Keys.systemAccessEducationSeen] = true
        }
    }

    override suspend fun markFirstRunComplete(version: Int) {
        context.focusRaidDataStore.edit {
            it[Keys.firstRunVersion] = version.coerceAtLeast(0)
        }
    }

    private fun encodeEntry(e: SessionHistoryEntry): String = JSONObject().apply {
        put("id", e.sessionId)
        put("generation", e.raidGeneration ?: JSONObject.NULL)
        put("startedAt", e.startedAtEpochMillis)
        put("at", e.completedAtEpochMillis)
        put("planned", e.plannedMinutes)
        put("credited", e.creditedMinutes)
        put("expedition", e.expedition.name)
        put("outcome", e.outcome.name)
        put("damage", e.damage)
        put("rarity", e.rarity?.name ?: JSONObject.NULL)
        put("discovery", e.discovery ?: JSONObject.NULL)
    }.toString()

    private fun decodeEntry(value: String): SessionHistoryEntry = JSONObject(value).let {
        SessionHistoryEntry(
            it.getString("id"), it.getLong("at"), it.getInt("planned"), it.getInt("credited"),
            Expedition.valueOf(it.getString("expedition")), SessionOutcome.valueOf(it.getString("outcome")),
            it.getInt("damage"), if (it.isNull("rarity")) null else Rarity.valueOf(it.getString("rarity")),
            if (it.isNull("discovery")) null else it.getString("discovery"),
            if (it.isNull("generation")) null else it.getString("generation"),
            it.optLong("startedAt", 0),
        )
    }
}
