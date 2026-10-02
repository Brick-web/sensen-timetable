package ren.hieu.sensenapp.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/** 周课表：当前教学周按星期汇总（高度随桌面格子伸缩） */
class WeekScheduleGlanceWidget : GlanceAppWidget() {
    // 默认 SizeMode.Single 下 LocalSize 是 provider 的最小尺寸，而非桌面实际尺寸
    override val sizeMode: SizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val week = loadWeekForWidget(context)
        val presentation = loadWidgetPresentation(context)
        provideContent {
            ScheduleGlanceTheme {
                GlanceWeekScheduleContent(week, presentation)
            }
        }
    }
}

class WeekScheduleGlanceReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeekScheduleGlanceWidget()
}
