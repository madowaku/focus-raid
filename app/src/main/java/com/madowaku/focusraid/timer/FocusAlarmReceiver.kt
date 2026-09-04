package com.madowaku.focusraid.timer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class FocusAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        FocusCompletionNotifier.show(context)
    }
}
