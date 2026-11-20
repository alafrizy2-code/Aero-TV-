package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.ThemeAccent

@Composable
fun AeroTvTheme(
    accent: ThemeAccent = ThemeAccent.CYAN_CYBER,
    content: @Composable () -> Unit
) {
    val accentColor = Color(accent.hex)

    val colorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = Color.Black,
        secondary = accentColor.copy(alpha = 0.8f),
        onSecondary = Color.Black,
        background = TvBackgroundDark,
        onBackground = TvTextPrimary,
        surface = TvSurfaceDark,
        onSurface = TvTextPrimary,
        surfaceVariant = TvSurfaceElevated,
        onSurfaceVariant = TvTextSecondary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
