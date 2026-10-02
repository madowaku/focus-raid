package com.madowaku.focusraid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * v0.10 vertical-slice world model.
 *
 * Progress remains sourced from FocusUiState.progress. This file only maps that value to a
 * presentation-stage and a virtual camera position. No timer/session state is duplicated here.
 */
internal enum class PixelExpeditionStage {
    CAMP,
    PATH,
    RIDGE,
    GATE,
    RAID,
}

internal data class PixelWorldStage(
    val stage: PixelExpeditionStage,
    val progressStart: Float,
    val cameraX: Float,
    val label: String,
)

internal data class PixelExpeditionWorldSpec(
    val id: String,
    val stages: List<PixelWorldStage>,
)

internal val DeepPixelExpedition = PixelExpeditionWorldSpec(
    id = "deep",
    stages = listOf(
        PixelWorldStage(PixelExpeditionStage.CAMP, 0.00f, 120f, "キャンプ"),
        PixelWorldStage(PixelExpeditionStage.PATH, 0.20f, 300f, "森道"),
        PixelWorldStage(PixelExpeditionStage.RIDGE, 0.40f, 500f, "山道"),
        PixelWorldStage(PixelExpeditionStage.GATE, 0.60f, 700f, "門"),
        PixelWorldStage(PixelExpeditionStage.RAID, 0.80f, 850f, "レイド地点"),
    ),
)

internal fun pixelExpeditionStage(progress: Float): PixelExpeditionStage {
    val safe = progress.coerceIn(0f, 1f)
    return when {
        safe < .20f -> PixelExpeditionStage.CAMP
        safe < .40f -> PixelExpeditionStage.PATH
        safe < .60f -> PixelExpeditionStage.RIDGE
        safe < .80f -> PixelExpeditionStage.GATE
        else -> PixelExpeditionStage.RAID
    }
}

/** Monotonic virtual camera coordinate. */
internal fun pixelExpeditionCameraX(progress: Float): Float {
    val safe = progress.coerceIn(0f, 1f)
    return 120f + (850f - 120f) * safe
}

internal fun pixelExpeditionStatusCopy(progress: Float, paused: Boolean): String {
    if (paused) return "旅はここで止まっています"
    return when (pixelExpeditionStage(progress)) {
        PixelExpeditionStage.CAMP -> "夜明け前のキャンプを出る"
        PixelExpeditionStage.PATH -> "灯りを頼りに、森道の奥へ"
        PixelExpeditionStage.RIDGE -> "もう山道まで来た"
        PixelExpeditionStage.GATE -> "火口の門が見えてきた"
        PixelExpeditionStage.RAID -> "ヴォルガの気配が近い"
    }
}

/** Scenery changes at stage boundaries; no continuous animation while focusing. */
@Composable
internal fun PixelExpeditionWorld(progress: Float, paused: Boolean, modifier: Modifier = Modifier) {
    val stage = pixelExpeditionStage(progress)
    val resource = when (stage) {
        PixelExpeditionStage.CAMP, PixelExpeditionStage.PATH -> com.madowaku.focusraid.R.drawable.world_camp_v13
        PixelExpeditionStage.RIDGE, PixelExpeditionStage.GATE -> com.madowaku.focusraid.R.drawable.world_ridge_v13
        PixelExpeditionStage.RAID -> com.madowaku.focusraid.R.drawable.world_arena_v13
    }
    Box(modifier) {
        RaidEnvironment(resource, Modifier.alpha(if (paused) .70f else 1f))
        if (stage == PixelExpeditionStage.RAID) {
            BossArtwork(Modifier.align(Alignment.CenterEnd).padding(end = 20.dp).size(150.dp),
                presentation = BossPresentation.Normal, frameless = true)
        }
        PixelRagEggSprite(Modifier.align(Alignment.BottomStart).padding(start = 36.dp, bottom = 16.dp).size(48.dp))
    }
}
