package com.madowaku.focusraid

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.madowaku.focusraid.core.domain.*
import com.madowaku.focusraid.ui.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.roundToInt

class ArtworkRenderingTest {
    @Test fun allProductionFramesDecodeWithinBoundsAndContainArtwork() {
        val resources = InstrumentationRegistry.getInstrumentation().targetContext.resources
        val keys = buildList<ArtworkKey> {
            CompanionIdentity.entries.forEach { identity -> CompanionStage.entries.forEach { stage ->
                CompanionMood.entries.forEach { add(ArtworkKey.Companion(stage, it, identity)) }
            } }
            BossIdentity.entries.forEach { identity -> BossPresentation.entries.forEach { add(ArtworkKey.Boss(identity, it)) } }
            ItemCatalog.all.forEach { add(ArtworkKey.Item(it.id, it.name)) }
        }
        val sources = keys.map { ArtworkCatalog.Production.resolve(it) as ArtworkSource.Atlas }
        sources.groupBy { it.resourceId }.forEach { (id, frames) ->
            val bitmap = requireNotNull(BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply { inScaled = false }))
            try {
                frames.forEach { source ->
                    val f = source.frame
                    val left = (f.left * bitmap.width).roundToInt(); val right = (f.right * bitmap.width).roundToInt()
                    val top = (f.top * bitmap.height).roundToInt(); val bottom = (f.bottom * bitmap.height).roundToInt()
                    assertTrue(left >= 0 && top >= 0 && right <= bitmap.width && bottom <= bitmap.height)
                    assertTrue(right - left >= 150 && bottom - top >= 150)
                    val colors = mutableSetOf<Int>()
                    for (x in left until right step 7) for (y in top until bottom step 7) colors.add(bitmap.getPixel(x, y))
                    assertTrue("Blank or missing art in $source", colors.size > 100)
                }
            } finally { bitmap.recycle() }
        }
        assertEquals(90, sources.size)
    }
}
