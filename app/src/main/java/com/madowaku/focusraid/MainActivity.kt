package com.madowaku.focusraid

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.madowaku.focusraid.data.FakeWorldRepository
import com.madowaku.focusraid.data.SessionPreferences
import com.madowaku.focusraid.timer.FocusAlarmScheduler
import com.madowaku.focusraid.ui.FocusRaidRoot
import com.madowaku.focusraid.ui.FocusSystemAccess
import com.madowaku.focusraid.ui.FocusViewModel
import com.madowaku.focusraid.ui.theme.FocusRaidTheme

class MainActivity : ComponentActivity() {
    private var systemAccess by mutableStateOf(FocusSystemAccess())

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshSystemAccess()
        }

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
        refreshSystemAccess()

        setContent {
            FocusRaidTheme {
                FocusRaidRoot(
                    viewModel = viewModel,
                    systemAccess = systemAccess,
                    onRequestNotificationPermission = ::requestNotificationAccess,
                    onRequestExactAlarmPermission = ::requestExactAlarmAccess,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshSystemAccess()
    }

    private fun requestNotificationAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                openNotificationSettings()
            }
            return
        }
        openNotificationSettings()
    }

    private fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        runCatching { startActivity(intent) }
            .onFailure { openApplicationSettings() }
    }

    private fun requestExactAlarmAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
        runCatching { startActivity(intent) }
            .onFailure { openApplicationSettings() }
    }

    private fun openApplicationSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            },
        )
    }

    private fun refreshSystemAccess() {
        val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
        val exactAlarmsEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }
        systemAccess = FocusSystemAccess(
            notificationsEnabled = notificationsEnabled,
            exactAlarmsEnabled = exactAlarmsEnabled,
        )
    }
}
