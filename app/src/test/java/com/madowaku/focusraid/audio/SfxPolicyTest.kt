package com.madowaku.focusraid.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SfxPolicyTest {
    @Test fun `sound disabled and silent mode both suppress playback`() {
        val policy = SfxPolicy()
        assertFalse(policy.audible(enabled = false, ringerNormal = true))
        assertFalse(policy.audible(enabled = true, ringerNormal = false))
        assertFalse(policy.audible(enabled = false, ringerNormal = false))
        assertTrue(policy.audible(enabled = true, ringerNormal = true))
    }
}
