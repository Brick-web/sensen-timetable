package ren.hieu.sensenapp.domain

import ren.hieu.sensenapp.data.model.CourseEventDto
import ren.hieu.sensenapp.data.model.DayCourseItem
import ren.hieu.sensenapp.data.model.HolidayCalendarDto
import ren.hieu.sensenapp.data.model.HolidayMappingDto
import ren.hieu.sensenapp.data.model.ScheduleExportDto
import ren.hieu.sensenapp.data.model.SectionTimeDto
import ren.hieu.sensenapp.data.model.TodaySchedule
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.min

object ScheduleCalculator {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val displayDateFmt = DateTimeFormatter.ofPattern("M月d日 EEEE")

    fun todaySchedule(
        export: ScheduleExportDto,
        holidays: HolidayCalendarDto,
        now: LocalDateTime = LocalDateTime.now(zone),
        applyHolidayAdjustments: Boolean = true,
    ): TodaySchedule {
        val today = now.toLocalDate()
        val term = export.term
        val firstMonday = LocalDate.parse(term.firstMonday, dateFmt)
        val weekNum = currentWeek(firstMonday, today, term.maxWeek)
        val mapping = if (applyHolidayAdjustments) {
            holidayMappingForDate(holidays, today)
        } else {
            null
        }
        val effectiveWeekday = effectiveWeekday(today, mapping)

        val courses = if (effectiveWeekday == null || weekNum <= 0) {
            emptyList()
        } else {
            buildDayCourses(export, weekNum, effectiveWeekday)
        }

        val next = findNextCourse(courses, now)
        val countdown = next?.let { formatCountdown(now, it) }

        return TodaySchedule(
            dateLabel = today.format(displayDateFmt),
            weekLabel = if (weekNum > 0) "第 ${weekNum} 周" else "学期未开始",
            courses = courses,
            nextCourse = next,
            nextCountdownLabel = countdown,
        )
    }

    fun holidayMappingForDate(holidays: HolidayCalendarDto, date: LocalDate): HolidayMappingDto? {
        return holidays.holidayMappings.firstOrNull { it.calendarDate == date.format(dateFmt) }
    }

    enum class DayHolidayHighlight {
        None,
        Off,
        MakeupWork,
    }

    fun dayHolidayHighlight(date: LocalDate, mapping: HolidayMappingDto?): DayHolidayHighlight {
        if (mapping == null) return DayHolidayHighlight.None
        val effective = effectiveWeekday(date, mapping) ?: return DayHolidayHighlight.Off
        val kind = mapping.kind.trim().lowercase()
        if (kind == "work" || kind == "on" || kind == "makeup") {
            return DayHolidayHighlight.MakeupWork
        }
        if (mapping.sourceWeekday in 1..7 && mapping.sourceWeekday != date.dayOfWeek.value) {
            return DayHolidayHighlight.MakeupWork
        }
        return DayHolidayHighlight.None
    }

    fun effectiveWeekday(date: LocalDate, mapping: HolidayMappingDto?): Int? {
        if (mapping == null) {
            return date.dayOfWeek.value
        }
        if (mapping.kind == "off" && mapping.sourceWeekday == 0 &&
            (mapping.sourceCalendarDate == null || mapping.sourceCalendarDate.isBlank())
        ) {
            return null
        }
        if (mapping.sourceWeekday in 1..7) {
            return mapping.sourceWeekday
        }
        return date.dayOfWeek.value
    }

    fun coursesOnDate(
        export: ScheduleExportDto,
        holidays: HolidayCalendarDto,
        date: LocalDate,
        applyHolidayAdjustments: Boolean = true,
    ): List<DayCourseItem> {
        val term = export.term
        val firstMonday = LocalDate.parse(term.firstMonday, dateFmt)
        val weekNum = currentWeek(firstMonday, date, term.maxWeek)
        val mapping = if (applyHolidayAdjustments) {
            holidayMappingForDate(holidays, date)
        } else {
            null
        }
        val effectiveWeekday = effectiveWeekday(date, mapping)
        if (effectiveWeekday == null || weekNum <= 0) return emptyList()
        return buildDayCourses(export, weekNum, effectiveWeekday)
    }

    fun currentWeek(firstMonday: LocalDate, today: LocalDate, maxWeek: Int): Int {
        if (today.isBefore(firstMonday)) return 0
        val days = ChronoUnit.DAYS.between(firstMonday, today)
        val week = (days / 7).toInt() + 1
        return min(week, maxWeek)
    }

    private fun buildDayCourses(
        export: ScheduleExportDto,
        weekNum: Int,
        weekday: Int,
    ): List<DayCourseItem> {
        val sectionMap = export.sectionTimes.associateBy { it.index }
        val items = mutableListOf<DayCourseItem>()

        for (event in export.events) {
            if (event.weekday != weekday) continue
            if (!event.weeks.contains(weekNum)) continue
            if (!matchesParity(weekNum, event.weekParity)) continue
            val firstSection = event.sections.minOrNull() ?: continue
            val lastSection = event.sections.maxOrNull() ?: firstSection
            val start = sectionMap[firstSection]?.start ?: continue
            val end = sectionMap[lastSection]?.end ?: start
            items.add(
                DayCourseItem(
                    event = event,
                    sectionStart = start,
                    sectionEnd = end,
                    sectionIndex = firstSection,
                ),
            )
        }

        return items.sortedWith(
            compareBy<DayCourseItem> { it.sectionStart }
                .thenBy { it.event.name },
        )
    }

    private fun matchesParity(weekNum: Int, parity: String): Boolean {
        return when (parity) {
            "odd" -> weekNum % 2 == 1
            "even" -> weekNum % 2 == 0
            else -> true
        }
    }

    private fun findNextCourse(courses: List<DayCourseItem>, now: LocalDateTime): DayCourseItem? {
        val today = now.toLocalDate()
        for (course in courses) {
            val start = parseTodayTime(today, course.sectionStart)
            if (start != null && !now.isAfter(start)) {
                return course
            }
        }
        return null
    }

    private fun formatCountdown(now: LocalDateTime, course: DayCourseItem): String? {
        val today = now.toLocalDate()
        val start = parseTodayTime(today, course.sectionStart) ?: return null
        val minutes = ChronoUnit.MINUTES.between(now, start)
        if (minutes < 0) return null
        if (minutes < 60) return "${minutes} 分钟后上课"
        val hours = minutes / 60
        val rem = minutes % 60
        return if (rem == 0L) "${hours} 小时后上课" else "${hours} 小时 ${rem} 分钟后上课"
    }

    private fun parseTodayTime(date: LocalDate, hhmm: String): LocalDateTime? {
        return try {
            val parts = hhmm.split(":")
            if (parts.size < 2) return null
            val time = LocalTime.of(parts[0].toInt(), parts[1].toInt())
            LocalDateTime.of(date, time)
        } catch (_: Exception) {
            null
        }
    }

    /** 供小组件刷新：返回今日剩余课节相关的触发时刻（节次开始/结束） */
    fun refreshTriggerTimes(
        export: ScheduleExportDto,
        holidays: HolidayCalendarDto,
        now: LocalDateTime = LocalDateTime.now(zone),
        applyHolidayAdjustments: Boolean = true,
    ): List<LocalDateTime> {
        val today = todaySchedule(export, holidays, now, applyHolidayAdjustments)
        val date = now.toLocalDate()
        val triggers = mutableListOf<LocalDateTime>()
        for (course in today.courses) {
            parseTodayTime(date, course.sectionStart)?.let { triggers.add(it) }
            parseTodayTime(date, course.sectionEnd)?.let { triggers.add(it) }
            parseTodayTime(date, course.sectionStart)?.let { start ->
                triggers.add(start.minusMinutes(15))
            }
        }
        return triggers.filter { it.isAfter(now) }.distinct().sorted()
    }
}
