package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color

private val ragEggPalette = mapOf(
    'O' to Color(0xFF5A241F),
    'R' to Color(0xFF9E3E24),
    'A' to Color(0xFFE86F2E),
    'G' to Color(0xFFFFB943),
    'Y' to Color(0xFFFFE28A),
    'W' to Color(0xFFFFF4C6),
)

private val ragEgg = listOf(
    ".....YYYY.....",
    "....YWWWWY....",
    "...YWWYYWWY...",
    "..YWWYYYYWWY..",
    "..YWWWYYWWWY..",
    ".YWWYYYYYYWWY.",
    ".YWWYGGGGYWWY.",
    ".YWWGGGGGGWWY.",
    ".YWWGGAGGGWWY.",
    "..YGGAAAAGY...",
    "..GAAAAAAAAG..",
    ".GAAARRRRAAAG.",
    ".AARRROORRRAA.",
    "..RROOOOOORR..",
    "...ROOOOOOR...",
    ".....OOOO.....",
)

private val volgaPalette = mapOf(
    'D' to Color(0xFF18121D),
    'P' to Color(0xFF2C2238),
    'S' to Color(0xFF4B3A56),
    'E' to Color(0xFFFF7A3D),
    'L' to Color(0xFFD34D31),
    'H' to Color(0xFFFFC15A),
)

private val volga = listOf(
    "..................D.....",
    ".................DPD....",
    "............D...DPPD....",
    "...........DPD.DPPPD....",
    ".........DDPPDDPPPDD....",
    "........DPPPPPPPPD......",
    "......DDPPPSPPPPPD.......",
    "....DDPPPPPPPPPPDDD......",
    "...DPPPEPPPPPPPPPPD......",
    "..DPPPPEEPPPLPPPPPPDD....",
    ".DPPPPPPPPPLLPPPPPPPD....",
    "DPPPPPPPPPPPPPPPPPPPD...",
    "DPPPPPPPPPPPPPPPPPPPPD..",
    ".DPPPPPPPPPPPPPPPPPPPPD.",
    "..DPPPPPPPPPPPPPPPPPPPD.",
    "...DDPPPPPPPPPPPPPPPPDD.",
    ".....DPPPPPPPPPPPPPPD....",
    "....DPPDDPPPPPPDDPPD.....",
    "...DPPD..DPPPPD..DPPD....",
    "...DDD....DDDD....DDD....",
)

@Composable
internal fun PixelRagEggSprite(modifier: Modifier = Modifier) {
    PixelSprite(pattern = ragEgg, palette = ragEggPalette, modifier = modifier)
}

@Composable
internal fun PixelVolgaSprite(modifier: Modifier = Modifier) {
    PixelSprite(pattern = volga, palette = volgaPalette, modifier = modifier)
}

@Composable
private fun PixelSprite(
    pattern: List<String>,
    palette: Map<Char, Color>,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        if (pattern.isEmpty()) return@Canvas
        val columns = pattern.maxOf { it.length }.coerceAtLeast(1)
        val rows = pattern.size.coerceAtLeast(1)
        val pixel = minOf(size.width / columns, size.height / rows)
        val drawWidth = pixel * columns
        val drawHeight = pixel * rows
        val origin = Offset((size.width - drawWidth) / 2f, (size.height - drawHeight) / 2f)

        pattern.forEachIndexed { row, line ->
            line.forEachIndexed { column, token ->
                val color = palette[token] ?: return@forEachIndexed
                drawRect(
                    color = color,
                    topLeft = Offset(origin.x + column * pixel, origin.y + row * pixel),
                    size = Size(pixel + .5f, pixel + .5f),
                )
            }
        }
    }
}
