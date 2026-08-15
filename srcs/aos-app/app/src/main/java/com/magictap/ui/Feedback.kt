package com.magictap.ui

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

/**
 * Subtle, setting-respecting haptics routed through the host [View]. Uses the platform
 * feedback constants (not the [Vibrator][android.os.Vibrator]) so it needs no VIBRATE
 * permission and honors the user's system haptic preference. minSdk 31 → CONFIRM/REJECT
 * (API 30) are always available.
 */
class Haptics(private val view: View) {
    /** Light tap for ordinary button presses and selections. */
    fun click() { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }

    /** Even lighter tick, e.g. moving between chips/segments. */
    fun tick() { view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }

    /** Long-press affordance (context menus). */
    fun longPress() { view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS) }

    /** Positive confirmation, e.g. a wake succeeded. */
    fun confirm() { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM) }

    /** Negative feedback, e.g. a wake failed or a destructive confirm. */
    fun reject() { view.performHapticFeedback(HapticFeedbackConstants.REJECT) }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    return remember(view) { Haptics(view) }
}

/**
 * Springy press feedback: the target shrinks slightly while held, then bounces back.
 * Pass the same [interactionSource] to the Button/clickable so it tracks the real press.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressedScale: Float = 0.94f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
