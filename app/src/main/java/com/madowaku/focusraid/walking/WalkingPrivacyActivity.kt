package com.madowaku.focusraid.walking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                        TextButton(onClick = { finish() }) { Text("閉じる") }
                    }
                }
            }
        }
    }
}
