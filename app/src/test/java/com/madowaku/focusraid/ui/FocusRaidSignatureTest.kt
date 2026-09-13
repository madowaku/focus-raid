package com.madowaku.focusraid.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusRaidSignatureTest {
    @Test
    fun journeyIndexClampsAndAdvancesAcrossFiveStops() {
        assertEquals(0, signatureJourneyIndex(-1f))
        assertEquals(0, signatureJourneyIndex(0.19f))
        assertEquals(1, signatureJourneyIndex(0.20f))
        assertEquals(3, signatureJourneyIndex(0.79f))
        assertEquals(4, signatureJourneyIndex(0.80f))
        assertEquals(4, signatureJourneyIndex(2f))
    }

    @Test
    fun departureCopyKeepsFocusMinutesPrimary() {
        assertEquals("25分集中する", signatureDepartureLabel(25))
        assertEquals("45分集中する", signatureDepartureLabel(45))
        assertEquals("60分集中する", signatureDepartureLabel(60))
    }

    @Test
    fun bossPresentationFollowsHpRatio() {
        assertEquals(BossPresentation.Normal, signatureBossPresentation(51, 100))
        assertEquals(BossPresentation.Damaged, signatureBossPresentation(50, 100))
        assertEquals(BossPresentation.Damaged, signatureBossPresentation(1, 100))
        assertEquals(BossPresentation.Defeated, signatureBossPresentation(0, 100))
    }
}
