package com.madowaku.focusraid.ui

import com.madowaku.focusraid.core.domain.CompanionStage
import org.junit.Assert.*
import org.junit.Test

class ArtworkCatalogTest {
    @Test fun `every growth mood and boss slot resolves to fallback by default`() {
        com.madowaku.focusraid.core.domain.CompanionIdentity.entries.forEach { identity -> CompanionStage.entries.forEach { stage -> CompanionMood.entries.forEach { mood ->
            val key = ArtworkKey.Companion(stage, mood, identity)
            assertEquals(ArtworkSource.CanvasFallback, ArtworkCatalog.Production.resolve(key))
            assertTrue(key.description().contains(stage.label))
        } } }
        BossPresentation.entries.forEach {
            assertEquals(ArtworkSource.CanvasFallback, ArtworkCatalog.Production.resolve(ArtworkKey.Boss(BossIdentity.VOLGA, it)))
        }
    }
    @Test fun `an asset replaces exactly its slot without changing neighboring states`() {
        val idle = ArtworkKey.Companion(CompanionStage.HATCHLING, CompanionMood.Idle)
        val catalog = ArtworkCatalog(mapOf(idle to ArtworkSource.Drawable(123)))
        assertEquals(ArtworkSource.Drawable(123), catalog.resolve(idle))
        assertEquals(ArtworkSource.CanvasFallback, catalog.resolve(idle.copy(mood = CompanionMood.Focused)))
        assertEquals(ArtworkSource.CanvasFallback, catalog.resolve(idle.copy(stage = CompanionStage.EGG)))
        assertFalse(ArtworkKey.Boss(BossIdentity.VOLGA, BossPresentation.Damaged).description().contains("討伐済み"))
    }
}
