package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * v0.11 raid arena presentation layer.
 *
 * This is deliberately presentation-only. ReturnRaidSequenceStateMachine remains the source of
 * truth for phase, echo order and HP. The arena merely makes those states feel like the physical
 * destination reached at the end of Pixel Expedition.
 */
@Composable
internal fun PixelReturnRaidWorld(
    phase: ReturnRaidPhase,
    armorBroken: Boolean,
    bossPresentation: BossPresentation,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val defeated = phase == ReturnRaidPhase.RESULT && armorBroken
            val result = phase == ReturnRaidPhase.RESULT || phase == ReturnRaidPhase.CAMP

            drawRect(
                brush = Brush.verticalGradient(
                    listOf(
                        if (defeated) Color(0xFF171725) else Color(0xFF120C16),
                        if (defeated) Color(0xFF2C2732) else Color(0xFF3A1816),
                        Color(0xFF080B12),
                    ),
                ),
                size = size,
            )

            repeat(7) { index ->
                val centerX = w * (index / 6f)
                val baseY = h * .55f
                val peakY = h * (.20f + (index % 3) * .055f)
                val mountain = Path().apply {
                    moveTo(centerX - w * .22f, baseY)
                    lineTo(centerX, peakY)
                    lineTo(centerX + w * .22f, baseY)
                    close()
                }
                drawPath(
                    mountain,
                    if (defeated) Color(0xFF2B2A38) else Color(0xFF241825),
                )
            }

            val arenaY = h * .72f
            drawRect(
                color = Color(0xFF16151D),
                topLeft = Offset(0f, arenaY),
                size = Size(w, h - arenaY),
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFF39313B), Color(0xFF6F4A3D), Color(0xFF3A3037)),
                ),
                topLeft = Offset(w * .06f, arenaY - h * .035f),
                size = Size(w * .88f, h * .075f),
            )

            listOf(.12f, .22f, .78f, .88f).forEachIndexed { index, xRatio ->
                val top = h * if (index % 2 == 0) .43f else .49f
                val columnH = arenaY - top
                drawRect(
                    color = Color(0xFF3B303A),
                    topLeft = Offset(w * xRatio, top),
                    size = Size(w * .035f, columnH),
                )
                drawRect(
                    color = Color(0xFF67504C),
                    topLeft = Offset(w * (xRatio - .012f), top - h * .018f),
                    size = Size(w * .06f, h * .024f),
                )
            }

            val glowAlpha = when {
                defeated -> .10f
                result -> .18f
                else -> .34f
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFFFF793C).copy(alpha = glowAlpha),
                        Color(0xFFB63A2A).copy(alpha = glowAlpha * .45f),
                        Color.Transparent,
                    ),
                    center = Offset(w * .66f, h * .40f),
                    radius = h * .32f,
                ),
                center = Offset(w * .66f, h * .40f),
                radius = h * .32f,
            )

            listOf(.18f to .64f, .30f to .67f, .42f to .65f).forEach { (x, y) ->
                drawCircle(
                    color = Color(0xFFFFCF74).copy(alpha = if (defeated) .28f else .58f),
                    radius = h * .009f,
                    center = Offset(w * x, h * y),
                )
                drawCircle(
                    color = Color(0xFFFF8A45).copy(alpha = if (defeated) .05f else .12f),
                    radius = h * .038f,
                    center = Offset(w * x, h * y),
                    style = Stroke(width = 2f, cap = StrokeCap.Round),
                )
            }
        }

        BossArtwork(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(if (phase == ReturnRaidPhase.RETURNING) 124.dp else 170.dp),
            presentation = bossPresentation,
            frameless = true,
        )

        if (phase in setOf(ReturnRaidPhase.YOUR_TURN, ReturnRaidPhase.STRIKING, ReturnRaidPhase.RESULT)) {
            PixelRagEggSprite(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(if (phase == ReturnRaidPhase.STRIKING) 62.dp else 54.dp),
            )
        }
    }
}
