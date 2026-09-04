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
import com.madowaku.focusraid.core.domain.CompanionStage
import kotlin.math.min

enum class CompanionMood {
    Idle,
    Focused,
    Celebrate,
}

private data class CompanionVisualSpec(
    val wingReach: Float,
    val wingRise: Float,
    val bodyHalfWidth: Float,
    val bodyTop: Float,
    val bodyBottom: Float,
    val hornReach: Float,
    val tailReach: Float,
    val eyeRadius: Float,
    val coreRadius: Float,
    val haloAlpha: Float,
    val shoulderSpikes: Boolean,
    val crownHorn: Boolean,
)

@Composable
internal fun CompanionArtwork(
    modifier: Modifier = Modifier,
    stage: CompanionStage = CompanionStage.HATCHLING,
    mood: CompanionMood = CompanionMood.Idle,
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.surface

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "相棒ラグ・${stage.label}"
        },
    ) {
        val unit = min(size.width, size.height)
        val center = Offset(size.width / 2f, size.height / 2f)
        val lift = when (mood) {
            CompanionMood.Idle -> 0f
            CompanionMood.Focused -> unit * 0.025f
            CompanionMood.Celebrate -> unit * 0.06f
        }

        if (stage == CompanionStage.EGG) {
            drawCompanionEgg(
                center = center,
                unit = unit,
                lift = lift,
                primary = primary,
                tertiary = tertiary,
                surface = surface,
                onSurface = onSurface,
                celebrate = mood == CompanionMood.Celebrate,
            )
            return@Canvas
        }

        val spec = companionSpec(stage)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = spec.haloAlpha), Color.Transparent),
                center = center,
                radius = unit * .54f,
            ),
            radius = unit * .52f,
            center = center,
        )

        if (spec.tailReach > 0f) {
            val tail = Path().apply {
                moveTo(center.x + unit * .12f, center.y + unit * .18f - lift)
                cubicTo(
                    center.x + unit * .30f,
                    center.y + unit * .28f - lift,
                    center.x + unit * spec.tailReach,
                    center.y + unit * .18f - lift,
                    center.x + unit * (spec.tailReach - .04f),
                    center.y + unit * .34f - lift,
                )
            }
            drawPath(
                path = tail,
                color = primary.copy(alpha = .78f),
                style = Stroke(width = unit * .065f, cap = StrokeCap.Round),
            )
        }

        drawCompanionWing(
            center = center,
            unit = unit,
            lift = lift,
            direction = -1f,
            reach = spec.wingReach,
            rise = spec.wingRise,
            color = primary.copy(alpha = .74f),
        )
        drawCompanionWing(
            center = center,
            unit = unit,
            lift = lift,
            direction = 1f,
            reach = spec.wingReach,
            rise = spec.wingRise,
            color = primary.copy(alpha = .74f),
        )

        if (spec.shoulderSpikes) {
            listOf(-1f, 1f).forEach { direction ->
                val spike = Path().apply {
                    moveTo(center.x + direction * unit * .18f, center.y - unit * .02f - lift)
                    lineTo(center.x + direction * unit * .31f, center.y - unit * .16f - lift)
                    lineTo(center.x + direction * unit * .24f, center.y + unit * .06f - lift)
                    close()
                }
                drawPath(spike, tertiary.copy(alpha = .70f))
            }
        }

        val body = Path().apply {
            moveTo(center.x, center.y - unit * spec.bodyTop - lift)
            cubicTo(
                center.x - unit * spec.bodyHalfWidth,
                center.y - unit * .20f - lift,
                center.x - unit * (spec.bodyHalfWidth + .02f),
                center.y + unit * .17f - lift,
                center.x,
                center.y + unit * spec.bodyBottom - lift,
            )
            cubicTo(
                center.x + unit * (spec.bodyHalfWidth + .02f),
                center.y + unit * .17f - lift,
                center.x + unit * spec.bodyHalfWidth,
                center.y - unit * .20f - lift,
                center.x,
                center.y - unit * spec.bodyTop - lift,
            )
            close()
        }
        drawPath(
            path = body,
            brush = Brush.verticalGradient(
                listOf(onSurface, primary.copy(alpha = .88f)),
                startY = center.y - unit * .40f,
                endY = center.y + unit * .38f,
            ),
        )

        drawCompanionHorn(
            center = center,
            unit = unit,
            lift = lift,
            direction = -1f,
            reach = spec.hornReach,
            color = if (stage == CompanionStage.HATCHLING) onSurface else tertiary.copy(alpha = .88f),
        )
        drawCompanionHorn(
            center = center,
            unit = unit,
            lift = lift,
            direction = 1f,
            reach = spec.hornReach,
            color = if (stage == CompanionStage.HATCHLING) onSurface else tertiary.copy(alpha = .88f),
        )

        if (spec.crownHorn) {
            val crown = Path().apply {
                moveTo(center.x - unit * .055f, center.y - unit * spec.bodyTop - lift)
                lineTo(center.x, center.y - unit * .49f - lift)
                lineTo(center.x + unit * .055f, center.y - unit * spec.bodyTop - lift)
                close()
            }
            drawPath(crown, tertiary.copy(alpha = .92f))
        }

        drawCircle(
            color = surface,
            radius = unit * spec.eyeRadius,
            center = Offset(center.x - unit * .09f, center.y - unit * .08f - lift),
        )
        drawCircle(
            color = surface,
            radius = unit * spec.eyeRadius,
            center = Offset(center.x + unit * .09f, center.y - unit * .08f - lift),
        )
        drawCircle(
            color = tertiary,
            radius = unit * spec.eyeRadius * .45f,
            center = Offset(center.x - unit * .09f, center.y - unit * .08f - lift),
        )
        drawCircle(
            color = tertiary,
            radius = unit * spec.eyeRadius * .45f,
            center = Offset(center.x + unit * .09f, center.y - unit * .08f - lift),
        )

        val moodScale = if (mood == CompanionMood.Celebrate) 1.24f else 1f
        val coreRadius = unit * spec.coreRadius * moodScale
        if (stage == CompanionStage.MATURE) {
            drawCircle(
                color = tertiary.copy(alpha = .30f),
                radius = coreRadius * 1.72f,
                center = Offset(center.x, center.y + unit * .08f - lift),
                style = Stroke(width = unit * .018f),
            )
        }
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

private fun companionSpec(stage: CompanionStage): CompanionVisualSpec = when (stage) {
    CompanionStage.EGG -> error("Egg uses its own silhouette")
    CompanionStage.HATCHLING -> CompanionVisualSpec(
        wingReach = .43f,
        wingRise = .27f,
        bodyHalfWidth = .23f,
        bodyTop = .32f,
        bodyBottom = .30f,
        hornReach = .39f,
        tailReach = 0f,
        eyeRadius = .045f,
        coreRadius = .060f,
        haloAlpha = .30f,
        shoulderSpikes = false,
        crownHorn = false,
    )
    CompanionStage.FIRST_GROWTH -> CompanionVisualSpec(
        wingReach = .47f,
        wingRise = .32f,
        bodyHalfWidth = .24f,
        bodyTop = .35f,
        bodyBottom = .32f,
        hornReach = .43f,
        tailReach = .41f,
        eyeRadius = .041f,
        coreRadius = .064f,
        haloAlpha = .34f,
        shoulderSpikes = false,
        crownHorn = false,
    )
    CompanionStage.SECOND_GROWTH -> CompanionVisualSpec(
        wingReach = .50f,
        wingRise = .37f,
        bodyHalfWidth = .25f,
        bodyTop = .37f,
        bodyBottom = .34f,
        hornReach = .47f,
        tailReach = .47f,
        eyeRadius = .038f,
        coreRadius = .070f,
        haloAlpha = .39f,
        shoulderSpikes = true,
        crownHorn = false,
    )
    CompanionStage.MATURE -> CompanionVisualSpec(
        wingReach = .53f,
        wingRise = .41f,
        bodyHalfWidth = .26f,
        bodyTop = .39f,
        bodyBottom = .35f,
        hornReach = .50f,
        tailReach = .52f,
        eyeRadius = .036f,
        coreRadius = .076f,
        haloAlpha = .46f,
        shoulderSpikes = true,
        crownHorn = true,
    )
}

private fun DrawScope.drawCompanionEgg(
    center: Offset,
    unit: Float,
    lift: Float,
    primary: Color,
    tertiary: Color,
    surface: Color,
    onSurface: Color,
    celebrate: Boolean,
) {
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(primary.copy(alpha = if (celebrate) .44f else .28f), Color.Transparent),
            center = center,
            radius = unit * .50f,
        ),
        radius = unit * .49f,
        center = center,
    )

    drawOval(
        brush = Brush.verticalGradient(
            listOf(onSurface, primary.copy(alpha = .82f)),
            startY = center.y - unit * .34f,
            endY = center.y + unit * .35f,
        ),
        topLeft = Offset(center.x - unit * .25f, center.y - unit * .35f - lift),
        size = Size(unit * .50f, unit * .70f),
    )
    drawOval(
        color = surface.copy(alpha = .46f),
        topLeft = Offset(center.x - unit * .13f, center.y - unit * .25f - lift),
        size = Size(unit * .17f, unit * .27f),
    )

    val crack = Path().apply {
        moveTo(center.x - unit * .03f, center.y - unit * .06f - lift)
        lineTo(center.x + unit * .06f, center.y + unit * .01f - lift)
        lineTo(center.x - unit * .01f, center.y + unit * .09f - lift)
        lineTo(center.x + unit * .08f, center.y + unit * .17f - lift)
    }
    drawPath(
        path = crack,
        color = tertiary.copy(alpha = .88f),
        style = Stroke(width = unit * .024f, cap = StrokeCap.Round),
    )
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White, tertiary, Color.Transparent),
            radius = unit * .14f,
        ),
        radius = unit * if (celebrate) .065f else .050f,
        center = Offset(center.x + unit * .03f, center.y + unit * .11f - lift),
    )
}

private fun DrawScope.drawCompanionWing(
    center: Offset,
    unit: Float,
    lift: Float,
    direction: Float,
    reach: Float,
    rise: Float,
    color: Color,
) {
    val wing = Path().apply {
        moveTo(center.x + direction * unit * .16f, center.y - unit * .02f - lift)
        cubicTo(
            center.x + direction * unit * (reach * .88f),
            center.y - unit * rise - lift,
            center.x + direction * unit * reach,
            center.y + unit * .01f - lift,
            center.x + direction * unit * .25f,
            center.y + unit * .14f - lift,
        )
        cubicTo(
            center.x + direction * unit * .18f,
            center.y + unit * .08f - lift,
            center.x + direction * unit * .15f,
            center.y + unit * .03f - lift,
            center.x + direction * unit * .16f,
            center.y - unit * .02f - lift,
        )
        close()
    }
    drawPath(wing, color)
}

private fun DrawScope.drawCompanionHorn(
    center: Offset,
    unit: Float,
    lift: Float,
    direction: Float,
    reach: Float,
    color: Color,
) {
    val horn = Path().apply {
        moveTo(center.x + direction * unit * .12f, center.y - unit * .25f - lift)
        lineTo(center.x + direction * unit * .25f, center.y - unit * reach - lift)
        lineTo(center.x + direction * unit * .035f, center.y - unit * .31f - lift)
        close()
    }
    drawPath(horn, color)
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
