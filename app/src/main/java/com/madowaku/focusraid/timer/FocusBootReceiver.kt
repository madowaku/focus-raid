package com.madowaku.focusraid.timer

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
        if (intent?.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val saved = SessionPreferences(context.applicationContext).session.first()
                if (saved.phase != SessionPhase.RUNNING || saved.endEpochMillis <= 0L) return@launch

                val now = System.currentTimeMillis()
                if (saved.endEpochMillis > now) {
                    FocusAlarmScheduler(context.applicationContext).schedule(saved.endEpochMillis)
                } else {
                    FocusCompletionNotifier.show(context.applicationContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
