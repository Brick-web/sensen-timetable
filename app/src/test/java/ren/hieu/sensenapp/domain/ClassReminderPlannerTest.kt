package ren.hieu.sensenapp.domain

import org.junit.Assert.*
import org.junit.Test
import ren.hieu.sensenapp.data.model.*
import java.time.LocalDateTime

class ClassReminderPlannerTest {
    private val export = ScheduleExportDto(
        term = TermDto(firstMonday = "2026-09-07", currentWeek = 1),
        sectionTimes = listOf(SectionTimeDto(1, "08:00", "09:40")),
        events = listOf(CourseEventDto(name = "数学", location = "101", weekday = 1, sections = listOf(1), weeks = listOf(8, 9))),
    )
    private val holidays = HolidayCalendarDto()

    @Test fun computesReminderFromActualWeekInsteadOfStaleServerWeek() {
        val reminder = ClassReminderPlanner.upcoming(export, holidays, now = LocalDateTime.parse("2026-10-25T20:00:00")).first()
        assertEquals(LocalDateTime.parse("2026-10-26T07:45:00"), reminder.fireAt)
        assertEquals("数学", reminder.courseName)
        assertEquals("101", reminder.location)
    }

    @Test fun sevenDayWindowSurvivesLongWeekendWithoutOpeningApp() {
        val reminders = ClassReminderPlanner.upcoming(export, holidays, now = LocalDateTime.parse("2026-10-27T08:00:00"))
        assertEquals(LocalDateTime.parse("2026-11-02T07:45:00"), reminders.single().fireAt)
    }

    @Test fun refreshingAfterDeliveredTimeDoesNotScheduleTheSameReminderAgain() {
        val reminders = ClassReminderPlanner.upcoming(export, holidays, now = LocalDateTime.parse("2026-10-26T07:45:00"))
        assertTrue(reminders.none { it.fireAt.toLocalDate().toString() == "2026-10-26" })
    }

    @Test fun cancelledHolidayDoesNotCreateReminderButDisabledHolidayAdjustmentDoes() {
        val holiday = HolidayCalendarDto(holidayMappings = listOf(HolidayMappingDto(calendarDate = "2026-10-26", kind = "off")))
        val now = LocalDateTime.parse("2026-10-25T20:00:00")
        assertTrue(ClassReminderPlanner.upcoming(export, holiday, now = now).isEmpty())
        assertEquals(1, ClassReminderPlanner.upcoming(export, holiday, now = now, applyHolidayAdjustments = false).size)
    }

    @Test fun rollingRefreshKeepsStableRequestCodesAcrossDaysAndChangesLeadTime() {
        val first = ClassReminderPlanner.upcoming(export, holidays, now = LocalDateTime.parse("2026-10-25T20:00:00")).first()
        val next = ClassReminderPlanner.upcoming(export, holidays, minutesBefore = 30, now = LocalDateTime.parse("2026-10-26T00:00:00")).first()
        assertEquals(first.requestCode, next.requestCode)
        assertEquals(LocalDateTime.parse("2026-10-26T07:30:00"), next.fireAt)
    }
}
