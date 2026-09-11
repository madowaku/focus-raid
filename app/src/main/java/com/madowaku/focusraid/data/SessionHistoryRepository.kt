package com.madowaku.focusraid.data

import com.madowaku.focusraid.core.model.SessionHistoryEntry
import kotlinx.coroutines.flow.Flow

interface SessionHistoryRepository {
    val recentSessions: Flow<List<SessionHistoryEntry>>

    suspend fun record(entry: SessionHistoryEntry)
}
