package com.madowaku.focusraid.core.domain

import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Rarity
import com.madowaku.focusraid.core.model.SessionReward
import kotlin.math.floor

object FocusRules {

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
        val pool = rarity?.let { ItemCatalog.pool(expedition, it) }.orEmpty()
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

    fun companionStage(totalMinutes: Int): String = CompanionGrowth.from(totalMinutes).stage.label
}
