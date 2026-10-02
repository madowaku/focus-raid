package com.madowaku.focusraid.ui
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.madowaku.focusraid.R

@Composable
internal fun PixelReturnRaidWorld(phase: ReturnRaidPhase, armorBroken: Boolean,
    bossPresentation: BossPresentation, modifier: Modifier = Modifier) {
    Box(modifier) {
        RaidEnvironment(R.drawable.world_arena_v13)
        BossArtwork(Modifier.align(Alignment.CenterEnd).padding(end = 12.dp).fillMaxWidth(.72f),
            presentation = if (armorBroken) BossPresentation.Defeated else bossPresentation, frameless = true)
        if (phase in setOf(ReturnRaidPhase.YOUR_TURN, ReturnRaidPhase.STRIKING, ReturnRaidPhase.RESULT)) {
            PixelRagEggSprite(Modifier.align(Alignment.BottomStart).padding(start = 24.dp, bottom = 20.dp).size(54.dp))
        }
    }
}
