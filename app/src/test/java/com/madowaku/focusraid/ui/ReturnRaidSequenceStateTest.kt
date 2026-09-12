package com.madowaku.focusraid.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReturnRaidSequenceStateTest {

    private fun scenario(
        bossHp: Int = 620,
        bossMaxHp: Int = 1000,
        playerDamage: Int = 100,
        creditedMinutes: Int = 25,
    ) = ReturnRaidScenario.demo(
        bossName = "炎喰らいヴァルグ",
        bossHp = bossHp,
        bossMaxHp = bossMaxHp,
        playerDamage = playerDamage,
        creditedMinutes = creditedMinutes,
    )

    @Test
    fun sequence_advances_return_echoes_and_waits_for_player() {
        val scenario = scenario()
        val machine = ReturnRaidSequenceStateMachine(scenario)

        assertEquals(ReturnRaidPhase.RETURNING, machine.state.phase)
        assertEquals(scenario.initialDisplayedHp, machine.state.displayedHp)

        machine.advance()
        assertEquals(ReturnRaidPhase.ECHO, machine.state.phase)
        assertEquals(0, machine.state.echoIndex)

        machine.advance()
        assertEquals(1, machine.state.echoIndex)

        machine.advance()
        assertEquals(2, machine.state.echoIndex)

        machine.advance()
        assertEquals(ReturnRaidPhase.YOUR_TURN, machine.state.phase)
        assertEquals(scenario.presentBossHp, machine.state.displayedHp)

        machine.advance()
        assertEquals(ReturnRaidPhase.YOUR_TURN, machine.state.phase)
    }

    @Test
    fun strike_is_committed_only_once() {
        val machine = ReturnRaidSequenceStateMachine(scenario())
        repeat(4) { machine.advance() }

        machine.strike()
        val first = machine.state
        machine.strike()
        val second = machine.state

        assertTrue(first.strikeCommitted)
        assertEquals(ReturnRaidPhase.STRIKING, first.phase)
        assertEquals(first, second)
    }

    @Test
    fun strike_then_result_then_camp_updates_chain() {
        val scenario = scenario()
        val machine = ReturnRaidSequenceStateMachine(scenario)
        repeat(4) { machine.advance() }

        machine.strike()
        assertEquals(scenario.hpAfterPlayerStrike, machine.state.displayedHp)

        machine.advance()
        assertEquals(ReturnRaidPhase.RESULT, machine.state.phase)
        assertFalse(machine.state.armorBroken)

        machine.advance()
        assertEquals(ReturnRaidPhase.CAMP, machine.state.phase)
        assertEquals(scenario.chainCountBefore + 1, machine.state.chainCount)
        assertEquals(scenario.chainMinutesBefore + 25, machine.state.chainMinutes)
    }

    @Test
    fun strike_marks_armor_break_when_damage_reaches_zero() {
        val scenario = scenario(bossHp = 80, playerDamage = 100)
        val machine = ReturnRaidSequenceStateMachine(scenario)
        repeat(4) { machine.advance() }
        machine.strike()
        machine.advance()

        assertEquals(0, machine.state.displayedHp)
        assertTrue(machine.state.armorBroken)
    }

    @Test
    fun first_25_moment_is_reserved_only_when_crossing_threshold() {
        assertTrue(reservesFirst25Moment(totalFocusMinutes = 25, creditedMinutes = 25))
        assertTrue(reservesFirst25Moment(totalFocusMinutes = 30, creditedMinutes = 10))
        assertFalse(reservesFirst25Moment(totalFocusMinutes = 24, creditedMinutes = 24))
        assertFalse(reservesFirst25Moment(totalFocusMinutes = 50, creditedMinutes = 25))
    }
}
