package com.madowaku.focusraid.data

import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionHistoryEntry
import kotlinx.coroutines.flow.Flow

/** Durable command boundary; callers publish successful transitions only after these return. */
interface SessionStore {
    val session: Flow<PersistedSession>
    suspend fun ensureSessionIdentity(sessionId: String)
    suspend fun setCompanion(identity: com.madowaku.focusraid.core.domain.CompanionIdentity)
    suspend fun setSelectedMinutes(minutes: Int)
    suspend fun setExpedition(expedition: Expedition)
    suspend fun saveRunning(minutes: Int, expedition: Expedition, endEpochMillis: Long, sessionId: String, raidGeneration: String? = null, startedAtEpochMillis: Long = 0)
    suspend fun savePaused(remainingMillis: Long)
    suspend fun saveReady()
    suspend fun stageFinishedSession(entry: SessionHistoryEntry)
    suspend fun commitFinishedSession(sessionId: String, creditedMinutes: Int)
    suspend fun markSystemAccessEducationSeen()
    suspend fun markFirstRunComplete(version: Int)
}
