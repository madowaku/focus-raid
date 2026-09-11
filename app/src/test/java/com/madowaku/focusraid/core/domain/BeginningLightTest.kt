package com.madowaku.focusraid.core.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
        assertEquals(25f / 75f, first.hatchProgress)

        val halfway = BeginningLight.from(50)
        assertEquals(25, halfway.hatchRemainingMinutes)
        assertEquals(50f / 75f, halfway.hatchProgress)

        val hatched = BeginningLight.from(75)
        assertEquals(0, hatched.hatchRemainingMinutes)
        assertEquals(1f, hatched.hatchProgress)
    }
}
