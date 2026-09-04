package com.madowaku.focusraid.data

import com.madowaku.focusraid.core.model.WorldSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteWorldMappingTest {
    @Test
    fun mapsRemoteFieldsOntoFallback() {
        val fallback = WorldSnapshot()
        val mapped = RemoteWorldMapping.fromMap(
            values = mapOf(
                "focusNow" to 5_001L,
                "bossName" to "星喰い竜ノクス",
                "bossHp" to 321_000L,
                "bossMaxHp" to 900_000L,
                "raidParticipants" to 13_579L,
                "towerFloor" to 4_400L,
                "abyssDepth" to 12_900L,
                "armoryReady" to 82L,
            ),
            fallback = fallback,
        )

        assertEquals(5_001, mapped.focusNow)
        assertEquals("星喰い竜ノクス", mapped.bossName)
        assertEquals(321_000, mapped.bossHp)
        assertEquals(900_000, mapped.bossMaxHp)
        assertEquals(13_579, mapped.raidParticipants)
        assertEquals(4_400, mapped.towerFloor)
        assertEquals(12_900, mapped.abyssDepth)
        assertEquals(82, mapped.armoryReady)
    }

    @Test
    fun missingOrInvalidValuesKeepFallback() {
        val fallback = WorldSnapshot(bossName = "fallback", bossHp = 123)
        val mapped = RemoteWorldMapping.fromMap(
            values = mapOf(
                "bossName" to 42,
                "bossHp" to Long.MAX_VALUE,
            ),
            fallback = fallback,
        )

        assertEquals("fallback", mapped.bossName)
        assertEquals(123, mapped.bossHp)
    }
}
