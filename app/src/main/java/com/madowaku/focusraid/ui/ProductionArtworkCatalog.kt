package com.madowaku.focusraid.ui

import com.madowaku.focusraid.R
import com.madowaku.focusraid.core.domain.*
import com.madowaku.focusraid.core.model.Expedition

internal fun productionArtworkCatalog(): ArtworkCatalog {
    val assets = mutableMapOf<ArtworkKey, ArtworkSource>()
    val companionSheets = mapOf(CompanionIdentity.RAG to R.drawable.art_rag,
        CompanionIdentity.MIKO to R.drawable.art_miko, CompanionIdentity.LUNE to R.drawable.art_lune)
    companionSheets.forEach { (identity, resource) ->
        CompanionStage.entries.forEachIndexed { column, stage ->
            CompanionMood.entries.forEachIndexed { row, mood ->
                assets[ArtworkKey.Companion(stage, mood, identity)] = ArtworkSource.Atlas(resource,
                    registeredArtworkFrame(resource, row * 5 + column))
            }
        }
    }
    BossIdentity.entries.forEachIndexed { column, identity ->
        BossPresentation.entries.forEachIndexed { row, presentation ->
            assets[ArtworkKey.Boss(identity, presentation)] = ArtworkSource.Atlas(R.drawable.art_bosses,
                registeredArtworkFrame(R.drawable.art_bosses, row * 3 + column))
        }
    }
    // Stable item IDs, explicitly registered in the illustrator's row-major order. Adding drops
    // cannot silently shift old icons. New common/rare items occupy the final two cells.
    val suffixes = listOf("common-1", "common-2", "common-3", "common-4", "rare-1", "rare-2",
        "rare-3", "epic-1", "epic-2", "legendary-1", "common-5", "rare-4")
    mapOf(Expedition.TOWER to R.drawable.art_items_tower, Expedition.ABYSS to R.drawable.art_items_abyss,
        Expedition.STAR_ROUTE to R.drawable.art_items_star).forEach { (expedition, resource) ->
        suffixes.forEachIndexed { index, suffix ->
            val item = ItemCatalog.all.single { it.id == "${expedition.name.lowercase()}-$suffix" }
            assets[ArtworkKey.Item(item.id, item.name)] = ArtworkSource.Atlas(resource,
                registeredArtworkFrame(resource, index))
        }
    }
    return ArtworkCatalog(assets)
}
