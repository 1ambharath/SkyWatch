package com.example.skywatch.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NightPrimary,
    onPrimary = NightBg,
    secondary = NightSecondary,
    tertiary = NightTertiary,
    background = NightBg,
    surface = NightSurface,
    onBackground = NightOnBg,
    onSurface = NightOnSurface,
    onSurfaceVariant = NightOnSurfaceMuted,
    outline = NightOutline
)

private val LightColorScheme = lightColorScheme(
    primary = DayPrimary,
    onPrimary = DayBg,
    secondary = DaySecondary,
    tertiary = DayTertiary,
    background = DayBg,
    surface = DaySurface,
    onBackground = DayOnBg,
    onSurface = DayOnSurface,
    onSurfaceVariant = DayOnSurfaceMuted,
    outline = DayOutline
)

@Composable
fun SkyWatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure our custom theme is applied
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}