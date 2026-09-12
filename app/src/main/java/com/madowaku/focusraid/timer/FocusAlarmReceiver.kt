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

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val saved = SessionPreferences(context.applicationContext).session.first()
                // Alarms only notify; all reward reconciliation belongs to the durable UI path.
                // An already-delivered alarm may race with pause, completion or a newer session.
                if (saved.phase == SessionPhase.RUNNING && saved.finishedEntry == null &&
                    saved.endEpochMillis > 0 && saved.endEpochMillis <= System.currentTimeMillis()) {
                    FocusCompletionNotifier.show(
                        context = context,
                        focusedMinutes = saved.selectedMinutes,
                        sessionId = saved.sessionId,
                    )
                }
            } catch (_: java.io.IOException) {
                // Leave persisted recovery intact if storage is temporarily unavailable.
            } finally {
                pending.finish()
            }
        }
    }
}
