package com.madowaku.focusraid

import android.app.Activity
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.data.SessionPreferences
import com.madowaku.focusraid.timer.FocusAlarmScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Debug-only control surface for emulator durability tests.
 *
 * This receiver exists only in src/debug and is exported only by the debug manifest.
 * It lets CI seed a short running session, inspect persisted state, and clear state
 * without weakening the production manifest or product APIs.
 */
class DurabilityDebugReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val preferences = SessionPreferences(appContext)
        val scheduler = FocusAlarmScheduler(appContext)

        when (intent?.action) {
            ACTION_SEED -> {
                val durationMillis = intent.getLongExtra(EXTRA_DURATION_MILLIS, 10_000L)
                    .coerceIn(2_000L, 120_000L)
                val endEpochMillis = System.currentTimeMillis() + durationMillis

                runBlocking {
                    preferences.saveRunning(
                        minutes = 1,
                        expedition = Expedition.TOWER,
                        endEpochMillis = endEpochMillis,
                    )
                }
                scheduler.schedule(endEpochMillis)

                setResultCode(Activity.RESULT_OK)
                setResultData(
                    "seeded=true;endEpochMillis=$endEpochMillis;durationMillis=$durationMillis;" +
                        "exact=${scheduler.canScheduleExactAlarms()}",
                )
            }

            ACTION_PROBE -> {
                val saved = runBlocking { preferences.session.first() }
                setResultCode(Activity.RESULT_OK)
                setResultData(
                    "phase=${saved.phase};endEpochMillis=${saved.endEpochMillis};" +
                        "pausedRemainingMillis=${saved.pausedRemainingMillis};" +
                        "nowEpochMillis=${System.currentTimeMillis()};" +
                        "exact=${scheduler.canScheduleExactAlarms()}",
                )
            }

            ACTION_CLEAR -> {
                scheduler.cancel()
                runBlocking { preferences.saveReady() }
                appContext.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
                setResultCode(Activity.RESULT_OK)
                setResultData("cleared=true")
            }

            else -> {
                setResultCode(Activity.RESULT_CANCELED)
                setResultData("unknown-action=${intent?.action}")
            }
        }
    }

    private companion object {
        const val ACTION_SEED = "com.madowaku.focusraid.debug.SEED_TIMER"
        const val ACTION_PROBE = "com.madowaku.focusraid.debug.PROBE_TIMER"
        const val ACTION_CLEAR = "com.madowaku.focusraid.debug.CLEAR_TIMER"
        const val EXTRA_DURATION_MILLIS = "duration_ms"
        const val NOTIFICATION_ID = 2500
    }
}
