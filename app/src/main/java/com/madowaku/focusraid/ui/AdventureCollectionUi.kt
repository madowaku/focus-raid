package com.madowaku.focusraid.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madowaku.focusraid.core.domain.*
import com.madowaku.focusraid.core.model.SessionHistoryEntry

@Composable
internal fun CompanionRoster(state: FocusUiState, onSelect: (CompanionIdentity) -> Unit) {
    val stage = CompanionGrowth.from(state.totalFocusMinutes).stage
    val unlockedCount = CompanionIdentity.entries.count { companion ->
        AdventureCollection.canSelect(companion, state.totalFocusMinutes)
    }

    Spacer(Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("相棒を選ぶ", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        Text(
            "$unlockedCount / ${CompanionIdentity.entries.size} 解放",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    CompanionIdentity.entries.forEach { companion ->
        val unlocked = AdventureCollection.canSelect(companion, state.totalFocusMinutes)
        val remainingMinutes = (companion.requiredMinutes - state.totalFocusMinutes).coerceAtLeast(0)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .78f),
            ),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (unlocked) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = .22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .72f)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (unlocked) {
                        CompanionArtwork(
                            modifier = Modifier.size(52.dp),
                            stage = stage,
                            identity = companion,
                        )
                    } else {
                        Text("🔒", fontSize = MaterialTheme.typography.titleMedium.fontSize)
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                ) {
                    Text(
                        if (unlocked) companion.label else "???",
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (unlocked) {
                            if (state.companion == companion) "一緒に冒険中" else "解放済み"
                        } else {
                            "あと${remainingMinutes}分で解放"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(
                    onClick = { onSelect(companion) },
                    enabled = unlocked && !state.saving && state.companion != companion,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                ) {
                    Text(
                        when {
                            state.companion == companion -> "選択中"
                            unlocked -> "相棒にする"
                            else -> "ロック中"
                        },
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
internal fun InventoryCard(entries: List<SessionHistoryEntry>) {
    val owned = ItemCatalog.owned(entries)
    Spacer(Modifier.height(16.dp))
    Text("持ちもの", style = MaterialTheme.typography.titleMedium)
    Text("集中で見つけた記念品", style = MaterialTheme.typography.bodySmall)
    if (owned.isEmpty()) {
        Text("まだありません。集中すると見つかります。", modifier = Modifier.padding(vertical = 10.dp), style = MaterialTheme.typography.bodySmall)
    }
    owned.forEach { ownedItem ->
        ListItem(
            headlineContent = { Text(ownedItem.item.name) },
            supportingContent = { Text(ownedItem.item.rarity.name) },
            leadingContent = { ItemArtwork(ownedItem.item, Modifier.size(64.dp)) },
            trailingContent = { Text("×${ownedItem.count}") },
        )
    }
}

@Composable
internal fun PersonalRaidCard(state: FocusUiState, boss: RaidBossIdentity) {
    val progress = AdventureCollection.personalBossProgress(boss, state.sessionHistory)
    val expedition = if (boss == RaidBossIdentity.MORD) "深層迷宮" else "天空塔"
    val artworkIdentity = if (boss == RaidBossIdentity.MORD) BossIdentity.MORD else BossIdentity.ZEPHYR
    Card(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("${expedition}の個人レイド", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                BossArtwork(Modifier.size(112.dp), artworkIdentity,
                    if (progress.defeated) BossPresentation.Defeated else if (progress.creditedMinutes > 0) BossPresentation.Damaged else BossPresentation.Normal)
                Column(Modifier.weight(1f)) {
                    Text(boss.label, style = MaterialTheme.typography.titleMedium)
                    Text(if (progress.defeated) "討伐達成！" else "残り ${progress.remainingHp} / ${progress.targetMinutes} HP")
                    LinearProgressIndicator(progress = { progress.progress }, modifier = Modifier.fillMaxWidth())
                }
            }
            Text("${expedition}での集中1分 = 1ダメージ。端末内の挑戦で、世界の共有HPとは別に記録します。", style = MaterialTheme.typography.bodySmall)
            Text("ホームで${expedition}を選んで集中すると進みます。", style = MaterialTheme.typography.bodySmall)
        }
    }
}
