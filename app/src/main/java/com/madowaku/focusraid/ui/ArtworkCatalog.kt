package com.madowaku.focusraid.ui

import com.madowaku.focusraid.core.domain.CompanionStage

enum class CompanionMood { Idle, Focused, Celebrate }
enum class BossIdentity { VOLGA, MORD }
enum class BossPresentation { Normal, Damaged, Defeated }

sealed interface ArtworkKey {
    data class Companion(val stage: CompanionStage, val mood: CompanionMood, val identity: com.madowaku.focusraid.core.domain.CompanionIdentity = com.madowaku.focusraid.core.domain.CompanionIdentity.RAG) : ArtworkKey
    data class Boss(val identity: BossIdentity, val presentation: BossPresentation) : ArtworkKey
}

sealed interface ArtworkSource {
    data object CanvasFallback : ArtworkSource
    data class Drawable(val resourceId: Int) : ArtworkSource
}

/** Exact slots only: missing celebrate/defeated assets must not silently use the wrong pose. */
class ArtworkCatalog(private val assets: Map<ArtworkKey, ArtworkSource.Drawable> = emptyMap()) {
    fun resolve(key: ArtworkKey): ArtworkSource = assets[key] ?: ArtworkSource.CanvasFallback
    companion object {
        // No final art has been approved. Enable approved resources here, without screen edits.
        val Production = ArtworkCatalog()
    }
}

fun ArtworkKey.description(): String = when (this) {
    is ArtworkKey.Companion -> "相棒${identity.label}・${stage.label}・" + when (mood) {
        CompanionMood.Idle -> "待機中"
        CompanionMood.Focused -> "一緒に集中中"
        CompanionMood.Celebrate -> "集中の戦果を喜んでいます"
    }
    is ArtworkKey.Boss -> (if (identity == BossIdentity.VOLGA) "灰燼竜ヴォルガ・" else "根晶獣モルド・") + when (presentation) {
        BossPresentation.Normal -> "健在"
        BossPresentation.Damaged -> "今回の集中による貢献"
        BossPresentation.Defeated -> "討伐済み"
    }
}
