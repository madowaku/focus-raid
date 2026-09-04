package com.madowaku.focusraid.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CompanionGrowthTest {
    @Test
    fun `fresh install starts as egg with 75 minutes remaining`() {
        val status = CompanionGrowth.from(0)

        assertEquals(CompanionStage.EGG, status.stage)
        assertEquals(75, status.remainingMinutes)
        assertEquals(75, status.nextThresholdMinutes)
        assertEquals("幼体", status.nextStageLabel)
        assertEquals(0f, status.progress, 0.0001f)
    }

    @Test
    fun `75 minutes enters hatchling stage`() {
        val status = CompanionGrowth.from(75)

        assertEquals(CompanionStage.HATCHLING, status.stage)
        assertEquals(645, status.remainingMinutes)
        assertEquals(720, status.nextThresholdMinutes)
        assertEquals(0f, status.progress, 0.0001f)
    }

    @Test
    fun `645 minutes is 75 minutes from first growth`() {
        val status = CompanionGrowth.from(645)

        assertEquals(CompanionStage.HATCHLING, status.stage)
        assertEquals(75, status.remainingMinutes)
        assertEquals("第一成長", status.nextStageLabel)
        assertTrue(status.progress > 0.88f && status.progress < 0.89f)
    }

    @Test
    fun `growth thresholds switch stages exactly`() {
        assertEquals(CompanionStage.FIRST_GROWTH, CompanionGrowth.from(720).stage)
        assertEquals(CompanionStage.SECOND_GROWTH, CompanionGrowth.from(1_800).stage)
        assertEquals(CompanionStage.MATURE, CompanionGrowth.from(4_500).stage)
    }

    @Test
    fun `mature companion has no next milestone`() {
        val status = CompanionGrowth.from(9_000)

        assertEquals(CompanionStage.MATURE, status.stage)
        assertEquals(1f, status.progress, 0.0001f)
        assertEquals(0, status.remainingMinutes)
        assertNull(status.nextThresholdMinutes)
        assertNull(status.nextStageLabel)
    }

    @Test
    fun `negative minutes are clamped to zero`() {
        val status = CompanionGrowth.from(-50)

        assertEquals(CompanionStage.EGG, status.stage)
        assertEquals(0, status.totalMinutes)
        assertEquals(75, status.remainingMinutes)
    }
}
