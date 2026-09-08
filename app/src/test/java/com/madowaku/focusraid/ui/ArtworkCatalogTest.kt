package com.madowaku.focusraid.ui

import com.madowaku.focusraid.core.domain.CompanionStage
import org.junit.Assert.*
import org.junit.Test

class ArtworkCatalogTest {
    @Test fun `every growth mood and boss slot resolves to fallback in an empty catalog`() {
        com.madowaku.focusraid.core.domain.CompanionIdentity.entries.forEach { identity -> CompanionStage.entries.forEach { stage -> CompanionMood.entries.forEach { mood ->
            val key = ArtworkKey.Companion(stage, mood, identity)
            assertEquals(ArtworkSource.CanvasFallback, ArtworkCatalog().resolve(key))
            assertTrue(key.description().contains(stage.label))
        } } }
        BossPresentation.entries.forEach {
            assertEquals(ArtworkSource.CanvasFallback, ArtworkCatalog().resolve(ArtworkKey.Boss(BossIdentity.VOLGA, it)))
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

    @Test fun `production has distinct registered artwork for every character state and item`() {
        val keys = buildList<ArtworkKey> {
            com.madowaku.focusraid.core.domain.CompanionIdentity.entries.forEach { identity ->
                CompanionStage.entries.forEach { stage -> CompanionMood.entries.forEach { mood -> add(ArtworkKey.Companion(stage, mood, identity)) } }
            }
            BossIdentity.entries.forEach { identity -> BossPresentation.entries.forEach { add(ArtworkKey.Boss(identity, it)) } }
            com.madowaku.focusraid.core.domain.ItemCatalog.all.forEach { add(ArtworkKey.Item(it.id, it.name)) }
        }
        assertEquals(90, keys.size)
        val sources = keys.map { ArtworkCatalog.Production.resolve(it) }
        assertTrue(sources.all { it is ArtworkSource.Atlas })
        assertEquals(90, sources.distinct().size)
        assertEquals(7, sources.filterIsInstance<ArtworkSource.Atlas>().map { it.resourceId }.distinct().size)
    }
    @Test fun `frame registration rejects out of bounds and legacy items retain safe fallback`() {
        assertThrows(IllegalArgumentException::class.java) { ArtworkFrame(-.1f, 0f, 1f, 1f) }
        assertThrows(IllegalArgumentException::class.java) { ArtworkFrame(.8f, 0f, .2f, 1f) }
        assertEquals(ArtworkSource.CanvasFallback, ArtworkCatalog.Production.resolve(ArtworkKey.Item("legacy-old", "昔の記念品")))
    }
}
