package ren.hieu.sensenapp.domain

import ren.hieu.sensenapp.data.model.DayCourseItem
import ren.hieu.sensenapp.data.model.HolidayCalendarDto
import ren.hieu.sensenapp.data.model.ScheduleExportDto
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class ClassReminderTrigger(
    val requestCode: Int,
    val fireAt: LocalDateTime,
    val courseName: String,
    val location: String,
    val startLabel: String,
)

object ClassReminderPlanner {
    private val zone = ZoneId.of("Asia/Shanghai")

    fun upcoming(
        export: ScheduleExportDto,
        holidays: HolidayCalendarDto,
        minutesBefore: Int = 15,
        now: LocalDateTime = LocalDateTime.now(zone),
        horizonDays: Int = 3,
        applyHolidayAdjustments: Boolean = true,
    ): List<ClassReminderTrigger> {
        val results = mutableListOf<ClassReminderTrigger>()
        val startDate = now.toLocalDate()
        for (offset in 0 until horizonDays) {
            val date = startDate.plusDays(offset.toLong())
            val courses = ScheduleCalculator.coursesOnDate(
                export,
                holidays,
                date,
                applyHolidayAdjustments = applyHolidayAdjustments,
            )
            for (course in courses) {
                val startAt = parseDateTime(date, course.sectionStart) ?: continue
                val fireAt = startAt.minusMinutes(minutesBefore.toLong())
                if (!fireAt.isAfter(now)) continue
                val code = reminderRequestCode(date, course)
                results.add(
                    ClassReminderTrigger(
                        requestCode = code,
                        fireAt = fireAt,
                        courseName = course.event.name,
                        location = course.event.location,
                        startLabel = course.sectionStart,
                    ),
                )
            }
        }
        return results.distinctBy { it.requestCode }.sortedBy { it.fireAt }
    }

    private fun reminderRequestCode(date: LocalDate, course: DayCourseItem): Int {
        val section = course.event.sections.minOrNull() ?: course.sectionIndex
        return 6000 + date.dayOfYear % 400 * 10 + section
    }

    private fun parseDateTime(date: LocalDate, hhmm: String): LocalDateTime? {
        return try {
            val parts = hhmm.split(":")
            if (parts.size < 2) return null
            LocalDateTime.of(date, LocalTime.of(parts[0].toInt(), parts[1].toInt()))
        } catch (_: Exception) {
            null
        }
    }
}
