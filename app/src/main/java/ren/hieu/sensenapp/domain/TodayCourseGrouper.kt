package ren.hieu.sensenapp.domain

import ren.hieu.sensenapp.data.model.DayCourseItem
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

data class WidgetCourseSlot(
    val course: DayCourseItem?,
    val slotStart: String,
    val slotEnd: String,
)

data class TodayCourseSection(
    val title: String,
    val items: List<DayCourseItem>,
    val slots: List<WidgetCourseSlot> = emptyList(),
)

enum class DayPeriod(val title: String) {
    MORNING("上午课程"),
    AFTERNOON("下午课程"),
    EVENING("晚上课程"),
}

enum class CourseTimeState {
    PAST,
    CURRENT,
    UPCOMING,
}

/** 小组件：全天课表为上午、下午各两个固定槽位；时段课表仅当前时段（含晚上） */
enum class WidgetSectionMode {
    ALL,
    CURRENT_PERIOD,
}

object TodayCourseGrouper {
    val zone: ZoneId = ZoneId.of("Asia/Shanghai")

    private data class SlotDef(
        val anchorSection: Int,
        val endSection: Int,
        val start: String,
        val end: String,
    )

    private val periodSlotDefs = mapOf(
        DayPeriod.MORNING to listOf(
            SlotDef(1, 2, "08:00", "09:40"),
            SlotDef(3, 4, "10:00", "11:40"),
        ),
        DayPeriod.AFTERNOON to listOf(
            SlotDef(5, 6, "14:00", "15:40"),
            SlotDef(7, 8, "16:00", "17:40"),
        ),
        DayPeriod.EVENING to listOf(
            SlotDef(9, 10, "19:00", "20:40"),
        ),
    )

    fun periodOf(item: DayCourseItem): DayPeriod {
        val hour = item.sectionStart.substringBefore(":").toIntOrNull() ?: 12
        return when {
            hour < 12 -> DayPeriod.MORNING
            hour < 18 -> DayPeriod.AFTERNOON
            else -> DayPeriod.EVENING
        }
    }

    fun currentPeriod(now: LocalTime = LocalTime.now(zone)): DayPeriod {
        return when {
            now.hour < 12 -> DayPeriod.MORNING
            now.hour < 18 -> DayPeriod.AFTERNOON
            else -> DayPeriod.EVENING
        }
    }

    fun timeState(
        item: DayCourseItem,
        now: LocalDateTime = LocalDateTime.now(zone),
    ): CourseTimeState {
        return timeStateForRange(item.sectionStart, item.sectionEnd, now)
    }

    fun slotTimeState(
        slot: WidgetCourseSlot,
        now: LocalDateTime = LocalDateTime.now(zone),
    ): CourseTimeState {
        if (slot.course != null) {
            return timeState(slot.course, now)
        }
        if (slot.slotStart == "—" || slot.slotEnd == "—") {
            return CourseTimeState.UPCOMING
        }
        return timeStateForRange(slot.slotStart, slot.slotEnd, now)
    }

    private fun timeStateForRange(
        startText: String,
        endText: String,
        now: LocalDateTime,
    ): CourseTimeState {
        val start = LocalTime.parse(startText)
        val end = LocalTime.parse(endText)
        val time = now.toLocalTime()
        return when {
            time.isBefore(start) -> CourseTimeState.UPCOMING
            !time.isBefore(end) -> CourseTimeState.PAST
            else -> CourseTimeState.CURRENT
        }
    }

    private fun firstSection(item: DayCourseItem): Int {
        return item.event.sections.minOrNull() ?: item.sectionIndex
    }

    private fun matchesSlot(item: DayCourseItem, def: SlotDef): Boolean {
        val first = firstSection(item)
        return first in def.anchorSection..def.endSection
    }

    private fun buildSection(period: DayPeriod, items: List<DayCourseItem>): TodayCourseSection {
        val defs = periodSlotDefs[period] ?: emptyList()
        val slots = defs.map { def ->
            val course = items.firstOrNull { matchesSlot(it, def) }
            WidgetCourseSlot(
                course = course,
                slotStart = course?.sectionStart ?: def.start,
                slotEnd = course?.sectionEnd ?: def.end,
            )
        }
        return TodayCourseSection(period.title, items, slots)
    }

    fun group(courses: List<DayCourseItem>): List<TodayCourseSection> {
        val morning = courses.filter { periodOf(it) == DayPeriod.MORNING }.sortedBy { it.sectionStart }
        val afternoon = courses.filter { periodOf(it) == DayPeriod.AFTERNOON }.sortedBy { it.sectionStart }
        val evening = courses.filter { periodOf(it) == DayPeriod.EVENING }.sortedBy { it.sectionStart }
        return listOf(
            buildSection(DayPeriod.MORNING, morning),
            buildSection(DayPeriod.AFTERNOON, afternoon),
            buildSection(DayPeriod.EVENING, evening),
        )
    }

    fun groupForDisplay(
        courses: List<DayCourseItem>,
        mode: WidgetSectionMode,
        now: LocalDateTime = LocalDateTime.now(zone),
    ): List<TodayCourseSection> {
        return when (mode) {
            WidgetSectionMode.ALL -> listOf(DayPeriod.MORNING, DayPeriod.AFTERNOON).map { period ->
                val items = courses.filter { periodOf(it) == period }.sortedBy { it.sectionStart }
                buildSection(period, items)
            }
            WidgetSectionMode.CURRENT_PERIOD -> {
                val period = currentPeriod(now.toLocalTime())
                val items = courses
                    .filter { periodOf(it) == period }
                    .sortedBy { it.sectionStart }
                listOf(buildSection(period, items))
            }
        }
    }

    fun emptySectionHint(mode: WidgetSectionMode): String {
        return when (mode) {
            WidgetSectionMode.ALL -> "无课"
            WidgetSectionMode.CURRENT_PERIOD -> "本时段无课"
        }
    }

    fun periodLabel(item: DayCourseItem): String {
        val start = item.sectionIndex
        val end = item.event.sections.maxOrNull() ?: start
        return if (start == end) "第${start}节" else "第${start}-${end}节"
    }

    fun metaLine(item: DayCourseItem): String {
        val room = item.event.location.ifBlank { "—" }
        val teacher = item.event.teacher.ifBlank { "—" }
        return "${periodLabel(item)} | $room | $teacher"
    }
}
