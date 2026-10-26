package com.madowaku.focusraid.walking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.madowaku.focusraid.BuildConfig
import com.madowaku.focusraid.ui.theme.FocusRaidTheme

class WalkingPrivacyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FocusRaidTheme {
                Surface {
                    Column(Modifier.safeDrawingPadding().padding(20.dp).verticalScroll(rememberScrollState())) {
                        Text("Focus Raid — 歩数データの利用")
                        Text(WALKING_EXPLANATION)
                        // Health Connect opens this screen as the app's privacy rationale; link the full policy too.
                        if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                            val uriHandler = LocalUriHandler.current
                            var openFailed by remember { mutableStateOf(false) }
                            TextButton(onClick = { openFailed = runCatching {
                                require(BuildConfig.PRIVACY_POLICY_URL.startsWith("https://"))
                                uriHandler.openUri(BuildConfig.PRIVACY_POLICY_URL)
                            }.isFailure }) { Text("プライバシーポリシー全文") }
                            if (openFailed) Text("ブラウザを開けませんでした。対応するブラウザを確認して再試行してください。", color = MaterialTheme.colorScheme.error)
                        }
                        TextButton(onClick = { finish() }) { Text("閉じる") }
                    }
                }
            }
        }
    }
}
