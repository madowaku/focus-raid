package com.madowaku.focusraid.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private const val PIXEL_RETURN_HOLD_MILLIS = 850L

/**
 * v0.11 ABYSS + Rag return flow.
 *
 * The existing ReturnRaidSequenceStateMachine remains authoritative. This composable only changes
 * the spatial presentation so the raid feels like the destination reached during focusing.
 */
@Composable
internal fun PixelReturnRaidSequence(
    scenario: ReturnRaidScenario,
    onFootprints: () -> Unit,
    onAgain: () -> Unit,
    onDone: () -> Unit,
    onStrikeAudio: () -> Unit = {},
    onVictoryAudio: () -> Unit = {},
    onEchoAudio: (Int) -> Unit = {},
) {
    val machine = remember(scenario) { ReturnRaidSequenceStateMachine(scenario) }
    var state by remember(scenario) { mutableStateOf(machine.state) }
    val flight = remember(scenario) { RaidFocusArrival() }
    var impactTrigger by remember(scenario) { mutableStateOf(0) }
    var arrivedHp by remember(scenario) { mutableStateOf(scenario.initialDisplayedHp) }
    val displayedHp = remember(scenario) { Animatable(scenario.initialDisplayedHp.toFloat()) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(arrivedHp) {
        displayedHp.animateTo(arrivedHp.toFloat(), animationSpec = tween(220))
    }

    LaunchedEffect(scenario, state.phase, state.echoIndex) {
        when (state.phase) {
            ReturnRaidPhase.RETURNING -> {
                delay(PIXEL_RETURN_HOLD_MILLIS)
                state = machine.advance()
            }
            ReturnRaidPhase.ECHO -> {
                flight.deliver(state.echoIndex) {
                    arrivedHp = state.displayedHp
                    impactTrigger += 1
                    onEchoAudio(state.echoIndex)
                }
                state = machine.advance()
            }
            ReturnRaidPhase.STRIKING -> {
                flight.deliver(0, self = true) {
                    arrivedHp = state.displayedHp
                    impactTrigger += 1
                    onStrikeAudio()
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
                state = machine.advance()
            }
            ReturnRaidPhase.RESULT -> {
                if (state.armorBroken) onVictoryAudio()
                delay(1_050)
                state = machine.advance()
            }
            ReturnRaidPhase.YOUR_TURN,
            ReturnRaidPhase.CAMP,
            -> Unit
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF070B12)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (state.phase == ReturnRaidPhase.CAMP) "遠征の火" else "火口の広場",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC8BFCE),
            )
            Spacer(Modifier.height(8.dp))

            if (state.phase != ReturnRaidPhase.CAMP) {
                val presentation = when {
                    state.phase == ReturnRaidPhase.RESULT && state.armorBroken -> BossPresentation.Defeated
                    displayedHp.value < scenario.initialDisplayedHp -> BossPresentation.Damaged
                    else -> BossPresentation.Normal
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                        .clip(RoundedCornerShape(28.dp)),
                ) {
                    PixelReturnRaidWorld(
                        phase = state.phase,
                        armorBroken = state.armorBroken,
                        bossPresentation = presentation,
                        modifier = Modifier.fillMaxSize(),
                    )
                    RaidFocusArrivalLayer(flight, Modifier.fillMaxSize())
                    KenneyCompletionOverlay(
                        triggerKey = if (state.phase == ReturnRaidPhase.RESULT && state.armorBroken) {
                            "pixel-raid-completion-${scenario.hashCode()}"
                        } else null,
                    )
                    KenneyRaidImpactOverlay(
                        trigger = impactTrigger,
                        emphasized = state.phase == ReturnRaidPhase.STRIKING || state.strikeCommitted,
                    )
                }
                Spacer(Modifier.height(10.dp))
                PixelRaidHpBar(
                    bossName = scenario.bossName,
                    hp = displayedHp.value.toInt(),
                    maxHp = scenario.bossMaxHp,
                )
                Spacer(Modifier.height(14.dp))
            }

            AnimatedContent(
                targetState = state.phase,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                label = "pixel-return-raid-phase",
                modifier = Modifier.weight(1f),
            ) { phase ->
                when (phase) {
                    ReturnRaidPhase.RETURNING -> PixelReturningMoment(scenario)
                    ReturnRaidPhase.ECHO -> PixelEchoMoment(scenario, state)
                    ReturnRaidPhase.YOUR_TURN -> PixelYourTurnMoment(scenario) {
                        if (state.phase == ReturnRaidPhase.YOUR_TURN) state = machine.strike()
                    }
                    ReturnRaidPhase.STRIKING -> PixelStrikeMoment(scenario)
                    ReturnRaidPhase.RESULT -> PixelResultMoment(scenario, state)
                    ReturnRaidPhase.CAMP -> PixelCampMoment(
                        scenario = scenario,
                        onFootprints = onFootprints,
                        onAgain = onAgain,
                        onDone = onDone,
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelRaidHpBar(bossName: String, hp: Int, maxHp: Int) {
    val safeMax = maxHp.coerceAtLeast(1)
    val safeHp = hp.coerceIn(0, safeMax)
    val fraction = safeHp.toFloat() / safeMax.toFloat()
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(bossName, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("$safeHp / $safeMax HP", fontSize = 11.sp, color = Color(0xFFBDB4C4))
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF292431)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFFB79AFF), Color(0xFFFF7D8C), Color(0xFFFFC867)),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun PixelReturningMoment(scenario: ReturnRaidScenario) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("火口へ到着した", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Text(
            "${scenario.creditedMinutes}分の遠征が、ここへ届いた。",
            textAlign = TextAlign.Center,
            color = Color(0xFFC8BFCE),
        )
    }
}

@Composable
private fun PixelEchoMoment(scenario: ReturnRaidScenario, state: ReturnRaidUiState) {
    val echo = scenario.echoes.getOrNull(state.echoIndex) ?: return
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("別の道から、灯が届く", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            "${echo.relativeTime} · ${echo.focusMinutes}分の集中",
            fontSize = 12.sp,
            color = Color(0xFFC8BFCE),
        )
        Text("−${echo.damage} HP", fontSize = 21.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF8B91))
    }
}

@Composable
private fun PixelYourTurnMoment(scenario: ReturnRaidScenario, onStrike: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (scenario.echoes.isEmpty()) "ここから、あなたの${scenario.creditedMinutes}分。"
            else "そして、あなたの${scenario.creditedMinutes}分が届く。",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text("ラグと、最後の一歩へ。", fontSize = 12.sp, color = Color(0xFFFFC867))
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = onStrike,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .testTag("first_raid_strike"),
            shape = RoundedCornerShape(30.dp),
        ) {
            Text("一撃を刻む", fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun PixelStrikeMoment(scenario: ReturnRaidScenario) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("あなたの${scenario.creditedMinutes}分", fontSize = 22.sp, fontWeight = FontWeight.Black)
        Text("火口の広場へ届いた", fontSize = 13.sp, color = Color(0xFFC8BFCE))
        Text("−${scenario.playerDamage} HP", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFFFF8B91))
    }
}

@Composable
private fun PixelResultMoment(scenario: ReturnRaidScenario, state: ReturnRaidUiState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (state.armorBroken) "討伐完了" else "一撃を刻んだ",
            fontSize = if (state.armorBroken) 30.sp else 22.sp,
            fontWeight = FontWeight.Black,
            color = if (state.armorBroken) Color(0xFFFFC867) else Color(0xFFF0EAF5),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (state.armorBroken) "あなたの${scenario.creditedMinutes}分が、最後の一撃になった。"
            else "この遠征は、次の誰かへつながっていく。",
            textAlign = TextAlign.Center,
            color = Color(0xFFC8BFCE),
        )
    }
}

@Composable
private fun PixelCampMoment(
    scenario: ReturnRaidScenario,
    onFootprints: () -> Unit,
    onAgain: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("first_raid_camp"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            scenario.echoes.forEach {
                SignatureRaidLight(modifier = Modifier.size(22.dp), color = MaterialTheme.colorScheme.primary)
            }
            SignatureRaidLight(modifier = Modifier.size(28.dp), color = Color(0xFFFFC867))
        }
        Spacer(Modifier.height(12.dp))
        Text("火口に、集中の灯が残った", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(
            "あなたの${scenario.creditedMinutes}分も、この遠征の一部になった。",
            textAlign = TextAlign.Center,
            color = Color(0xFFC8BFCE),
        )
        Spacer(Modifier.height(18.dp))
        OutlinedButton(onClick = onFootprints, modifier = Modifier.fillMaxWidth()) {
            Text("この場所に足跡を残す")
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onAgain,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = RoundedCornerShape(28.dp),
        ) {
            Text("もう一度、遠征へ", fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onDone) { Text("今日はここまで") }
    }
}
