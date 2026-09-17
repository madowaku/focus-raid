package com.madowaku.focusraid.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelExpeditionDestinationTest {
    @Test
    fun locationTitlesDescribePlacesNotProgressLabels() {
        assertEquals("夜明け前のキャンプ", destinationLocationTitle(PixelExpeditionStage.CAMP))
        assertEquals("灯りの森道", destinationLocationTitle(PixelExpeditionStage.PATH))
        assertEquals("灰の稜線", destinationLocationTitle(PixelExpeditionStage.RIDGE))
        assertEquals("火口への石段", destinationLocationTitle(PixelExpeditionStage.GATE))
        assertEquals("火口の門", destinationLocationTitle(PixelExpeditionStage.RAID))
    }

    @Test
    fun lastMinuteNamesVolgaAsDestination() {
        assertEquals(
            "あと1分、ヴォルガの棲む火口へ。",
            destinationJourneyCopy(PixelExpeditionStage.RAID, paused = false, lastMinute = true),
        )
    }

    @Test
    fun pausedCopyKeepsCurrentPlace() {
        assertEquals(
            "ラグの卵と、稜線でひと休み。",
            destinationJourneyCopy(PixelExpeditionStage.RIDGE, paused = true, lastMinute = false),
        )
        assertEquals(
            "ラグの卵と、門の前でひと休み。",
            destinationJourneyCopy(PixelExpeditionStage.RAID, paused = true, lastMinute = true),
        )
    }

    @Test
    fun departureCopyPointsTowardTheFireMountain() {
        assertEquals(
            "ラグの卵と、遠くに灯る火口へ。",
            destinationJourneyCopy(PixelExpeditionStage.CAMP, paused = false, lastMinute = false),
        )
    }
}
