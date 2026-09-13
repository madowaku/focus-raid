package com.madowaku.focusraid

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.billing.AccessLevel
import com.madowaku.focusraid.core.domain.CompanionIdentity
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.WorldSnapshot
import com.madowaku.focusraid.data.RaidEcho
import com.madowaku.focusraid.ui.FocusRaidSignatureContent
import com.madowaku.focusraid.ui.FirstRunMoment
import com.madowaku.focusraid.ui.FocusUiState
import com.madowaku.focusraid.ui.LocalCompanionIdentity
import com.madowaku.focusraid.ui.LocalProAccessLevel
import com.madowaku.focusraid.ui.ReturnRaidScenario
import com.madowaku.focusraid.ui.ReturnRaidSequence
import com.madowaku.focusraid.ui.SignatureBackdrop

@Composable
internal fun SignatureVisualQaScreen(phase: String) {
    when (phase) {
        "SIGNATURE_LAUNCHER" -> SignatureLauncherQa()

        "FIRST_RUN_1", "FIRST_RUN_2", "FIRST_RUN_3" -> FirstRunMoment(
            state = firstRunVisualState(),
            initialAct = phase.removePrefix("FIRST_RUN_").toInt() - 1,
            onSkip = {},
            onComplete = {},
        )

        "FIRST_RUN_HOME" -> {
            val state = firstRunVisualState().copy(firstRunVersion = 1)
            CompositionLocalProvider(
                LocalCompanionIdentity provides state.companion,
                LocalProAccessLevel provides AccessLevel.FREE,
            ) {
                FocusRaidSignatureContent(
                    state = state,
                    tab = com.madowaku.focusraid.ui.MainTab.HOME,
                    onTabChange = {},
                    onSelectCompanion = {},
                    onSelectMinutes = {},
                    onSelectExpedition = {},
                    onTimerClick = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onFinishEarly = {},
                    onAgain = {},
                    onDone = {},
                )
            }
        }

        "SIGNATURE_RETURN_ECHOES" -> {
            ReturnRaidSequence(
                scenario = ReturnRaidScenario.demo(
                    bossName = "環焔竜ヴォルガ",
                    bossHp = 181,
                    bossMaxHp = 250,
                    playerDamage = 25,
                    creditedMinutes = 25,
                ),
                onFootprints = {},
                onAgain = {},
                onDone = {},
            )
        }

        "SIGNATURE_RETURN_DEFEATED" -> {
            ReturnRaidSequence(
                scenario = ReturnRaidScenario.fromEchoes(
                    bossName = "環焔竜ヴォルガ",
                    bossHp = 40,
                    bossMaxHp = 250,
                    playerDamage = 60,
                    creditedMinutes = 25,
                    echoes = emptyList(),
                    chainCountBefore = 0,
                    chainMinutesBefore = 0,
                ),
                onFootprints = {},
                onAgain = {},
                onDone = {},
            )
        }

        else -> {
            val state = signatureVisualState(phase)
            CompositionLocalProvider(
                LocalCompanionIdentity provides state.companion,
                LocalProAccessLevel provides AccessLevel.FREE,
            ) {
                FocusRaidSignatureContent(
                    state = state,
                    tab = com.madowaku.focusraid.ui.MainTab.HOME,
                    recentEchoes = if (phase == "SIGNATURE_HOME_ECHOES") {
                        listOf(
                            RaidEcho("12分前", 25, 25),
                            RaidEcho("51分前", 50, 50),
                            RaidEcho("3時間前", 25, 25),
                        )
                    } else {
                        emptyList()
                    },
                    onTabChange = {},
                    onSelectCompanion = {},
                    onSelectMinutes = {},
                    onSelectExpedition = {},
                    onTimerClick = {},
                    onStart = {},
                    onPause = {},
                    onResume = {},
                    onFinishEarly = {},
                    onAgain = {},
                    onDone = {},
                )
            }
        }
    }
}

private fun firstRunVisualState(): FocusUiState = FocusUiState(
    initialized = true,
    totalFocusMinutes = 0,
    world = WorldSnapshot(
        bossName = "環焔竜ヴォルガ",
        bossHp = 428_192,
        bossMaxHp = 650_000,
    ),
)

@Composable
private fun SignatureLauncherQa() {
    SignatureBackdrop {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("FOCUS RAID", fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text("launcher emblem", fontSize = 12.sp, color = Color(0xFFB8A9D8))
            Spacer(Modifier.size(22.dp))
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(Color(0xFF2A1644), RoundedCornerShape(42.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "Focus Raid adaptive launcher foreground",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.size(24.dp))
            Text("Focus Ring  ·  Raid Strike  ·  Meteor Spark", fontSize = 12.sp)
            Spacer(Modifier.size(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                LauncherSwatch(R.drawable.ic_launcher_foreground, "adaptive")
                LauncherSwatch(R.drawable.ic_launcher_monochrome, "monochrome")
            }
        }
    }
}

@Composable
private fun LauncherSwatch(resourceId: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .background(Color(0xFF2A1644), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(resourceId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(58.dp),
            )
        }
        Spacer(Modifier.size(5.dp))
        Text(label, fontSize = 10.sp, color = Color(0xFFB8A9D8))
    }
}

private fun signatureVisualState(phase: String): FocusUiState {
    val baseWorld = WorldSnapshot(
        totalFocusMinutes = if (phase == "SIGNATURE_HOME_ECHOES") 420 else 0,
        focusNow = 0,
        raidParticipants = 0,
        bossHp = when (phase) {
            "SIGNATURE_HOME_DAMAGED" -> 125_000
            "SIGNATURE_HOME_DEFEATED" -> 0
            else -> 428_192
        },
        bossMaxHp = 650_000,
    )
    return when (phase) {
        "SIGNATURE_FOCUS_25" -> FocusUiState(
            phase = SessionPhase.RUNNING,
            selectedMinutes = 25,
            durationSeconds = 25 * 60,
            remainingSeconds = 25 * 60,
            totalFocusMinutes = 0,
            world = baseWorld,
        )

        "SIGNATURE_FOCUS_00_59" -> FocusUiState(
            phase = SessionPhase.RUNNING,
            selectedMinutes = 25,
            durationSeconds = 25 * 60,
            remainingSeconds = 59,
            totalFocusMinutes = 0,
            world = baseWorld,
        )

        "SIGNATURE_HOME_EGG" -> FocusUiState(
            totalFocusMinutes = 0,
            world = baseWorld,
        )

        else -> FocusUiState(
            companion = CompanionIdentity.RAG,
            totalFocusMinutes = 0,
            world = baseWorld,
        )
    }
}
