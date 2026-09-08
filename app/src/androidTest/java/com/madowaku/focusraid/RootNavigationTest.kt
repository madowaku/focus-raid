package com.madowaku.focusraid

import android.app.Activity
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.ViewModelStore
import androidx.test.platform.app.InstrumentationRegistry
import android.view.KeyEvent
import com.madowaku.focusraid.billing.*
import com.madowaku.focusraid.core.domain.CompanionIdentity
import com.madowaku.focusraid.core.model.*
import com.madowaku.focusraid.data.*
import com.madowaku.focusraid.timer.SessionAlarm
import com.madowaku.focusraid.ui.*
import com.madowaku.focusraid.ui.theme.FocusRaidTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RootNavigationTest {
    @get:Rule val compose = createComposeRule()
    private fun pressBack() { InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK); compose.waitForIdle() }
    private val store = NavigationStore()
    private val pro = object : ProAccessRepository {
        override val access = MutableStateFlow(ProAccessState(AccessLevel.PRO))
        override val purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
        override suspend fun refresh() = Unit
        override suspend fun purchasePro(activity: Activity) = Unit
        override suspend fun restorePurchases() = Unit
        override fun clearPurchaseState() { purchaseState.value = PurchaseState.Idle }
    }
    private fun show(education: Boolean) {
        compose.setContent {
            val models = remember { ViewModelStore() }
            val vm = remember {
                FocusViewModel(store, FakeWorldRepository(), object : SessionHistoryRepository {
                    override val recentSessions = MutableStateFlow<List<SessionHistoryEntry>>(emptyList())
                    override suspend fun record(entry: SessionHistoryEntry) { recentSessions.value += entry }
                }, object : SessionAlarm { override fun schedule(endEpochMillis: Long) = Unit; override fun cancel() = Unit })
                    .also { models.put("focus", it) }
            }
            val billing = remember { ProAccessViewModel(pro).also { models.put("billing", it) } }
            DisposableEffect(Unit) { onDispose { models.clear() } }
            FocusRaidTheme {
                FocusRaidRoot(vm, billing, FocusSystemAccess(!education, !education))
            }
        }
    }
    @Test fun earnedLuneCanBeSelectedAndAppearsOnHome() {
        store.session.value = PersistedSession(totalFocusMinutes = 180)
        show(education = false)
        compose.onNodeWithText("相棒").performClick()
        compose.onAllNodesWithText("相棒にする").onLast().performScrollTo().performClick()
        compose.runOnIdle { assertEquals(CompanionIdentity.LUNE, store.session.value.companion) }
        pressBack()
        compose.onNodeWithText("ルネ · 幼体").assertIsDisplayed()
    }
    @Test fun educationRechecksEntitlementBeforeStartingStarRoute() {
        store.session.value = PersistedSession(expedition = Expedition.STAR_ROUTE)
        show(education = true)
        compose.onNodeWithText("✦  星渡りへ出航").performScrollTo().performClick()
        compose.onNodeWithText("このまま開始").assertIsDisplayed()
        compose.runOnIdle { pro.access.value = ProAccessState(AccessLevel.FREE) }
        compose.onNodeWithText("このまま開始").performClick()
        compose.onNodeWithText("Focus Raid Pro").assertIsDisplayed()
        assertEquals(SessionPhase.READY, store.session.value.phase)
    }
    @Test fun backNavigatesLogAndConfirmsActiveSessionThenDismissesResult() {
        show(education = false)
        compose.onNodeWithText("ログ").performClick()
        compose.onNodeWithText("ADVENTURE LOG").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("⚔  レイド開始").performScrollTo().performClick()
        compose.onNodeWithContentDescription("集中を一時停止").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("セッションを終了しますか？").assertIsDisplayed()
        compose.onNodeWithText("集中を続ける").performClick()
        assertEquals(SessionPhase.RUNNING, store.session.value.phase)
        compose.onNodeWithText("セッションを終了").performScrollTo().performClick()
        compose.onNodeWithText("終了する").performClick()
        compose.onNodeWithText("ホームへ").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("⚔  レイド開始").performScrollTo().assertIsDisplayed()
    }
}

private class NavigationStore : SessionStore {
    override val session = MutableStateFlow(PersistedSession())
    private val value get() = session.value
    override suspend fun ensureSessionIdentity(sessionId: String) { session.value = value.copy(sessionId = sessionId) }
    override suspend fun setCompanion(identity: CompanionIdentity) { session.value = value.copy(companion = identity) }
    override suspend fun setSelectedMinutes(minutes: Int) { session.value = value.copy(selectedMinutes = minutes) }
    override suspend fun setExpedition(expedition: Expedition) { session.value = value.copy(expedition = expedition) }
    override suspend fun saveRunning(minutes: Int, expedition: Expedition, endEpochMillis: Long, sessionId: String) {
        session.value = value.copy(selectedMinutes = minutes, expedition = expedition, endEpochMillis = endEpochMillis,
            sessionId = sessionId, phase = SessionPhase.RUNNING, finishedEntry = null)
    }
    override suspend fun savePaused(remainingMillis: Long) { session.value = value.copy(phase = SessionPhase.PAUSED, pausedRemainingMillis = remainingMillis) }
    override suspend fun saveReady() { session.value = value.copy(phase = SessionPhase.READY, sessionId = null, finishedEntry = null) }
    override suspend fun stageFinishedSession(entry: SessionHistoryEntry) { session.value = value.copy(finishedEntry = entry) }
    override suspend fun commitFinishedSession(sessionId: String, creditedMinutes: Int) {
        if (value.sessionId == sessionId) session.value = value.copy(phase = SessionPhase.READY, sessionId = null,
            totalFocusMinutes = value.totalFocusMinutes + creditedMinutes)
    }
    override suspend fun markSystemAccessEducationSeen() { session.value = value.copy(systemAccessEducationSeen = true) }
}
