package com.madowaku.focusraid.core.domain

import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Rarity
import com.madowaku.focusraid.core.model.SessionReward
import kotlin.math.floor

object FocusRules {
    private val drops: Map<Expedition, Map<Rarity, List<String>>> = mapOf(
        Expedition.TOWER to mapOf(
            Rarity.COMMON to listOf("鉄の剣", "木の弓", "鉄鉱石", "旅人の盾"),
            Rarity.RARE to listOf("白銀の槍", "氷晶の弓", "騎士の盾"),
            Rarity.EPIC to listOf("雷撃砲の部品", "蒼天の大槍"),
            Rarity.LEGENDARY to listOf("星喰らいの大剣"),
        ),
        Expedition.ABYSS to mapOf(
            Rarity.COMMON to listOf("魔力石", "古い地図片", "薬草", "青晶石"),
            Rarity.RARE to listOf("古代の鍵", "耐火の護符", "月影の水晶"),
            Rarity.EPIC to listOf("共鳴結晶", "弱点解析器"),
            Rarity.LEGENDARY to listOf("深淵の羅針盤"),
        ),
    )

    fun rarityFromRoll(roll: Double): Rarity = when {
        roll < 0.005 -> Rarity.LEGENDARY
        roll < 0.04 -> Rarity.EPIC
        roll < 0.23 -> Rarity.RARE
        else -> Rarity.COMMON
    }

    fun resolveSession(
        creditedMinutes: Int,
        expedition: Expedition,
        discoveryProgressMinutes: Int,
        roll: Double = Math.random(),
    ): SessionReward {
        val minutes = creditedMinutes.coerceAtLeast(0)
        val discoveries = floor((discoveryProgressMinutes + minutes) / 25.0).toInt()
        val rarity = if (discoveries > 0) rarityFromRoll(roll) else null
        val pool = rarity?.let { drops.getValue(expedition).getValue(it) }.orEmpty()
        val discovery = if (pool.isEmpty()) null else pool[(floor(roll * 1000).toInt() % pool.size)]
        val armoryPoints = when (rarity) {
            Rarity.COMMON -> 1
            Rarity.RARE -> 2
            Rarity.EPIC -> 5
            Rarity.LEGENDARY -> 10
            null -> 0
        }

        return SessionReward(
            creditedMinutes = minutes,
            personalDamage = minutes,
            worldEp = minutes,
            defeated = minutes / 25,
            rarity = rarity,
            discovery = discovery,
            armoryPoints = armoryPoints,
        )
    }

    fun companionStage(totalMinutes: Int): String = when {
        totalMinutes < 75 -> "卵"
        totalMinutes < 720 -> "幼体"
        totalMinutes < 1_800 -> "第一成長"
        totalMinutes < 4_500 -> "第二成長"
        else -> "成熟"
    }
}
