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
    primary = Color(0xFFFFCB80),
    onPrimary = Color(0xFF30200C),
    primaryContainer = Color(0xFF514027),
    onPrimaryContainer = Color(0xFFFFE4B8),
    secondary = Color(0xFF8CD5C8),
    onSecondary = Color(0xFF092F29),
    tertiary = Color(0xFFFFD36A),
    onTertiary = Color(0xFF2F2200),
    background = Color(0xFF070B12),
    onBackground = Color(0xFFF1F5F8),
    surface = Color(0xFF101C29),
    onSurface = Color(0xFFF1F5F8),
    surfaceVariant = Color(0xFF243442),
    onSurfaceVariant = Color(0xFFCBD6DF),
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
