package com.magictap.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.ColorFilter
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.datastore.preferences.core.Preferences
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.magictap.MainActivity
import com.magictap.R
import com.magictap.appContainer
import com.magictap.data.model.Pc

/** Single-icon widget: one power button that wakes one preselected PC. */
class SingleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load the document once; the selection is read reactively below so the widget
        // recomposes the instant the configuration activity writes its state.
        val data = context.appContainer.repository.current()
        provideContent {
            val prefs = currentState<Preferences>()
            val profileId = prefs?.get(WidgetKeys.PROFILE_ID)
            val pcId = prefs?.get(WidgetKeys.PC_ID)
            val pc = if (profileId != null && pcId != null) {
                data.profiles.firstOrNull { it.id == profileId }?.pcs?.firstOrNull { it.id == pcId }
            } else {
                null
            }
            GlanceTheme(colors = MagicTapGlanceColors) {
                SingleContent(context, profileId, pc)
            }
        }
    }
}

class SingleWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SingleWidget()
}

@Composable
private fun SingleContent(context: Context, profileId: String?, pc: Pc?) {
    if (profileId == null || pc == null) {
        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WidgetSurface)
                .cornerRadius(20.dp)
                .clickable(
                    actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                context.getString(R.string.widget_not_configured),
                style = TextStyle(color = ColorProvider(WidgetOnSurfaceMuted), fontSize = 11.sp, textAlign = TextAlign.Center),
            )
        }
        return
    }

    // Neutral rounded tile filling the cell (nothing clipped). The per-PC accent is kept
    // to just the power icon, so multiple widgets are still tell-apart-able without a loud
    // full-colour block. The alias gets two lines.
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetSurface)
            .cornerRadius(20.dp)
            .clickable(actionStartActivity(WidgetIntents.confirm(context, profileId, pc.id)))
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_power),
                contentDescription = pc.alias,
                colorFilter = ColorFilter.tint(ColorProvider(tileColorFor(pc.id))),
                modifier = GlanceModifier.size(22.dp),
            )
            Spacer(GlanceModifier.height(3.dp))
            Text(
                pc.alias,
                style = TextStyle(
                    color = ColorProvider(WidgetOnSurface),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 2,
            )
        }
    }
}

/**
 * Stable, distinct tile colour per PC so multiple single widgets are easy to tell apart.
 * Pulled from the brand gradient (indigo → violet → magenta → pink) plus the golden bolt
 * accent and one teal, so the accents stay on-brand while remaining easy to distinguish.
 */
private val TILE_COLORS = listOf(
    Color(0xFF6D34E6), Color(0xFFC24AE0), Color(0xFFFF5C93),
    Color(0xFFFF9526), Color(0xFF5B4BF5), Color(0xFF00B0A6),
)

private fun tileColorFor(id: String): Color =
    TILE_COLORS[(id.hashCode() and 0x7FFFFFFF) % TILE_COLORS.size]
