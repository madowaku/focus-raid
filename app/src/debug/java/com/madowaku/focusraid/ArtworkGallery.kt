package com.madowaku.focusraid

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.core.domain.*
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.ui.*

/** Debug-only registration proof; uses the same production resolver/renderer as real screens. */
@Composable
internal fun ArtworkGallery(phase: String) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.systemBarsPadding().padding(12.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FOCUS RAID · ART", style = MaterialTheme.typography.titleMedium)
            Text(phase.removePrefix("ART_"), style = MaterialTheme.typography.labelMedium)
            when {
                phase == "ART_BOSSES" -> BossPresentation.entries.forEach { presentation ->
                    Row(Modifier.fillMaxWidth()) { BossIdentity.entries.forEach { boss ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            BossArtwork(Modifier.size(100.dp), boss, presentation)
                            Text("${boss.name} · ${presentation.name}", fontSize = 10.sp, textAlign = TextAlign.Center)
                        }
                    } }
                }
                phase.startsWith("ART_ITEMS_") -> {
                    val expedition = when (phase) { "ART_ITEMS_TOWER" -> Expedition.TOWER; "ART_ITEMS_ABYSS" -> Expedition.ABYSS; else -> Expedition.STAR_ROUTE }
                    ItemCatalog.all.filter { it.expedition == expedition }.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth()) { row.forEach { item ->
                            Column(Modifier.weight(1f).heightIn(min = 132.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                ItemArtwork(item, Modifier.size(92.dp))
                                Text(item.name, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        } }
                    }
                }
                else -> {
                    val identity = when (phase) { "ART_RAG" -> CompanionIdentity.RAG; "ART_MIKO" -> CompanionIdentity.MIKO; else -> CompanionIdentity.LUNE }
                    CompanionStage.entries.forEach { stage ->
                        Row(Modifier.fillMaxWidth()) { CompanionMood.entries.forEach { mood ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                CompanionArtwork(Modifier.size(96.dp), stage, mood, identity)
                                Text("${stage.label} · ${mood.name}", fontSize = 9.sp, textAlign = TextAlign.Center)
                            }
                        } }
                    }
                }
            }
        }
    }
}
