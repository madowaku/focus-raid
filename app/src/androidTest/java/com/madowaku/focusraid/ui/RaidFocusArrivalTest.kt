package com.madowaku.focusraid.ui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class RaidFocusArrivalTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun arrival_occurs_after_travel_once_and_three_deliveries_stop() {
        compose.mainClock.autoAdvance = false
        val flight = RaidFocusArrival()
        val arrivals = mutableListOf<Int>()
        compose.setContent {
            LaunchedEffect(Unit) {
                repeat(3) { index -> flight.deliver(index) { arrivals += index } }
            }
        }
        compose.mainClock.advanceTimeBy(300)
        compose.runOnIdle {
            assertTrue(flight.progress.value > 0f)
            assertTrue(arrivals.isEmpty())
        }
        compose.mainClock.advanceTimeBy(400)
        compose.runOnIdle { assertEquals(listOf(0), arrivals) }
        compose.mainClock.advanceTimeBy(2_300)
        compose.runOnIdle {
            assertEquals(listOf(0, 1, 2), arrivals)
            assertEquals(1f, flight.progress.value)
        }
        compose.mainClock.advanceTimeBy(5_000)
        compose.runOnIdle { assertEquals(listOf(0, 1, 2), arrivals) }
    }

    @Test
    fun leaving_during_travel_cancels_arrival() {
        compose.mainClock.autoAdvance = false
        val visible = mutableStateOf(true)
        val flight = RaidFocusArrival()
        var arrivals = 0
        compose.setContent {
            if (visible.value) LaunchedEffect(Unit) { flight.deliver(0) { arrivals++ } }
        }
        compose.mainClock.advanceTimeBy(250)
        compose.runOnIdle { visible.value = false }
        compose.mainClock.advanceTimeBy(2_000)
        compose.runOnIdle { assertEquals(0, arrivals) }
    }

    @Test
    fun disabled_motion_finishes_without_losing_or_repeating_arrival() {
        val flight = RaidFocusArrival()
        var arrivals = 0
        compose.setContent {
            LaunchedEffect(Unit) {
                withContext(object : MotionDurationScale { override val scaleFactor = 0f }) {
                    flight.deliver(0, self = true) { arrivals++ }
                }
            }
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, arrivals)
            assertEquals(1f, flight.progress.value)
        }
    }
}
