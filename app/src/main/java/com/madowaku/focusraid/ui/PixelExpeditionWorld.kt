package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.roundToInt

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
        PixelExpeditionStage.CAMP -> "キャンプを離れ、静かに歩きはじめた"
        PixelExpeditionStage.PATH -> "森道を越えて、世界の奥へ"
        PixelExpeditionStage.RIDGE -> "もう山道まで来た"
        PixelExpeditionStage.GATE -> "レイド地点の灯が近づいてきた"
        PixelExpeditionStage.RAID -> "レイド地点の灯が見えてきた"
    }
}

/**
 * Procedural first-pass renderer for the Pixel Expedition vertical slice.
 *
 * This intentionally uses geometry instead of final bitmap art so the travel/camera language can
 * be validated before committing to a sprite pipeline. Final art can replace each layer without
 * changing the progress/camera contract.
 */
@Composable
internal fun PixelExpeditionWorld(
    progress: Float,
    paused: Boolean,
    modifier: Modifier = Modifier,
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val cameraX = pixelExpeditionCameraX(safeProgress)
    val stage = pixelExpeditionStage(safeProgress)

    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            fun sx(worldX: Float, factor: Float = 1f): Float {
                val virtualViewport = 420f
                val center = 500f
                return center + (worldX - cameraX * factor) / virtualViewport * w
            }

            val skyTop = if (stage >= PixelExpeditionStage.GATE) Color(0xFF151126) else Color(0xFF091120)
            val skyBottom = if (stage >= PixelExpeditionStage.GATE) Color(0xFF45211F) else Color(0xFF172744)
            drawRect(
                brush = Brush.verticalGradient(listOf(skyTop, skyBottom)),
                size = size,
            )

            // Moon / distant raid glow.
            drawCircle(
                color = Color(0xFFE6E3C9).copy(alpha = if (paused) .30f else .62f),
                radius = h * .045f,
                center = Offset(w * .15f, h * .19f),
            )
            val raidGlowX = sx(920f, .30f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF9B45).copy(alpha = if (paused) .10f else .36f + safeProgress * .22f),
                        Color.Transparent,
                    ),
                    center = Offset(raidGlowX, h * .36f),
                    radius = h * .28f,
                ),
                radius = h * .28f,
                center = Offset(raidGlowX, h * .36f),
            )

            // Parallax mountain silhouettes.
            repeat(8) { index ->
                val worldX = index * 170f - 60f
                val x = sx(worldX, .30f)
                val baseY = h * .58f
                val mountain = Path().apply {
                    moveTo(x - w * .22f, baseY)
                    lineTo(x, h * (.26f + (index % 3) * .035f))
                    lineTo(x + w * .22f, baseY)
                    close()
                }
                drawPath(mountain, Color(0xFF17243A).copy(alpha = .92f))
            }

            // Chasm / lower world.
            drawRect(Color(0xFF070B12), topLeft = Offset(0f, h * .64f), size = Size(w, h * .36f))

            // Travel path, transformed by the main camera.
            val pathPoints = listOf(
                70f to .76f,
                190f to .68f,
                320f to .71f,
                450f to .61f,
                585f to .66f,
                720f to .56f,
                840f to .60f,
                930f to .50f,
            )
            val road = Path()
            pathPoints.forEachIndexed { index, (worldX, yRatio) ->
                val p = Offset(sx(worldX), h * yRatio)
                if (index == 0) road.moveTo(p.x, p.y) else road.lineTo(p.x, p.y)
            }
            drawPath(
                path = road,
                color = Color(0xFF7D7161),
                style = Stroke(width = h * .045f, cap = StrokeCap.Round),
            )
            drawPath(
                path = road,
                color = Color(0xFFD1B57A).copy(alpha = if (paused) .30f else .54f),
                style = Stroke(width = h * .012f, cap = StrokeCap.Round),
            )

            // Camp at the beginning of the world.
            val campX = sx(85f)
            val campY = h * .74f
            drawRect(Color(0xFF6E4935), Offset(campX - w * .055f, campY - h * .05f), Size(w * .11f, h * .055f))
            val tent = Path().apply {
                moveTo(campX - w * .10f, campY)
                lineTo(campX, campY - h * .13f)
                lineTo(campX + w * .10f, campY)
                close()
            }
            drawPath(tent, Color(0xFF9A633C))
            drawCircle(
                color = Color(0xFFFFC14F).copy(alpha = if (paused) .35f else .95f),
                radius = h * .025f,
                center = Offset(campX + w * .13f, campY),
            )

            // Gate / raid approach.
            val gateX = sx(760f)
            val gateY = h * .59f
            drawRect(Color(0xFF39313A), Offset(gateX - w * .10f, gateY - h * .15f), Size(w * .055f, h * .17f))
            drawRect(Color(0xFF39313A), Offset(gateX + w * .045f, gateY - h * .15f), Size(w * .055f, h * .17f))
            drawRect(Color(0xFF55424A), Offset(gateX - w * .10f, gateY - h * .15f), Size(w * .20f, h * .035f))

            // Distant Volga: first-pass silhouette. Scale/visibility grows as the raid approaches.
            val bossX = sx(920f)
            val bossY = h * .42f
            val bossScale = .55f + safeProgress * .70f
            val bossAlpha = if (paused) .34f else .48f + safeProgress * .38f
            drawCircle(Color(0xFF1B1420).copy(alpha = bossAlpha), h * .105f * bossScale, Offset(bossX, bossY))
            drawCircle(Color(0xFFDF6235).copy(alpha = bossAlpha), h * .018f * bossScale, Offset(bossX - w * .018f, bossY - h * .015f))
            drawLine(
                color = Color(0xFFD34930).copy(alpha = bossAlpha),
                start = Offset(bossX, bossY + h * .04f),
                end = Offset(bossX + w * .10f * bossScale, bossY - h * .11f * bossScale),
                strokeWidth = h * .015f,
                cap = StrokeCap.Round,
            )

            // Companion traveler remains near the middle while the world moves around it.
            val travelerX = w * .48f
            val travelerY = when (stage) {
                PixelExpeditionStage.CAMP -> h * .72f
                PixelExpeditionStage.PATH -> h * .66f
                PixelExpeditionStage.RIDGE -> h * .60f
                PixelExpeditionStage.GATE -> h * .56f
                PixelExpeditionStage.RAID -> h * .52f
            }
            val eggW = w * .075f
            val eggH = h * .105f
            drawOval(
                brush = Brush.verticalGradient(
                    listOf(Color(0xFFFFE8A2), Color(0xFFFF9A3E), Color(0xFFC64D27)),
                    startY = travelerY - eggH / 2f,
                    endY = travelerY + eggH / 2f,
                ),
                topLeft = Offset(travelerX - eggW / 2f, travelerY - eggH / 2f),
                size = Size(eggW, eggH),
            )
            drawCircle(
                color = Color(0xFFFFD76A).copy(alpha = if (paused) .20f else .32f),
                radius = h * .085f,
                center = Offset(travelerX, travelerY),
                style = Stroke(width = 2f),
            )

            // Sparse world lights hint that other focus journeys exist without implying fake live users.
            listOf(340f, 545f, 675f).forEachIndexed { index, worldX ->
                val x = sx(worldX)
                val y = h * listOf(.61f, .58f, .54f)[index]
                drawCircle(
                    color = Color(0xFFDDE6FF).copy(alpha = if (paused) .12f else .34f),
                    radius = h * .008f,
                    center = Offset(x, y),
                )
            }

            if (paused) {
                drawRect(Color.Black.copy(alpha = .30f), size = size)
            }
        }
    }
}
