package com.callankan.poloapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.callankan.poloapp.data.settings.ThemeMode

private val DarkScheme = darkColorScheme(
    primary = PoloPalette.Red,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1215),
    onPrimaryContainer = Color(0xFFFFDAD8),
    secondary = PoloPalette.Coral,
    onSecondary = Color(0xFF2B0E05),
    secondaryContainer = Color(0xFF2A1D1B),
    onSecondaryContainer = Color(0xFFFFDBD1),
    tertiary = PoloPalette.Blue,
    onTertiary = Color(0xFF00213F),
    tertiaryContainer = Color(0xFF12263D),
    onTertiaryContainer = Color(0xFFD3E4FF),
    background = PoloPalette.Graphite1,
    onBackground = Color(0xFFF2F3F5),
    surface = PoloPalette.Graphite1,
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = PoloPalette.Graphite4,
    onSurfaceVariant = Color(0xFF9AA1AC),
    surfaceContainerLowest = PoloPalette.Graphite0,
    surfaceContainerLow = PoloPalette.Graphite2,
    surfaceContainer = PoloPalette.Graphite3,
    surfaceContainerHigh = PoloPalette.Graphite4,
    surfaceContainerHighest = PoloPalette.Graphite5,
    surfaceBright = PoloPalette.Graphite6,
    surfaceDim = PoloPalette.Graphite0,
    outline = Color(0xFF3A404A),
    outlineVariant = Color(0xFF262A31),
    error = Color(0xFFFF5A5F),
    onError = Color.White,
    errorContainer = Color(0xFF3B1417),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFF2F3F5),
    inverseOnSurface = PoloPalette.Graphite2,
    inversePrimary = PoloPalette.RedDeep,
    scrim = Color.Black,
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFFD11E2A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3E1),
    onPrimaryContainer = Color(0xFF410005),
    secondary = Color(0xFFC4502B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE7DD),
    onSecondaryContainer = Color(0xFF3A0B00),
    tertiary = Color(0xFF1F6FD1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0ECFF),
    onTertiaryContainer = Color(0xFF001B3E),
    background = PoloPalette.Mist0,
    onBackground = Color(0xFF121418),
    surface = PoloPalette.Mist0,
    onSurface = Color(0xFF121418),
    surfaceVariant = PoloPalette.Mist4,
    onSurfaceVariant = Color(0xFF5C6470),
    surfaceContainerLowest = PoloPalette.Mist1,
    surfaceContainerLow = PoloPalette.Mist2,
    surfaceContainer = PoloPalette.Mist1,
    surfaceContainerHigh = PoloPalette.Mist3,
    surfaceContainerHighest = PoloPalette.Mist4,
    surfaceBright = PoloPalette.Mist1,
    surfaceDim = PoloPalette.Mist5,
    outline = Color(0xFFC3C9D2),
    outlineVariant = Color(0xFFE2E5EA),
    error = Color(0xFFD92D3A),
    onError = Color.White,
    errorContainer = Color(0xFFFFE1E1),
    onErrorContainer = Color(0xFF410005),
    inverseSurface = Color(0xFF1C2026),
    inverseOnSurface = Color(0xFFF2F3F5),
    inversePrimary = PoloPalette.RedLight,
    scrim = Color.Black,
)

@Composable
fun PoloTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkScheme
        else -> LightScheme
    }
    val extended = if (dark) DarkExtendedColors else LightExtendedColors
    CompositionLocalProvider(LocalPoloColors provides extended) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PoloTypography,
            shapes = PoloShapes,
            content = content,
        )
    }
}

object PoloTheme {
    val colors: PoloExtendedColors
        @Composable @ReadOnlyComposable get() = LocalPoloColors.current
}
