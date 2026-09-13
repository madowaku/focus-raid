package com.madowaku.focusraid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.core.domain.CompanionIdentity
import com.madowaku.focusraid.core.domain.CompanionStage
import com.madowaku.focusraid.core.model.SessionPhase
import kotlin.math.max
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay

internal const val CURRENT_FIRST_RUN_VERSION = 1

internal fun shouldShowFirstRun(state: FocusUiState): Boolean =
    state.initialized &&
        state.phase == SessionPhase.READY &&
        state.firstRunVersion < CURRENT_FIRST_RUN_VERSION &&
        state.totalFocusMinutes == 0

internal fun firstRunActOneHp(currentHp: Int, maxHp: Int): Int {
    val current = currentHp.coerceAtLeast(0)
    if (current == 0) return 0
    return (current - max(1, maxHp.coerceAtLeast(1) / 40)).coerceAtLeast(0)
}

internal fun firstRunActTwoHp(actOneHp: Int, maxHp: Int, lightCount: Int): Int {
    val step = max(1, maxHp.coerceAtLeast(1) / 100)
    return (actOneHp - step * lightCount.coerceIn(0, 3)).coerceAtLeast(0)
}

@Composable
internal fun FirstRunMoment(
    state: FocusUiState,
    initialAct: Int = 0,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    onSelfStrike: () -> Unit = {},
    onOtherLight: (Int) -> Unit = {},
) {
    var actIndex by remember(initialAct) { mutableIntStateOf(initialAct.coerceIn(0, 2)) }
    var otherLightCount by remember(actIndex) { mutableIntStateOf(0) }
    var impactTrigger by remember { mutableIntStateOf(0) }
    val flight = remember(actIndex) { RaidFocusArrival() }

    LaunchedEffect(actIndex) {
        otherLightCount = 0
        when (actIndex) {
            0 -> {
                onSelfStrike()
                impactTrigger += 1
            }

            1 -> repeat(3) { index ->
                if (index == 0 && (coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) > 0f) {
                    delay(250L)
                }
                flight.deliver(index) {
                    otherLightCount = index + 1
                    onOtherLight(index)
                }
            }

            else -> Unit
        }
    }

    BackHandler(enabled = !state.saving) {
        onSkip()
    }

    Box(Modifier.fillMaxSize()) {
        FirstRunScene(
            state = state,
            actIndex = actIndex,
            otherLightCount = otherLightCount,
            flight = flight,
            onPrimary = {
                if (!state.saving) {
                    if (actIndex < 2) actIndex += 1 else onComplete()
                }
            },
            onSkip = onSkip,
            saving = state.saving,
        )
        if (actIndex == 0) {
            KenneyRaidImpactOverlay(
                trigger = impactTrigger,
                emphasized = actIndex == 0,
            )
        }
    }
}

@Composable
private fun FirstRunScene(
    state: FocusUiState,
    actIndex: Int,
    otherLightCount: Int,
    flight: RaidFocusArrival,
    onPrimary: () -> Unit,
    onSkip: () -> Unit,
    saving: Boolean,
) {
    val title = firstRunTitle(actIndex)
    val subtitle = firstRunSubtitle(actIndex)
    val actLabel = "${actIndex + 1}/3"

    SignatureBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .semantics {
                    contentDescription = "FIRST RUN $actLabel。$title。$subtitle"
                    stateDescription = actLabel
                },
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "FIRST RUN",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    actLabel,
                    modifier = Modifier.testTag("first_run_progress"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
            FirstRunWorldScene(
                state = state,
                actIndex = actIndex,
                otherLightCount = otherLightCount,
                flight = flight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(318.dp),
            )

            Spacer(Modifier.height(12.dp))
            Text(
                title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = if (actIndex == 1) 16.dp else 0.dp)
                    .semantics { heading() },
                textAlign = TextAlign.Center,
                fontSize = if (actIndex == 1) 27.sp else 28.sp,
                lineHeight = if (actIndex == 1) 32.sp else 34.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onPrimary,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp)
                    .testTag("first_run_primary"),
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
                Text(
                    if (actIndex == 2) "最初の集中へ" else "次へ",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                )
            }
            if (actIndex < 2) {
                TextButton(
                    onClick = onSkip,
                    enabled = !saving,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .testTag("first_run_skip"),
                ) {
                    Text("あとで見る", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun FirstRunWorldScene(
    state: FocusUiState,
    actIndex: Int,
    otherLightCount: Int,
    flight: RaidFocusArrival,
    modifier: Modifier = Modifier,
) {
    val world = state.world
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xCC130D26),
                        Color(0xB5221637),
                        Color(0x992D1B43),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        FirstRunStarField(Modifier.fillMaxSize())
        if (actIndex == 2) {
            FirstRunCompanionScene()
        } else {
            val actOneHp = firstRunActOneHp(world.bossHp, world.bossMaxHp)
            val displayedHp = firstRunActTwoHp(actOneHp, world.bossMaxHp, otherLightCount)
            FirstRunBossScene(
                bossName = world.bossName,
                currentHp = displayedHp,
                maxHp = world.bossMaxHp,
                actIndex = actIndex,
                otherLightCount = otherLightCount,
                flight = flight,
            )
        }
    }
}

@Composable
private fun FirstRunStarField(modifier: Modifier) {
    Canvas(modifier) {
        val points = listOf(
            .12f to .18f,
            .27f to .72f,
            .71f to .16f,
            .86f to .42f,
            .76f to .84f,
            .42f to .88f,
        )
        points.forEachIndexed { index, (x, y) ->
            drawCircle(
                color = Color.White.copy(alpha = if (index % 2 == 0) .20f else .11f),
                radius = if (index % 2 == 0) 2.4f else 1.5f,
                center = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y),
            )
        }
    }
}

@Composable
private fun FirstRunBossScene(
    bossName: String,
    currentHp: Int,
    maxHp: Int,
    actIndex: Int,
    otherLightCount: Int,
    flight: RaidFocusArrival,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp),
            contentAlignment = Alignment.Center,
        ) {
            SignatureBrandMark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 2.dp)
                    .size(48.dp),
                alpha = if (actIndex == 0) .28f else .16f,
            )
            Surface(
                modifier = Modifier.size(206.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = .13f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    BossArtwork(
                        modifier = Modifier.size(178.dp),
                        presentation = signatureBossPresentation(currentHp, maxHp),
                    )
                    if (actIndex == 0) {
                        FirstRunStrikeMark(Modifier.size(122.dp))
                    }
                }
            }
            if (actIndex == 1) {
                RaidFocusArrivalLayer(flight, Modifier.fillMaxSize())
            }
        }
        if (actIndex == 1 && otherLightCount > 0) {
            val visibleLightCount = otherLightCount.coerceAtMost(3)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(visibleLightCount) { index ->
                    SignatureRaidLight(
                        modifier = Modifier.size(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (index < visibleLightCount - 1) Spacer(Modifier.size(5.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Text(
            bossName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        FirstRunHpBar(
            current = currentHp,
            max = maxHp,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (actIndex == 0) "あなたの一撃が届いた" else "ひとつの灯は、誰かの集中。",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FirstRunHpBar(current: Int, max: Int, modifier: Modifier = Modifier) {
    val progress = if (max <= 0) 0f else (current.toFloat() / max.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(9.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .80f))
            .semantics {
                contentDescription = "ボスHP"
                stateDescription = "$current / $max"
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(9.dp)
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
private fun FirstRunStrikeMark(modifier: Modifier) {
    val tertiary = MaterialTheme.colorScheme.tertiary
    Canvas(modifier) {
        drawLine(
            color = tertiary.copy(alpha = .86f),
            start = androidx.compose.ui.geometry.Offset(size.width * .20f, size.height * .78f),
            end = androidx.compose.ui.geometry.Offset(size.width * .80f, size.height * .22f),
            strokeWidth = size.minDimension * .08f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = Color.White.copy(alpha = .92f),
            radius = size.minDimension * .06f,
            center = androidx.compose.ui.geometry.Offset(size.width * .50f, size.height * .50f),
        )
    }
}

@Composable
private fun FirstRunCompanionScene() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(238.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(228.dp)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = .22f),
                                MaterialTheme.colorScheme.primary.copy(alpha = .09f),
                                Color.Transparent,
                            ),
                        ),
                        CircleShape,
                    ),
            )
            SignatureBrandMark(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 6.dp)
                    .size(42.dp),
                alpha = .08f,
            )
            CompanionArtwork(
                modifier = Modifier.size(176.dp),
                stage = CompanionStage.EGG,
                identity = CompanionIdentity.RAG,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "ラグの卵",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "ここから一緒に育っていく",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun firstRunTitle(index: Int): String = when (index) {
    0 -> "25分が、一撃になる。"
    1 -> "世界中の集中が、\n同じボスへ。"
    else -> "相棒も、一緒に育つ。"
}

private fun firstRunSubtitle(index: Int): String = when (index) {
    0 -> "集中を完走すると、その時間がRaidへ届きます。"
    1 -> "あなたの25分も、誰かの25分も。\nみんなで、このレイドボスを倒します。"
    else -> "集中した時間は、旅と成長に残ります。"
}
