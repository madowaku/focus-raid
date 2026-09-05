package com.madowaku.focusraid.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StarRouteTest {
    @Test
    fun `checkpoint advances every 25 accumulated focus minutes`() {
        assertEquals(1, StarRoute.checkpoint(0))
        assertEquals(1, StarRoute.checkpoint(24))
        assertEquals(2, StarRoute.checkpoint(25))
        assertEquals(5, StarRoute.checkpoint(100))
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
