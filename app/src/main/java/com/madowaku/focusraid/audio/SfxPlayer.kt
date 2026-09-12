package com.madowaku.focusraid.audio

enum class Sfx {
    FOCUS_START,
    UI_CONFIRM,
    FOCUS_COMPLETE,
    COMPANION_KNOCK,
    RAID_HIT_SELF,
    RAID_HIT_OTHER,
    RAID_VICTORY,
}

/** Presentation events only; tests never need an Android audio device. */
interface SfxPlayer {
    fun play(sound: Sfx, eventId: String? = null, variant: Int = 0, volume: Float = 1f, pitch: Float = 1f)
    fun setEnabled(enabled: Boolean) {}
    fun close() {}
}

object SilentSfxPlayer : SfxPlayer {
    override fun play(sound: Sfx, eventId: String?, variant: Int, volume: Float, pitch: Float) = Unit
}
