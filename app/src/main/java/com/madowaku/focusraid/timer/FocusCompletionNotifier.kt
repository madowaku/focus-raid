package com.madowaku.focusraid.timer

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.media.AudioAttributes
import android.net.Uri
import com.madowaku.focusraid.audio.AudioPreferences
import com.madowaku.focusraid.audio.Sfx
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.madowaku.focusraid.MainActivity
import com.madowaku.focusraid.R

object FocusCompletionNotifier {
    fun show(context: Context, focusedMinutes: Int? = null, sessionId: String? = null) {
        val audio = AudioPreferences(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.focus_complete_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    description = context.getString(R.string.focus_complete_channel_description)
                    setSound(
                        Uri.parse("android.resource://${context.packageName}/raw/fr_sfx_focus_complete"),
                        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
                    )
                },
            )
        }

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        if (sessionId != null && !audio.claim(Sfx.FOCUS_COMPLETE, sessionId)) return

        val launch = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val safeMinutes = focusedMinutes?.coerceAtLeast(1)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(
                if (safeMinutes == null) "集中完了" else "${safeMinutes}分の集中、完了"
            )
            .setContentText("戻って記録とレイドの続きを確認できます。")
            .setContentIntent(launch)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setSilent(!audio.enabled)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
        recordDelivery(context)
    }

    /**
     * Records that NotificationManager accepted a completion notification request.
     *
     * The timestamp is intentionally tiny, local-only diagnostic state. It lets debug durability
     * tests prove that an alarm woke the app through screen-off, Doze, process death, and reboot
     * without depending on unstable `dumpsys notification` text formatting.
     */
    private fun recordDelivery(context: Context) {
        context.getSharedPreferences(DIAGNOSTIC_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_POSTED_AT, System.currentTimeMillis())
            .apply()
    }

    internal fun clearDeliveryMarker(context: Context) {
        context.getSharedPreferences(DIAGNOSTIC_PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LAST_POSTED_AT)
            .commit()
    }

    internal const val DIAGNOSTIC_PREFS = "focus_completion_delivery"
    internal const val KEY_LAST_POSTED_AT = "last_posted_at"

    // Android channels keep their original sound; use a new ID for the audio pass.
    private const val CHANNEL_ID = "focus_complete_audio_v1"
    private const val NOTIFICATION_ID = 2500
}
