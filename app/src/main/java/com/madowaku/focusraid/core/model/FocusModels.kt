package com.madowaku.focusraid.core.model

enum class Expedition {
    TOWER,
    ABYSS,
    STAR_ROUTE,
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

data class FootprintPreset(
    val id: String,
    val glyph: String,
    val text: String,
)

object FootprintPresets {
    val all: List<FootprintPreset> = listOf(
        FootprintPreset("made_it", "⚑", "ここまで来たぞ！"),
        FootprintPreset("keep_going", "✦", "がんばろう！"),
        FootprintPreset("almost", "↟", "まだ先へ行ける"),
        FootprintPreset("one_step", "◆", "今日も一歩！"),
        FootprintPreset("rest", "☕", "休憩も大事"),
        FootprintPreset("waiting", "⌁", "先で待ってる！"),
        FootprintPreset("fire", "🔥", "いい集中だった！"),
        FootprintPreset("strong", "💪", "積み重ねていこう"),
    )

    fun byId(id: String): FootprintPreset? = all.firstOrNull { it.id == id }
}

data class Footprint(
    val expedition: Expedition,
    val checkpoint: Int,
    val presetId: String,
    val glyph: String,
    val text: String,
    val relativeLabel: String,
)

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
