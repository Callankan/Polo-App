package com.callankan.poloapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.callankan.poloapp.R

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

/** Tipografía para cifras: km, euros, días. */
val SpaceGrotesk = FontFamily(
    Font(R.font.spacegrotesk_medium, FontWeight.Medium),
    Font(R.font.spacegrotesk_semibold, FontWeight.SemiBold),
    Font(R.font.spacegrotesk_bold, FontWeight.Bold),
)

private fun manrope(weight: FontWeight, size: Int, line: Int, spacing: Double = 0.0) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.em,
)

private fun grotesk(weight: FontWeight, size: Int, line: Int, spacing: Double = -0.02) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = spacing.em,
)

val PoloTypography = Typography(
    displayLarge = grotesk(FontWeight.Bold, 56, 60, -0.03),
    displayMedium = grotesk(FontWeight.Bold, 44, 48, -0.03),
    displaySmall = grotesk(FontWeight.Bold, 34, 38, -0.02),
    headlineLarge = manrope(FontWeight.ExtraBold, 30, 36, -0.02),
    headlineMedium = manrope(FontWeight.ExtraBold, 26, 32, -0.02),
    headlineSmall = manrope(FontWeight.Bold, 22, 28, -0.01),
    titleLarge = manrope(FontWeight.Bold, 20, 26, -0.01),
    titleMedium = manrope(FontWeight.Bold, 16, 22),
    titleSmall = manrope(FontWeight.SemiBold, 14, 20),
    bodyLarge = manrope(FontWeight.Medium, 16, 24),
    bodyMedium = manrope(FontWeight.Medium, 14, 20),
    bodySmall = manrope(FontWeight.Medium, 12, 16),
    labelLarge = manrope(FontWeight.Bold, 14, 20, 0.01),
    labelMedium = manrope(FontWeight.SemiBold, 12, 16, 0.02),
    labelSmall = manrope(FontWeight.Bold, 11, 14, 0.06),
)

object NumberStyles {
    val hero = grotesk(FontWeight.Bold, 44, 48, -0.035)
    val large = grotesk(FontWeight.Bold, 30, 34, -0.03)
    val medium = grotesk(FontWeight.SemiBold, 22, 26, -0.02)
    val small = grotesk(FontWeight.SemiBold, 16, 20, -0.01)
    val tiny = grotesk(FontWeight.Medium, 13, 16, 0.0)
}
