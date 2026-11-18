package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val FolioLightColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = DayOnPrimary,
    primaryContainer = DayPrimaryContainer,
    onPrimaryContainer = DayOnPrimaryContainer,
    secondary = DaySecondary,
    onSecondary = DayOnSecondary,
    secondaryContainer = DaySecondaryContainer,
    onSecondaryContainer = DayOnSecondaryContainer,
    tertiary = DayTertiary,
    onTertiary = DayOnTertiary,
    tertiaryContainer = DayTertiaryContainer,
    onTertiaryContainer = DayOnTertiaryContainer,
    background = DayBackground,
    onBackground = DayOnBackground,
    surface = DaySurface,
    onSurface = DayOnSurface,
    surfaceVariant = DaySurfaceContainer,
    onSurfaceVariant = DayOnSurfaceVariant,
    surfaceContainerLow = DaySurface,
    surfaceContainer = DaySurfaceContainer,
    surfaceContainerHigh = DaySurfaceContainerHigh,
    outline = DayOutline,
    outlineVariant = DayOutline,
    error = DayError
)

private val FolioDarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightOnPrimary,
    primaryContainer = NightPrimaryContainer,
    onPrimaryContainer = NightOnPrimaryContainer,
    secondary = NightSecondary,
    onSecondary = NightOnSecondary,
    secondaryContainer = NightSecondaryContainer,
    onSecondaryContainer = NightOnSecondaryContainer,
    tertiary = NightTertiary,
    onTertiary = NightOnTertiary,
    tertiaryContainer = NightTertiaryContainer,
    onTertiaryContainer = NightOnTertiaryContainer,
    background = NightBackground,
    onBackground = NightOnBackground,
    surface = NightSurface,
    onSurface = NightOnSurface,
    surfaceVariant = NightSurfaceContainer,
    onSurfaceVariant = NightOnSurfaceVariant,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurfaceContainer,
    surfaceContainerHigh = NightSurfaceContainerHigh,
    outline = NightOutline,
    outlineVariant = NightOutline,
    error = NightError
)

val FolioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun FolioTheme(
    nightMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (nightMode) FolioDarkColorScheme else FolioLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !nightMode
                isAppearanceLightNavigationBars = !nightMode
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FolioTypography,
        shapes = FolioShapes,
        content = content
    )
}
