package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.min

internal enum class CompanionMood {
    Idle,
    Focused,
    Celebrate,
}

@Composable
internal fun RagArtwork(
    modifier: Modifier = Modifier,
    mood: CompanionMood = CompanionMood.Idle,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val unit = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = .28f), Color.Transparent),
                center = Offset(center.x, center.y + unit * .05f),
                radius = unit * .52f,
            ),
            radius = unit * .48f,
            center = Offset(center.x, center.y + unit * .04f),
        )

        val tailPath = Path().apply {
            moveTo(center.x + unit * .20f, center.y + unit * .13f)
            cubicTo(
                center.x + unit * .42f,
                center.y + unit * .02f,
                center.x + unit * .42f,
                center.y + unit * .31f,
                center.x + unit * .21f,
                center.y + unit * .28f,
            )
        }
        drawPath(
            path = tailPath,
            color = primary,
            style = Stroke(width = unit * .12f, cap = StrokeCap.Round),
        )

        drawOval(
            brush = Brush.verticalGradient(
                listOf(onSurface.copy(alpha = .98f), surface),
                startY = center.y - unit * .06f,
                endY = center.y + unit * .36f,
            ),
            topLeft = Offset(center.x - unit * .23f, center.y - unit * .04f),
            size = Size(unit * .46f, unit * .38f),
        )

        drawCircle(
            brush = Brush.radialGradient(
                listOf(onSurface.copy(alpha = .98f), surface),
                center = Offset(center.x, center.y - unit * .12f),
                radius = unit * .28f,
            ),
            radius = unit * .24f,
            center = Offset(center.x, center.y - unit * .13f),
        )

        val leftEar = Path().apply {
            moveTo(center.x - unit * .16f, center.y - unit * .28f)
            lineTo(center.x - unit * .07f, center.y - unit * .47f)
            lineTo(center.x + unit * .00f, center.y - unit * .28f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(center.x + unit * .00f, center.y - unit * .28f)
            lineTo(center.x + unit * .08f, center.y - unit * .47f)
            lineTo(center.x + unit * .17f, center.y - unit * .27f)
            close()
        }
        drawPath(leftEar, onSurface)
        drawPath(rightEar, onSurface)

        drawPath(
            path = Path().apply {
                moveTo(center.x - unit * .13f, center.y - unit * .28f)
                lineTo(center.x - unit * .08f, center.y - unit * .39f)
                lineTo(center.x - unit * .03f, center.y - unit * .28f)
                close()
            },
            color = secondary.copy(alpha = .66f),
        )
        drawPath(
            path = Path().apply {
                moveTo(center.x + unit * .03f, center.y - unit * .28f)
                lineTo(center.x + unit * .08f, center.y - unit * .39f)
                lineTo(center.x + unit * .13f, center.y - unit * .28f)
                close()
            },
            color = secondary.copy(alpha = .66f),
        )

        val eyeOffsetY = center.y - unit * .13f
        when (mood) {
            CompanionMood.Idle -> {
                drawCircle(tertiary, radius = unit * .022f, center = Offset(center.x - unit * .075f, eyeOffsetY))
                drawCircle(tertiary, radius = unit * .022f, center = Offset(center.x + unit * .075f, eyeOffsetY))
            }

            CompanionMood.Focused -> {
                drawLine(
                    color = tertiary,
                    start = Offset(center.x - unit * .11f, eyeOffsetY),
                    end = Offset(center.x - unit * .04f, eyeOffsetY - unit * .015f),
                    strokeWidth = unit * .025f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tertiary,
                    start = Offset(center.x + unit * .04f, eyeOffsetY - unit * .015f),
                    end = Offset(center.x + unit * .11f, eyeOffsetY),
                    strokeWidth = unit * .025f,
                    cap = StrokeCap.Round,
                )
            }

            CompanionMood.Celebrate -> {
                drawCircle(tertiary, radius = unit * .024f, center = Offset(center.x - unit * .075f, eyeOffsetY))
                drawCircle(tertiary, radius = unit * .024f, center = Offset(center.x + unit * .075f, eyeOffsetY))
                drawArc(
                    color = secondary,
                    startAngle = 8f,
                    sweepAngle = 164f,
                    useCenter = false,
                    topLeft = Offset(center.x - unit * .065f, center.y - unit * .08f),
                    size = Size(unit * .13f, unit * .09f),
                    style = Stroke(width = unit * .022f, cap = StrokeCap.Round),
                )
            }
        }

        drawCircle(
            color = secondary,
            radius = unit * .025f,
            center = Offset(center.x, center.y - unit * .055f),
        )

        drawOval(
            color = primary,
            topLeft = Offset(center.x - unit * .18f, center.y + unit * .12f),
            size = Size(unit * .36f, unit * .13f),
        )
        drawCircle(
            color = tertiary,
            radius = unit * .04f,
            center = Offset(center.x, center.y + unit * .18f),
        )
    }
}

@Composable
internal fun VolgaArtwork(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val unit = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            brush = Brush.radialGradient(
                listOf(secondary.copy(alpha = .28f), primary.copy(alpha = .10f), Color.Transparent),
                center = center,
                radius = unit * .55f,
            ),
            radius = unit * .52f,
            center = center,
        )

        val leftHorn = Path().apply {
            moveTo(center.x - unit * .25f, center.y - unit * .21f)
            lineTo(center.x - unit * .42f, center.y - unit * .42f)
            lineTo(center.x - unit * .14f, center.y - unit * .30f)
            close()
        }
        val rightHorn = Path().apply {
            moveTo(center.x + unit * .25f, center.y - unit * .21f)
            lineTo(center.x + unit * .42f, center.y - unit * .42f)
            lineTo(center.x + unit * .14f, center.y - unit * .30f)
            close()
        }
        drawPath(leftHorn, secondary)
        drawPath(rightHorn, secondary)

        drawOval(
            brush = Brush.verticalGradient(
                listOf(onSurface.copy(alpha = .92f), surfaceVariant),
                startY = center.y - unit * .38f,
                endY = center.y + unit * .40f,
            ),
            topLeft = Offset(center.x - unit * .34f, center.y - unit * .30f),
            size = Size(unit * .68f, unit * .62f),
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
    }
}

@Composable
internal fun RaidDuelArtwork(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        RagArtwork(
            modifier = Modifier
                .size(86.dp),
            mood = CompanionMood.Celebrate,
        )
        VolgaArtwork(
            modifier = Modifier
                .size(98.dp),
        )
        RaidClashMark(
            modifier = Modifier
                .fillMaxSize(),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawEmberCracks(
    center: Offset,
    unit: Float,
    color: Color,
) {
    val stroke = unit * .025f
    drawLine(
        color = color,
        start = Offset(center.x - unit * .08f, center.y + unit * .04f),
        end = Offset(center.x - unit * .14f, center.y + unit * .18f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(center.x - unit * .14f, center.y + unit * .18f),
        end = Offset(center.x - unit * .08f, center.y + unit * .25f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(center.x + unit * .10f, center.y + unit * .02f),
        end = Offset(center.x + unit * .16f, center.y + unit * .17f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}
