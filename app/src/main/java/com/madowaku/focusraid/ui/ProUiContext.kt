package com.madowaku.focusraid.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.madowaku.focusraid.billing.AccessLevel

internal val LocalProAccessLevel = staticCompositionLocalOf { AccessLevel.FREE }

internal val LocalOpenProPaywall = staticCompositionLocalOf<() -> Unit> { {} }
