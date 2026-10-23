package com.madowaku.focusraid.walking

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId

internal data class WalkingActions(
    val state: WalkingUiState,
    val enable: () -> Unit,
    val refresh: () -> Unit,
    val settings: () -> Unit,
    val disable: () -> Unit,
)
internal val LocalWalking = staticCompositionLocalOf<WalkingActions?> { null }

@Composable
fun WalkingProvider(viewModel: WalkingViewModel, ready: Boolean, content: @Composable () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var launcherError by remember { mutableStateOf(false) }
    val permission = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.refresh()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { if (ready) viewModel.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.cancelRead() }
    LaunchedEffect(ready) { if (ready) viewModel.refresh() else viewModel.cancelRead() }
    val actions = WalkingActions(
        state = if (launcherError) state.copy(status = WalkingStatus.ERROR) else state,
        enable = {
            launcherError = false
            viewModel.enable {
                runCatching {
                    if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                        permission.launch(HealthConnectStepsRepository.permissions)
                    } else viewModel.refresh()
                }.onFailure { launcherError = true }
            }
        },
        refresh = { launcherError = false; viewModel.refresh() },
        settings = {
            launcherError = false
            runCatching {
                val intent = if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                    Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                } else Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))
                context.startActivity(intent)
            }.onFailure { launcherError = true }
        },
        disable = viewModel::disable,
    )
    CompositionLocalProvider(LocalWalking provides actions, content = content)
}

const val WALKING_EXPLANATION = "歩数探索は無料・任意です。Health Connectの歩数を、アプリを開いた時に読み取り、景色や記念品を発見します。歩行中の画面操作は不要です。\n\n歩数・発見は端末内だけに保存し、Firebaseや第三者へ送信しません。位置情報の取得、常駐サービス、歩数の書き込みはありません。集中時間・相棒の集中成長・レイド攻撃力には変換しません。\n\n端末によってはHealth Connectへ歩数を記録する別のアプリが必要です。記録されない歩数は取得できません。開始した日から直近30日を再確認します。旅先でも開始時のタイムゾーンで日付を区切ります。\n\nアプリ内で読み取りを停止できます。権限はHealth Connectの設定でいつでも取り消せます。探索記録の削除はアプリのストレージ消去で行えます（集中記録も削除されます）。歩かない日も不利益はありません。"

@Composable
internal fun WalkingHomeCard() {
    val actions = LocalWalking.current ?: return
    WalkingPanel(actions, journalOnly = false)
}

@Composable
internal fun WalkingNotebookEntry() {
    val actions = LocalWalking.current ?: return
    WalkingPanel(actions, journalOnly = true)
}

@Composable
internal fun WalkingPanel(actions: WalkingActions, journalOnly: Boolean = false) {
    var explanation by rememberSaveable { mutableStateOf(false) }
    var notebook by rememberSaveable { mutableStateOf(false) }
    val state = actions.state
    val journal = state.journal
    val today = Instant.now().atZone(ZoneId.of(journal.zone.ifEmpty { ZoneId.systemDefault().id })).toLocalDate()
    val day = journal.days[today]
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(if (journalOnly) "歩きの探索手帳" else "今日の道のり", style = MaterialTheme.typography.titleSmall)
        if (!journalOnly) {
            val summary = when (state.status) {
                WalkingStatus.LOADING -> "道のりを確認しています…"
                WalkingStatus.DISABLED -> "歩く日も、休む日も、あなたのペースで。"
                WalkingStatus.PERMISSION_REQUIRED -> "歩数の読み取りは許可されていません。集中はいつもどおり使えます。"
                WalkingStatus.UPDATE_REQUIRED -> "Health Connectのインストール・更新が必要です。"
                WalkingStatus.UNAVAILABLE -> "この端末では歩数を読み取れません。集中の旅は続けられます。"
                WalkingStatus.ERROR -> "歩数を確認・保存できませんでした。保存済みの発見は手帳で読めます。"
                WalkingStatus.READY -> when (day?.steps) {
                    null -> "歩数データはまだありません。Health Connectの記録元を確認できます。"
                    0L -> "確認できた歩数は0歩。今日はひと休みでも大丈夫。"
                    else -> "今日の探索: ${day.steps}歩"
                }
            }
            Text(summary, style = MaterialTheme.typography.bodyMedium)
            if (state.status == WalkingStatus.READY) {
                val latest = journal.days.entries.filter { it.value.checkpoints.isNotEmpty() }.maxByOrNull { it.key }
                latest?.let { (date, record) ->
                    val discovery = WalkingExpedition.discovery(date, record.checkpoints.max())
                    Text("${if (date == today) "" else "$date · "}${discovery.place}を発見", style = MaterialTheme.typography.titleSmall)
                    Text(discovery.story, style = MaterialTheme.typography.bodySmall)
                }
                if ((day?.steps ?: 0) >= 5_000) Text("この先の歩みも、今日の景色として手帳に残ります。", style = MaterialTheme.typography.bodySmall)
                Text("確認: ${state.checkedAt?.atZone(ZoneId.of(journal.zone))?.toLocalTime()?.withNano(0)} · ${journal.zone}", style = MaterialTheme.typography.labelSmall)
            }
        }
        if (state.status == WalkingStatus.DISABLED || state.status == WalkingStatus.PERMISSION_REQUIRED) {
            TextButton(onClick = { explanation = true }) { Text("歩数を使う") }
        }
        if (state.status == WalkingStatus.UPDATE_REQUIRED || state.status == WalkingStatus.PERMISSION_REQUIRED) {
            TextButton(onClick = actions.settings) { Text("Health Connectを確認") }
        }
        if (state.status == WalkingStatus.ERROR) {
            TextButton(onClick = actions.refresh) { Text("歩数を再確認") }
        }
        TextButton(onClick = { notebook = true }) { Text("探索手帳を開く") }
    }
    if (explanation) AlertDialog(
        onDismissRequest = { explanation = false },
        title = { Text("日常の歩行を、小さな冒険に") },
        text = { Text(WALKING_EXPLANATION, Modifier.verticalScroll(rememberScrollState())) },
        confirmButton = { TextButton(modifier = Modifier.testTag("walking-consent-confirm"), onClick = { explanation = false; actions.enable() }) { Text("歩数を使う") } },
        dismissButton = { TextButton(onClick = { explanation = false }) { Text("今は使わない") } },
    )
    if (notebook) AlertDialog(
        onDismissRequest = { notebook = false },
        title = { Text("歩きの探索手帳") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item { Text("歩いて見つけた景色と記念品。集中の報酬とは別の記録です。", style = MaterialTheme.typography.bodySmall) }
                if (journal.days.isEmpty()) item { Text("まだ記録はありません。旅はいつでも始められます。") }
                items(journal.days.entries.sortedByDescending { it.key }, key = { it.key.toString() }) { (date, record) ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("$date · ${record.steps?.let { "${it}歩" } ?: "歩数データなし"}", style = MaterialTheme.typography.titleSmall)
                        record.checkpoints.sorted().forEach { checkpoint ->
                            val discovery = WalkingExpedition.discovery(date, checkpoint)
                            Text(discovery.place, style = MaterialTheme.typography.titleSmall)
                            Text(discovery.story, style = MaterialTheme.typography.bodySmall)
                            Text("記念品: ${discovery.keepsake}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (record.checkpoints.isEmpty()) Text("道の途中の景色も、ここに。", style = MaterialTheme.typography.bodySmall)
                    }
                }
                item { Text("開始日から直近30日を再集計。発見済みの記録は残ります。記録の区切り: ${journal.zone}", style = MaterialTheme.typography.bodySmall) }
                if (journal.enabled) item {
                    TextButton(onClick = actions.refresh) { Text("歩数を再確認") }
                    TextButton(onClick = actions.settings) { Text("Health Connectを確認") }
                }
                if (journal.enabled) item {
                    OutlinedButton(onClick = actions.disable) { Text("歩数の読み取りを停止") }
                    Text("記録は残ります。権限の取り消しはHealth Connectで行えます。", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = { TextButton(onClick = { notebook = false }) { Text("閉じる") } },
    )
}
