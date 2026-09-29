package com.qingjizhang.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightScheme = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealDark,
    secondary = Color(0xFFB08968),
    onSecondary = Color.White,
    secondaryContainer = Sand,
    onSecondaryContainer = Ink,
    tertiary = Income,
    background = Paper,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Sand,
    onSurfaceVariant = InkMuted,
    outline = Color(0xFFD5D0C8),
    error = Over,
    errorContainer = OverSoft,
    onErrorContainer = Over,
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFF7EC8C3),
    onPrimary = Color(0xFF003734),
    primaryContainer = Color(0xFF145551),
    onPrimaryContainer = Color(0xFFD7EDEA),
    secondary = Color(0xFFD4B48C),
    onSecondary = Color(0xFF3A2A18),
    secondaryContainer = Color(0xFF3D3428),
    onSecondaryContainer = Color(0xFFF4EFE6),
    tertiary = Color(0xFF6FCF97),
    background = Color(0xFF121716),
    onBackground = Color(0xFFE8EEED),
    surface = Color(0xFF1A211F),
    onSurface = Color(0xFFE8EEED),
    surfaceVariant = Color(0xFF2A3331),
    onSurfaceVariant = Color(0xFFB5C0BF),
    outline = Color(0xFF4A5554),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFFFDAD6),
)

@Composable
fun QingJiZhangTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography,
        content = content,
    )
}
