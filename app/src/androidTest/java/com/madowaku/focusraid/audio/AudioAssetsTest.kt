package com.madowaku.focusraid.audio

import android.media.AudioAttributes
import android.media.SoundPool
import androidx.test.platform.app.InstrumentationRegistry
import com.madowaku.focusraid.R
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioAssetsTest {
    @Test fun allProductionCuesDecodeInSoundPoolOnDevice() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resources = listOf(
            R.raw.fr_sfx_focus_start,
            R.raw.fr_sfx_ui_confirm,
            R.raw.fr_sfx_companion_knock,
            R.raw.fr_sfx_focus_complete,
            R.raw.fr_sfx_raid_hit_self,
            R.raw.fr_sfx_raid_hit_other_01,
            R.raw.fr_sfx_raid_hit_other_02,
            R.raw.fr_sfx_raid_hit_other_03,
            R.raw.fr_sfx_raid_victory,
        )
        val pool = SoundPool.Builder().setMaxStreams(3).setAudioAttributes(
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).build()).build()
        val latch = CountDownLatch(resources.size)
        val failures = AtomicInteger()
        try {
            pool.setOnLoadCompleteListener { _, _, status ->
                if (status != 0) failures.incrementAndGet()
                latch.countDown()
            }
            resources.forEach { assertTrue(pool.load(context, it, 1) != 0) }
            assertTrue("All cues must preload within 10 seconds", latch.await(10, TimeUnit.SECONDS))
            assertEquals(0, failures.get())
        } finally {
            pool.release()
        }
    }
}
