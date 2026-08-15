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
import androidx.glance.ColorFilter
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.background
import androidx.glance.unit.ColorProvider
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
import androidx.datastore.preferences.core.Preferences
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
        // Load the document once; the selection is read reactively below so the widget
        // recomposes the instant the configuration activity writes its state.
        val data = context.appContainer.repository.current()
        provideContent {
            val profileId = currentState<Preferences>()?.get(WidgetKeys.PROFILE_ID)
            val profile = profileId?.let { pid -> data.profiles.firstOrNull { it.id == pid } }
            GlanceTheme(colors = MagicTapGlanceColors) {
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
        .background(WidgetSurface)
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
                style = TextStyle(color = ColorProvider(WidgetOnSurfaceMuted), fontSize = 12.sp),
            )
        }
        return
    }

    Column(modifier = container) {
        Text(
            profile.alias,
            style = TextStyle(color = ColorProvider(WidgetOnSurface), fontSize = 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier.padding(start = 6.dp, bottom = 10.dp),
        )
        if (profile.pcs.isEmpty()) {
            Text(
                context.getString(R.string.widget_empty),
                style = TextStyle(color = ColorProvider(WidgetOnSurfaceMuted), fontSize = 12.sp),
                modifier = GlanceModifier.padding(6.dp),
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
    // The outer bottom padding is the real gap between cards: it sits OUTSIDE the card's
    // background. (A padding applied *before* background() gets covered by the fill, which
    // is why the previous rows looked glued together.)
    Box(GlanceModifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(WidgetCard)
                .cornerRadius(18.dp)
                .clickable(actionStartActivity(WidgetIntents.confirm(context, profileId, pcId)))
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(34.dp)
                    .background(WidgetAccent)
                    .cornerRadius(17.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.ic_widget_power),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ColorProvider(WidgetOnAccent)),
                    modifier = GlanceModifier.size(18.dp),
                )
            }
            Spacer(GlanceModifier.width(12.dp))
            Text(
                alias,
                style = TextStyle(
                    color = ColorProvider(WidgetOnCard),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}
