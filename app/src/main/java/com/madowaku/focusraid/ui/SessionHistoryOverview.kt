package com.madowaku.focusraid.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.BuildConfig
import com.madowaku.focusraid.billing.AccessLevel
import com.madowaku.focusraid.billing.DetailedHistoryStats
import com.madowaku.focusraid.billing.FeatureAccess
import com.madowaku.focusraid.billing.HistoryAccessPolicy
import com.madowaku.focusraid.billing.ProFeature
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionHistoryEntry
import com.madowaku.focusraid.core.model.SessionOutcome
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun SessionHistoryOverview(
    state: FocusUiState,
    privacyPolicyUrl: String = BuildConfig.PRIVACY_POLICY_URL,
) {
    val accessLevel = LocalProAccessLevel.current
    val openProPaywall = LocalOpenProPaywall.current
    val uriHandler = LocalUriHandler.current
    val nowEpochMillis = System.currentTimeMillis()
    val allHistory = state.sessionHistory
    val visibleHistory = remember(allHistory, accessLevel, nowEpochMillis) {
        HistoryAccessPolicy.visibleEntries(
            entries = allHistory,
            accessLevel = accessLevel,
            nowEpochMillis = nowEpochMillis,
        )
    }
    val hasFullHistory = FeatureAccess.canUse(ProFeature.FULL_HISTORY, accessLevel)
    val hasDetailedStats = FeatureAccess.canUse(ProFeature.DETAILED_STATS, accessLevel)
    val hiddenHistoryCount = (allHistory.size - visibleHistory.size).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("ADVENTURE LOG", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "集中した時間を、消えない冒険記録として残します",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            AccessBadge(accessLevel)
        }

        Spacer(Modifier.height(16.dp))
        BasicHistoryCard(
            state = state,
            visibleCount = visibleHistory.size,
            hasFullHistory = hasFullHistory,
        )

        Spacer(Modifier.height(14.dp))
        if (hasDetailedStats) {
            DetailedStatsCard(HistoryAccessPolicy.detailedStats(allHistory))
        } else {
            ProLockedCard(
                title = "詳細統計",
                body = "平均集中時間、完走率、最長セッション、遠征別の集中時間をProで確認できます。",
                buttonLabel = "Proで詳細統計を解放",
                onClick = openProPaywall,
            )
        }

        Spacer(Modifier.height(18.dp))
        Text(
            if (hasFullHistory) "全期間の集中" else "直近7日の集中",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (hasFullHistory) {
                "保存されているすべてのセッションを表示中"
            } else {
                "記録自体は7日より前も端末内に保存されています"
            },
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))

        if (visibleHistory.isEmpty()) {
            EmptyHistoryCard()
        } else {
            var previousDay: LocalDate? = null
            visibleHistory.forEach { entry ->
                val day = entry.completedAtEpochMillis.toLocalDate()
                if (day != previousDay) {
                    if (previousDay != null) Spacer(Modifier.height(10.dp))
                    Text(
                        day.dayLabel(),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    previousDay = day
                }
                SessionHistoryCard(entry)
                Spacer(Modifier.height(8.dp))
            }
        }

        if (!hasFullHistory) {
            Spacer(Modifier.height(10.dp))
            ProLockedCard(
                title = "全期間の冒険記録",
                body = if (hiddenHistoryCount > 0) {
                    "さらに${hiddenHistoryCount}件の過去ログを保存済みです。Proにすると、その場ですべて見えるようになります。"
                } else {
                    "7日を超えた記録も削除せず保存します。Proなら全期間をいつでも振り返れます。"
                },
                buttonLabel = "全期間の履歴を見る",
                onClick = openProPaywall,
            )
        }

        if (privacyPolicyUrl.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = { uriHandler.openUri(privacyPolicyUrl) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("プライバシーポリシー")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AccessBadge(accessLevel: AccessLevel) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (accessLevel == AccessLevel.PRO) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            if (accessLevel == AccessLevel.PRO) "PRO" else "FREE · 7 DAYS",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun BasicHistoryCard(
    state: FocusUiState,
    visibleCount: Int,
    hasFullHistory: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .90f),
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("累計集中", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(3.dp))
            Text(
                "${state.totalFocusMinutes / 60}h ${state.totalFocusMinutes % 60}m",
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryMetric(
                    label = "今日",
                    value = "${state.todayFocusMinutes}m",
                    modifier = Modifier.weight(1f),
                )
                HistoryMetric(
                    label = "連続",
                    value = "🔥${state.streakDays}",
                    modifier = Modifier.weight(1f),
                )
                HistoryMetric(
                    label = if (hasFullHistory) "全記録" else "7日",
                    value = visibleCount.toString(),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DetailedStatsCard(stats: DetailedHistoryStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = .75f),
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("PRO DETAIL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${stats.sessionCount} sessions",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .72f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HistoryMetric(
                    label = "完走率",
                    value = "${stats.completionRatePercent}%",
                    modifier = Modifier.weight(1f),
                )
                HistoryMetric(
                    label = "平均",
                    value = "%.1fm".format(stats.averageMinutes),
                    modifier = Modifier.weight(1f),
                )
                HistoryMetric(
                    label = "最長",
                    value = "${stats.longestMinutes}m",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "完走 ${stats.completedCount} · 撤退 ${stats.abortedCount}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .76f),
            )
            Text(
                "天空塔 ${stats.towerMinutes}分 · 深層迷宮 ${stats.abyssMinutes}分",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .76f),
            )
            Text(
                "星渡り航路 ${stats.starRouteMinutes}分",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = .76f),
            )
        }
    }
}

@Composable
private fun ProLockedCard(
    title: String,
    body: String,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .86f),
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("🔒  $title", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(buttonLabel)
            }
        }
    }
}

@Composable
private fun HistoryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f),
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyHistoryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .84f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("▤", fontSize = 44.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text("まだ冒険記録はありません", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "次の集中が、最初の1行になります。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SessionHistoryCard(entry: SessionHistoryEntry) {
    val completed = entry.outcome == SessionOutcome.COMPLETED
    val expeditionLabel = when (entry.expedition) {
        Expedition.TOWER -> "天空塔"
        Expedition.ABYSS -> "深層迷宮"
        Expedition.STAR_ROUTE -> "星渡り航路"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .88f),
        ),
    ) {
        Column(Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (completed) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                    ) {
                        Text(
                            if (completed) "完走" else "撤退",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(expeditionLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    entry.completedAtEpochMillis.timeLabel(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "${entry.creditedMinutes}分 集中",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (!completed && entry.creditedMinutes < entry.plannedMinutes) {
                        Text(
                            "予定 ${entry.plannedMinutes}分",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    "+${entry.damage} DAMAGE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (entry.discovery != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "🎁 ${entry.rarity ?: "DROP"} · ${entry.discovery}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .toLocalDate()

private fun LocalDate.dayLabel(): String {
    val today = LocalDate.now()
    return when (this) {
        today -> "今日"
        today.minusDays(1) -> "昨日"
        else -> format(DateTimeFormatter.ofPattern("M月d日"))
    }
}

private fun Long.timeLabel(): String = Instant.ofEpochMilli(this)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("H:mm"))
