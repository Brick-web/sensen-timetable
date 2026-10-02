package ren.hieu.sensenapp.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/** 全天课表：今日最多 5 节课，按时间排序 */
class FullDayScheduleGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val today = loadTodayForWidget(context)
        val presentation = loadWidgetPresentation(context)
        provideContent {
            ScheduleGlanceTheme {
                GlanceFullDayScheduleContent(today, presentation)
            }
        }
    }
}

class FullDayScheduleGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = FullDayScheduleGlanceWidget()
}
