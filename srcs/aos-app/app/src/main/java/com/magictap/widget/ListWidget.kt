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
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.magictap.MainActivity
import com.magictap.R
import com.magictap.appContainer
import com.magictap.data.model.Profile

/** List widget: shows a profile's PCs, each row a wake button. Scrolls when tall enough. */
class ListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = getAppWidgetState(context, PreferencesGlanceStateDefinition, id)
        val profileId = prefs[WidgetKeys.PROFILE_ID]
        val profile = profileId?.let { pid ->
            context.appContainer.repository.current().profiles.firstOrNull { it.id == pid }
        }
        android.util.Log.i("MagicTapWidget", "list render: id=$id profileId=$profileId found=${profile != null}")
        provideContent {
            GlanceTheme {
                ListContent(context, profile)
            }
        }
    }
}

class ListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ListWidget()
}

@Composable
private fun ListContent(context: Context, profile: Profile?) {
    val container = GlanceModifier
        .fillMaxSize()
        .background(GlanceTheme.colors.widgetBackground)
        .cornerRadius(16.dp)
        .padding(10.dp)

    if (profile == null) {
        Box(
            modifier = container.clickable(
                actionStartActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                context.getString(R.string.widget_not_configured),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
            )
        }
        return
    }

    Column(modifier = container) {
        Text(
            profile.alias,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold),
            modifier = GlanceModifier.padding(start = 4.dp, bottom = 6.dp),
        )
        if (profile.pcs.isEmpty()) {
            Text(
                context.getString(R.string.widget_empty),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                modifier = GlanceModifier.padding(4.dp),
            )
        } else {
            LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                items(count = profile.pcs.size) { index ->
                    val pc = profile.pcs[index]
                    PcWidgetRow(context, profile.id, pc.id, pc.alias)
                }
            }
        }
    }
}

@Composable
private fun PcWidgetRow(context: Context, profileId: String, pcId: String, alias: String) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .background(GlanceTheme.colors.secondaryContainer)
            .cornerRadius(12.dp)
            .clickable(actionStartActivity(WidgetIntents.confirm(context, profileId, pcId)))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier.size(28.dp).background(GlanceTheme.colors.primary).cornerRadius(14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(R.drawable.ic_widget_power),
                contentDescription = null,
                modifier = GlanceModifier.size(16.dp),
            )
        }
        Spacer(GlanceModifier.width(10.dp))
        Text(
            alias,
            style = TextStyle(color = GlanceTheme.colors.onSecondaryContainer, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
    }
}
