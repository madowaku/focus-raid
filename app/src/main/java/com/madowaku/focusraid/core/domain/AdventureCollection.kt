package com.madowaku.focusraid.core.domain

import com.madowaku.focusraid.core.model.*

enum class CompanionIdentity(val label: String, val requiredMinutes: Int, val description: String) {
    RAG("ラグ", 0, "小さな竜の旅人。あなたの集中と一緒に育ちます。"),
    MIKO("ミコ", 75, "若葉のきつね。静かな森の道を案内してくれます。"),
    LUNE("ルネ", 180, "月灯りのふくろう。静かな夜の集中を見守ります。"),
}

enum class RaidBossIdentity(val label: String) {
    VOLGA("環焔竜ヴォルガ"), MORD("根晶獣モルド"), ZEPHYR("星嵐鳥ゼファー"),
}

data class LocalBossProgress(val creditedMinutes: Int, val targetMinutes: Int = 180) {
    val remainingHp get() = (targetMinutes - creditedMinutes).coerceAtLeast(0)
    val defeated get() = remainingHp == 0
    val progress get() = (creditedMinutes.toFloat() / targetMinutes).coerceIn(0f, 1f)
}

object AdventureCollection {
    fun canSelect(companion: CompanionIdentity, totalMinutes: Int) = totalMinutes >= companion.requiredMinutes
    fun mordProgress(entries: List<SessionHistoryEntry>) = personalBossProgress(RaidBossIdentity.MORD, entries)
    fun personalBossProgress(boss: RaidBossIdentity, entries: List<SessionHistoryEntry>): LocalBossProgress {
        require(boss != RaidBossIdentity.VOLGA) { "Volga is a shared world raid" }
        val expedition = if (boss == RaidBossIdentity.MORD) Expedition.ABYSS else Expedition.TOWER
        val minutes = entries.distinctBy { it.sessionId }.filter { it.expedition == expedition }
            .sumOf { it.creditedMinutes.coerceAtLeast(0).toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        return LocalBossProgress(minutes, if (boss == RaidBossIdentity.MORD) 180 else 300)
    }
}

data class AdventureItem(val id: String, val name: String, val expedition: Expedition, val rarity: Rarity) {
    val glyph get() = when (expedition) { Expedition.TOWER -> "⚔"; Expedition.ABYSS -> "◆"; Expedition.STAR_ROUTE -> "✦" }
}
data class OwnedItem(val item: AdventureItem, val count: Int)

object ItemCatalog {
    private val drops: Map<Expedition, Map<Rarity, List<String>>> = mapOf(
        Expedition.TOWER to mapOf(
            Rarity.COMMON to listOf("鉄の剣", "木の弓", "鉄鉱石", "旅人の盾", "風音の鈴"),
            Rarity.RARE to listOf("白銀の槍", "氷晶の弓", "騎士の盾", "嵐羽のブローチ"),
            Rarity.EPIC to listOf("雷撃砲の部品", "蒼天の大槍"),
            Rarity.LEGENDARY to listOf("星喰らいの大剣"),
        ),
        Expedition.ABYSS to mapOf(
            Rarity.COMMON to listOf("魔力石", "古い地図片", "薬草", "青晶石", "苔灯のランタン"),
            Rarity.RARE to listOf("古代の鍵", "耐火の護符", "月影の水晶", "根晶の印章"),
            Rarity.EPIC to listOf("共鳴結晶", "弱点解析器"),
            Rarity.LEGENDARY to listOf("深淵の羅針盤"),
        ),
        Expedition.STAR_ROUTE to mapOf(
            Rarity.COMMON to listOf("星砂の小瓶", "破れた星図", "導光石", "古い航海札", "月読のしおり"),
            Rarity.RARE to listOf("彗星のコンパス", "夜光帆", "星詠みのレンズ", "彗星インク"),
            Rarity.EPIC to listOf("星環炉の欠片", "虚空航路図"),
            Rarity.LEGENDARY to listOf("天球儀アストラル"),
        ),
    )

    val all: List<AdventureItem> = drops.flatMap { (expedition, rarities) ->
        rarities.flatMap { (rarity, names) -> names.mapIndexed { index, name ->
            AdventureItem("${expedition.name.lowercase()}-${rarity.name.lowercase()}-${index + 1}", name, expedition, rarity)
        } }
    }
    fun pool(expedition: Expedition, rarity: Rarity): List<String> = drops.getValue(expedition).getValue(rarity)
    fun owned(entries: List<SessionHistoryEntry>): List<OwnedItem> = entries.distinctBy { it.sessionId }
        .mapNotNull { entry -> entry.discovery?.let { name ->
            all.firstOrNull { it.name == name } ?: AdventureItem("legacy-$name", name, entry.expedition, entry.rarity ?: Rarity.COMMON)
        } }.groupingBy { it }.eachCount().map { (item, count) -> OwnedItem(item, count) }
        .sortedWith(compareByDescending<OwnedItem> { it.item.rarity.ordinal }.thenBy { it.item.id })
}
