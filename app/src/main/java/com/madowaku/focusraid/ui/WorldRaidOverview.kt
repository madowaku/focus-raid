package com.madowaku.focusraid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.billing.FeatureAccess
import com.madowaku.focusraid.core.domain.StarRoute
import com.madowaku.focusraid.core.model.Expedition

@Composable
internal fun WorldRaidOverview(state: FocusUiState) {
    val accessLevel = LocalProAccessLevel.current
    val openProPaywall = LocalOpenProPaywall.current
    val starRouteUnlocked = FeatureAccess.canUse(Expedition.STAR_ROUTE, accessLevel)
    val starRouteMinutes = state.sessionHistory
        .asSequence()
        .filter { it.expedition == Expedition.STAR_ROUTE }
        .sumOf { it.creditedMinutes }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Text("WORLD RAID", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(
            "集中した時間が、3つの遠征を少しずつ前へ進めます",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        WorldBossOverviewCard(state)

        Spacer(Modifier.height(14.dp))
        Text("遠征マップ", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ExpeditionLocationCard(
                eyebrow = "SKY",
                title = "天空塔",
                location = "${state.world.towerFloor}F",
                glyph = "△",
                modifier = Modifier.weight(1f),
            )
            ExpeditionLocationCard(
                eyebrow = "ABYSS",
                title = "深層迷宮",
                location = "${state.world.abyssDepth}m",
                glyph = "▽",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(14.dp))
        if (starRouteUnlocked) {
            StarRouteOverviewCard(
                state = state,
                starRouteMinutes = starRouteMinutes,
            )
        } else {
            LockedStarRouteOverviewCard(onUnlock = openProPaywall)
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WorldBossOverviewCard(state: FocusUiState) {
    val world = state.world
    val hpFraction = if (world.bossMaxHp <= 0) {
        0f
    } else {
        (world.bossHp.toFloat() / world.bossMaxHp.toFloat()).coerceIn(0f, 1f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .90f),
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "CURRENT WORLD RAID",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(world.bossName, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        "${world.raidParticipants}人",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(hpFraction)
                        .height(9.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${world.bossHp} / ${world.bossMaxHp} HP",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "ARMORY ${world.armoryReady}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExpeditionLocationCard(
    eyebrow: String,
    title: String,
    location: String,
    glyph: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.height(132.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .86f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    eyebrow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(glyph, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(location, fontSize = 26.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun StarRouteOverviewCard(
    state: FocusUiState,
    starRouteMinutes: Int,
) {
    val reached = StarRoute.reachedCheckpoint(state.totalFocusMinutes)
    val target = StarRoute.targetCheckpoint(state.totalFocusMinutes)
    val remaining = StarRoute.minutesUntilTarget(state.totalFocusMinutes)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .86f),
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "✦  PRO EXPEDITION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .72f),
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "星渡り航路",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = .16f),
                ) {
                    Text(
                        "PRO",
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StarRouteMetric(
                    label = "到達",
                    value = if (reached == 0) "未到達" else "第${reached}星標",
                    modifier = Modifier.weight(1f),
                )
                StarRouteMetric(
                    label = "次の星標",
                    value = "あと${remaining}分",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "次の目的地 · 第${target}星標",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                "星渡りで集中した時間 ${starRouteMinutes}分",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .76f),
            )
        }
    }
}

@Composable
private fun StarRouteMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .34f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Text(
                label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .68f),
            )
        }
    }
}

@Composable
private fun LockedStarRouteOverviewCard(onUnlock: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .86f),
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "🔒  PRO EXPEDITION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(3.dp))
            Text("星渡り航路", fontSize = 20.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(6.dp))
            Text(
                "集中で5つの星標を灯し、まだ見ぬ航路へ進むPro専用遠征。解放後もこれまでの累計集中は失われません。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("星渡り航路を解放")
            }
        }
    }
}
