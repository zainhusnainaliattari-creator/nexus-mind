package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.data.model.MindmapTheme

private val ObsidianColorScheme = darkColorScheme(
    primary = ObsidianPrimary,
    onPrimary = Color.Black,
    secondary = ObsidianSecondary,
    onSecondary = Color.Black,
    tertiary = ObsidianAccent,
    background = ObsidianBg,
    onBackground = ObsidianText,
    surface = ObsidianSurface,
    onSurface = ObsidianText,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = ObsidianSubtext,
    outline = ObsidianBorder
)

private val CyberpunkColorScheme = darkColorScheme(
    primary = CyberpunkPrimary,
    onPrimary = Color.Black,
    secondary = CyberpunkSecondary,
    onSecondary = Color.White,
    tertiary = CyberpunkAccent,
    background = CyberpunkBg,
    onBackground = Color(0xFFF0F6FC),
    surface = CyberpunkSurface,
    onSurface = Color(0xFFF0F6FC),
    outline = Color(0xFF381559)
)

private val DraculaColorScheme = darkColorScheme(
    primary = DraculaPrimary,
    onPrimary = Color.Black,
    secondary = DraculaSecondary,
    onSecondary = Color.Black,
    background = DraculaBg,
    onBackground = Color(0xFFF8F8F2),
    surface = DraculaSurface,
    onSurface = Color(0xFFF8F8F2),
    outline = Color(0xFF6272A4)
)

private val NordColorScheme = darkColorScheme(
    primary = NordPrimary,
    onPrimary = Color.Black,
    secondary = NordSecondary,
    onSecondary = Color.Black,
    background = NordBg,
    onBackground = Color(0xFFECEFF4),
    surface = NordSurface,
    onSurface = Color(0xFFECEFF4),
    outline = Color(0xFF4C566A)
)

private val CleanLightColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MindNexusTheme(
    selectedTheme: MindmapTheme = MindmapTheme.OBSIDIAN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when (selectedTheme) {
        MindmapTheme.OBSIDIAN -> ObsidianColorScheme
        MindmapTheme.CYBERPUNK -> CyberpunkColorScheme
        MindmapTheme.DRACULA -> DraculaColorScheme
        MindmapTheme.NORD -> NordColorScheme
        MindmapTheme.SOLARIZED -> ObsidianColorScheme.copy(
            background = Color(0xFF002B36),
            surface = Color(0xFF073642),
            primary = Color(0xFF268BD2),
            secondary = Color(0xFF2AA198)
        )
        MindmapTheme.CLEAN_LIGHT -> CleanLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = selectedTheme == MindmapTheme.CLEAN_LIGHT
            insetsController.isAppearanceLightNavigationBars = selectedTheme == MindmapTheme.CLEAN_LIGHT
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
