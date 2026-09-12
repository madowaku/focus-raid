package com.madowaku.focusraid.audio

import android.content.Context

/** Small audio-only delivery journal, shared by the alarm and presentation path. */
class AudioPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("focus_raid_audio", Context.MODE_PRIVATE)
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(value) { prefs.edit().putBoolean("enabled", value).apply() }

    fun claim(sound: Sfx, id: String): Boolean = synchronized(lock) {
        val key = "delivered_${sound.name}"
        val recent = prefs.getStringSet(key, emptySet()).orEmpty()
        if (id in recent) return@synchronized false
        // At-most-once delivery: a process killed after this commit must not replay a reward.
        prefs.edit().putStringSet(key, (recent.toList().takeLast(63) + id).toSet()).commit()
    }

    companion object { private val lock = Any() }
}
