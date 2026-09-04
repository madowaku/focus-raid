package com.madowaku.focusraid.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val FocusRaidColors = darkColorScheme(
    primary = Color(0xFFB69CFF),
    onPrimary = Color(0xFF1B0B38),
    primaryContainer = Color(0xFF3E246C),
    onPrimaryContainer = Color(0xFFF0E8FF),
    secondary = Color(0xFFFF7A96),
    onSecondary = Color(0xFF3B0715),
    tertiary = Color(0xFFFFD36A),
    onTertiary = Color(0xFF2F2200),
    background = Color(0xFF090712),
    onBackground = Color(0xFFF5F0FF),
    surface = Color(0xFF161126),
    onSurface = Color(0xFFF5F0FF),
    surfaceVariant = Color(0xFF272037),
    onSurfaceVariant = Color(0xFFD3CBE3),
    error = Color(0xFFFF7B88),
)

private val FocusRaidShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FocusRaidTheme(content: @Composable () -> Unit) {
    MaterialExpressiveTheme(
        colorScheme = FocusRaidColors,
        shapes = FocusRaidShapes,
        typography = Typography(),
        motionScheme = MotionScheme.expressive(),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent,
            contentColor = FocusRaidColors.onBackground,
            content = content,
        )
    }
}
