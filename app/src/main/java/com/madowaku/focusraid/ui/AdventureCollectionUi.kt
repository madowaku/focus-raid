package com.madowaku.focusraid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.madowaku.focusraid.core.domain.*
import com.madowaku.focusraid.core.model.SessionHistoryEntry

@Composable
internal fun CompanionRoster(state: FocusUiState, onSelect: (CompanionIdentity) -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text("旅の仲間", style = MaterialTheme.typography.titleMedium)
    Text("成長は全員で共有。相棒を替えても集中時間は減りません。", style = MaterialTheme.typography.bodySmall)
    CompanionIdentity.entries.forEach { companion ->
        val unlocked = AdventureCollection.canSelect(companion, state.totalFocusMinutes)
        Card(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                CompanionArtwork(Modifier.size(64.dp), CompanionGrowth.from(state.totalFocusMinutes).stage, identity = companion)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(companion.label, fontWeight = FontWeight.Bold)
                    Text(companion.description, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onSelect(companion) }, enabled = unlocked && !state.saving && state.companion != companion) {
                        Text(when {
                            state.companion == companion -> "一緒に冒険中"
                            unlocked -> "相棒にする"
                            else -> "累計${companion.requiredMinutes}分で出会う"
                        })
                    }
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
    Text("集中で見つけた冒険の記念品。Freeでも獲得した持ちものはすべて残ります。", style = MaterialTheme.typography.bodySmall)
    if (owned.isEmpty()) {
        Text("まだ持ちものはありません。集中が累計25分進むと、遠征先の発見が記録されます。", modifier = Modifier.padding(vertical = 12.dp))
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
