package ren.hieu.sensenapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ScheduleExportDto(
    @Json(name = "schema_version") val schemaVersion: Int = 1,
    val school: String = "",
    val term: TermDto = TermDto(),
    val source: SourceDto = SourceDto(),
    @Json(name = "section_times") val sectionTimes: List<SectionTimeDto> = emptyList(),
    val events: List<CourseEventDto> = emptyList(),
    @Json(name = "display_hints") val displayHints: DisplayHintsDto? = null,
)

@JsonClass(generateAdapter = true)
data class TermDto(
    @Json(name = "first_monday") val firstMonday: String = "",
    @Json(name = "max_week") val maxWeek: Int = 17,
    @Json(name = "current_week") val currentWeek: Int = 0,
    val timezone: String = "Asia/Shanghai",
)

@JsonClass(generateAdapter = true)
data class SourceDto(
    val revision: String = "",
    @Json(name = "fetched_at") val fetchedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class SectionTimeDto(
    val index: Int = 0,
    val start: String = "",
    val end: String = "",
)

@JsonClass(generateAdapter = true)
data class CourseEventDto(
    val id: String = "",
    val name: String = "",
    val teacher: String = "",
    val location: String = "",
    val weekday: Int = 1,
    val sections: List<Int> = emptyList(),
    val weeks: List<Int> = emptyList(),
    @Json(name = "week_parity") val weekParity: String = "all",
)

@JsonClass(generateAdapter = true)
data class DisplayHintsDto(
    val daily: List<DailyHintDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DailyHintDto(
    @Json(name = "calendar_date") val calendarDate: String = "",
    val summary: String = "",
)

@JsonClass(generateAdapter = true)
data class HolidayCalendarDto(
    @Json(name = "schema_version") val schemaVersion: Int = 1,
    val school: String = "",
    val term: HolidayTermDto = HolidayTermDto(),
    @Json(name = "holiday_mappings") val holidayMappings: List<HolidayMappingDto> = emptyList(),
    @Json(name = "rules_meta") val rulesMeta: RulesMetaDto? = null,
)

@JsonClass(generateAdapter = true)
data class HolidayTermDto(
    @Json(name = "first_monday") val firstMonday: String = "",
    @Json(name = "max_week") val maxWeek: Int = 17,
)

@JsonClass(generateAdapter = true)
data class HolidayMappingDto(
    @Json(name = "calendar_date") val calendarDate: String = "",
    val kind: String = "",
    @Json(name = "source_weekday") val sourceWeekday: Int = 0,
    @Json(name = "source_calendar_date") val sourceCalendarDate: String? = null,
)

@JsonClass(generateAdapter = true)
data class RulesMetaDto(
    @Json(name = "updated_at") val updatedAt: String = "",
)

@JsonClass(generateAdapter = true)
data class ApiErrorDto(
    val status: String? = null,
    val message: String? = null,
    val errno: String? = null,
)

data class DayCourseItem(
    val event: CourseEventDto,
    val sectionStart: String,
    val sectionEnd: String,
    val sectionIndex: Int,
)

data class TodaySchedule(
    val dateLabel: String,
    val weekLabel: String,
    val courses: List<DayCourseItem>,
    val nextCourse: DayCourseItem?,
    val nextCountdownLabel: String?,
)
