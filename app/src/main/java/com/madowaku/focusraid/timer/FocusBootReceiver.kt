package com.madowaku.focusraid.timer

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.data.SessionPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FocusBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (
            intent?.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            )
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val appContext = context.applicationContext
                val saved = SessionPreferences(appContext).session.first()
                if (saved.phase != SessionPhase.RUNNING || saved.endEpochMillis <= 0L) return@launch

                val now = System.currentTimeMillis()
                if (saved.endEpochMillis > now) {
                    FocusAlarmScheduler(appContext).schedule(saved.endEpochMillis)
                } else {
                    FocusCompletionNotifier.show(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
