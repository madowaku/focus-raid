package com.madowaku.focusraid.ui

data class FocusSystemAccess(
    val notificationsEnabled: Boolean = true,
    val exactAlarmsEnabled: Boolean = true,
) {
    val isReady: Boolean
        get() = notificationsEnabled && exactAlarmsEnabled
}
