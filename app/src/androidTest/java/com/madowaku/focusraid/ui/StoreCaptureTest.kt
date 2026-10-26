package com.madowaku.focusraid.ui

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.madowaku.focusraid.StoreCaptureActivity
import com.madowaku.focusraid.billing.AccessLevel
import com.madowaku.focusraid.core.domain.CompanionIdentity
import com.madowaku.focusraid.core.model.Expedition
import com.madowaku.focusraid.core.model.SessionPhase
import com.madowaku.focusraid.core.model.WorldSnapshot
import com.madowaku.focusraid.data.RaidEcho
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import com.madowaku.focusraid.walking.LocalWalking
import com.madowaku.focusraid.walking.WalkingActions
import com.madowaku.focusraid.walking.WalkingDay
import com.madowaku.focusraid.walking.WalkingJournal
import com.madowaku.focusraid.walking.WalkingStatus
import com.madowaku.focusraid.walking.WalkingUiState
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.Instant
import java.time.ZoneId

/**
 * Opt-in export of unmodified production composables for store screenshots.
 * Fixture data is a local demo, NOT live activity: no timers, billing, Health Connect or world requests.
 */
class StoreCaptureTest {
    @get:Rule val compose = createAndroidComposeRule<StoreCaptureActivity>()

    private val fixture = FocusUiState(
        initialized = true,
        firstRunVersion = CURRENT_FIRST_RUN_VERSION,
        companion = CompanionIdentity.RAG,
        expedition = Expedition.ABYSS,
        world = WorldSnapshot(bossName = "環焔竜ヴォルガ", bossHp = 181, bossMaxHp = 250),
    )

    // Today's demo walk: 3,240 steps reached the first two checkpoints. Device-local only.
    private val walking: WalkingActions = run {
        val zone = ZoneId.systemDefault()
        val now = Instant.now()
        val today = now.atZone(zone).toLocalDate()
        WalkingActions(
            state = WalkingUiState(
                journal = WalkingJournal(
                    enabled = true,
                    firstDay = today.minusDays(2),
                    zone = zone.id,
                    days = mapOf(today to WalkingDay(steps = 3_240, checkpoints = setOf(1_000, 3_000))),
                ),
                status = WalkingStatus.READY,
                checkedAt = now,
            ),
            enable = {}, refresh = {}, settings = {}, disable = {},
        )
    }

    @Before fun requireOptIn() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("storeCapture") == "true")
        compose.mainClock.autoAdvance = false
    }

    private fun content(body: @Composable () -> Unit) {
        compose.setContent {
            FocusRaidTheme {
                CompositionLocalProvider(
                    LocalCompanionIdentity provides fixture.companion,
                    LocalProAccessLevel provides AccessLevel.FREE,
                    LocalWalking provides walking,
                ) { body() }
            }
        }
        compose.mainClock.advanceTimeBy(32)
    }

    /** Advances the paused clock in small steps; animation timings changed across versions. */
    private fun advanceUntil(limitMillis: Long = 12_000, condition: () -> Boolean) {
        var elapsed = 0L
        while (!condition()) {
            check(elapsed < limitMillis) { "Condition not met within ${limitMillis}ms" }
            compose.mainClock.advanceTimeBy(50)
            elapsed += 50
        }
    }

    private fun hasTag(tag: String) = compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
    private fun hasText(text: String) = compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()

    private fun save(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val expectedHeight = InstrumentationRegistry.getArguments()
            .getString("captureHeight")?.toIntOrNull() ?: 2400
        check(bitmap.width == 1080 && bitmap.height == expectedHeight) {
            "Expected 1080x${expectedHeight}px, got ${bitmap.width}x${bitmap.height}"
        }
        val directory = File(compose.activity.getExternalFilesDir(null), "store-captures")
        check(directory.mkdirs() || directory.isDirectory)
        File(directory, "$name.png").outputStream().use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    @Test fun firstRunStrike() {
        content { FirstRunMoment(fixture, onSkip = {}, onComplete = {}) }
        compose.mainClock.advanceTimeBy(1_000)
        compose.onNodeWithText("25分が、一撃になる。").assertIsDisplayed()
        save("first-run-strike")
    }

    @Test fun sharedLightSequence() {
        content { FirstRunMoment(fixture, initialAct = 1, onSkip = {}, onComplete = {}) }
        compose.mainClock.advanceTimeBy(550)
        save("shared-left")
        compose.mainClock.advanceTimeBy(880)
        save("shared-right")
        compose.mainClock.advanceTimeBy(880)
        save("shared-bottom")
        advanceUntil { hasText("同じボスへ") }
        compose.mainClock.advanceTimeBy(300)
        save("shared-arrived")
    }

    @Test fun companionBeginning() {
        content { FirstRunMoment(fixture, initialAct = 2, onSkip = {}, onComplete = {}) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("ラグの卵").assertIsDisplayed()
        save("companion-egg")
    }

    @Test fun companionLocks() {
        content { app(fixture, MainTab.COMPANION) }
        compose.mainClock.advanceTimeBy(500)
        save("companion-locks")
    }

    /** 深層 + ラグ uses the Pixel Return Raid in production (FocusRaidV07Root). */
    @Test fun returnRaid() {
        content {
            PixelReturnRaidSequence(
                scenario = ReturnRaidScenario.fromEchoes(
                    bossName = fixture.world.bossName, bossHp = 181, bossMaxHp = 250,
                    playerDamage = 25, creditedMinutes = 25,
                    echoes = listOf(RaidEcho("演出デモ", 25, 25), RaidEcho("演出デモ", 25, 25)),
                    chainCountBefore = 2, chainMinutesBefore = 50,
                ),
                onFootprints = {}, onAgain = {}, onDone = {},
            )
        }
        compose.mainClock.advanceTimeBy(1_280)
        save("return-other")
        advanceUntil { hasTag("first_raid_strike") }
        compose.mainClock.advanceTimeBy(300)
        compose.onNodeWithTag("first_raid_strike").assertIsDisplayed().performClick()
        compose.mainClock.advanceTimeBy(420)
        save("return-self")
        advanceUntil { hasText("HP") && hasText("−25") }
        compose.mainClock.advanceTimeBy(200)
        save("return-result")
        advanceUntil { hasTag("first_raid_camp") }
        compose.mainClock.advanceTimeBy(300)
        save("return-camp")
    }

    @Test fun home() {
        content { app(fixture) }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("今日の探索", substring = true).assertExists()
        save("home")
    }

    /** 深層 + ラグ focusing is the Pixel Expedition destination screen in production (FocusRaidV10Root). */
    @Test fun focusing() {
        val running = fixture.copy(
            phase = SessionPhase.RUNNING,
            selectedMinutes = 25,
            durationSeconds = 25 * 60,
            remainingSeconds = 12 * 60 + 30,
        )
        content {
            PixelExpeditionDestinationScreen(
                state = running, progressOverride = .5f,
                onPause = {}, onResume = {}, onRequestFinish = {},
            )
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("12:30").assertIsDisplayed()
        save("focusing")
    }

    @Composable private fun app(state: FocusUiState, tab: MainTab = MainTab.HOME) {
        FocusRaidSignatureContent(
            state = state, tab = tab, onTabChange = {}, onSelectCompanion = {},
            onSelectMinutes = {}, onSelectExpedition = {}, onTimerClick = {},
            onStart = {}, onPause = {}, onResume = {}, onFinishEarly = {},
            onAgain = {}, onDone = {},
        )
    }
}
