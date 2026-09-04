package com.madowaku.focusraid.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madowaku.focusraid.core.domain.FocusRules
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.SessionReward
import kotlin.math.max

private enum class MainTab(val label: String, val glyph: String) {
    HOME("ホーム", "⌂"),
    RAID("レイド", "⚔"),
    COMPANION("相棒", "◆"),
    LOG("ログ", "▤"),
}

@Composable
fun FocusRaidApp(viewModel: FocusViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    val inSession = state.phase != SessionPhase.READY

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (!inSession) {
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = Color(0xE61B1430),
                ) {
                    MainTab.entries.forEach { item ->
                        NavigationBarItem(
                            selected = tab == item,
                            onClick = { tab = item },
                            icon = { Text(item.glyph, fontSize = 20.sp) },
                            label = { Text(item.label, fontSize = 11.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        FantasyBackground {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (inSession) {
                    AnimatedContent(targetState = state.phase, label = "session-state") { phase ->
                        when (phase) {
                            SessionPhase.RUNNING, SessionPhase.PAUSED -> RaidScreen(
                                state = state,
                                onPause = viewModel::pause,
                                onResume = viewModel::resume,
                                onFinishEarly = viewModel::finishEarly,
                            )
                            SessionPhase.COMPLETED -> VictoryScreen(
                                state = state,
                                onAgain = viewModel::startAgain,
                                onDone = viewModel::resetAfterResult,
                            )
                            else -> Unit
                        }
                    }
                } else {
                    when (tab) {
                        MainTab.HOME -> ReadyScreen(
                            state = state,
                            onSelectMinutes = viewModel::selectMinutes,
                            onSelectExpedition = viewModel::selectExpedition,
                            onStart = viewModel::start,
                        )
                        MainTab.RAID -> RaidOverview(state)
                        MainTab.COMPANION -> CompanionOverview(state)
                        MainTab.LOG -> LogOverview(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun FantasyBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0B0816),
                        Color(0xFF17102A),
                        Color(0xFF26133C),
                    ),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize().alpha(0.26f)) {
            drawCircle(Color(0xFF7C4DFF), radius = size.minDimension * 0.34f, center = Offset(size.width * .82f, size.height * .22f))
            drawCircle(Color(0xFF3CD9C5), radius = size.minDimension * 0.22f, center = Offset(size.width * .08f, size.height * .76f))
            drawCircle(Color(0xFFFF7A96), radius = size.minDimension * 0.18f, center = Offset(size.width * .92f, size.height * .88f))
        }
        content()
    }
}

@Composable
private fun ReadyScreen(
    state: FocusUiState,
    onSelectMinutes: (Int) -> Unit,
    onSelectExpedition: (Expedition) -> Unit,
    onStart: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TopBar(streak = state.streakDays)
        Spacer(Modifier.height(10.dp))

        CompanionHero(stage = FocusRules.companionStage(state.totalFocusMinutes))
        Spacer(Modifier.height(4.dp))

        TimerRing(
            text = formatClock(state.selectedMinutes * 60),
            progress = 1f,
            diameter = 184,
        )

        Spacer(Modifier.height(12.dp))
        DurationSelector(state.selectedMinutes, onSelectMinutes)

        Spacer(Modifier.height(12.dp))
        ExpeditionSelector(state.expedition, onSelectExpedition)

        Spacer(Modifier.height(12.dp))
        StartButton(onStart)

        Spacer(Modifier.height(16.dp))
        BossCard(state)

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TopBar(streak: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "Focus Raid",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "集中を、冒険に変える。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
        ) {
            Text(
                "🔥 $streak",
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun CompanionHero(stage: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFB69CFF), Color.Transparent),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text("🐉", fontSize = 42.sp)
        }
        Text(
            "ラグ · $stage",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DurationSelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(15, 25, 45, 60).forEach { minutes ->
            FilterChip(
                selected = selected == minutes,
                onClick = { onSelect(minutes) },
                label = {
                    Text(
                        "$minutes",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            )
        }
    }
}

@Composable
private fun ExpeditionSelector(selected: Expedition, onSelect: (Expedition) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Expedition.entries.forEach { expedition ->
            FilterChip(
                selected = selected == expedition,
                onClick = { onSelect(expedition) },
                label = {
                    Text(
                        if (expedition == Expedition.TOWER) "天空塔" else "深層迷宮",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StartButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentPadding = PaddingValues(horizontal = 24.dp),
        shapes = ButtonDefaults.shapes(
            shape = RoundedCornerShape(32.dp),
            pressedShape = RoundedCornerShape(20.dp),
        ),
    ) {
        Text(
            "⚔  レイド開始",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BossCard(state: FocusUiState) {
    val world = state.world
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("CURRENT RAID", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                Text("${world.raidParticipants}人参加予定", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(world.bossName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    RaidHpBar(world.bossHp, world.bossMaxHp)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${world.bossHp.toStringWithCommas()} / ${world.bossMaxHp.toStringWithCommas()} HP",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("🎁 25分集中で探索ロール", fontSize = 12.sp)
                }
                Text("🐲", fontSize = 56.sp)
            }
        }
    }
}

@Composable
private fun RaidScreen(
    state: FocusUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinishEarly: () -> Unit,
) {
    val paused = state.phase == SessionPhase.PAUSED
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Focus Raid", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                if (paused) "PAUSED" else "RAID",
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.weight(0.35f))
        Text("🐉", fontSize = 46.sp)
        Spacer(Modifier.height(6.dp))

        TimerRing(
            text = formatClock(state.remainingSeconds),
            progress = state.progress,
            diameter = 208,
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = if (paused) onResume else onPause,
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(if (paused) "▶" else "Ⅱ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onFinishEarly) {
            Text("セッションを終了")
        }

        Spacer(Modifier.weight(0.25f))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f)),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(state.world.bossName, fontWeight = FontWeight.Bold)
                    Text("🐲", fontSize = 28.sp)
                }
                Spacer(Modifier.height(10.dp))
                RaidHpBar(state.world.bossHp, state.world.bossMaxHp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "今回の貢献  +${max(0, (state.durationSeconds - state.remainingSeconds) / 60)} DAMAGE",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VictoryScreen(
    state: FocusUiState,
    onAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val reward = state.reward ?: SessionReward(0, 0, 0, 0, null, null, 0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✦", fontSize = 28.sp, color = MaterialTheme.colorScheme.tertiary)
        Text(
            "FOCUS COMPLETE",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        Text("${reward.creditedMinutes}分", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("🐉  ⚔  🐲", fontSize = 42.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "+${reward.personalDamage} DAMAGE",
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
        )
        Text(
            "集中した時間がそのまま戦果になりました",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f)),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(state.world.bossName, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                RaidHpBar(
                    current = (state.world.bossHp - reward.personalDamage).coerceAtLeast(0),
                    max = state.world.bossMaxHp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "${(state.world.bossHp - reward.personalDamage).coerceAtLeast(0).toStringWithCommas()} HP",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .85f)),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("獲得", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Text("+${reward.worldEp} WORLD EP", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (reward.discovery != null) {
                    Spacer(Modifier.height(6.dp))
                    Text("🎁 ${reward.rarity} · ${reward.discovery}", fontSize = 15.sp)
                } else {
                    Spacer(Modifier.height(6.dp))
                    Text("探索は次回へ続く", fontSize = 14.sp)
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
        ) {
            Text("もう${state.selectedMinutes}分集中する", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        TextButton(onClick = onDone) {
            Text("完了")
        }
    }
}

@Composable
private fun RaidOverview(state: FocusUiState) {
    SimpleOverview(
        title = "WORLD RAID",
        glyph = "🐲",
        body = "${state.world.bossName}\n${state.world.raidParticipants.toStringWithCommas()}人が参加予定\nARMORY ${state.world.armoryReady}% READY",
    )
}

@Composable
private fun CompanionOverview(state: FocusUiState) {
    SimpleOverview(
        title = "YOUR COMPANION",
        glyph = "🐉",
        body = "ラグ · ${FocusRules.companionStage(state.totalFocusMinutes)}\n一緒に集中した時間 ${state.totalFocusMinutes / 60}h ${state.totalFocusMinutes % 60}m",
    )
}

@Composable
private fun LogOverview(state: FocusUiState) {
    SimpleOverview(
        title = "ADVENTURE LOG",
        glyph = "▤",
        body = "累計集中 ${state.totalFocusMinutes / 60}h ${state.totalFocusMinutes % 60}m\n天空塔 ${state.world.towerFloor}F\n深層迷宮 ${state.world.abyssDepth}m",
    )
}

@Composable
private fun SimpleOverview(title: String, glyph: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f)),
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(glyph, fontSize = 64.sp)
                Spacer(Modifier.height(12.dp))
                Text(body, textAlign = TextAlign.Center, lineHeight = 24.sp)
            }
        }
    }
}

@Composable
private fun RaidHpBar(current: Int, max: Int) {
    val progress = if (max <= 0) 0f else (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
    }
}

@Composable
private fun TimerRing(text: String, progress: Float, diameter: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(350),
        label = "timer-progress",
    )

    Box(
        modifier = Modifier.size(diameter.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2f
            drawArc(
                color = Color(0xFF3A3150),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(Color(0xFFB69CFF), Color(0xFFFF7A96), Color(0xFFB69CFF))),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress.coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Text(
            text,
            fontSize = if (diameter >= 200) 68.sp else 60.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = (-2).sp,
        )
    }
}

private fun formatClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

private fun Int.toStringWithCommas(): String = "%,d".format(this)
