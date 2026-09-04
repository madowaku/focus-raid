package com.madowaku.focusraid.billing

import com.madowaku.focusraid.core.model.Expedition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAccessTest {
    @Test
    fun `all Pro features are locked for Free and open for Pro`() {
        ProFeature.entries.forEach { feature ->
            assertFalse(FeatureAccess.canUse(feature, AccessLevel.FREE))
            assertTrue(FeatureAccess.canUse(feature, AccessLevel.PRO))
        }
    }

    @Test
    fun `launch expeditions remain Free`() {
        Expedition.entries.forEach { expedition ->
            assertEquals(RaidAccess.FREE, FeatureAccess.raidAccess(expedition))
            assertTrue(FeatureAccess.canUse(expedition, AccessLevel.FREE))
        }
    }
}
