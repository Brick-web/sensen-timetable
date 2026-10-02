package ren.hieu.sensenapp.widget

import android.content.Context
import ren.hieu.sensenapp.SenSenApplication
import ren.hieu.sensenapp.data.model.TodaySchedule
import ren.hieu.sensenapp.data.network.ApiClient
import ren.hieu.sensenapp.data.prefs.RemoteConfigStore
import ren.hieu.sensenapp.domain.CurriculumWeekBuilder
import ren.hieu.sensenapp.domain.TodayCourseGrouper
import java.time.LocalDate

data class WidgetPresentation(
    val bannerText: String?,
    val noClassHintOverride: String?,
)

fun loadWidgetPresentation(context: Context): WidgetPresentation {
    val store = RemoteConfigStore(context.applicationContext, ApiClient.moshi)
    return WidgetPresentation(
        bannerText = store.activeBanner(),
        noClassHintOverride = store.activeNoClassHint(),
    )
}

suspend fun loadTodayForWidget(context: Context): TodaySchedule? {
    val app = context.applicationContext as? SenSenApplication ?: return null
    return app.container.scheduleRepository.getTodaySnapshot()
}

suspend fun loadWeekForWidget(context: Context): WeekWidgetData? {
    val app = context.applicationContext as? SenSenApplication ?: return null
    val repo = app.container.scheduleRepository
    val settings = app.container.settingsStore
    val pair = repo.getExportForWidget() ?: return null
    val (export, holidays) = pair
    val maxWeek = export.term.maxWeek.coerceIn(1, 17)
    val weekNum = export.term.currentWeek.coerceIn(1, maxWeek).takeIf { it > 0 } ?: 1
    val dates = CurriculumWeekBuilder.weekDates(export.term.firstMonday, weekNum)
    val applyHolidays = settings.holidayAdjustmentsEnabled
    val columnCourses = (1..7).map { day ->
        CurriculumWeekBuilder.coursesForColumn(
            export = export,
            weekNum = weekNum,
            columnWeekday = day,
            holidays = holidays,
            applyHolidayAdjustments = applyHolidays,
        )
    }
    return WeekWidgetData(
        weekNum = weekNum,
        weekRangeText = CurriculumWeekBuilder.weekRangeText(dates),
        columnCourses = columnCourses,
        weekDates = dates,
        todayWeekday = LocalDate.now(TodayCourseGrouper.zone).dayOfWeek.value,
    )
}
