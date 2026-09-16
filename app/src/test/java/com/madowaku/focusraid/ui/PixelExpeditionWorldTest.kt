package com.madowaku.focusraid.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelExpeditionWorldTest {
    @Test
    fun progress_maps_to_expected_world_stages() {
        assertEquals(PixelExpeditionStage.CAMP, pixelExpeditionStage(0f))
        assertEquals(PixelExpeditionStage.CAMP, pixelExpeditionStage(.199f))
        assertEquals(PixelExpeditionStage.PATH, pixelExpeditionStage(.20f))
        assertEquals(PixelExpeditionStage.RIDGE, pixelExpeditionStage(.40f))
        assertEquals(PixelExpeditionStage.GATE, pixelExpeditionStage(.60f))
        assertEquals(PixelExpeditionStage.RAID, pixelExpeditionStage(.80f))
        assertEquals(PixelExpeditionStage.RAID, pixelExpeditionStage(1f))
    }

    @Test
    fun progress_is_clamped_safely() {
        assertEquals(PixelExpeditionStage.CAMP, pixelExpeditionStage(-1f))
        assertEquals(PixelExpeditionStage.RAID, pixelExpeditionStage(2f))
        assertEquals(120f, pixelExpeditionCameraX(-1f), 0.001f)
        assertEquals(850f, pixelExpeditionCameraX(2f), 0.001f)
    }

    @Test
    fun camera_moves_monotonically_toward_raid() {
        val samples = (0..20).map { pixelExpeditionCameraX(it / 20f) }
        samples.zipWithNext().forEach { (a, b) -> assertTrue(b >= a) }
        assertEquals(120f, samples.first(), 0.001f)
        assertEquals(850f, samples.last(), 0.001f)
    }

    @Test
    fun status_copy_preserves_pause_and_raid_meaning() {
        assertEquals("旅はここで止まっています", pixelExpeditionStatusCopy(.5f, paused = true))
        assertEquals("もう山道まで来た", pixelExpeditionStatusCopy(.5f, paused = false))
        assertEquals("レイド地点の灯が見えてきた", pixelExpeditionStatusCopy(.95f, paused = false))
    }

    @Test
    fun deep_world_spec_has_ordered_five_stage_route() {
        assertEquals(5, DeepPixelExpedition.stages.size)
        assertEquals(
            listOf(
                PixelExpeditionStage.CAMP,
                PixelExpeditionStage.PATH,
                PixelExpeditionStage.RIDGE,
                PixelExpeditionStage.GATE,
                PixelExpeditionStage.RAID,
            ),
            DeepPixelExpedition.stages.map { it.stage },
        )
        assertTrue(DeepPixelExpedition.stages.zipWithNext().all { (a, b) -> b.progressStart > a.progressStart })
        assertTrue(DeepPixelExpedition.stages.zipWithNext().all { (a, b) -> b.cameraX > a.cameraX })
    }
}
