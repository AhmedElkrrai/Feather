package com.elkrrai.feather.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.elkrrai.feather.R
import com.elkrrai.feather.feature.quickadd.QuickAddActivity
import com.elkrrai.feather.reminders.withAppLocale

/**
 * The 4 by 1 widget: the bird, "What needs doing?" and a plus. One tap anywhere on it opens quick
 * add. It has no data to refresh.
 */
class QuickAddWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val localized = context.withAppLocale()
        val hint = localized.getString(R.string.quick_add_hint)
        val label = localized.getString(R.string.action_add_task)
        provideContent {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ImageProvider(R.drawable.widget_bg_card))
                    .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                    .clickable(actionStartActivity(Intent(context, QuickAddActivity::class.java))),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    provider = ImageProvider(R.drawable.widget_owl_idle),
                    contentDescription = null,
                    modifier = GlanceModifier.size(40.dp),
                )
                Spacer(GlanceModifier.width(12.dp))
                Text(
                    text = hint,
                    maxLines = 1,
                    style = TextStyle(color = WidgetColors.inkSoft, fontSize = 16.sp),
                    modifier = GlanceModifier.defaultWeight(),
                )
                Box(
                    modifier = GlanceModifier
                        .size(48.dp)
                        .background(WidgetColors.primary)
                        .cornerRadius(18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_add),
                        contentDescription = label,
                        colorFilter = ColorFilter.tint(WidgetColors.onPrimary),
                        modifier = GlanceModifier.size(24.dp),
                    )
                }
            }
        }
    }
}

class QuickAddWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickAddWidget()
}
