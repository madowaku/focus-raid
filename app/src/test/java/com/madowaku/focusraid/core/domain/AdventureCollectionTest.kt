package com.madowaku.focusraid.core.domain

import com.madowaku.focusraid.core.model.*
import org.junit.Test
import org.junit.Assert.*

class AdventureCollectionTest {
    private fun entry(id: String, minutes: Int, expedition: Expedition = Expedition.ABYSS) = SessionHistoryEntry(
        id, 1, minutes, minutes, expedition, SessionOutcome.ABORTED, minutes, Rarity.RARE, "古代の鍵",
    )
    @Test fun `companion unlock is earned at exact threshold and never paid`() {
        assertTrue(AdventureCollection.canSelect(CompanionIdentity.RAG, 0))
        assertFalse(AdventureCollection.canSelect(CompanionIdentity.MIKO, 74))
        assertTrue(AdventureCollection.canSelect(CompanionIdentity.MIKO, 75))
    }
    @Test fun `boss counts only Abyss persisted credit including partial sessions once`() {
        val row = entry("a", 100)
        val progress = AdventureCollection.mordProgress(listOf(row, row, entry("b", 80), entry("c", 500, Expedition.TOWER)))
        assertEquals(180, progress.creditedMinutes)
        assertEquals(0, progress.remainingHp)
        assertTrue(progress.defeated)
        assertEquals(1f, progress.progress)
    }
    @Test fun `inventory preserves old items and does not duplicate replayed history`() {
        val row = entry("a", 25)
        val old = entry("old", 25).copy(discovery = "過去の記念品")
        val inventory = ItemCatalog.owned(listOf(row, row, entry("b",25), old))
        assertEquals(2, inventory.single { it.item.name == "古代の鍵" }.count)
        assertEquals(1, inventory.single { it.item.name == "過去の記念品" }.count)
        assertEquals(ItemCatalog.all.size, ItemCatalog.all.map { it.id }.distinct().size)
        Expedition.entries.forEach { expedition -> Rarity.entries.forEach { rarity ->
            assertTrue(ItemCatalog.pool(expedition, rarity).isNotEmpty())
        } }
    }
}
