package com.madowaku.focusraid.ui

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/** Seven shared sheets, bounded to 48 MiB; cells never allocate independent bitmap copies. */
internal object ArtworkBitmapCache {
    private val cache = object : LruCache<Int, ImageBitmap>(48 * 1024) {
        override fun sizeOf(key: Int, value: ImageBitmap) = value.width * value.height * 4 / 1024
    }
    @Synchronized fun load(resources: Resources, id: Int): ImageBitmap = cache[id] ?: run {
        val options = BitmapFactory.Options().apply { inScaled = false }
        requireNotNull(BitmapFactory.decodeResource(resources, id, options)).asImageBitmap().also { cache.put(id, it) }
    }

    /** Crops one boss pose and removes only the atlas' connected purple backdrop. */
    fun loadTransparentFrame(resources: Resources, id: Int, frame: ArtworkFrame): ImageBitmap {
        val options = BitmapFactory.Options().apply {
            inScaled = false
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val sheet = requireNotNull(BitmapFactory.decodeResource(resources, id, options))
        val left = (frame.left * sheet.width).roundToInt()
        val top = (frame.top * sheet.height).roundToInt()
        val right = (frame.right * sheet.width).roundToInt()
        val bottom = (frame.bottom * sheet.height).roundToInt()
        val pose = Bitmap.createBitmap(sheet, left, top, right - left, bottom - top)
            .copy(Bitmap.Config.ARGB_8888, true)
        sheet.recycle()
        removePurpleBackdrop(pose)
        return pose.asImageBitmap()
    }

    private fun removePurpleBackdrop(bitmap: Bitmap) {
        val width = bitmap.width
        val height = bitmap.height
        if (width < 2 || height < 2) return

        bitmap.setHasAlpha(true)
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val corners = intArrayOf(
            pixels[0],
            pixels[width - 1],
            pixels[(height - 1) * width],
            pixels[pixels.lastIndex],
        )

        fun channel(color: Int, shift: Int) = (color ushr shift) and 0xFF
        fun expected(top: Int, bottom: Int, fraction: Float): Int =
            (channel(top, 16) + (channel(bottom, 16) - channel(top, 16)) * fraction).roundToInt() shl 16 or
                ((channel(top, 8) + (channel(bottom, 8) - channel(top, 8)) * fraction).roundToInt() shl 8) or
                (channel(top, 0) + (channel(bottom, 0) - channel(top, 0)) * fraction).roundToInt()

        for (y in 0 until height) {
            val verticalFraction = y.toFloat() / (height - 1).toFloat()
            val rowStart = expected(corners[0], corners[2], verticalFraction)
            val rowEnd = expected(corners[1], corners[3], verticalFraction)
            for (x in 0 until width) {
                val index = y * width + x
                val horizontalFraction = x.toFloat() / (width - 1).toFloat()
                val background = expected(rowStart, rowEnd, horizontalFraction)
                val pixel = pixels[index]
                val red = channel(pixel, 16)
                val green = channel(pixel, 8)
                val blue = channel(pixel, 0)
                val backgroundRed = channel(background, 16)
                val backgroundGreen = channel(background, 8)
                val backgroundBlue = channel(background, 0)
                val distance = abs(red - backgroundRed) +
                    abs(green - backgroundGreen) +
                    abs(blue - backgroundBlue)
                val purpleBackdrop = blue >= red + 10 && blue >= green + 14 && red < 80
                if (purpleBackdrop && distance <= 72) {
                    val alpha = ((distance - 34).coerceIn(0, 38) * 255 / 38)
                    pixels[index] = (alpha shl 24) or (red shl 16) or (green shl 8) or blue
                }
            }
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    }
}

@Composable
internal fun AtlasArtwork(
    source: ArtworkSource.Atlas,
    modifier: Modifier = Modifier,
    transparentBackground: Boolean = false,
) {
    val resources = LocalContext.current.resources
    if (transparentBackground) {
        val pose = remember(resources, source.resourceId, source.frame) {
            ArtworkBitmapCache.loadTransparentFrame(resources, source.resourceId, source.frame)
        }
        Canvas(modifier) {
            val scale = min(size.width / pose.width, size.height / pose.height)
            val destination = IntSize((pose.width * scale).roundToInt(), (pose.height * scale).roundToInt())
            drawImage(
                pose,
                dstOffset = IntOffset(
                    ((size.width - destination.width) / 2).roundToInt(),
                    ((size.height - destination.height) / 2).roundToInt(),
                ),
                dstSize = destination,
                filterQuality = FilterQuality.High,
            )
        }
        return
    }

    val bitmap = remember(resources, source.resourceId) { ArtworkBitmapCache.load(resources, source.resourceId) }
    Canvas(modifier.clip(RoundedCornerShape(14.dp))) {
        val frame = source.frame
        val left = (frame.left * bitmap.width).roundToInt()
        val top = (frame.top * bitmap.height).roundToInt()
        val width = (frame.right * bitmap.width).roundToInt() - left
        val height = (frame.bottom * bitmap.height).roundToInt() - top
        val scale = min(size.width / width, size.height / height)
        val destination = IntSize((width * scale).roundToInt(), (height * scale).roundToInt())
        drawRect(Color(0xFF171126))
        drawImage(bitmap, IntOffset(left, top), IntSize(width, height),
            IntOffset(((size.width - destination.width) / 2).roundToInt(), ((size.height - destination.height) / 2).roundToInt()),
            destination, filterQuality = FilterQuality.High)
    }
}
