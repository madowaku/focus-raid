package com.madowaku.focusraid.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StarRouteTest {
    @Test
    fun `target checkpoint points to the next 25 minute destination`() {
        assertEquals(1, StarRoute.targetCheckpoint(0))
        assertEquals(1, StarRoute.targetCheckpoint(24))
        assertEquals(2, StarRoute.targetCheckpoint(25))
        assertEquals(3, StarRoute.targetCheckpoint(50))
        assertEquals(5, StarRoute.targetCheckpoint(100))
    }

    @Test
    fun `reached checkpoint names the destination just completed`() {
        assertEquals(0, StarRoute.reachedCheckpoint(0))
        assertEquals(1, StarRoute.reachedCheckpoint(1))
        assertEquals(1, StarRoute.reachedCheckpoint(25))
        assertEquals(2, StarRoute.reachedCheckpoint(50))
        assertEquals(3, StarRoute.reachedCheckpoint(75))
        assertEquals(4, StarRoute.reachedCheckpoint(100))
    }

    @Test
    fun `five beacons light across one session`() {
        assertEquals(0, StarRoute.litBeacons(0f))
        assertEquals(1, StarRoute.litBeacons(0.20f))
        assertEquals(2, StarRoute.litBeacons(0.40f))
        assertEquals(4, StarRoute.litBeacons(0.99f))
        assertEquals(5, StarRoute.litBeacons(1f))
    }
}
