package com.madowaku.focusraid.data

data class RaidEcho(
    val relativeLabel: String,
    val focusMinutes: Int,
    val damage: Int,
)

suspend fun WorldRepository.loadRecentRaidEchoes(limit: Int = 3): List<RaidEcho> =
    (this as? FirebaseWorldRepository)?.recentRaidEchoes(limit) ?: emptyList()
