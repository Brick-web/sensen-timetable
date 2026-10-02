package ren.hieu.sensenapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

suspend fun updateAllScheduleWidgets(context: Context) {
    PeriodScheduleGlanceWidget().updateAll(context)
    FullDayScheduleGlanceWidget().updateAll(context)
    WeekScheduleGlanceWidget().updateAll(context)
}
