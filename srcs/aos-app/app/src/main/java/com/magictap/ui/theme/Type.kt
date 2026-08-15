package com.magictap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type scale tuned on top of the Material 3 default: headings and titles carry a little
 * more weight and tighter tracking for a crisper, more intentional feel; body text is
 * left at the well-proven defaults for readability.
 */
private val Default = Typography()

val AppTypography = Default.copy(
    headlineMedium = Default.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.25).sp),
    headlineSmall = Default.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = Default.labelLarge.copy(fontWeight = FontWeight.Medium),
)
