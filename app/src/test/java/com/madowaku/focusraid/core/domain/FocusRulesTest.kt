package com.madowaku.focusraid.core.domain

import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Rarity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FocusRulesTest {
    @Test
    fun `rarity thresholds match the MVP distribution`() {
        assertEquals(Rarity.LEGENDARY, FocusRules.rarityFromRoll(0.001))
        assertEquals(Rarity.EPIC, FocusRules.rarityFromRoll(0.01))
        assertEquals(Rarity.RARE, FocusRules.rarityFromRoll(0.1))
        assertEquals(Rarity.COMMON, FocusRules.rarityFromRoll(0.5))
    }

    @Test
    fun `25 credited minutes become personal damage and world EP`() {
        val result = FocusRules.resolveSession(
            creditedMinutes = 25,
            expedition = Expedition.TOWER,
            discoveryProgressMinutes = 0,
            roll = 0.5,
        )

        assertEquals(25, result.personalDamage)
        assertEquals(25, result.worldEp)
        assertEquals(1, result.defeated)
        assertNotNull(result.discovery)
    }

    @Test
    fun `short sessions remain meaningful without inventing a discovery`() {
        val result = FocusRules.resolveSession(
            creditedMinutes = 10,
            expedition = Expedition.ABYSS,
            discoveryProgressMinutes = 0,
            roll = 0.5,
        )

        assertEquals(10, result.personalDamage)
        assertEquals(0, result.defeated)
        assertNull(result.discovery)
    }

    @Test
    fun `companion growth uses cumulative focus minutes`() {
        assertEquals("卵", FocusRules.companionStage(74))
        assertEquals("幼体", FocusRules.companionStage(75))
        assertEquals("第一成長", FocusRules.companionStage(720))
    }
}
