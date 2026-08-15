package com.magictap.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.preferences.core.stringPreferencesKey

/** Per-widget selection, stored in each widget's Glance preferences state. */
object WidgetKeys {
    val PROFILE_ID = stringPreferencesKey("widget_profile_id")
    val PC_ID = stringPreferencesKey("widget_pc_id")
}

/** Extras and intent construction for launching [ConfirmActivity] from a widget tap. */
object WidgetIntents {
    const val EXTRA_PROFILE_ID = "com.magictap.extra.PROFILE_ID"
    const val EXTRA_PC_ID = "com.magictap.extra.PC_ID"
    private const val ACTION_WAKE = "com.magictap.action.WAKE"

    /**
     * Builds a confirm intent. A unique [Intent.setData] per (profile, pc) is essential:
     * PendingIntent equality ignores extras, so without a distinguishing URI multiple
     * widget buttons would collapse into one and all wake the same PC.
     */
    fun confirm(context: Context, profileId: String, pcId: String): Intent =
        Intent(context, ConfirmActivity::class.java).apply {
            action = ACTION_WAKE
            data = Uri.parse("magictap://wake/$profileId/$pcId")
            putExtra(EXTRA_PROFILE_ID, profileId)
            putExtra(EXTRA_PC_ID, pcId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
}
