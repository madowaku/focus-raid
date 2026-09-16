package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

/**
 * Procedural first-pass renderer for the Pixel Expedition vertical slice.
 *
 * Geometry owns the world depth and camera language. Crisp text-defined sprites sit above it so
 * the slice already reads as an intentional pixel world before final bitmap art exists.
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
                val center = w * .50f
                return center + (worldX - cameraX * factor) / virtualViewport * w
            }

            val skyTop = if (stage >= PixelExpeditionStage.GATE) Color(0xFF171024) else Color(0xFF08101E)
            val skyBottom = if (stage >= PixelExpeditionStage.GATE) Color(0xFF54261E) else Color(0xFF1C2C49)
            drawRect(
                brush = Brush.verticalGradient(listOf(skyTop, skyBottom)),
                size = size,
            )

            // Sparse stars and a moon keep the upper half calm rather than game-HUD busy.
            listOf(
                .07f to .17f, .18f to .10f, .31f to .20f, .48f to .12f,
                .62f to .18f, .78f to .09f, .92f to .22f,
            ).forEach { (x, y) ->
                drawRect(
                    Color(0xFFDCE6F6).copy(alpha = if (paused) .22f else .52f),
                    topLeft = Offset(w * x, h * y),
                    size = Size(w * .006f, w * .006f),
                )
            }
            drawCircle(
                color = Color(0xFFE6E3C9).copy(alpha = if (paused) .26f else .58f),
                radius = h * .045f,
                center = Offset(w * .15f, h * .19f),
            )

            // The raid glow gets warmer and larger as the camera approaches Volga.
            val raidGlowX = sx(920f, .30f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFF9B45).copy(alpha = if (paused) .10f else .28f + safeProgress * .34f),
                        Color(0xFFE85732).copy(alpha = if (paused) .04f else .10f + safeProgress * .12f),
                        Color.Transparent,
                    ),
                    center = Offset(raidGlowX, h * .36f),
                    radius = h * (.24f + safeProgress * .08f),
                ),
                radius = h * (.24f + safeProgress * .08f),
                center = Offset(raidGlowX, h * .36f),
            )

            // Far mountains: slow parallax.
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
                drawPath(
                    mountain,
                    if (index % 2 == 0) Color(0xFF1A2940) else Color(0xFF222A45),
                )
            }

            // Mid-ground cliffs create a valley the traveler actually crosses.
            repeat(6) { index ->
                val worldX = index * 210f - 40f
                val x = sx(worldX, .72f)
                val base = h * .72f
                val ridge = Path().apply {
                    moveTo(x - w * .30f, base)
                    lineTo(x - w * .14f, h * (.52f + (index % 2) * .06f))
                    lineTo(x + w * .04f, h * (.58f - (index % 3) * .025f))
                    lineTo(x + w * .27f, base)
                    close()
                }
                drawPath(ridge, Color(0xFF101A2A).copy(alpha = .96f))
            }

            // Chasm / lower world.
            drawRect(Color(0xFF050910), topLeft = Offset(0f, h * .70f), size = Size(w, h * .30f))

            // Travel path, transformed by the main camera.
            val pathPoints = listOf(
                70f to .78f,
                190f to .70f,
                320f to .73f,
                450f to .63f,
                585f to .67f,
                720f to .57f,
                840f to .61f,
                930f to .51f,
            )
            val road = Path()
            pathPoints.forEachIndexed { index, (worldX, yRatio) ->
                val p = Offset(sx(worldX), h * yRatio)
                if (index == 0) road.moveTo(p.x, p.y) else road.lineTo(p.x, p.y)
            }
            drawPath(
                path = road,
                color = Color(0xFF6E665E),
                style = Stroke(width = h * .050f, cap = StrokeCap.Round),
            )
            drawPath(
                path = road,
                color = Color(0xFFCDB07A).copy(alpha = if (paused) .24f else .56f),
                style = Stroke(width = h * .012f, cap = StrokeCap.Round),
            )

            // Camp at the beginning of the world.
            val campX = sx(85f)
            val campY = h * .77f
            val tent = Path().apply {
                moveTo(campX - w * .10f, campY)
                lineTo(campX, campY - h * .13f)
                lineTo(campX + w * .10f, campY)
                close()
            }
            drawPath(tent, Color(0xFF8B5A3B))
            drawPath(
                Path().apply {
                    moveTo(campX - w * .065f, campY)
                    lineTo(campX, campY - h * .09f)
                    lineTo(campX + w * .065f, campY)
                    close()
                },
                Color(0xFFB67B4B),
            )
            drawCircle(
                color = Color(0xFFFFC14F).copy(alpha = if (paused) .30f else .95f),
                radius = h * .024f,
                center = Offset(campX + w * .14f, campY),
            )
            drawCircle(
                color = Color(0xFFFF8D3B).copy(alpha = if (paused) .12f else .24f),
                radius = h * .085f,
                center = Offset(campX + w * .14f, campY),
            )

            // Lanterns mark the route without pretending to be live users.
            listOf(250f to .69f, 470f to .62f, 675f to .58f).forEach { (worldX, yRatio) ->
                val x = sx(worldX)
                val y = h * yRatio
                drawLine(
                    color = Color(0xFF554B45),
                    start = Offset(x, y),
                    end = Offset(x, y - h * .085f),
                    strokeWidth = w * .010f,
                )
                drawRect(
                    color = Color(0xFFFFD06C).copy(alpha = if (paused) .26f else .92f),
                    topLeft = Offset(x - w * .012f, y - h * .095f),
                    size = Size(w * .024f, h * .030f),
                )
                drawCircle(
                    color = Color(0xFFFFB44E).copy(alpha = if (paused) .08f else .14f),
                    radius = h * .060f,
                    center = Offset(x, y - h * .080f),
                )
            }

            // Gate / raid approach.
            val gateX = sx(760f)
            val gateY = h * .60f
            drawRect(Color(0xFF352F39), Offset(gateX - w * .105f, gateY - h * .17f), Size(w * .060f, h * .19f))
            drawRect(Color(0xFF352F39), Offset(gateX + w * .045f, gateY - h * .17f), Size(w * .060f, h * .19f))
            drawRect(Color(0xFF58434A), Offset(gateX - w * .105f, gateY - h * .17f), Size(w * .21f, h * .038f))
            drawRect(
                Color(0xFFFF8B43).copy(alpha = .18f + safeProgress * .20f),
                Offset(gateX - w * .08f, gateY - h * .125f),
                Size(w * .16f, h * .115f),
            )

            // Near foreground: faster parallax makes the scene feel spatial, not wallpaper-flat.
            val leftForeground = sx(cameraX - 210f, 1.15f)
            val rightForeground = sx(cameraX + 230f, 1.15f)
            val foregroundColor = Color(0xFF05080D).copy(alpha = .98f)
            drawPath(
                Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * .80f)
                    lineTo(leftForeground, h * .74f)
                    lineTo(leftForeground + w * .18f, h)
                    close()
                },
                foregroundColor,
            )
            drawPath(
                Path().apply {
                    moveTo(w, h)
                    lineTo(w, h * .76f)
                    lineTo(rightForeground, h * .70f)
                    lineTo(rightForeground - w * .18f, h)
                    close()
                },
                foregroundColor,
            )

            if (paused) {
                drawRect(Color.Black.copy(alpha = .27f), size = size)
            }
        }

        // Crisp sprites live above the procedural depth layers. They can later be replaced by final
        // bitmap/atlas assets without touching camera or session logic.
        val bossSize = when (stage) {
            PixelExpeditionStage.CAMP -> 54.dp
            PixelExpeditionStage.PATH -> 64.dp
            PixelExpeditionStage.RIDGE -> 76.dp
            PixelExpeditionStage.GATE -> 94.dp
            PixelExpeditionStage.RAID -> 116.dp
        }
        PixelVolgaSprite(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = when (stage) {
                    PixelExpeditionStage.CAMP -> 62.dp
                    PixelExpeditionStage.PATH -> 66.dp
                    PixelExpeditionStage.RIDGE -> 72.dp
                    PixelExpeditionStage.GATE -> 76.dp
                    PixelExpeditionStage.RAID -> 82.dp
                }, end = 18.dp)
                .size(bossSize)
                .alpha(if (paused) .38f else .62f + safeProgress * .30f),
        )

        PixelRagEggSprite(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = when (stage) {
                        PixelExpeditionStage.CAMP -> 78.dp
                        PixelExpeditionStage.PATH -> 104.dp
                        PixelExpeditionStage.RIDGE -> 128.dp
                        PixelExpeditionStage.GATE -> 146.dp
                        PixelExpeditionStage.RAID -> 164.dp
                    },
                    bottom = when (stage) {
                        PixelExpeditionStage.CAMP -> 58.dp
                        PixelExpeditionStage.PATH -> 76.dp
                        PixelExpeditionStage.RIDGE -> 92.dp
                        PixelExpeditionStage.GATE -> 104.dp
                        PixelExpeditionStage.RAID -> 112.dp
                    },
                )
                .size(if (stage >= PixelExpeditionStage.GATE) 52.dp else 46.dp)
                .alpha(if (paused) .56f else 1f),
        )
    }
}
