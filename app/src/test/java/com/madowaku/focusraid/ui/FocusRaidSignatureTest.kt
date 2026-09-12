package com.madowaku.focusraid.ui

import com.madowaku.focusraid.core.model.Expedition
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
        assertEquals("25分、出発する", signatureDepartureLabel(25, Expedition.TOWER))
        assertEquals("45分、出発する", signatureDepartureLabel(45, Expedition.ABYSS))
        assertEquals("✦  25分、星渡りへ", signatureDepartureLabel(25, Expedition.STAR_ROUTE))
    }
}
