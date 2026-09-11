package com.madowaku.focusraid.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BeginningLightTest {
    @Test
    fun lightTurnsOnAtTwentyFiveMinutesAndPersists() {
        assertFalse(BeginningLight.from(24).lit)
        assertTrue(BeginningLight.from(25).lit)
        assertTrue(BeginningLight.from(500).lit)
    }

    @Test
    fun hatchProgressUsesTheWholeZeroToSeventyFiveMinuteJourney() {
        val first = BeginningLight.from(25)
        assertEquals(50, first.hatchRemainingMinutes)
        assertEquals(25f / 75f, first.hatchProgress, 0.0001f)
        assertFalse(first.hatched)

        val halfway = BeginningLight.from(50)
        assertEquals(25, halfway.hatchRemainingMinutes)
        assertEquals(50f / 75f, halfway.hatchProgress, 0.0001f)
        assertFalse(halfway.hatched)

        val hatched = BeginningLight.from(75)
        assertEquals(0, hatched.hatchRemainingMinutes)
        assertEquals(1f, hatched.hatchProgress, 0.0001f)
        assertTrue(hatched.hatched)
        assertTrue(BeginningLight.from(90).hatched)
    }
}
