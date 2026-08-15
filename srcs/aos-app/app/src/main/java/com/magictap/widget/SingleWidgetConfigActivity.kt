package com.magictap.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.magictap.MainActivity
import com.magictap.ui.theme.MagicTapTheme
import kotlinx.coroutines.launch

/** Configuration activity for the single-icon widget: pick profile → PC. */
class SingleWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MagicTapTheme {
                WidgetConfigScreen(
                    single = true,
                    onPickProfile = { },
                    onPickPc = { profileId, pcId -> save(profileId, pcId) },
                    onOpenApp = { startActivity(Intent(this, MainActivity::class.java)) },
                )
            }
        }
    }

    private fun save(profileId: String, pcId: String) {
        // Application context: the per-widget Glance DataStore must outlive this activity
        // (which finish()es right after). Writing via an activity context is the usual
        // reason a configured widget still reads empty state.
        val ctx = applicationContext
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(ctx).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(ctx, glanceId) { prefs ->
                prefs[WidgetKeys.PROFILE_ID] = profileId
                prefs[WidgetKeys.PC_ID] = pcId
            }
            SingleWidget().update(ctx, glanceId)
            android.util.Log.i("MagicTapWidget", "single saved: appWidgetId=$appWidgetId glanceId=$glanceId profileId=$profileId pcId=$pcId")
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
