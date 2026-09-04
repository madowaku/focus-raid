package com.madowaku.focusraid.data

import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Footprint
import com.madowaku.focusraid.core.model.FootprintPresets
import com.madowaku.focusraid.core.model.WorldSnapshot

interface WorldRepository {
    fun snapshot(): WorldSnapshot

    fun footprints(
        expedition: Expedition,
        checkpoint: Int,
        limit: Int = 3,
    ): List<Footprint>

    fun leaveFootprint(
        expedition: Expedition,
        checkpoint: Int,
        presetId: String,
    ): Footprint?
}

class FakeWorldRepository : WorldRepository {
    private val world = WorldSnapshot()
    private val footprints = mutableListOf(
        seed(Expedition.TOWER, world.towerFloor, "waiting", "3日前"),
        seed(Expedition.TOWER, world.towerFloor, "one_step", "昨日"),
        seed(Expedition.TOWER, world.towerFloor, "rest", "6時間前"),
        seed(Expedition.ABYSS, world.abyssDepth, "made_it", "2日前"),
        seed(Expedition.ABYSS, world.abyssDepth, "keep_going", "昨日"),
        seed(Expedition.ABYSS, world.abyssDepth, "strong", "4時間前"),
    )

    override fun snapshot(): WorldSnapshot = world

    override fun footprints(
        expedition: Expedition,
        checkpoint: Int,
        limit: Int,
    ): List<Footprint> = footprints
        .asReversed()
        .asSequence()
        .filter { it.expedition == expedition && it.checkpoint == checkpoint }
        .take(limit.coerceAtLeast(0))
        .toList()

    override fun leaveFootprint(
        expedition: Expedition,
        checkpoint: Int,
        presetId: String,
    ): Footprint? {
        val preset = FootprintPresets.byId(presetId) ?: return null
        val footprint = Footprint(
            expedition = expedition,
            checkpoint = checkpoint,
            presetId = preset.id,
            glyph = preset.glyph,
            text = preset.text,
            relativeLabel = "たった今",
        )
        footprints += footprint
        return footprint
    }

    private fun seed(
        expedition: Expedition,
        checkpoint: Int,
        presetId: String,
        relativeLabel: String,
    ): Footprint {
        val preset = checkNotNull(FootprintPresets.byId(presetId))
        return Footprint(
            expedition = expedition,
            checkpoint = checkpoint,
            presetId = preset.id,
            glyph = preset.glyph,
            text = preset.text,
            relativeLabel = relativeLabel,
        )
    }
}
