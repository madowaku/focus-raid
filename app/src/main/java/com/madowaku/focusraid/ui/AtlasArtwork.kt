package com.madowaku.focusraid.ui

import android.content.res.Resources
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
}

@Composable
internal fun AtlasArtwork(source: ArtworkSource.Atlas, modifier: Modifier = Modifier) {
    val resources = LocalContext.current.resources
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
