package com.magictap.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.state.PreferencesGlanceStateDefinition
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
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val profileId = prefs[WidgetKeys.PROFILE_ID]
        val pcId = prefs[WidgetKeys.PC_ID]
        val pc = if (profileId != null && pcId != null) {
            context.appContainer.repository.findPc(profileId, pcId)?.second
        } else {
            null
        }
        android.util.Log.i("MagicTapWidget", "single render: id=$id profileId=$profileId pcId=$pcId found=${pc != null}")
        provideContent {
            GlanceTheme {
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
    val background = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .cornerRadius(24.dp)

    if (profileId == null || pc == null) {
        Box(
            modifier = background.clickable(
                actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                context.getString(R.string.widget_not_configured),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp, textAlign = TextAlign.Center),
            )
        }
        return
    }

    Box(
        modifier = background
            .clickable(actionStartActivity(WidgetIntents.confirm(context, profileId, pc.id)))
            .padding(6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(52.dp)
                    .background(GlanceTheme.colors.primary)
                    .cornerRadius(26.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_power),
                    contentDescription = pc.alias,
                    modifier = GlanceModifier.size(28.dp),
                )
            }
            Spacer(GlanceModifier.height(6.dp))
            Text(
                pc.alias,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
    }
}
