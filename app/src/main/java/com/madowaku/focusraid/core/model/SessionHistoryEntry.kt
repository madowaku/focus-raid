package com.madowaku.focusraid.core.model

enum class SessionOutcome {
    COMPLETED,
    ABORTED,
}

data class SessionHistoryEntry(
    val sessionId: String,
    val completedAtEpochMillis: Long,
    val plannedMinutes: Int,
    val creditedMinutes: Int,
    val expedition: Expedition,
    val outcome: SessionOutcome,
    val damage: Int,
    val rarity: Rarity?,
    val discovery: String?,
)
