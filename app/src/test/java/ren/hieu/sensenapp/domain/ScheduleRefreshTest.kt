package ren.hieu.sensenapp.domain

import org.junit.Assert.*
import org.junit.Test
import ren.hieu.sensenapp.data.model.*
import java.time.LocalDate
import java.time.LocalDateTime

class ScheduleRefreshTest {
    private val export = ScheduleExportDto(term = TermDto(firstMonday = "2026-09-07", maxWeek = 17, currentWeek = 1))
    private val holidays = HolidayCalendarDto()

    @Test fun openSemesterCanDisplayWeeksBeyondSeventeen() {
        val local = export.copy(term = export.term.copy(maxWeek = 20))
        assertEquals(20, ScheduleCalculator.displayWeek(local, LocalDate.parse("2027-01-18")))
    }

    @Test fun cachedServerWeekDoesNotFreezeDisplayedWeek() {
        assertEquals(7, ScheduleCalculator.displayWeek(export, LocalDate.parse("2026-10-25")))
        assertEquals(8, ScheduleCalculator.displayWeek(export, LocalDate.parse("2026-10-26")))
    }

    @Test fun displayWeekStaysWithinTermBeforeAndAfterSemester() {
        assertEquals(1, ScheduleCalculator.displayWeek(export, LocalDate.parse("2026-09-01")))
        assertEquals(17, ScheduleCalculator.displayWeek(export, LocalDate.parse("2027-09-01")))
    }

    @Test fun emptyDayStillRefreshesAtPeriodBoundariesAndMidnight() {
        val now = LocalDateTime.parse("2026-10-25T11:59:00")
        val triggers = ScheduleCalculator.refreshTriggerTimes(export, holidays, now)
        assertEquals(LocalDateTime.parse("2026-10-25T12:00:00"), triggers.first())
        assertTrue(triggers.contains(LocalDateTime.parse("2026-10-25T18:00:00")))
        assertEquals(LocalDateTime.parse("2026-10-26T00:00:00"), triggers.last())
        assertTrue(triggers.all { it.isAfter(now) })
        assertEquals(triggers.distinct().sorted(), triggers)
    }

    @Test fun lateNightAndFollowingDayKeepTheRefreshChainAlive() {
        val now = LocalDateTime.parse("2026-10-25T23:59:59")
        assertEquals(listOf(LocalDateTime.parse("2026-10-26T00:00:00")),
            ScheduleCalculator.refreshTriggerTimes(export, holidays, now))
        val next = ScheduleCalculator.refreshTriggerTimes(export, holidays, now.toLocalDate().plusDays(1).atStartOfDay())
        assertEquals(LocalDateTime.parse("2026-10-27T00:00:00"), next.last())
    }

    @Test fun courseTransitionsAndFifteenMinuteWarningArePreserved() {
        val withCourse = export.copy(
            sectionTimes = listOf(SectionTimeDto(1, "08:30", "09:15")),
            events = listOf(CourseEventDto(name = "test", weekday = 1, sections = listOf(1), weeks = listOf(8))),
        )
        val triggers = ScheduleCalculator.refreshTriggerTimes(withCourse, holidays, LocalDateTime.parse("2026-10-26T08:01:00"))
        assertEquals(LocalDateTime.parse("2026-10-26T08:15:00"), triggers.first())
        assertTrue(triggers.contains(LocalDateTime.parse("2026-10-26T08:30:00")))
        assertTrue(triggers.contains(LocalDateTime.parse("2026-10-26T09:15:00")))
    }
}
