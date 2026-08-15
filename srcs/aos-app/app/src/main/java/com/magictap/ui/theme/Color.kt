package com.magictap.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Crafted brand palette seeded from the launcher icon — a diagonal indigo → violet →
 * magenta → pink gradient with a warm golden bolt accent. Neutrals are tinted toward the
 * violet hue rather than pure white/black, and a full tonal set is defined for every
 * Material 3 role so surfaces read as intentional and colourful, not flat grey.
 */

// Seed — kept for reference / non-scheme usage.
val Brand = Color(0xFF8B4CF0)

/** Vivid four-stop brand gradient for hero accents (logo, primary buttons, highlights). */
val BrandGradient = Brush.linearGradient(
    listOf(
        Color(0xFF5B4BF5), // indigo
        Color(0xFF8B4CF0), // violet
        Color(0xFFC24AE0), // magenta
        Color(0xFFFF5C93), // pink
    ),
)

/** Warm golden accent (the bolt) — used for "energy"/success sparkle moments. */
val BrandGold = Color(0xFFFFC24F)
val BrandGoldGradient = Brush.linearGradient(
    listOf(Color(0xFFFFEE9C), Color(0xFFFFCB4F), Color(0xFFFF9526)),
)

// ---- Light ----
val primaryLight = Color(0xFF6D34E6)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFE9DDFF)
val onPrimaryContainerLight = Color(0xFF23005C)
val secondaryLight = Color(0xFF715C9E)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFECDDFF)
val onSecondaryContainerLight = Color(0xFF281151)
val tertiaryLight = Color(0xFFB23A6F)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFFFD9E3)
val onTertiaryContainerLight = Color(0xFF40052A)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF410002)
val backgroundLight = Color(0xFFFBF7FF)
val onBackgroundLight = Color(0xFF191320)
val surfaceLight = Color(0xFFFBF7FF)
val onSurfaceLight = Color(0xFF191320)
val surfaceVariantLight = Color(0xFFE9E0F3)
val onSurfaceVariantLight = Color(0xFF4A4458)
val outlineLight = Color(0xFF7B748A)
val outlineVariantLight = Color(0xFFCCC4DA)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF6F0FD)
val surfaceContainerLight = Color(0xFFF0E9FA)
val surfaceContainerHighLight = Color(0xFFEAE2F5)
val surfaceContainerHighestLight = Color(0xFFE4DCF0)
val inverseSurfaceLight = Color(0xFF322F3A)
val inverseOnSurfaceLight = Color(0xFFF4EFFA)
val inversePrimaryLight = Color(0xFFD3BBFF)

// ---- Dark ----
val primaryDark = Color(0xFFD3BBFF)
val onPrimaryDark = Color(0xFF3C0E8A)
val primaryContainerDark = Color(0xFF532AB8)
val onPrimaryContainerDark = Color(0xFFE9DDFF)
val secondaryDark = Color(0xFFD5BEF3)
val onSecondaryDark = Color(0xFF382A54)
val secondaryContainerDark = Color(0xFF40315F)
val onSecondaryContainerDark = Color(0xFFECDDFF)
val tertiaryDark = Color(0xFFFFB1CC)
val onTertiaryDark = Color(0xFF571133)
val tertiaryContainerDark = Color(0xFF7A2A54)
val onTertiaryContainerDark = Color(0xFFFFD9E3)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF141026)
val onBackgroundDark = Color(0xFFE7E0F5)
val surfaceDark = Color(0xFF141026)
val onSurfaceDark = Color(0xFFE7E0F5)
val surfaceVariantDark = Color(0xFF494458)
val onSurfaceVariantDark = Color(0xFFCBC3DB)
val outlineDark = Color(0xFF948DA4)
val outlineVariantDark = Color(0xFF494458)
val surfaceContainerLowestDark = Color(0xFF0E0A1D)
val surfaceContainerLowDark = Color(0xFF1B1630)
val surfaceContainerDark = Color(0xFF1F1A35)
val surfaceContainerHighDark = Color(0xFF2A2440)
val surfaceContainerHighestDark = Color(0xFF352E4B)
val inverseSurfaceDark = Color(0xFFE7E0F5)
val inverseOnSurfaceDark = Color(0xFF322F3A)
val inversePrimaryDark = Color(0xFF6D34E6)
