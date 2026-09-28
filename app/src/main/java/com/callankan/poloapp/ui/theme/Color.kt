package com.callankan.poloapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Paleta base: grafito profundo + rojo inspirado en el Polo.
internal object PoloPalette {
    val Red = Color(0xFFE8373E)
    val RedDeep = Color(0xFFB3141C)
    val RedLight = Color(0xFFFF6B6B)
    val Coral = Color(0xFFFF8A65)

    val Graphite0 = Color(0xFF08090B)
    val Graphite1 = Color(0xFF0B0C0F)
    val Graphite2 = Color(0xFF111317)
    val Graphite3 = Color(0xFF16191E)
    val Graphite4 = Color(0xFF1C2026)
    val Graphite5 = Color(0xFF232830)
    val Graphite6 = Color(0xFF2C323C)

    val Mist0 = Color(0xFFF4F5F7)
    val Mist1 = Color(0xFFFFFFFF)
    val Mist2 = Color(0xFFF8F9FB)
    val Mist3 = Color(0xFFEFF1F4)
    val Mist4 = Color(0xFFE7EAEE)
    val Mist5 = Color(0xFFDDE1E7)

    val Green = Color(0xFF2FD08A)
    val Amber = Color(0xFFFFB020)
    val Blue = Color(0xFF4DA3FF)
    val Violet = Color(0xFF9B87F5)
    val Teal = Color(0xFF2DD4BF)
}

/** Colores semánticos que Material 3 no cubre (estado, gráficas, héroe). */
@Immutable
data class PoloExtendedColors(
    val success: Color,
    val warning: Color,
    val danger: Color,
    val info: Color,
    val violet: Color,
    val teal: Color,
    val heroTop: Color,
    val heroBottom: Color,
    val heroGlow: Color,
    val cardBorder: Color,
    val chartGrid: Color,
    val isDark: Boolean,
)

val DarkExtendedColors = PoloExtendedColors(
    success = PoloPalette.Green,
    warning = PoloPalette.Amber,
    danger = Color(0xFFFF5A5F),
    info = PoloPalette.Blue,
    violet = PoloPalette.Violet,
    teal = PoloPalette.Teal,
    heroTop = Color(0xFF1A1D23),
    heroBottom = Color(0xFF0D0E11),
    heroGlow = Color(0xFFE8373E),
    cardBorder = Color(0x14FFFFFF),
    chartGrid = Color(0x1AFFFFFF),
    isDark = true,
)

val LightExtendedColors = PoloExtendedColors(
    success = Color(0xFF12A56A),
    warning = Color(0xFFE08A00),
    danger = Color(0xFFD92D3A),
    info = Color(0xFF1F7AE0),
    violet = Color(0xFF6E56CF),
    teal = Color(0xFF0F9F8F),
    heroTop = Color(0xFF22262E),
    heroBottom = Color(0xFF0F1115),
    heroGlow = Color(0xFFE8373E),
    cardBorder = Color(0x0F000000),
    chartGrid = Color(0x14000000),
    isDark = false,
)

val LocalPoloColors = staticCompositionLocalOf { DarkExtendedColors }
