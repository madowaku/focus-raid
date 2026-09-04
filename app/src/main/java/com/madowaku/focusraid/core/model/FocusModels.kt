package com.madowaku.focusraid.core.model

enum class Expedition {
    TOWER,
    ABYSS,
}

enum class Rarity {
    COMMON,
    RARE,
    EPIC,
    LEGENDARY,
}

data class SessionReward(
    val creditedMinutes: Int,
    val personalDamage: Int,
    val worldEp: Int,
    val defeated: Int,
    val rarity: Rarity?,
    val discovery: String?,
    val armoryPoints: Int,
)

enum class SessionPhase {
    READY,
    RUNNING,
    PAUSED,
    COMPLETED,
    ABORTED,
}

data class WorldSnapshot(
    val focusNow: Int = 4_218,
    val bossName: String = "灰燼竜ヴォルガ",
    val bossHp: Int = 428_192,
    val bossMaxHp: Int = 650_000,
    val raidParticipants: Int = 12_481,
    val towerFloor: Int = 4_281,
    val abyssDepth: Int = 12_481,
    val armoryReady: Int = 68,
)
