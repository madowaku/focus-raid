package com.madowaku.focusraid.ui

import com.madowaku.focusraid.R

/** The eight short footprint messages keep their existing server ids and gain a small visual cue. */
internal object KenneyFootprintEmotes {
    private val resources = mapOf(
        "made_it" to R.drawable.fr_emote_made_it,
        "keep_going" to R.drawable.fr_emote_keep_going,
        "almost" to R.drawable.fr_emote_almost,
        "one_step" to R.drawable.fr_emote_one_step,
        "rest" to R.drawable.fr_emote_rest,
        "waiting" to R.drawable.fr_emote_waiting,
        "fire" to R.drawable.fr_emote_fire,
        "strong" to R.drawable.fr_emote_strong,
    )

    fun resourceFor(presetId: String): Int? = resources[presetId]
}
