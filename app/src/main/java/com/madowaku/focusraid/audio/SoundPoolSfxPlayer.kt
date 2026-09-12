package com.madowaku.focusraid.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.SystemClock
import com.madowaku.focusraid.R

/** Owned by FocusViewModel, uses application context, released in onCleared. */
class SoundPoolSfxPlayer(context: Context) : SfxPlayer {
    private val context = context.applicationContext
    private val preferences = AudioPreferences(this.context)
    private val policy = SfxPolicy()
    private val manager = this.context.getSystemService(AudioManager::class.java)
    private val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(),
    ).build()
    private val loaded = mutableSetOf<Int>()
    private data class Pending(val sample: Int, val gain: Float, val pitch: Float, val deadline: Long)
    private val pending = ArrayDeque<Pending>()
    private val samples = mutableMapOf<Sfx, List<Int>>()
    private var closed = false
    private var lastKnock = -1_000L

    init {
        pool.setOnLoadCompleteListener { _, id, status ->
            synchronized(this) {
                if (!closed) {
                    if (status == 0) loaded += id
                    val requests = pending.filter { it.sample == id }
                    pending.removeAll(requests.toSet())
                    if (status == 0 && audible()) requests.filter { it.deadline >= SystemClock.elapsedRealtime() }
                        .forEach { pool.play(id, it.gain, it.gain, 1, 0, it.pitch) }
                }
            }
        }
        val resources = mapOf(
            Sfx.FOCUS_START to listOf(R.raw.fr_sfx_focus_start),
            Sfx.UI_CONFIRM to listOf(R.raw.fr_sfx_ui_confirm),
            Sfx.FOCUS_COMPLETE to listOf(R.raw.fr_sfx_focus_complete),
            Sfx.COMPANION_KNOCK to listOf(R.raw.fr_sfx_companion_knock),
            Sfx.RAID_HIT_SELF to listOf(R.raw.fr_sfx_raid_hit_self),
            Sfx.RAID_HIT_OTHER to listOf(
                R.raw.fr_sfx_raid_hit_other_01,
                R.raw.fr_sfx_raid_hit_other_02,
                R.raw.fr_sfx_raid_hit_other_03,
            ),
            Sfx.RAID_VICTORY to listOf(R.raw.fr_sfx_raid_victory),
        )
        resources.forEach { (sound, ids) -> samples[sound] = ids.map { pool.load(this.context, it, 1) } }
    }

    private fun audible() = policy.audible(preferences.enabled, manager.ringerMode == AudioManager.RINGER_MODE_NORMAL)

    @Synchronized
    override fun play(sound: Sfx, eventId: String?, variant: Int, volume: Float, pitch: Float) {
        if (closed) return
        if (eventId != null && !preferences.claim(sound, eventId)) return
        if (!audible()) return
        val now = SystemClock.elapsedRealtime()
        if (sound == Sfx.COMPANION_KNOCK) {
            if (now - lastKnock < 100) return
            lastKnock = now
        }
        val ids = samples.getValue(sound)
        val id = ids[Math.floorMod(variant, ids.size)]
        val base = when (sound) {
            Sfx.FOCUS_COMPLETE -> .24f
            Sfx.RAID_HIT_SELF -> .22f
            Sfx.RAID_VICTORY -> .26f
            Sfx.FOCUS_START, Sfx.UI_CONFIRM, Sfx.COMPANION_KNOCK -> .20f
            Sfx.RAID_HIT_OTHER -> .20f
        }
        val gain = base * volume.coerceIn(0f, 1f)
        val rate = pitch.coerceIn(.9f, 1.1f)
        if (id in loaded) pool.play(id, gain, gain, 1, 0, rate)
        else if (id != 0) {
            if (pending.size >= 8) pending.removeFirst()
            pending.addLast(Pending(id, gain, rate, now + 750))
        }
    }

    @Synchronized
    override fun setEnabled(enabled: Boolean) {
        preferences.enabled = enabled
        if (!enabled) { pending.clear(); pool.autoPause() }
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        pending.clear()
        pool.setOnLoadCompleteListener(null)
        pool.release()
    }
}
