package ren.hieu.sensenapp.data.local

import ren.hieu.sensenapp.data.model.HolidayCalendarDto
import ren.hieu.sensenapp.data.model.HolidayTermDto
import ren.hieu.sensenapp.data.model.RulesMetaDto
import ren.hieu.sensenapp.data.model.ScheduleExportDto
import ren.hieu.sensenapp.data.model.SourceDto
import ren.hieu.sensenapp.data.model.TermDto
import ren.hieu.sensenapp.domain.ScheduleCalculator
import ren.hieu.sensenapp.domain.TodayCourseGrouper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object LocalScheduleDefaults {
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    fun emptyExport(now: LocalDate = LocalDate.now(TodayCourseGrouper.zone)): ScheduleExportDto {
        val firstMonday = semesterFirstMonday(now)
        val maxWeek = 20
        val currentWeek = ScheduleCalculator.currentWeek(firstMonday, now, maxWeek).coerceAtLeast(1)
        return ScheduleExportDto(
            school = "本地课表",
            term = TermDto(
                firstMonday = firstMonday.format(dateFmt),
                maxWeek = maxWeek,
                currentWeek = currentWeek,
            ),
            source = SourceDto(revision = "local", fetchedAt = nowIso(now)),
            events = emptyList(),
        )
    }

    fun emptyHolidays(export: ScheduleExportDto): HolidayCalendarDto {
        return HolidayCalendarDto(
            school = "local",
            term = HolidayTermDto(
                firstMonday = export.term.firstMonday,
                maxWeek = export.term.maxWeek,
            ),
            holidayMappings = emptyList(),
            rulesMeta = RulesMetaDto(updatedAt = nowIso(LocalDate.now(TodayCourseGrouper.zone))),
        )
    }

    private fun semesterFirstMonday(today: LocalDate): LocalDate {
        val year = today.year
        val fallStart = LocalDate.of(year, 9, 1).with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY))
        val springStart = LocalDate.of(year, 3, 1).with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.MONDAY))
        val candidates = listOf(fallStart, springStart, fallStart.minusYears(1), springStart.minusYears(1))
            .filter { !it.isAfter(today) }
        return candidates.maxOrNull() ?: today.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
    }

    private fun nowIso(date: LocalDate): String = "${date.format(dateFmt)}T00:00:00+08:00"
}
