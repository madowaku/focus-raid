package com.madowaku.focusraid.ui

import com.madowaku.focusraid.core.domain.CompanionStage

enum class CompanionMood { Idle, Focused, Celebrate }
enum class BossIdentity { VOLGA, MORD, ZEPHYR }
enum class BossPresentation { Normal, Damaged, Defeated }

sealed interface ArtworkKey {
    data class Companion(val stage: CompanionStage, val mood: CompanionMood, val identity: com.madowaku.focusraid.core.domain.CompanionIdentity = com.madowaku.focusraid.core.domain.CompanionIdentity.RAG) : ArtworkKey
    data class Boss(val identity: BossIdentity, val presentation: BossPresentation) : ArtworkKey
    data class Item(val id: String, val name: String) : ArtworkKey
}

sealed interface ArtworkSource {
    data object CanvasFallback : ArtworkSource
    data class Drawable(val resourceId: Int) : ArtworkSource
    data class Atlas(val resourceId: Int, val frame: ArtworkFrame) : ArtworkSource
}

/** Exact slots only: missing celebrate/defeated assets must not silently use the wrong pose. */
class ArtworkCatalog(private val assets: Map<ArtworkKey, ArtworkSource> = emptyMap()) {
    fun resolve(key: ArtworkKey): ArtworkSource = assets[key] ?: ArtworkSource.CanvasFallback
    companion object {
        val Production: ArtworkCatalog by lazy { productionArtworkCatalog() }
    }
}

fun ArtworkKey.description(): String = when (this) {
    is ArtworkKey.Item -> "持ちもの・$name"
    is ArtworkKey.Companion -> "相棒${identity.label}・${stage.label}・" + when (mood) {
        CompanionMood.Idle -> "待機中"
        CompanionMood.Focused -> "一緒に集中中"
        CompanionMood.Celebrate -> "集中の戦果を喜んでいます"
    }
    is ArtworkKey.Boss -> (when (identity) { BossIdentity.VOLGA -> "環焔竜ヴォルガ・"; BossIdentity.MORD -> "根晶獣モルド・"; BossIdentity.ZEPHYR -> "星嵐鳥ゼファー・" }) + when (presentation) {
        BossPresentation.Normal -> "健在"
        BossPresentation.Damaged -> "今回の集中による貢献"
        BossPresentation.Defeated -> "討伐済み"
    }
}

/** Normalized registration bounds measured against the generated atlas, not assumed grid cells. */
data class ArtworkFrame(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init { require(left >= 0f && top >= 0f && right <= 1f && bottom <= 1f && left < right && top < bottom) }
}
