package com.madowaku.focusraid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.billing.FeatureAccess
import com.madowaku.focusraid.core.domain.CompanionGrowth
import com.madowaku.focusraid.core.domain.CompanionStage
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.data.RaidEcho

@Composable
internal fun FocusRaidSignatureContent(
    state: FocusUiState,
    tab: MainTab,
    onTabChange: (MainTab) -> Unit,
    onSelectCompanion: (com.madowaku.focusraid.core.domain.CompanionIdentity) -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onSelectExpedition: (Expedition) -> Unit,
    onTimerClick: (() -> Unit)?,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinishEarly: () -> Unit,
    onAgain: () -> Unit,
    onDone: () -> Unit,
    recentEchoes: List<RaidEcho> = emptyList(),
) {
    when {
        state.phase == SessionPhase.READY && tab == MainTab.HOME -> SignatureReadyScreen(
            state = state,
            recentEchoes = recentEchoes,
            onTabChange = onTabChange,
            onSelectMinutes = onSelectMinutes,
            onSelectExpedition = onSelectExpedition,
            onTimerClick = onTimerClick,
            onStart = onStart,
        )

        state.phase == SessionPhase.RUNNING || state.phase == SessionPhase.PAUSED -> SignatureFocusingScreen(
            state = state,
            onPause = onPause,
            onResume = onResume,
            onFinishEarly = onFinishEarly,
        )

        else -> FocusRaidAppContent(
            state = state,
            tab = tab,
            onTabChange = onTabChange,
            onSelectCompanion = onSelectCompanion,
            onSelectMinutes = onSelectMinutes,
            onSelectExpedition = onSelectExpedition,
            onTimerClick = onTimerClick,
            onStart = onStart,
            onPause = onPause,
            onResume = onResume,
            onFinishEarly = onFinishEarly,
            onAgain = onAgain,
            onDone = onDone,
        )
    }
}

@Composable
private fun SignatureReadyScreen(
    state: FocusUiState,
    recentEchoes: List<RaidEcho>,
    onTabChange: (MainTab) -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onSelectExpedition: (Expedition) -> Unit,
    onTimerClick: (() -> Unit)?,
    onStart: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = { SignatureBottomNavigation(MainTab.HOME, onTabChange) },
    ) { padding ->
        SignatureBackdrop {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SignatureTopBar(state)
                SignatureRaidHero(state)

                Box(Modifier.fillMaxWidth()) {
                    Button(
                        onClick = onStart,
                        enabled = state.initialized && !state.saving && state.persistenceError == null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 62.dp),
                        shape = RoundedCornerShape(28.dp),
                        contentPadding = PaddingValues(horizontal = 22.dp),
                    ) {
                        Text(
                            signatureDepartureLabel(state.selectedMinutes, state.expedition),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                    SignatureBrandMark(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(28.dp),
                        alpha = .10f,
                    )
                }

                if (recentEchoes.isNotEmpty()) SignatureRecentEchoes(recentEchoes)
                SignatureCompanionRow(state)

                SignatureDurationSelector(
                    selected = state.selectedMinutes,
                    onSelect = onSelectMinutes,
                    onCustom = onTimerClick,
                )
                SignatureExpeditionSelector(
                    selected = state.expedition,
                    onSelect = onSelectExpedition,
                )
                SignatureWorldPresence(state)

                if (!state.initialized || state.saving) {
                    Text(
                        "遠征記録を確認しています…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                WorldStatusLabel(state.worldSyncStatus)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun SignatureTopBar(state: FocusUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text("Focus Raid", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(
                "集中すると、世界が進む",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = LocalOpenProPaywall.current) {
                Text(
                    if (LocalProAccessLevel.current == com.madowaku.focusraid.billing.AccessLevel.PRO) "PRO" else "Pro",
                    fontSize = 11.sp,
                )
            }
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .78f),
            ) {
                Text(
                    "🔥 ${state.streakDays}",
                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SignatureRaidHero(
    state: FocusUiState,
) {
    val world = state.world
    val bossPresentation = signatureBossPresentation(world.bossHp, world.bossMaxHp)
    val quietWorld = world.focusNow.coerceAtLeast(0) == 0 &&
        world.raidParticipants.coerceAtLeast(0) == 0
    val lowHp = world.bossHp > 0 && world.bossMaxHp > 0 &&
        world.bossHp.toFloat() / world.bossMaxHp.toFloat() <= .25f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .90f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = .14f),
                            MaterialTheme.colorScheme.surface.copy(alpha = .02f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = .12f),
                        ),
                    ),
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "WORLD RAID",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        world.bossName,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(9.dp))
                    SignatureHpBar(world.bossHp, world.bossMaxHp)
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "${signatureComma(world.bossHp)} / ${signatureComma(world.bossMaxHp)} HP",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(154.dp)
                        .offset(x = 12.dp, y = (-4).dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(154.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = .24f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = .08f),
                                        Color.Transparent,
                                    ),
                                ),
                                CircleShape,
                            ),
                    )
                    BossArtwork(
                        modifier = Modifier.size(148.dp),
                        presentation = bossPresentation,
                        frameless = true,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .62f),
            ) {
                Text(
                    when {
                        lowHp -> "あと少し。世界中の集中が押し込んでいる"
                        bossPresentation == BossPresentation.Defeated -> "このRaidは討伐済み。次の旅を待っています"
                        else -> "${state.selectedMinutes}分集中すると、この世界へ一撃が届く。"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            if (quietWorld) {
                Text(
                    if (world.totalFocusMinutes > 0L) {
                        "これまで${signatureCommaLong(world.totalFocusMinutes)}分の集中が届いた"
                    } else {
                        "あなたの${state.selectedMinutes}分から、このRaidが動き出す"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "いま ${signatureComma(world.focusNow.coerceAtLeast(0))}人が集中中",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${signatureComma(world.raidParticipants.coerceAtLeast(0))}人が参加",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

        }
    }
}

@Composable
private fun SignatureRecentEchoes(echoes: List<RaidEcho>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .20f),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SignatureRaidLight(Modifier.size(6.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    "最近届いた集中",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .80f),
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(3.dp))
            echoes.take(3).forEach { echo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 19.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SignatureRaidLight(Modifier.size(5.dp))
                    Spacer(Modifier.size(7.dp))
                    Text(
                        echo.relativeLabel,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "+${echo.focusMinutes.coerceAtLeast(0)}分",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = .84f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SignatureCompanionRow(state: FocusUiState) {
    val growth = CompanionGrowth.from(state.totalFocusMinutes)
    val identity = LocalCompanionIdentity.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .52f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompanionArtwork(
                modifier = Modifier.size(82.dp),
                stage = growth.stage,
                mood = CompanionMood.Idle,
            )
            Spacer(Modifier.size(9.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${identity.label}  ·  ${growth.stage.label}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    companionStatusCopy(identity.label, growth),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                growth.nextStageLabel?.let { nextStage ->
                    Text(
                        "あと${growth.remainingMinutes}分で$nextStage",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SignatureDurationSelector(
    selected: Int,
    onSelect: (Int) -> Unit,
    onCustom: (() -> Unit)?,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                            fontSize = 12.sp,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 42.dp),
                )
            }
        }
        TextButton(
            onClick = { onCustom?.invoke() },
            enabled = onCustom != null,
            modifier = Modifier.align(Alignment.End),
        ) {
            Text("時間を細かく設定", fontSize = 11.sp)
        }
    }
}

@Composable
private fun SignatureExpeditionSelector(
    selected: Expedition,
    onSelect: (Expedition) -> Unit,
) {
    val access = LocalProAccessLevel.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Expedition.entries.forEach { expedition ->
            val locked = !FeatureAccess.canUse(expedition, access)
            FilterChip(
                selected = selected == expedition,
                onClick = { onSelect(expedition) },
                label = {
                    Text(
                        when (expedition) {
                            Expedition.TOWER -> "天空塔"
                            Expedition.ABYSS -> "深層"
                            Expedition.STAR_ROUTE -> if (locked) "🔒 星渡り" else "✦ 星渡り"
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 42.dp),
            )
        }
    }
}

@Composable
private fun SignatureWorldPresence(state: FocusUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .58f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("✦", color = MaterialTheme.colorScheme.tertiary, fontSize = 16.sp)
            Spacer(Modifier.size(8.dp))
            Text(
                "遠くでも集中が積み上がっています。あなたの${state.selectedMinutes}分も、その続きになる。",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SignatureFocusingScreen(
    state: FocusUiState,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinishEarly: () -> Unit,
) {
    val paused = state.phase == SessionPhase.PAUSED
    val stage = CompanionGrowth.from(state.totalFocusMinutes).stage
    val lastMinute = !paused && state.remainingSeconds in 1..60

    SignatureBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 46.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Focus Raid", fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (paused) "旅はここで止まっています" else "静かに進んでいます",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = if (paused) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                ) {
                    Text(
                        when {
                            paused -> "PAUSED"
                            state.expedition == Expedition.STAR_ROUTE -> "STAR ROUTE"
                            else -> "FOCUS"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(
                signatureClock(state.remainingSeconds),
                fontSize = 58.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp,
                modifier = Modifier.semantics {
                    contentDescription = "残り時間"
                    stateDescription = signatureClock(state.remainingSeconds)
                },
            )
            Text(
                if (lastMinute) "レイド地点の灯が見えてきた" else "${state.selectedMinutes}分の旅",
                fontSize = 12.sp,
                color = if (lastMinute) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (lastMinute) FontWeight.Bold else FontWeight.Normal,
            )

            Spacer(Modifier.height(22.dp))
            SignatureJourneyRail(
                progress = state.progress,
                stage = stage,
                expedition = state.expedition,
                paused = paused,
                lastMinute = lastMinute,
            )

            Spacer(Modifier.height(10.dp))
            SignatureRaidApproachLink(lastMinute = lastMinute, paused = paused)
            Spacer(Modifier.height(10.dp))
            SignatureFocusWorldCard(state)
            Spacer(Modifier.height(18.dp))

            if (paused) {
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 60.dp),
                    shape = RoundedCornerShape(28.dp),
                ) {
                    Text("▶  集中を再開", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = onPause,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp),
                    shape = RoundedCornerShape(26.dp),
                ) {
                    Text("Ⅱ  一時停止", fontWeight = FontWeight.SemiBold)
                }
            }
            TextButton(onClick = onFinishEarly) {
                Text("セッションを終了", fontSize = 12.sp)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SignatureRaidApproachLink(lastMinute: Boolean, paused: Boolean) {
    val alpha = when {
        paused -> .16f
        lastMinute -> .84f
        else -> .34f
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 38.dp)
                .width(2.dp)
                .height(24.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = alpha * .70f),
                            MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                            Color.Transparent,
                        ),
                    ),
                    CircleShape,
                ),
        )
        SignatureBrandMark(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 22.dp)
                .size(25.dp),
            alpha = if (paused) .035f else if (lastMinute) .16f else .065f,
        )
        if (lastMinute && !paused) {
            SignatureRaidLight(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 35.dp)
                    .size(6.dp),
            )
        }
    }
}

@Composable
private fun SignatureJourneyRail(
    progress: Float,
    stage: com.madowaku.focusraid.core.domain.CompanionStage,
    expedition: Expedition,
    paused: Boolean,
    lastMinute: Boolean,
) {
    val safeProgress = progress.coerceIn(0f, 1f)
    val currentIndex = signatureJourneyIndex(safeProgress)
    val labels = when (expedition) {
        Expedition.TOWER -> listOf("CAMP", "PATH", "RIDGE", "GATE", "RAID")
        Expedition.ABYSS -> listOf("CAMP", "DESCENT", "VEIN", "GATE", "RAID")
        Expedition.STAR_ROUTE -> listOf("DOCK", "DRIFT", "ORBIT", "GATE", "BEACON")
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .72f),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .semantics {
                    contentDescription = "集中の旅"
                    stateDescription = "${(safeProgress * 100).toInt()}パーセント進行"
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .offset(y = 43.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(safeProgress)
                    .padding(start = 12.dp)
                    .offset(y = 43.dp)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = if (paused) .28f else .54f),
                                MaterialTheme.colorScheme.tertiary.copy(alpha = if (paused) .34f else .72f),
                            ),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 7.dp)
                    .offset(y = 38.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                labels.indices.forEach { index ->
                    val active = index <= currentIndex
                    val finalGlow = index == labels.lastIndex && lastMinute
                    Box(
                        modifier = Modifier
                            .size(if (finalGlow) 17.dp else 13.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    finalGlow -> MaterialTheme.colorScheme.tertiary
                                    active -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                            ),
                    )
                }
            }

            val travelerX = 4.dp + (maxWidth - 76.dp) * safeProgress
            CompanionArtwork(
                modifier = Modifier
                    .offset(x = travelerX, y = 16.dp)
                    .size(44.dp),
                stage = stage,
                mood = if (paused) CompanionMood.Idle else CompanionMood.Focused,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                labels.forEachIndexed { index, label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = when (index) {
                            0 -> TextAlign.Start
                            labels.lastIndex -> TextAlign.End
                            else -> TextAlign.Center
                        },
                        fontSize = if (label.length > 6) 8.sp else 9.sp,
                        color = if (index <= currentIndex) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .62f)
                        },
                        fontWeight = if (index == currentIndex) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

@Composable
private fun SignatureFocusWorldCard(state: FocusUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .82f),
        ),
    ) {
        if (state.expedition == Expedition.STAR_ROUTE) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("✦", fontSize = 30.sp, color = MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.size(12.dp))
                Column {
                    Text("星渡り航路", fontWeight = FontWeight.Bold)
                    Text(
                        "完走すると、この集中が航路の進行へ加わります。",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return@Card
        }

        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(state.world.bossName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(7.dp))
                SignatureHpBar(state.world.bossHp, state.world.bossMaxHp)
                Spacer(Modifier.height(6.dp))
                Text(
                    "完走後に、この集中を世界へ送信",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(10.dp))
            BossArtwork(
                Modifier.size(50.dp),
                presentation = signatureBossPresentation(
                    state.world.bossHp,
                    state.world.bossMaxHp,
                ),
            )
        }
    }
}

@Composable
private fun SignatureHpBar(current: Int, max: Int) {
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
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary,
                            MaterialTheme.colorScheme.tertiary,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun SignatureBottomNavigation(
    selected: MainTab,
    onTabChange: (MainTab) -> Unit,
) {
    NavigationBar(
        modifier = Modifier.navigationBarsPadding(),
        containerColor = Color(0xE61B1430),
    ) {
        MainTab.entries.forEach { item ->
            NavigationBarItem(
                selected = selected == item,
                onClick = { onTabChange(item) },
                icon = {
                    Icon(
                        painter = painterResource(item.iconRes),
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp),
                    )
                },
                label = { Text(item.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    }
}

@Composable
internal fun SignatureBackdrop(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090713),
                        Color(0xFF151024),
                        Color(0xFF211431),
                    ),
                ),
            ),
    ) {
        content()
    }
}

@Composable
internal fun SignatureRaidLight(modifier: Modifier = Modifier.size(8.dp)) {
    Box(
        modifier = modifier
            .background(
                Brush.radialGradient(
                    listOf(
                        Color.White.copy(alpha = .96f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = .92f),
                        Color.Transparent,
                    ),
                ),
                CircleShape,
            ),
    )
}

@Composable
private fun SignatureBrandMark(
    modifier: Modifier = Modifier,
    alpha: Float,
) {
    val ring = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
    val strike = MaterialTheme.colorScheme.secondary.copy(alpha = alpha * .95f)
    Canvas(modifier) {
        val strokeWidth = size.minDimension * .11f
        val inset = strokeWidth / 2f
        drawArc(
            color = ring,
            startAngle = -52f,
            sweepAngle = 282f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(
            color = strike,
            start = Offset(size.width * .24f, size.height * .76f),
            end = Offset(size.width * .76f, size.height * .24f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

internal fun signatureBossPresentation(current: Int, max: Int): BossPresentation {
    if (current <= 0) return BossPresentation.Defeated
    if (max <= 0) return BossPresentation.Damaged
    return if (current.toFloat() / max.toFloat() > .5f) {
        BossPresentation.Normal
    } else {
        BossPresentation.Damaged
    }
}

private fun companionStatusCopy(name: String, growth: com.madowaku.focusraid.core.domain.CompanionGrowthStatus): String =
    when (growth.stage) {
        CompanionStage.EGG -> "${name}は出発を待っている"
        CompanionStage.MATURE -> "${name}は次の旅を待っている"
        else -> "${name}は一緒に進む準備ができている"
    }

internal fun signatureJourneyIndex(progress: Float): Int =
    (progress.coerceIn(0f, 1f) * 5f).toInt().coerceIn(0, 4)

internal fun signatureDepartureLabel(minutes: Int, expedition: Expedition): String = when (expedition) {
    Expedition.STAR_ROUTE -> "✦  ${minutes}分、星渡りへ"
    else -> "${minutes}分、出発する"
}

private fun signatureClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(safe / 60, safe % 60)
}

private fun signatureComma(value: Int): String = "%,d".format(value)

private fun signatureCommaLong(value: Long): String = "%,d".format(value.coerceAtLeast(0L))
