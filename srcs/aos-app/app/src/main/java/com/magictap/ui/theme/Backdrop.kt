package com.magictap.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * A soft brand wash for full-screen surfaces: a faint violet tint at the top fading to a
 * faint pink tint at the bottom — the same indigo→pink sweep as the logo, kept at a low
 * enough alpha that text contrast is untouched. Ties every screen back to the icon so the
 * app feels of a piece rather than a flat sheet of one colour.
 */
@Composable
fun Modifier.brandWash(): Modifier {
    val scheme = MaterialTheme.colorScheme
    val brush = Brush.verticalGradient(
        0.0f to scheme.primary.copy(alpha = 0.08f),
        0.38f to Color.Transparent,
        0.72f to Color.Transparent,
        1.0f to scheme.tertiary.copy(alpha = 0.07f),
    )
    return this.background(brush)
}
