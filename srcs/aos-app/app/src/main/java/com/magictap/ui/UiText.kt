package com.magictap.ui

import android.content.Context
import com.magictap.R
import com.magictap.net.WakeOutcome

/** Maps a [WakeOutcome] to a localized, user-facing message (toast/snackbar). */
fun WakeOutcome.toUiMessage(context: Context): String = when (this) {
    WakeOutcome.Success -> context.getString(R.string.wake_success)
    WakeOutcome.AuthFailed -> context.getString(R.string.wake_fail_auth)
    WakeOutcome.Unreachable -> context.getString(R.string.wake_fail_unreachable)
    WakeOutcome.Timeout -> context.getString(R.string.wake_fail_timeout)
    is WakeOutcome.Error -> "${context.getString(R.string.wake_fail_generic)}: $message"
}
