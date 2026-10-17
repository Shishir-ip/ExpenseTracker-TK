package com.expensetracker.tk.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Selectable accent colours — re-tints buttons, chips, FAB, nav across the app. */
data class AccentOption(val label: String, val seed: Long, val containerLight: Long, val onContainerLight: Long, val containerDark: Long, val onContainerDark: Long)

val ACCENTS = listOf(
    AccentOption("Indigo", 0xFF6366F1, 0xFFE0E7FF, 0xFF4F46E5, 0xFF3730A3, 0xFFE0E7FF),
    AccentOption("Emerald", 0xFF10B981, 0xFFD1FAE5, 0xFF065F46, 0xFF064E3B, 0xFFD1FAE5),
    AccentOption("Rose", 0xFFF43F5E, 0xFFFFE4E6, 0xFF9F1239, 0xFF881337, 0xFFFFE4E6),
    AccentOption("Amber", 0xFFF59E0B, 0xFFFEF3C7, 0xFF92400E, 0xFF78350F, 0xFFFEF3C7),
    AccentOption("Cyan", 0xFF06B6D4, 0xFFCFFAFE, 0xFF155E75, 0xFF164E63, 0xFFCFFAFE),
    AccentOption("Violet", 0xFF8B5CF6, 0xFFEDE9FE, 0xFF5B21B6, 0xFF4C1D95, 0xFFEDE9FE)
)

private fun accentLight(index: Int) =
    if (index in 1..ACCENTS.lastIndex) {
        val a = ACCENTS[index]
        LightColors.copy(primary = Color(a.seed), onPrimary = Color.White, primaryContainer = Color(a.containerLight), onPrimaryContainer = Color(a.onContainerLight))
    } else LightColors

private fun accentDark(index: Int) =
    if (index in 1..ACCENTS.lastIndex) {
        val a = ACCENTS[index]
        DarkColors.copy(primary = Color(a.seed), onPrimary = Color.Black, primaryContainer = Color(a.containerDark), onPrimaryContainer = Color(a.onContainerDark))
    } else DarkColors

private val LightColors = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = PrimaryDark,
    secondary = AiGlow,
    onSecondary = Color.White,
    tertiary = Cyan,
    background = Slate50,
    onBackground = Slate800,
    surface = Color.White,
    onSurface = Slate800,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate500,
    outline = Slate200,
    outlineVariant = Slate200,
    error = Danger,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Slate900,
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Slate100,
    secondary = Color(0xFFA78BFA),
    onSecondary = Slate900,
    tertiary = Color(0xFF22D3EE),
    background = Slate900,
    onBackground = Slate100,
    surface = Slate800,
    onSurface = Slate100,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate400,
    outline = Slate700,
    outlineVariant = Slate700,
    error = Color(0xFFF87171),
    onError = Slate900
)

/** AMOLED: pure black background to save battery on OLED screens. */
private val BlackColors = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Slate100,
    secondary = Color(0xFFA78BFA),
    onSecondary = Color.Black,
    tertiary = Color(0xFF22D3EE),
    background = Color.Black,
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF161616),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF262626),
    outlineVariant = Color(0xFF262626),
    error = Color(0xFFF87171),
    onError = Color.Black
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ExpenseTrackerTheme(
    themeMode: Int = 0,   // 0 system, 1 light, 2 dark, 3 dynamic (Material You), 4 black
    accentIndex: Int = 0,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colorScheme = when {
        themeMode == 3 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        themeMode == 4 -> accentDark(accentIndex).copy(
            background = Color.Black, surface = Color(0xFF0A0A0A),
            surfaceVariant = Color(0xFF161616), outline = Color(0xFF262626)
        )
        dark -> accentDark(accentIndex)
        else -> accentLight(accentIndex)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
