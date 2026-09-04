package com.madowaku.focusraid

import android.Manifest
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.madowaku.focusraid.data.FakeWorldRepository
import com.madowaku.focusraid.data.SessionPreferences
import com.madowaku.focusraid.timer.FocusAlarmScheduler
import com.madowaku.focusraid.ui.FocusRaidApp
import com.madowaku.focusraid.ui.FocusViewModel
import com.madowaku.focusraid.ui.theme.FocusRaidTheme

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val viewModel: FocusViewModel by viewModels {
        FocusViewModel.Factory(
            preferences = SessionPreferences(applicationContext),
            worldRepository = FakeWorldRepository(),
            alarmScheduler = FocusAlarmScheduler(applicationContext),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            FocusRaidTheme {
                FocusRaidApp(viewModel)
            }
        }
    }
}
