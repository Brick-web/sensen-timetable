package ren.hieu.sensenapp.domain

import androidx.compose.ui.graphics.Color
import ren.hieu.sensenapp.data.model.CourseEventDto
import ren.hieu.sensenapp.data.model.HolidayCalendarDto
import ren.hieu.sensenapp.data.model.ScheduleExportDto
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class GridCourse(
    val id: String,
    val title: String,
    val room: String,
    val teacher: String,
    val weekday: Int,
    val startPeriod: Int,
    val endPeriod: Int,
    val span: Int,
    val color: Color,
)

data class WeekDateCell(
    val month: Int,
    val day: Int,
)

object CurriculumWeekBuilder {
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun weekDates(firstMonday: String, weekNum: Int): List<WeekDateCell> {
        return weekCalendarDates(firstMonday, weekNum).map { d ->
            WeekDateCell(month = d.monthValue, day = d.dayOfMonth)
        }
    }

    fun weekCalendarDates(firstMonday: String, weekNum: Int): List<LocalDate> {
        val start = LocalDate.parse(firstMonday, dateFmt)
        val monday = start.plusWeeks((weekNum - 1).toLong())
        return (0..6).map { offset -> monday.plusDays(offset.toLong()) }
    }

    fun coursesForColumn(
        export: ScheduleExportDto,
        weekNum: Int,
        columnWeekday: Int,
        holidays: HolidayCalendarDto?,
        applyHolidayAdjustments: Boolean,
    ): List<GridCourse> {
        val allCourses = buildWeekCourses(export, weekNum)
        if (!applyHolidayAdjustments || holidays == null) {
            return allCourses.filter { it.weekday == columnWeekday }
        }
        val date = weekCalendarDates(export.term.firstMonday, weekNum)[columnWeekday - 1]
        val mapping = ScheduleCalculator.holidayMappingForDate(holidays, date)
        val effectiveWeekday = ScheduleCalculator.effectiveWeekday(date, mapping) ?: return emptyList()
        return allCourses.filter { it.weekday == effectiveWeekday }
    }

    fun columnHolidayHighlight(
        export: ScheduleExportDto,
        weekNum: Int,
        columnWeekday: Int,
        holidays: HolidayCalendarDto?,
        applyHolidayAdjustments: Boolean,
    ): ScheduleCalculator.DayHolidayHighlight {
        if (!applyHolidayAdjustments || holidays == null) {
            return ScheduleCalculator.DayHolidayHighlight.None
        }
        val date = weekCalendarDates(export.term.firstMonday, weekNum)[columnWeekday - 1]
        val mapping = ScheduleCalculator.holidayMappingForDate(holidays, date)
        return ScheduleCalculator.dayHolidayHighlight(date, mapping)
    }

    fun weekRangeText(dates: List<WeekDateCell>): String {
        if (dates.isEmpty()) return ""
        val first = dates.first()
        val last = dates.last()
        return "${first.month}/${first.day} - ${last.month}/${last.day}"
    }

    fun buildWeekCourses(export: ScheduleExportDto, weekNum: Int): List<GridCourse> {
        val temp = mutableListOf<Triple<CourseEventDto, Int, Int>>()
        for (event in export.events) {
            if (!event.weeks.contains(weekNum)) continue
            if (!matchesParity(weekNum, event.weekParity)) continue
            val sections = event.sections.sorted()
            if (sections.isEmpty()) continue
            val start = sections.first()
            val end = sections.last()
            temp.add(Triple(event, start, end))
        }

        temp.sortWith(compareBy({ it.first.weekday }, { it.second }))

        val merged = mutableListOf<Triple<CourseEventDto, Int, Int>>()
        for (item in temp) {
            val last = merged.lastOrNull()
            if (last != null &&
                last.first.weekday == item.first.weekday &&
                last.first.name == item.first.name &&
                last.first.teacher == item.first.teacher &&
                last.first.location == item.first.location &&
                last.third + 1 == item.second
            ) {
                merged[merged.size - 1] = Triple(last.first, last.second, item.third)
            } else {
                merged.add(item)
            }
        }

        val colors = CurriculumPalette.colorsForCourses(
            export.events.map { courseColorKey(it) },
        )
        return merged.map { (event, startPeriod, endPeriod) ->
            GridCourse(
                id = event.id,
                title = event.name.ifBlank { "未知课程" },
                room = event.location.ifBlank { "未知教室" },
                teacher = event.teacher,
                weekday = event.weekday,
                startPeriod = startPeriod,
                endPeriod = endPeriod,
                span = endPeriod - startPeriod + 1,
                color = colors[courseColorKey(event)] ?: CurriculumPalette.classicColor(0),
            )
        }
    }

    private fun courseColorKey(event: CourseEventDto): String {
        val name = event.name.trim()
        val teacher = event.teacher.trim()
        return if (teacher.isEmpty()) name else "$name|$teacher"
    }

    private fun matchesParity(weekNum: Int, parity: String): Boolean {
        return when (parity) {
            "odd" -> weekNum % 2 == 1
            "even" -> weekNum % 2 == 0
            else -> true
        }
    }
}
