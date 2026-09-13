package com.madowaku.focusraid.ui

import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.WorldSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FirstRunTest {
    private val fresh = FocusUiState(
        initialized = true,
        phase = SessionPhase.READY,
        firstRunVersion = 0,
        totalFocusMinutes = 0,
    )

    @Test
    fun fresh_user_starts_first_run_at_act_one() {
        assertTrue(shouldShowFirstRun(fresh))
    }

    @Test
    fun current_version_returns_to_home() {
        assertFalse(shouldShowFirstRun(fresh.copy(firstRunVersion = CURRENT_FIRST_RUN_VERSION)))
    }

    @Test
    fun any_credited_focus_returns_to_home() {
        assertFalse(shouldShowFirstRun(fresh.copy(totalFocusMinutes = 1)))
    }

    @Test
    fun active_and_result_phases_never_show_first_run() {
        listOf(
            SessionPhase.RUNNING,
            SessionPhase.PAUSED,
            SessionPhase.COMPLETED,
            SessionPhase.ABORTED,
        ).forEach { phase ->
            assertFalse("phase=$phase", shouldShowFirstRun(fresh.copy(phase = phase)))
        }
    }

    @Test
    fun initialization_gate_waits_for_restore() {
        assertFalse(shouldShowFirstRun(fresh.copy(initialized = false)))
    }

    @Test
    fun first_run_hp_changes_are_small_and_stepwise() {
        val world = WorldSnapshot(bossHp = 428_192, bossMaxHp = 650_000)
        val actOne = firstRunActOneHp(world.bossHp, world.bossMaxHp)
        val afterOne = firstRunActTwoHp(actOne, world.bossMaxHp, 1)
        val afterThree = firstRunActTwoHp(actOne, world.bossMaxHp, 3)

        assertTrue(actOne < world.bossHp)
        assertTrue(afterOne < actOne)
        assertTrue(afterThree < afterOne)
        val afterTwo = firstRunActTwoHp(actOne, world.bossMaxHp, 2)
        assertEquals(actOne - afterOne, afterOne - afterTwo)
        assertEquals(afterOne - afterTwo, afterTwo - afterThree)
    }
}
