package com.madowaku.focusraid.data

import com.madowaku.focusraid.core.model.WorldSnapshot

internal object RemoteWorldMapping {
    fun fromMap(
        values: Map<String, Any?>,
        fallback: WorldSnapshot,
    ): WorldSnapshot = fallback.copy(
        generation = (values["generation"] as? String)?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{1,80}")) },
        totalFocusMinutes = (values["totalFocusMinutes"] as? Number)?.toLong()?.coerceAtLeast(0) ?: 0,
        focusNow = values.intValue("focusNow") ?: fallback.focusNow,
        bossName = values["bossName"] as? String ?: fallback.bossName,
        bossHp = values.intValue("bossHp") ?: fallback.bossHp,
        bossMaxHp = values.intValue("bossMaxHp") ?: fallback.bossMaxHp,
        raidParticipants = values.intValue("raidParticipants") ?: fallback.raidParticipants,
        towerFloor = values.intValue("towerFloor") ?: fallback.towerFloor,
        abyssDepth = values.intValue("abyssDepth") ?: fallback.abyssDepth,
        armoryReady = values.intValue("armoryReady") ?: fallback.armoryReady,
    )

    private fun Map<String, Any?>.intValue(key: String): Int? {
        val value = this[key] as? Number ?: return null
        val long = value.toLong()
        if (long !in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) return null
        return long.toInt()
    }
}
