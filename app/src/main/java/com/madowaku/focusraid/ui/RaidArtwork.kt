package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlin.math.min

enum class CompanionMood {
    Idle,
    Focused,
    Celebrate,
}

@Composable
internal fun CompanionArtwork(
    modifier: Modifier = Modifier,
    mood: CompanionMood = CompanionMood.Idle,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "相棒ラグ"
        },
    ) {
        val unit = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val lift = when (mood) {
            CompanionMood.Idle -> 0f
            CompanionMood.Focused -> unit * 0.025f
            CompanionMood.Celebrate -> unit * 0.06f
        }

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = .34f), Color.Transparent),
                center = center,
                radius = unit * .52f,
            ),
            radius = unit * .50f,
            center = center,
        )

        val leftWing = Path().apply {
            moveTo(center.x - unit * .16f, center.y - unit * .03f - lift)
            cubicTo(
                center.x - unit * .40f,
                center.y - unit * .29f - lift,
                center.x - unit * .46f,
                center.y + unit * .01f - lift,
                center.x - unit * .25f,
                center.y + unit * .13f - lift,
            )
            cubicTo(
                center.x - unit * .18f,
                center.y + unit * .08f - lift,
                center.x - unit * .15f,
                center.y + unit * .03f - lift,
                center.x - unit * .16f,
                center.y - unit * .03f - lift,
            )
            close()
        }
        drawPath(leftWing, primary.copy(alpha = .76f))

        val rightWing = Path().apply {
            moveTo(center.x + unit * .16f, center.y - unit * .03f - lift)
            cubicTo(
                center.x + unit * .40f,
                center.y - unit * .29f - lift,
                center.x + unit * .46f,
                center.y + unit * .01f - lift,
                center.x + unit * .25f,
                center.y + unit * .13f - lift,
            )
            cubicTo(
                center.x + unit * .18f,
                center.y + unit * .08f - lift,
                center.x + unit * .15f,
                center.y + unit * .03f - lift,
                center.x + unit * .16f,
                center.y - unit * .03f - lift,
            )
            close()
        }
        drawPath(rightWing, primary.copy(alpha = .76f))

        val body = Path().apply {
            moveTo(center.x, center.y - unit * .33f - lift)
            cubicTo(
                center.x - unit * .25f,
                center.y - unit * .22f - lift,
                center.x - unit * .27f,
                center.y + unit * .17f - lift,
                center.x,
                center.y + unit * .31f - lift,
            )
            cubicTo(
                center.x + unit * .27f,
                center.y + unit * .17f - lift,
                center.x + unit * .25f,
                center.y - unit * .22f - lift,
                center.x,
                center.y - unit * .33f - lift,
            )
            close()
        }
        drawPath(
            path = body,
            brush = Brush.verticalGradient(
                listOf(onSurface, primary.copy(alpha = .88f)),
                startY = center.y - unit * .35f,
                endY = center.y + unit * .34f,
            ),
        )

        val earLeft = Path().apply {
            moveTo(center.x - unit * .13f, center.y - unit * .24f - lift)
            lineTo(center.x - unit * .25f, center.y - unit * .41f - lift)
            lineTo(center.x - unit * .04f, center.y - unit * .31f - lift)
            close()
        }
        val earRight = Path().apply {
            moveTo(center.x + unit * .13f, center.y - unit * .24f - lift)
            lineTo(center.x + unit * .25f, center.y - unit * .41f - lift)
            lineTo(center.x + unit * .04f, center.y - unit * .31f - lift)
            close()
        }
        drawPath(earLeft, onSurface)
        drawPath(earRight, onSurface)

        drawCircle(
            color = surface,
            radius = unit * .045f,
            center = Offset(center.x - unit * .09f, center.y - unit * .08f - lift),
        )
        drawCircle(
            color = surface,
            radius = unit * .045f,
            center = Offset(center.x + unit * .09f, center.y - unit * .08f - lift),
        )
        drawCircle(
            color = tertiary,
            radius = unit * .020f,
            center = Offset(center.x - unit * .09f, center.y - unit * .08f - lift),
        )
        drawCircle(
            color = tertiary,
            radius = unit * .020f,
            center = Offset(center.x + unit * .09f, center.y - unit * .08f - lift),
        )

        val coreRadius = if (mood == CompanionMood.Celebrate) unit * .075f else unit * .060f
        drawCircle(
            brush = Brush.radialGradient(
                listOf(Color.White, tertiary, primary.copy(alpha = .30f)),
                radius = coreRadius * 2.4f,
            ),
            radius = coreRadius,
            center = Offset(center.x, center.y + unit * .08f - lift),
        )
    }
}

@Composable
internal fun BossArtwork(modifier: Modifier = Modifier) {
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "灰燼竜ヴォルガ"
        },
    ) {
        val unit = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(secondary.copy(alpha = .26f), Color.Transparent),
                center = center,
                radius = unit * .55f,
            ),
            radius = unit * .52f,
            center = center,
        )

        val leftHorn = Path().apply {
            moveTo(center.x - unit * .18f, center.y - unit * .18f)
            lineTo(center.x - unit * .43f, center.y - unit * .40f)
            lineTo(center.x - unit * .30f, center.y - unit * .04f)
            close()
        }
        val rightHorn = Path().apply {
            moveTo(center.x + unit * .18f, center.y - unit * .18f)
            lineTo(center.x + unit * .43f, center.y - unit * .40f)
            lineTo(center.x + unit * .30f, center.y - unit * .04f)
            close()
        }
        drawPath(leftHorn, tertiary.copy(alpha = .80f))
        drawPath(rightHorn, tertiary.copy(alpha = .80f))

        val head = Path().apply {
            moveTo(center.x, center.y - unit * .36f)
            cubicTo(
                center.x - unit * .34f,
                center.y - unit * .28f,
                center.x - unit * .37f,
                center.y + unit * .12f,
                center.x - unit * .18f,
                center.y + unit * .30f,
            )
            cubicTo(
                center.x - unit * .07f,
                center.y + unit * .40f,
                center.x + unit * .07f,
                center.y + unit * .40f,
                center.x + unit * .18f,
                center.y + unit * .30f,
            )
            cubicTo(
                center.x + unit * .37f,
                center.y + unit * .12f,
                center.x + unit * .34f,
                center.y - unit * .28f,
                center.x,
                center.y - unit * .36f,
            )
            close()
        }
        drawPath(
            path = head,
            brush = Brush.verticalGradient(
                listOf(onSurface.copy(alpha = .92f), surfaceVariant),
                startY = center.y - unit * .38f,
                endY = center.y + unit * .40f,
            ),
        )

        drawRoundRect(
            color = secondary.copy(alpha = .45f),
            topLeft = Offset(center.x - unit * .27f, center.y - unit * .04f),
            size = Size(unit * .18f, unit * .075f),
            cornerRadius = CornerRadius(unit * .04f, unit * .04f),
        )
        drawRoundRect(
            color = secondary.copy(alpha = .45f),
            topLeft = Offset(center.x + unit * .09f, center.y - unit * .04f),
            size = Size(unit * .18f, unit * .075f),
            cornerRadius = CornerRadius(unit * .04f, unit * .04f),
        )
        drawCircle(
            color = tertiary,
            radius = unit * .035f,
            center = Offset(center.x - unit * .18f, center.y - unit * .002f),
        )
        drawCircle(
            color = tertiary,
            radius = unit * .035f,
            center = Offset(center.x + unit * .18f, center.y - unit * .002f),
        )

        val jaw = Path().apply {
            moveTo(center.x - unit * .18f, center.y + unit * .17f)
            quadraticTo(center.x, center.y + unit * .34f, center.x + unit * .18f, center.y + unit * .17f)
        }
        drawPath(
            path = jaw,
            color = secondary,
            style = Stroke(width = unit * .045f, cap = StrokeCap.Round),
        )

        drawEmberCracks(center, unit, secondary)
    }
}

@Composable
internal fun RaidClashMark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Canvas(modifier = modifier) {
        val stroke = min(size.width, size.height) * .10f
        drawLine(
            color = primary,
            start = Offset(size.width * .28f, size.height * .18f),
            end = Offset(size.width * .72f, size.height * .82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tertiary,
            start = Offset(size.width * .72f, size.height * .18f),
            end = Offset(size.width * .28f, size.height * .82f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = Color.White,
            radius = min(size.width, size.height) * .065f,
            center = Offset(size.width / 2f, size.height / 2f),
        )
    }
}

private fun DrawScope.drawEmberCracks(center: Offset, unit: Float, color: Color) {
    val crackWidth = unit * .025f
    drawLine(
        color = color.copy(alpha = .72f),
        start = Offset(center.x, center.y - unit * .28f),
        end = Offset(center.x - unit * .06f, center.y - unit * .11f),
        strokeWidth = crackWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color.copy(alpha = .58f),
        start = Offset(center.x - unit * .06f, center.y - unit * .11f),
        end = Offset(center.x + unit * .02f, center.y + unit * .03f),
        strokeWidth = crackWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color.copy(alpha = .52f),
        start = Offset(center.x + unit * .12f, center.y + unit * .14f),
        end = Offset(center.x + unit * .20f, center.y + unit * .28f),
        strokeWidth = crackWidth,
        cap = StrokeCap.Round,
    )
}
