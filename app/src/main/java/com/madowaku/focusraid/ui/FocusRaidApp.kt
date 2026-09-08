package com.madowaku.focusraid.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.madowaku.focusraid.billing.FeatureAccess
import com.madowaku.focusraid.core.domain.CompanionEvolution
import com.madowaku.focusraid.core.domain.CompanionGrowth
import com.madowaku.focusraid.core.domain.CompanionStage
import com.madowaku.focusraid.core.domain.FocusRules
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.SessionReward
import kotlin.math.max
import kotlinx.coroutines.delay

internal enum class MainTab(val label: String, val glyph: String) {
    HOME("ホーム", "⌂"),
    RAID("レイド", "⚔"),
    COMPANION("相棒", "◆"),
    LOG("ログ", "▤"),
}

private enum class OverviewArtwork {
    Boss,
    Companion,
    Log,
}

@Composable
fun FocusRaidApp(viewModel: FocusViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showCustomDuration by rememberSaveable { mutableStateOf(false) }
    var customDurationMinutes by rememberSaveable { mutableStateOf(state.selectedMinutes) }

    FocusRaidAppContent(
        state = state,
        tab = tab,
        onTabChange = { tab = it },
        onSelectMinutes = viewModel::selectMinutes,
        onSelectExpedition = viewModel::selectExpedition,
        onTimerClick = {
            customDurationMinutes = state.selectedMinutes
            showCustomDuration = true
        },
        onStart = viewModel::start,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onFinishEarly = viewModel::finishEarly,
        onAgain = viewModel::startAgain,
        onDone = viewModel::resetAfterResult,
    )

    if (showCustomDuration && state.phase == SessionPhase.READY) {
        CustomDurationSheet(
            minutes = customDurationMinutes,
            onMinutesChange = { customDurationMinutes = it },
            onConfirm = {
                viewModel.selectMinutes(customDurationMinutes)
                showCustomDuration = false
            },
            onDismiss = { showCustomDuration = false },
        )
    }
}

@Composable
internal fun FocusRaidAppContent(
    state: FocusUiState,
    tab: MainTab = MainTab.HOME,
    onTabChange: (MainTab) -> Unit = {},
    onSelectMinutes: (Int) -> Unit = {},
    onSelectExpedition: (Expedition) -> Unit = {},
    onTimerClick: (() -> Unit)? = null,
    onStart: () -> Unit = {},
    onPause: () -> Unit = {},
    onResume: () -> Unit = {},
    onFinishEarly: () -> Unit = {},
    onAgain: () -> Unit = {},
    onDone: () -> Unit = {},
) {
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
                            onClick = { onTabChange(item) },
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
        FantasyBackground(phase = state.phase) {
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
                                onPause = onPause,
                                onResume = onResume,
                                onFinishEarly = onFinishEarly,
                            )

                            SessionPhase.COMPLETED -> VictoryScreen(
                                state = state,
                                onAgain = onAgain,
                                onDone = onDone,
                            )

                            SessionPhase.ABORTED -> AbortedScreen(
                                state = state,
                                onDone = onDone,
                            )

                            SessionPhase.READY -> Unit
                        }
                    }
                } else {
                    when (tab) {
                        MainTab.HOME -> ReadyScreen(
                            state = state,
                            onSelectMinutes = onSelectMinutes,
                            onSelectExpedition = onSelectExpedition,
                            onTimerClick = onTimerClick,
                            onStart = onStart,
                        )

                        MainTab.RAID -> WorldRaidOverview(state)
                        MainTab.COMPANION -> CompanionProgressOverview(state)
                        MainTab.LOG -> SessionHistoryOverview(state)
                    }
                }
            }
        }
    }
}

@Composable
private fun FantasyBackground(phase: SessionPhase, content: @Composable () -> Unit) {
    val colors = when (phase) {
        SessionPhase.RUNNING -> listOf(
            Color(0xFF080611),
            Color(0xFF120D22),
            Color(0xFF1D1030),
        )

        SessionPhase.PAUSED -> listOf(
            Color(0xFF08080D),
            Color(0xFF111019),
            Color(0xFF17151F),
        )

        SessionPhase.COMPLETED -> listOf(
            Color(0xFF12091D),
            Color(0xFF28133B),
            Color(0xFF3A2141),
        )

        SessionPhase.ABORTED -> listOf(
            Color(0xFF100A10),
            Color(0xFF1D111B),
            Color(0xFF2A1822),
        )

        SessionPhase.READY -> listOf(
            Color(0xFF0B0816),
            Color(0xFF17102A),
            Color(0xFF26133C),
        )
    }

    val glowAlpha = when (phase) {
        SessionPhase.RUNNING -> 0.16f
        SessionPhase.PAUSED -> 0.08f
        else -> 0.24f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors)),
    ) {
        Canvas(Modifier.fillMaxSize().alpha(glowAlpha)) {
            drawCircle(
                Color(0xFF7C4DFF),
                radius = size.minDimension * 0.34f,
                center = Offset(size.width * .82f, size.height * .22f),
            )
            drawCircle(
                Color(0xFF3CD9C5),
                radius = size.minDimension * 0.22f,
                center = Offset(size.width * .08f, size.height * .76f),
            )
            drawCircle(
                if (phase == SessionPhase.COMPLETED) Color(0xFFFFC857) else Color(0xFFFF7A96),
                radius = size.minDimension * 0.18f,
                center = Offset(size.width * .92f, size.height * .88f),
            )
        }
        content()
    }
}

@Composable
private fun ReadyScreen(
    state: FocusUiState,
    onSelectMinutes: (Int) -> Unit,
    onSelectExpedition: (Expedition) -> Unit,
    onTimerClick: (() -> Unit)?,
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
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(218.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            TimerRing(
                text = formatClock(state.selectedMinutes * 60),
                progress = 1f,
                diameter = 184,
                onClick = onTimerClick,
            )
            CompanionHero(
                stage = CompanionGrowth.from(state.totalFocusMinutes).stage,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 2.dp),
            )
        }

        Spacer(Modifier.height(8.dp))
        DurationSelector(state.selectedMinutes, onSelectMinutes)

        Spacer(Modifier.height(8.dp))
        ExpeditionSelector(state.expedition, onSelectExpedition)

        Spacer(Modifier.height(12.dp))
        StartButton(expedition = state.expedition, onClick = onStart)

        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = state.expedition,
            label = "ready-expedition-card",
        ) { expedition ->
            if (expedition == Expedition.STAR_ROUTE) {
                StarRouteLaunchCard(state)
            } else {
                BossCard(state)
            }
        }
        Spacer(Modifier.height(12.dp))
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
        Text(
            "Focus Raid",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
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
private fun CompanionHero(stage: CompanionStage, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CompanionArtwork(
            modifier = Modifier.size(62.dp),
            stage = stage,
            mood = CompanionMood.Idle,
        )
        Surface(
            shape = CircleShape,
            color = Color(0xCC211735),
        ) {
            Text(
                "ラグ · ${stage.label}",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
    val accessLevel = LocalProAccessLevel.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Expedition.entries.forEach { expedition ->
            val locked = !FeatureAccess.canUse(expedition, accessLevel)
            val label = when (expedition) {
                Expedition.TOWER -> "天空塔"
                Expedition.ABYSS -> "深層迷宮"
                Expedition.STAR_ROUTE -> if (locked) "🔒 星渡り" else "✦ 星渡り"
            }
            FilterChip(
                selected = selected == expedition,
                onClick = { onSelect(expedition) },
                label = {
                    Text(
                        label,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun StartButton(expedition: Expedition, onClick: () -> Unit) {
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
            if (expedition == Expedition.STAR_ROUTE) "✦  星渡りへ出航" else "⚔  レイド開始",
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
                Text(
                    "${world.raidParticipants.toStringWithCommas()}人",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(world.bossName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    RaidHpBar(world.bossHp, world.bossMaxHp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "${world.bossHp.toStringWithCommas()} / ${world.bossMaxHp.toStringWithCommas()} HP",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text("🎁 25分集中で探索ロール", fontSize = 12.sp)
                }
                BossArtwork(Modifier.size(64.dp))
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
    val companionStage = CompanionGrowth.from(state.totalFocusMinutes).stage
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
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    when {
                        paused -> "PAUSED"
                        state.expedition == Expedition.STAR_ROUTE -> "STAR ROUTE"
                        else -> "RAID"
                    },
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.weight(if (paused) 0.20f else 0.28f))
        CompanionArtwork(
            modifier = Modifier.size(48.dp),
            stage = companionStage,
            mood = if (paused) CompanionMood.Idle else CompanionMood.Focused,
        )
        Spacer(Modifier.height(4.dp))

        TimerRing(
            text = formatClock(state.remainingSeconds),
            progress = state.progress,
            diameter = 208,
        )

        Spacer(Modifier.height(18.dp))
        if (paused) {
            Text(
                "集中は止まっています",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onResume,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
            ) {
                Text("▶  再開する", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            TextButton(onClick = onFinishEarly) {
                Text("セッションを終了")
            }
            Spacer(Modifier.weight(0.32f))
        } else {
            Button(
                onClick = onPause,
                modifier = Modifier.size(64.dp),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text("Ⅱ", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onFinishEarly) {
                Text("セッションを終了")
            }

            Spacer(Modifier.weight(0.22f))
            RaidMiniCard(state)
        }
    }
}

@Composable
private fun RaidMiniCard(state: FocusUiState) {
    if (state.expedition == Expedition.STAR_ROUTE) {
        StarRouteProgressCard(state)
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(state.world.bossName, fontWeight = FontWeight.Bold)
                BossArtwork(Modifier.size(36.dp))
            }
            Spacer(Modifier.height(9.dp))
            RaidHpBar(state.world.bossHp, state.world.bossMaxHp)
            Spacer(Modifier.height(5.dp))
            Text(
                "今回の貢献  +${max(0, (state.durationSeconds - state.remainingSeconds) / 60)} DAMAGE",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AbortedScreen(
    state: FocusUiState,
    onDone: () -> Unit,
) {
    val reward = state.reward ?: SessionReward(0, 0, 0, 0, null, null, 0)
    val remainingBossHp = (state.world.bossHp - reward.personalDamage).coerceAtLeast(0)
    val companionStage = CompanionGrowth.from(state.totalFocusMinutes).stage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.16f))
        Text(
            "レイド撤退",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${reward.creditedMinutes}分間 集中しました",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))
        CompanionArtwork(
            modifier = Modifier.size(56.dp),
            stage = companionStage,
            mood = CompanionMood.Idle,
        )
        state.companionEvolution?.let { evolution ->
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
                Text(
                    "✦ RAG EVOLVED!  ${evolution.from.label} → ${evolution.to.label}",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "+${reward.personalDamage} DAMAGE",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (reward.creditedMinutes > 0) {
                        "途中終了でも、集中した分は戦果として残ります"
                    } else {
                        "1分未満だったため、今回は戦果の加算なし"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.world.bossName, fontWeight = FontWeight.Bold)
                    BossArtwork(Modifier.size(34.dp))
                }
                Spacer(Modifier.height(8.dp))
                RaidHpBar(remainingBossHp, state.world.bossMaxHp)
                Spacer(Modifier.height(5.dp))
                Text(
                    "${remainingBossHp.toStringWithCommas()} HP",
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Start,
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(32.dp),
        ) {
            Text("ホームへ", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.weight(0.18f))
    }
}

@Composable
private fun VictoryScreen(
    state: FocusUiState,
    onAgain: () -> Unit,
    onDone: () -> Unit,
) {
    val reward = state.reward ?: SessionReward(0, 0, 0, 0, null, null, 0)
    val companionStage = CompanionGrowth.from(state.totalFocusMinutes).stage

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("✦", fontSize = 24.sp, color = MaterialTheme.colorScheme.tertiary)
        Text(
            "FOCUS COMPLETE",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))

        state.companionEvolution?.let { evolution ->
            CompanionEvolutionCard(evolution)
            Spacer(Modifier.height(12.dp))
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .90f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("${reward.creditedMinutes}分集中", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CompanionArtwork(
                        modifier = Modifier.size(40.dp),
                        stage = companionStage,
                        mood = CompanionMood.Celebrate,
                    )
                    RaidClashMark(Modifier.size(28.dp))
                    BossArtwork(Modifier.size(40.dp))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "+${reward.personalDamage} DAMAGE",
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    "集中した時間がそのまま戦果になりました",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        if (state.expedition == Expedition.STAR_ROUTE) {
            Spacer(Modifier.height(12.dp))
            StarRouteVictoryCard(state)
        }

        Spacer(Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
            ),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(state.world.bossName, fontWeight = FontWeight.Bold)
                    Text("残HP", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(9.dp))
                RaidHpBar(
                    current = (state.world.bossHp - reward.personalDamage).coerceAtLeast(0),
                    max = state.world.bossMaxHp,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    "${(state.world.bossHp - reward.personalDamage).coerceAtLeast(0).toStringWithCommas()} HP",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            RewardMetricCard(
                title = "WORLD EP",
                value = "+${reward.worldEp}",
                modifier = Modifier.weight(1f),
            )
            RewardMetricCard(
                title = "探索",
                value = reward.discovery ?: "次回へ",
                modifier = Modifier.weight(1f),
            )
        }
        if (reward.discovery != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                "🎁 ${reward.rarity} · ARMORY +${reward.armoryPoints}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))
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
private fun CompanionEvolutionCard(evolution: CompanionEvolution) {
    var revealNewStage by remember(evolution) { mutableStateOf(false) }

    LaunchedEffect(evolution) {
        delay(650L)
        revealNewStage = true
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .94f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "RAG EVOLVED!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${evolution.from.label}  →  ${evolution.to.label}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(4.dp))
            AnimatedContent(
                targetState = if (revealNewStage) evolution.to else evolution.from,
                label = "rag-evolution",
            ) { stage ->
                CompanionArtwork(
                    modifier = Modifier.size(112.dp),
                    stage = stage,
                    mood = if (stage == evolution.to) CompanionMood.Celebrate else CompanionMood.Idle,
                )
            }
            Text(
                if (revealNewStage) {
                    "集中時間が、新しい姿を解放しました。"
                } else {
                    "ラグの気配が変わっていく…"
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .82f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RewardMetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .80f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun RaidOverview(state: FocusUiState) {
    SimpleOverview(
        title = "WORLD RAID",
        artwork = OverviewArtwork.Boss,
        body = "${state.world.bossName}\n${state.world.raidParticipants.toStringWithCommas()}人が参加予定\nARMORY ${state.world.armoryReady}% READY",
    )
}

@Composable
private fun CompanionOverview(state: FocusUiState) {
    SimpleOverview(
        title = "YOUR COMPANION",
        artwork = OverviewArtwork.Companion,
        body = "ラグ · ${FocusRules.companionStage(state.totalFocusMinutes)}\n一緒に集中した時間 ${state.totalFocusMinutes / 60}h ${state.totalFocusMinutes % 60}m",
    )
}

@Composable
private fun LogOverview(state: FocusUiState) {
    SimpleOverview(
        title = "ADVENTURE LOG",
        artwork = OverviewArtwork.Log,
        body = "累計集中 ${state.totalFocusMinutes / 60}h ${state.totalFocusMinutes % 60}m\n天空塔 ${state.world.towerFloor}F\n深層迷宮 ${state.world.abyssDepth}m",
    )
}

@Composable
private fun SimpleOverview(title: String, artwork: OverviewArtwork, body: String) {
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
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .9f),
            ),
        ) {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (artwork) {
                    OverviewArtwork.Boss -> BossArtwork(Modifier.size(96.dp))
                    OverviewArtwork.Companion -> CompanionArtwork(Modifier.size(96.dp))
                    OverviewArtwork.Log -> Text("▤", fontSize = 64.sp)
                }
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
private fun TimerRing(
    text: String,
    progress: Float,
    diameter: Int,
    onClick: (() -> Unit)? = null,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(350),
        label = "timer-progress",
    )

    val ringModifier = Modifier
        .size(diameter.dp)
        .then(
            if (onClick == null) {
                Modifier
            } else {
                Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = "集中時間を変更",
                    onClick = onClick,
                )
            },
        )

    Box(
        modifier = ringModifier,
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
                brush = Brush.sweepGradient(
                    listOf(Color(0xFFB69CFF), Color(0xFFFF7A96), Color(0xFFB69CFF)),
                ),
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
