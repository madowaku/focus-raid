package com.madowaku.focusraid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** One finite delivery, driven by the owning screen. Disposal cancels the sequence.
 * Animatable respects the system duration scale, including disabled animations.
 */
internal class RaidFocusArrival {
    val progress = Animatable(1f)
    var origin by mutableIntStateOf(0)
        private set
    var isSelf by mutableStateOf(false)
        private set

    suspend fun deliver(index: Int, self: Boolean = false, onArrival: () -> Unit) {
        origin = index
        isSelf = self
        progress.snapTo(0f)
        progress.animateTo(.75f, tween(600, easing = FastOutSlowInEasing))
        onArrival()
        progress.animateTo(1f, tween(250, easing = LinearEasing))
    }
}

/** Draw in the boss's own bounds so every origin converges on the same chest point. */
@Composable
internal fun RaidFocusArrivalLayer(flight: RaidFocusArrival, modifier: Modifier = Modifier) {
    val light = if (flight.isSelf) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
    Canvas(modifier.clearAndSetSemantics { }) {
        val progress = flight.progress.value
        if (progress >= 1f) return@Canvas
        val target = Offset(size.width * .5f, size.height * .54f)
        val start = if (flight.isSelf) Offset(size.width * .5f, size.height * .94f) else when (flight.origin % 3) {
            0 -> Offset(size.width * .06f, size.height * .25f)
            1 -> Offset(size.width * .94f, size.height * .30f)
            else -> Offset(size.width * .16f, size.height * .88f)
        }
        val control = Offset(start.x, target.y + size.height * .16f)
        fun point(t: Float): Offset = start * ((1f - t) * (1f - t)) +
            control * (2f * (1f - t) * t) + target * (t * t)
        val radius = (if (flight.isSelf) 5.2f else 4f).dp.toPx()
        if (progress < .75f) {
            val t = progress / .75f
            val fade = (t * 8f).coerceAtMost(1f)
            // A short soft tail, never a persistent beam or particle emitter.
            repeat(6) { index ->
                val tail = point((t - index * .028f).coerceAtLeast(0f))
                drawCircle(light.copy(alpha = fade * .20f * (1f - index / 6f)), radius * (1f - index / 8f), tail)
            }
            val center = point(t)
            drawCircle(Brush.radialGradient(listOf(light.copy(alpha = .48f * fade), Color.Transparent), center, radius * 4f), radius * 4f, center)
            drawCircle(light.copy(alpha = fade), radius, center)
            drawCircle(Color.White.copy(alpha = fade), radius * .45f, center)
        } else {
            val settle = (progress - .75f) / .25f
            val radiusNow = radius * (2f + settle * 4f)
            drawCircle(light.copy(alpha = (1f - settle) * .38f), radiusNow, target, style = Stroke(1.5.dp.toPx()))
            drawCircle(Brush.radialGradient(listOf(light.copy(alpha = (1f - settle) * .35f), Color.Transparent), target, radiusNow * 1.6f), radiusNow * 1.6f, target)
        }
    }
}
