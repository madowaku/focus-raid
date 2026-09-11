package com.madowaku.focusraid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.madowaku.focusraid.data.ContributionStatus
import com.madowaku.focusraid.data.WorldContribution

internal val LocalRetryWorld = staticCompositionLocalOf<() -> Unit> { {} }

internal fun contributionMessage(row: WorldContribution): String = when (row.state) {
    ContributionStatus.PENDING -> "${row.creditedMinutes}分を端末に保存しました。世界への送信待ちです。"
    ContributionStatus.RETRYING -> "世界へ届けています。受け付けの確認を待っています。"
    ContributionStatus.OFFLINE -> "世界への反映を確認できていません。通信が戻ると重複しないよう確認・再送します。個人の記録は保存済みです。"
    ContributionStatus.AUTH_REQUIRED -> "接続の認証を確認できません。記録を保管して再試行します。"
    ContributionStatus.ACCEPTED -> "あなたの${row.creditedMinutes}分が世界の集中に加わりました。ボスへ${row.appliedDamage}ダメージ！"
    ContributionStatus.ALREADY_COUNTED -> "この${row.creditedMinutes}分は世界に反映済みです。重複せず、1回分として記録されています。"
    ContributionStatus.STALE -> "参加先のレイドは終了しました。次のボスには加算せず、個人の集中と報酬を残しました。"
    ContributionStatus.REJECTED -> "この記録は世界に加算できませんでした。個人の集中と報酬は保存されています。"
    ContributionStatus.UNAVAILABLE -> "この旧バージョンの記録は個人の集中として保存されています。次の集中から世界へ参加できます。"
}

@Composable
internal fun WorldContributionCard(state: FocusUiState, result: Boolean = false) {
    val row = if (result) state.contributions.firstOrNull { it.sessionId == state.resultSessionId }
        else state.contributions.firstOrNull()
    val pending = state.contributions.count { it.state in setOf(ContributionStatus.PENDING,
        ContributionStatus.RETRYING, ContributionStatus.OFFLINE, ContributionStatus.AUTH_REQUIRED) }
    val retry = LocalRetryWorld.current
    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.padding(14.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("世界に届ける集中", style = MaterialTheme.typography.titleMedium)
            Text(row?.let(::contributionMessage) ?: if (state.world.generation == null)
                "共有レイドへの接続が必要です。個人の集中はいつでも記録できます。"
                else "完走した集中を、世界のみんなの力に。", style = MaterialTheme.typography.bodyMedium)
            if (row != null && row.state in setOf(ContributionStatus.ACCEPTED, ContributionStatus.ALREADY_COUNTED) &&
                row.generation != null && state.world.generation != null && row.generation != state.world.generation) {
                Text("この記録は以前のレイドへの貢献です。", style = MaterialTheme.typography.bodySmall)
            }
            if (pending > 0) Text("端末に保管中：${pending}件", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = retry) { Text(if (pending > 0) "接続を確認して再送" else "世界の状況を更新") }
        }
    }
}
