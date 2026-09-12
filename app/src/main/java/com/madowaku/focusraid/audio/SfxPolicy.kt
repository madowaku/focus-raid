package com.madowaku.focusraid.audio

/** Device-independent audibility/rate policy, including queued preload requests. */
internal class SfxPolicy {
    fun audible(enabled: Boolean, ringerNormal: Boolean) = enabled && ringerNormal
}
