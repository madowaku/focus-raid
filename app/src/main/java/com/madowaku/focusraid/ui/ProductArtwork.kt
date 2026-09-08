package com.madowaku.focusraid.ui

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import com.madowaku.focusraid.core.domain.CompanionStage

val LocalCompanionIdentity = staticCompositionLocalOf { com.madowaku.focusraid.core.domain.CompanionIdentity.RAG }
val LocalArtworkCatalog = staticCompositionLocalOf { ArtworkCatalog.Production }

/** Stable screen API. Catalog entries may be vectors or raster drawables. No animation runtime. */
@Composable
internal fun CompanionArtwork(
    modifier: Modifier = Modifier,
    stage: CompanionStage = CompanionStage.HATCHLING,
    mood: CompanionMood = CompanionMood.Idle,
    identity: com.madowaku.focusraid.core.domain.CompanionIdentity = LocalCompanionIdentity.current,
) {
    val key = ArtworkKey.Companion(stage, mood, identity)
    val accessible = modifier.clearAndSetSemantics { contentDescription = key.description() }
    when (val source = LocalArtworkCatalog.current.resolve(key)) {
        ArtworkSource.CanvasFallback -> if (identity == com.madowaku.focusraid.core.domain.CompanionIdentity.RAG) {
            FallbackCompanionArtwork(accessible, stage, mood)
        } else FallbackMikoArtwork(accessible, stage, mood)
        is ArtworkSource.Atlas -> AtlasArtwork(source, accessible)
        is ArtworkSource.Drawable -> Image(painterResource(source.resourceId), null, accessible, contentScale = ContentScale.Fit)
    }
}

@Composable
internal fun BossArtwork(
    modifier: Modifier = Modifier,
    identity: BossIdentity = BossIdentity.VOLGA,
    presentation: BossPresentation = BossPresentation.Normal,
) {
    val key = ArtworkKey.Boss(identity, presentation)
    val accessible = modifier.clearAndSetSemantics { contentDescription = key.description() }
    when (val source = LocalArtworkCatalog.current.resolve(key)) {
        ArtworkSource.CanvasFallback -> {
            val pose = accessible.graphicsLayer {
            rotationZ = when (presentation) { BossPresentation.Normal -> 0f; BossPresentation.Damaged -> 8f; BossPresentation.Defeated -> 90f }
            alpha = if (presentation == BossPresentation.Defeated) .5f else 1f
            }
            if (identity == BossIdentity.VOLGA) FallbackBossArtwork(pose) else FallbackMordArtwork(pose)
        }
        is ArtworkSource.Atlas -> AtlasArtwork(source, accessible)
        is ArtworkSource.Drawable -> Image(painterResource(source.resourceId), null, accessible, contentScale = ContentScale.Fit)
    }
}

@Composable
internal fun ItemArtwork(item: com.madowaku.focusraid.core.domain.AdventureItem, modifier: Modifier = Modifier) {
    val key = ArtworkKey.Item(item.id, item.name)
    val accessible = modifier.clearAndSetSemantics { contentDescription = key.description() }
    when (val source = LocalArtworkCatalog.current.resolve(key)) {
        is ArtworkSource.Atlas -> AtlasArtwork(source, accessible)
        is ArtworkSource.Drawable -> Image(painterResource(source.resourceId), null, accessible, contentScale = ContentScale.Fit)
        ArtworkSource.CanvasFallback -> androidx.compose.material3.Text(item.glyph, modifier = accessible)
    }
}
