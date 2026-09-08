package com.madowaku.focusraid.data

import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.Footprint
import com.madowaku.focusraid.core.model.FootprintPresets
import com.madowaku.focusraid.core.model.WorldSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WorldSyncStatus {
    LOCAL_PREVIEW,
    CONNECTING,
    LIVE,
    OFFLINE,
}

interface WorldRepository {
    val world: StateFlow<WorldSnapshot>
    val syncStatus: StateFlow<WorldSyncStatus>

    fun snapshot(): WorldSnapshot = world.value

    suspend fun refresh()

    suspend fun footprints(
        expedition: Expedition,
        checkpoint: Int,
        limit: Int = 3,
    ): List<Footprint>

    suspend fun leaveFootprint(
        expedition: Expedition,
        checkpoint: Int,
        presetId: String,
    ): Footprint?
}

class FakeWorldRepository : WorldRepository {
    private val initialWorld = WorldSnapshot()
    private val _world = MutableStateFlow(initialWorld)
    override val world: StateFlow<WorldSnapshot> = _world.asStateFlow()

    private val _syncStatus = MutableStateFlow(WorldSyncStatus.LOCAL_PREVIEW)
    override val syncStatus: StateFlow<WorldSyncStatus> = _syncStatus.asStateFlow()

    private val footprints = mutableListOf(
        seed(Expedition.TOWER, initialWorld.towerFloor, "waiting", "3日前"),
        seed(Expedition.TOWER, initialWorld.towerFloor, "one_step", "昨日"),
        seed(Expedition.TOWER, initialWorld.towerFloor, "rest", "6時間前"),
        seed(Expedition.ABYSS, initialWorld.abyssDepth, "made_it", "2日前"),
        seed(Expedition.ABYSS, initialWorld.abyssDepth, "keep_going", "昨日"),
        seed(Expedition.ABYSS, initialWorld.abyssDepth, "strong", "4時間前"),
        seed(Expedition.STAR_ROUTE, 1, "waiting", "昨日"),
        seed(Expedition.STAR_ROUTE, 1, "fire", "2時間前"),
    )

    override suspend fun refresh() = Unit

    override suspend fun footprints(
        expedition: Expedition,
        checkpoint: Int,
        limit: Int,
    ): List<Footprint> = footprints
        .asReversed()
        .asSequence()
        .filter { it.expedition == expedition && it.checkpoint == checkpoint }
        .take(limit.coerceAtLeast(0))
        .toList()

    override suspend fun leaveFootprint(
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
