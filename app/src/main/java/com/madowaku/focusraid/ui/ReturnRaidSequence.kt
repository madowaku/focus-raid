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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun ReturnRaidSequence(
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
    var impactTrigger by remember(scenario) { mutableStateOf(0) }
    val displayedHp = remember(scenario) { Animatable(scenario.initialDisplayedHp.toFloat()) }
    val haptics = LocalHapticFeedback.current

    LaunchedEffect(state.displayedHp) {
        displayedHp.animateTo(
            targetValue = state.displayedHp.toFloat(),
            animationSpec = tween(
                durationMillis = if (state.phase == ReturnRaidPhase.STRIKING) 650 else 420,
            ),
        )
    }

    LaunchedEffect(state.phase, state.echoIndex) {
        when (state.phase) {
            ReturnRaidPhase.RETURNING -> {
                delay(1_250)
                state = machine.advance()
            }

            ReturnRaidPhase.ECHO -> {
                onEchoAudio(state.echoIndex)
                impactTrigger += 1
                delay(820)
                state = machine.advance()
            }

            ReturnRaidPhase.STRIKING -> {
                delay(700)
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

    SignatureBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "FIRST RAID",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = state.phase,
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "first-raid-phase",
                    modifier = Modifier.fillMaxSize(),
                ) { phase ->
                    when (phase) {
                        ReturnRaidPhase.RETURNING -> ReturnMoment(scenario)
                        ReturnRaidPhase.ECHO -> EchoMoment(
                            scenario = scenario,
                            state = state,
                            displayedHp = displayedHp.value.toInt(),
                        )

                        ReturnRaidPhase.YOUR_TURN -> YourTurnMoment(
                            scenario = scenario,
                            displayedHp = displayedHp.value.toInt(),
                            onStrike = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (state.phase == ReturnRaidPhase.YOUR_TURN) {
                                    onStrikeAudio()
                                    impactTrigger += 1
                                    state = machine.strike()
                                }
                            },
                        )

                        ReturnRaidPhase.STRIKING -> StrikeMoment(
                            scenario = scenario,
                            displayedHp = displayedHp.value.toInt(),
                        )

                        ReturnRaidPhase.RESULT -> ResultMoment(
                            scenario = scenario,
                            state = state,
                            displayedHp = displayedHp.value.toInt(),
                        )

                        ReturnRaidPhase.CAMP -> CampMoment(
                            scenario = scenario,
                            onFootprints = onFootprints,
                            onAgain = onAgain,
                            onDone = onDone,
                        )
                    }
                }
                KenneyCompletionOverlay(
                    triggerKey = if (state.phase == ReturnRaidPhase.RESULT && state.armorBroken) {
                        "raid-completion-${scenario.hashCode()}"
                    } else {
                        null
                    },
                )
                KenneyRaidImpactOverlay(trigger = impactTrigger)
            }
        }
    }
}

@Composable
private fun ReturnMoment(scenario: ReturnRaidScenario) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("帰還しました", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        Text(
            "%d:%02d".format(scenario.creditedMinutes, 0),
            fontSize = 48.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "集中 +${scenario.creditedMinutes}分",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            if (scenario.echoes.isEmpty()) "現在の遠征地点へ戻っています…" else "残響をたどっています…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        BossArtwork(Modifier.size(86.dp), presentation = BossPresentation.Normal)
    }
}

@Composable
private fun EchoMoment(
    scenario: ReturnRaidScenario,
    state: ReturnRaidUiState,
    displayedHp: Int,
) {
    val echo = scenario.echoes.getOrNull(state.echoIndex) ?: return
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BossArtwork(
            Modifier.size(116.dp),
            presentation = BossPresentation.Damaged,
        )
        Spacer(Modifier.height(12.dp))
        RaidIntegrityCard(scenario, displayedHp)
        Spacer(Modifier.height(14.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .90f),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SignatureRaidLight()
                    Spacer(Modifier.size(7.dp))
                    Text("${echo.relativeTime}  ·  残響が届いた", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(4.dp))
                Text("みんなの先行する一撃", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${echo.focusMinutes}分", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "−${echo.damage} HP",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun YourTurnMoment(
    scenario: ReturnRaidScenario,
    displayedHp: Int,
    onStrike: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BossArtwork(
            Modifier.size(120.dp),
            presentation = BossPresentation.Damaged,
        )
        Spacer(Modifier.height(16.dp))
        RaidIntegrityCard(scenario, displayedHp)
        Spacer(Modifier.height(24.dp))
        Text(
            if (scenario.echoes.isEmpty()) "まだ他の残響はありません" else "みんなの灯のあとへ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (scenario.echoes.isEmpty()) {
                "ここから、あなたの${scenario.creditedMinutes}分。"
            } else {
                "そして、あなたの${scenario.creditedMinutes}分が届く。"
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(7.dp))
        Text(
            "最後に、あなたの集中が届く",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(17.dp))
        Button(
            onClick = onStrike,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .testTag("first_raid_strike"),
            shape = RoundedCornerShape(32.dp),
        ) {
            Text("一撃を刻む", fontSize = 19.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
private fun StrikeMoment(
    scenario: ReturnRaidScenario,
    displayedHp: Int,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BossArtwork(
            Modifier.size(128.dp),
            presentation = BossPresentation.Damaged,
        )
        Spacer(Modifier.height(18.dp))
        Text("あなたの${scenario.creditedMinutes}分", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("↓", fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "−${scenario.playerDamage} HP",
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(18.dp))
        RaidIntegrityCard(scenario, displayedHp)
    }
}

@Composable
private fun ResultMoment(
    scenario: ReturnRaidScenario,
    state: ReturnRaidUiState,
    displayedHp: Int,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            if (state.armorBroken) "討伐完了" else "一撃を刻みました",
            fontSize = if (state.armorBroken) 38.sp else 24.sp,
            fontWeight = FontWeight.Black,
            color = if (state.armorBroken) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        BossArtwork(
            Modifier.size(126.dp),
            presentation = if (state.armorBroken) {
                BossPresentation.Defeated
            } else {
                BossPresentation.Damaged
            },
        )
        Spacer(Modifier.height(18.dp))
        RaidIntegrityCard(scenario, displayedHp)
        Spacer(Modifier.height(12.dp))
        Text(
            if (state.armorBroken) {
                "あなたの${scenario.creditedMinutes}分が最後の一撃になりました"
            } else if (scenario.echoes.isEmpty()) {
                "あなたの${scenario.creditedMinutes}分が、最初の足跡になりました"
            } else {
                "あなたの${scenario.creditedMinutes}分が、遠征隊の続きになりました"
            },
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CampMoment(
    scenario: ReturnRaidScenario,
    onFootprints: () -> Unit,
    onAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val recentMinutes = scenario.echoes.sumOf { it.focusMinutes.coerceAtLeast(0) }
    val visibleLights = (scenario.echoes.size + 1).coerceIn(1, 4)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("first_raid_camp"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        Text("第001遠征隊", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    repeat(visibleLights) {
                        Text("🔥", fontSize = 28.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("集中の火", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                if (scenario.echoes.isEmpty()) {
                    Text(
                        "まだ他の残響はありません",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "あなた +${scenario.creditedMinutes}分",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "あなたが最初の火を残しました",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                } else {
                    Text(
                        "最近の残響 ${scenario.echoes.size}件 · ${recentMinutes}分",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "+ あなた ${scenario.creditedMinutes}分",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "火をつなぎました",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onClick = onFootprints,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("👣  足跡を見る・残す")
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onAgain,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            shape = RoundedCornerShape(32.dp),
        ) {
            Text(
                "もう${scenario.creditedMinutes.coerceAtLeast(1)}分",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        TextButton(onClick = onDone) {
            Text("今日はここまで")
        }
    }
}

@Composable
private fun RaidIntegrityCard(scenario: ReturnRaidScenario, displayedHp: Int) {
    val safeHp = displayedHp.coerceIn(0, scenario.bossMaxHp)
    val progress = safeHp.toFloat() / scenario.bossMaxHp.toFloat()
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(scenario.bossName, fontWeight = FontWeight.Bold)
            Text("装甲耐久", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            "$safeHp / ${scenario.bossMaxHp}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
