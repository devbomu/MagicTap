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

/** Configuration activity for the list widget: pick the profile it should display. */
class ListWidgetConfigActivity : ComponentActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Default result: if the user backs out, the placement is cancelled.
        setResult(RESULT_CANCELED, resultIntent())
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContent {
            MagicTapTheme {
                WidgetConfigScreen(
                    single = false,
                    onPickProfile = { profileId -> save(profileId) },
                    onPickPc = { _, _ -> },
                    onOpenApp = { startActivity(Intent(this, MainActivity::class.java)) },
                )
            }
        }
    }

    private fun save(profileId: String) {
        val ctx = applicationContext
        lifecycleScope.launch {
            val glanceId = GlanceAppWidgetManager(ctx).getGlanceIdBy(appWidgetId)
            updateAppWidgetState(ctx, glanceId) { prefs ->
                prefs[WidgetKeys.PROFILE_ID] = profileId
                prefs.remove(WidgetKeys.PC_ID)
            }
            ListWidget().update(ctx, glanceId)
            setResult(RESULT_OK, resultIntent())
            finish()
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
