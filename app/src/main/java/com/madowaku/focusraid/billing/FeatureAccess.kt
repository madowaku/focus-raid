package com.madowaku.focusraid.billing

import com.madowaku.focusraid.core.model.Expedition

enum class ProFeature {
    PRO_RAIDS,
    FULL_HISTORY,
    DETAILED_STATS,
    ALL_THEMES,
    ADVANCED_CUSTOMIZATION,
    FOOTPRINT_CUSTOMIZATION,
    SPECIAL_EFFECTS,
}

enum class RaidAccess {
    FREE,
    PRO,
}

object FeatureAccess {
    fun canUse(feature: ProFeature, accessLevel: AccessLevel): Boolean =
        accessLevel == AccessLevel.PRO

    fun raidAccess(expedition: Expedition): RaidAccess = when (expedition) {
        // Both launch expeditions remain free. New expeditions should be classified here explicitly.
        Expedition.TOWER,
        Expedition.ABYSS,
        -> RaidAccess.FREE
    }

    fun canUse(expedition: Expedition, accessLevel: AccessLevel): Boolean =
        raidAccess(expedition) == RaidAccess.FREE || accessLevel == AccessLevel.PRO
}
