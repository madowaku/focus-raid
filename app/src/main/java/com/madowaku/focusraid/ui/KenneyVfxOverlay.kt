package com.madowaku.focusraid.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.madowaku.focusraid.R

/**
 * A short, non-interactive overlay for the two moments where a little magic helps the product
 * feel alive. It deliberately renders only a handful of cached PNGs and never loops.
 */
@Composable
internal fun KenneyCompletionOverlay(
    triggerKey: String?,
    anchorTop: Dp? = null,
) {
    if (triggerKey == null) return

    val progress = remember(triggerKey) { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_000, easing = FastOutSlowInEasing),
        )
    }

    KenneyVfxLayer(
        progress = progress.value,
        mode = KenneyVfxMode.COMPLETION,
        anchorTop = anchorTop,
    )
}

@Composable
internal fun KenneyRaidImpactOverlay(
    trigger: Int,
    emphasized: Boolean = false,
) {
    if (trigger == 0) return

    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 680, easing = FastOutSlowInEasing),
        )
    }

    KenneyVfxLayer(
        progress = progress.value,
        mode = KenneyVfxMode.RAID_IMPACT,
        emphasized = emphasized,
    )
}

private enum class KenneyVfxMode {
    COMPLETION,
    RAID_IMPACT,
}

private data class ParticleSpec(
    val resourceId: Int,
    val x: Float,
    val y: Float,
    val size: Float,
    val rotation: Float,
    val delay: Float,
)

@Composable
private fun KenneyVfxLayer(
    progress: Float,
    mode: KenneyVfxMode,
    anchorTop: Dp? = null,
    emphasized: Boolean = false,
) {
    if (progress >= .999f) return

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val flash = if (progress < .28f) {
        progress / .28f
    } else {
        ((1f - progress) / .72f).coerceAtLeast(0f)
    }
    val overlayModifier = Modifier
        .fillMaxSize()
        // VFX is decorative: it must never add a TalkBack node or an accessibility action.
        .clearAndSetSemantics { }

    BoxWithConstraints(overlayModifier) {
        val artworkSize = if (mode == KenneyVfxMode.RAID_IMPACT) {
            minOf(maxWidth * .42f, 160.dp)
        } else {
            minOf(maxWidth * .58f, 220.dp)
        }
        val anchorModifier = if (anchorTop == null) {
            Modifier.align(Alignment.Center)
        } else {
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = anchorTop)
        }

        Box(
            modifier = anchorModifier.size(artworkSize),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.fr_vfx_light_soft),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(tertiary),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = flash * when {
                            mode == KenneyVfxMode.COMPLETION -> .34f
                            emphasized -> .24f
                            else -> .18f
                        }
                        val scale = if (mode == KenneyVfxMode.COMPLETION) {
                            .68f + flash * .28f
                        } else {
                            .76f + flash * .20f
                        }
                        scaleX = scale
                        scaleY = scale
                    },
            )
            Image(
                painter = painterResource(R.drawable.fr_vfx_light_ring),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(primary),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = flash * when {
                            mode == KenneyVfxMode.COMPLETION -> .52f
                            emphasized -> .60f
                            else -> .50f
                        }
                        val scale = if (mode == KenneyVfxMode.COMPLETION) {
                            .56f + flash * .62f
                        } else {
                            .62f + flash * .54f
                        }
                        scaleX = scale
                        scaleY = scale
                        rotationZ = if (mode == KenneyVfxMode.RAID_IMPACT) progress * 18f else progress * -8f
                    },
            )
            if (mode == KenneyVfxMode.RAID_IMPACT) {
                Image(
                    painter = painterResource(R.drawable.fr_vfx_particle_circle),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(tertiary),
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = flash * if (emphasized) .38f else .30f
                            val scale = .44f + flash * .50f
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }

            val particles = if (mode == KenneyVfxMode.COMPLETION) {
                listOf(
                    ParticleSpec(R.drawable.fr_vfx_particle_star, -.27f, -.20f, .21f, -18f, .08f),
                    ParticleSpec(R.drawable.fr_vfx_particle_flare, .28f, -.06f, .18f, 16f, .17f),
                    ParticleSpec(R.drawable.fr_vfx_particle_twirl, -.12f, .27f, .25f, -28f, .24f),
                )
            } else {
                listOf(
                    ParticleSpec(R.drawable.fr_vfx_particle_flare, -.30f, -.12f, .19f, -22f, .03f),
                    ParticleSpec(R.drawable.fr_vfx_particle_star, .28f, -.16f, .18f, 20f, .10f),
                    ParticleSpec(R.drawable.fr_vfx_particle_twirl, -.18f, .24f, .24f, -42f, .16f),
                    ParticleSpec(R.drawable.fr_vfx_particle_circle, .20f, .22f, .18f, 30f, .22f),
                )
            }
            particles.forEach { spec ->
                val local = ((progress - spec.delay) / .62f).coerceIn(0f, 1f)
                val particleAlpha = if (progress < spec.delay) 0f else {
                    (1f - local) * if (emphasized) .86f else .78f
                }
                Image(
                    painter = painterResource(spec.resourceId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    colorFilter = ColorFilter.tint(if (mode == KenneyVfxMode.COMPLETION) tertiary else primary),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(artworkSize * spec.size)
                        .offset(
                            x = (artworkSize.value * spec.x * (.70f + local * .30f)).dp,
                            y = (artworkSize.value * spec.y * (.70f + local * .30f)).dp,
                        )
                        .graphicsLayer {
                            alpha = particleAlpha
                            rotationZ = spec.rotation + local * 42f
                            val scale = .70f + local * .42f
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
        }
    }
}
