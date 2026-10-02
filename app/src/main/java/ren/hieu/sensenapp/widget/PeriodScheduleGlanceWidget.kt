package ren.hieu.sensenapp.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/** 时段课表：仅当前上午 / 下午 / 晚上 */
class PeriodScheduleGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = loadTodayForWidget(context)
        val presentation = loadWidgetPresentation(context)
        provideContent {
            ScheduleGlanceTheme {
                GlancePeriodScheduleContent(today, presentation)
            }
        }
    }
}

class PeriodScheduleGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PeriodScheduleGlanceWidget()
}
