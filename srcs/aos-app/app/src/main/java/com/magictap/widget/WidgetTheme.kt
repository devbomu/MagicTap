package com.magictap.widget

import androidx.compose.ui.graphics.Color
import androidx.glance.material3.ColorProviders
import com.magictap.ui.theme.DarkColors
import com.magictap.ui.theme.LightColors

/**
 * Brand colours for the widgets. Used as the [androidx.glance.GlanceTheme] default so any
 * theme-derived Glance internals stay on-brand; the visible surfaces below are set
 * explicitly so the widget keeps a clean, fixed light look on any home screen.
 */
val MagicTapGlanceColors = ColorProviders(light = LightColors, dark = DarkColors)

// Fixed, always-light widget palette. The background is white regardless of the system
// dark-mode setting, and the content colours are dark so text stays readable on it.
val WidgetSurface = Color(0xFFFFFFFF)        // white widget background
val WidgetOnSurface = Color(0xFF1B1B23)      // title / primary text on white
val WidgetOnSurfaceMuted = Color(0xFF6B6675) // placeholder / secondary text on white
val WidgetCard = Color(0xFFF1EBFF)           // light lavender row card (separates from white)
val WidgetOnCard = Color(0xFF231047)         // text on the lavender card
val WidgetAccent = Color(0xFF6D34E6)         // brand violet (list power circle)
val WidgetOnAccent = Color(0xFFFFFFFF)       // icon on the brand violet
