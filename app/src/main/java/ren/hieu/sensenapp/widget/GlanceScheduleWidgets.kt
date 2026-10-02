package ren.hieu.sensenapp.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ren.hieu.sensenapp.MainActivity
import ren.hieu.sensenapp.data.model.DayCourseItem
import ren.hieu.sensenapp.data.model.TodaySchedule
import ren.hieu.sensenapp.domain.CourseTimeState
import ren.hieu.sensenapp.domain.GridCourse
import ren.hieu.sensenapp.domain.TodayCourseGrouper
import ren.hieu.sensenapp.domain.TodayCourseSection
import ren.hieu.sensenapp.domain.WidgetCourseSlot
import ren.hieu.sensenapp.domain.WidgetSectionMode
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.accent
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.accentSoft
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.bannerBg
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.bannerText
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.barPast
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.bgInner
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.chipText
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.rowCurrentBg
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.textPast
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.textPrimary
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.textSecondary
import ren.hieu.sensenapp.widget.ScheduleWidgetGlanceColors.todayBgSoft
import java.time.LocalDateTime

// RemoteViews 限制：每个 Row / Column 最多渲染 10 个直接子项，超出部分会被静默丢弃。

private val ShellPaddingH = 14.dp
private val ShellPaddingV = 10.dp
private val CourseRowGap = 4.dp
private val SectionGap = 8.dp
private val SectionHeaderHeight = 16.dp
private val SectionHeaderGap = 6.dp

private val WeekTitleHeight = 18.dp
private val WeekTitleGap = 6.dp
private val WeekDayHeaderHeight = 30.dp
private val WeekDayHeaderGap = 4.dp
private val WeekBannerHeight = 34.dp
private val WeekPeriodLabelWidth = 12.dp
private val BannerGap = 6.dp
private const val WEEK_DEFAULT_PERIODS = 8
private const val WEEK_MAX_PERIODS = 10

@Composable
fun GlancePeriodScheduleContent(
    today: TodaySchedule?,
    presentation: WidgetPresentation = WidgetPresentation(null, null),
) {
    GlanceDayScheduleContent(today, WidgetSectionMode.CURRENT_PERIOD, presentation)
}

@Composable
fun GlanceFullDayScheduleContent(
    today: TodaySchedule?,
    presentation: WidgetPresentation = WidgetPresentation(null, null),
) {
    GlanceDayScheduleContent(today, WidgetSectionMode.ALL, presentation)
}

@Composable
private fun GlanceDayScheduleContent(
    today: TodaySchedule?,
    mode: WidgetSectionMode,
    presentation: WidgetPresentation,
) {
    val now = LocalDateTime.now(TodayCourseGrouper.zone)
    val emptyHint = presentation.noClassHintOverride?.takeIf { it.isNotBlank() }
        ?: TodayCourseGrouper.emptySectionHint(mode)
    val hasBanner = !presentation.bannerText.isNullOrBlank()
    val sections = today?.let {
        TodayCourseGrouper.groupForDisplay(it.courses, mode, now)
    }.orEmpty()
    val metrics = dayLayoutMetrics(sections, hasBanner, LocalSize.current.height)
    GlanceWidgetShell(
        bannerText = presentation.bannerText,
        verticalPadding = metrics.paddingV,
        centerVertically = true,
    ) {
        if (today == null) {
            UnboundHint()
            return@GlanceWidgetShell
        }
        sections.forEachIndexed { index, section ->
            if (index > 0) Spacer(GlanceModifier.height(metrics.sectionGap))
            GlanceSection(
                section = section,
                trailing = if (index == 0) today.dateLabel else null,
                emptyHint = emptyHint,
                now = now,
                metrics = metrics,
            )
        }
    }
}

private data class DayLayoutMetrics(
    val paddingV: Dp,
    val headerHeight: Dp,
    val headerGap: Dp,
    val sectionGap: Dp,
    val rowGap: Dp,
    val rowHeight: Dp,
) {
    val compact: Boolean get() = rowHeight < CompactRowThreshold
}

private val NormalMinRowHeight = 30.dp
private val MaxRowHeight = 40.dp
private val CompactMinRowHeight = 20.dp
private val CompactRowThreshold = 30.dp
private val EmptyHintHeight = 14.dp

private fun sectionSlots(section: TodayCourseSection): List<WidgetCourseSlot> =
    section.slots.ifEmpty {
        section.items.map { WidgetCourseSlot(it, it.sectionStart, it.sectionEnd) }
    }.take(4)

/** 根据桌面实际分配的高度，选择常规或紧凑排版，并把剩余空间均分给课程行。 */
private fun dayLayoutMetrics(
    sections: List<TodayCourseSection>,
    hasBanner: Boolean,
    available: Dp,
): DayLayoutMetrics {
    val rowCounts = sections.map { sectionSlots(it).size }
    val rows = rowCounts.sum()
    val emptySections = rowCounts.count { it == 0 }
    val rowGaps = rowCounts.sumOf { (it - 1).coerceAtLeast(0) }
    val sectionGaps = (sections.size - 1).coerceAtLeast(0)
    val banner = if (hasBanner) WeekBannerHeight + BannerGap else 0.dp

    fun build(paddingV: Dp, header: Dp, headerGap: Dp, sectionGap: Dp, rowGap: Dp, minRow: Dp): DayLayoutMetrics? {
        val fixed = paddingV * 2 + banner + (header + headerGap) * sections.size +
            sectionGap * sectionGaps + rowGap * rowGaps + EmptyHintHeight * emptySections
        val row = if (rows == 0) MaxRowHeight else (available - fixed) / rows
        if (row < minRow) return null
        return DayLayoutMetrics(paddingV, header, headerGap, sectionGap, rowGap, row.coerceAtMost(MaxRowHeight))
    }

    return build(ShellPaddingV, SectionHeaderHeight, SectionHeaderGap, SectionGap, CourseRowGap, NormalMinRowHeight)
        ?: build(8.dp, 14.dp, 2.dp, 4.dp, 2.dp, CompactMinRowHeight)
        ?: DayLayoutMetrics(8.dp, 14.dp, 2.dp, 4.dp, 2.dp, CompactMinRowHeight)
}

@Composable
private fun GlanceSection(
    section: TodayCourseSection,
    trailing: String?,
    emptyHint: String,
    now: LocalDateTime,
    metrics: DayLayoutMetrics,
) {
    Column(GlanceModifier.fillMaxWidth()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().height(metrics.headerHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = section.title.trim(),
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary),
                maxLines = 1,
            )
            if (!trailing.isNullOrBlank()) {
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = trailing,
                    style = TextStyle(fontSize = 11.sp, color = textSecondary),
                    maxLines = 1,
                )
            }
        }
        Spacer(GlanceModifier.height(metrics.headerGap))
        val slots = sectionSlots(section)
        if (slots.isEmpty()) {
            Text(
                text = emptyHint,
                style = TextStyle(fontSize = 12.sp, color = textSecondary),
                maxLines = 1,
                modifier = GlanceModifier.height(EmptyHintHeight),
            )
        } else {
            slots.forEachIndexed { index, slot ->
                if (index > 0) Spacer(GlanceModifier.height(metrics.rowGap))
                GlanceSlotRow(slot, now, emptyHint, metrics.rowHeight, metrics.compact)
            }
        }
    }
}

@Composable
private fun GlanceSlotRow(
    slot: WidgetCourseSlot,
    now: LocalDateTime,
    emptyHint: String,
    rowHeight: Dp,
    compact: Boolean,
) {
    val course = slot.course
    val state = TodayCourseGrouper.slotTimeState(slot, now)
    val isPast = state == CourseTimeState.PAST
    val isCurrent = course != null && state == CourseTimeState.CURRENT
    val primary = if (isPast) textPast else textPrimary
    val secondary = if (isPast) textPast else textSecondary
    val barColor = when {
        course == null || isPast -> barPast
        isCurrent -> accent
        else -> accentSoft
    }
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(rowHeight)
            .cornerRadius(8.dp)
            .background(if (isCurrent) rowCurrentBg else bgInner)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = GlanceModifier.width(34.dp)) {
            Text(
                text = slot.slotStart,
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = primary),
                maxLines = 1,
            )
            if (!compact) {
                Text(
                    text = slot.slotEnd,
                    style = TextStyle(fontSize = 10.sp, color = secondary),
                    maxLines = 1,
                )
            }
        }
        Box(
            modifier = GlanceModifier
                .width(3.dp)
                .height((rowHeight - 8.dp).coerceIn(12.dp, 28.dp))
                .cornerRadius(2.dp)
                .background(barColor),
        ) {}
        Spacer(GlanceModifier.width(8.dp))
        if (course == null) {
            Text(
                text = emptyHint,
                style = TextStyle(fontSize = 12.sp, color = secondary),
                maxLines = 1,
            )
        } else if (compact) {
            Text(
                text = course.event.name,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = primary,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = courseMeta(course),
                style = TextStyle(fontSize = 10.sp, color = secondary),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
        } else {
            Column(GlanceModifier.defaultWeight()) {
                Text(
                    text = course.event.name,
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = primary,
                    ),
                    maxLines = 1,
                )
                Text(
                    text = courseMeta(course),
                    style = TextStyle(fontSize = 10.sp, color = secondary),
                    maxLines = 1,
                )
            }
        }
    }
}

private fun courseMeta(item: DayCourseItem): String =
    listOf(item.event.location, item.event.teacher)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(" · ")
        .ifEmpty { TodayCourseGrouper.periodLabel(item) }

@Composable
fun GlanceWeekScheduleContent(
    data: WeekWidgetData?,
    presentation: WidgetPresentation = WidgetPresentation(null, null),
) {
    val hasBanner = !presentation.bannerText.isNullOrBlank()
    GlanceWidgetShell(bannerText = presentation.bannerText) {
        if (data == null) {
            UnboundHint()
            return@GlanceWidgetShell
        }
        val allCourses = data.columnCourses.flatten()
        val periods = if (allCourses.any { it.endPeriod > WEEK_DEFAULT_PERIODS }) {
            WEEK_MAX_PERIODS
        } else {
            WEEK_DEFAULT_PERIODS
        }
        val bannerBlock = if (hasBanner) WeekBannerHeight + BannerGap else 0.dp
        val headerBlock = WeekTitleHeight + WeekTitleGap + WeekDayHeaderHeight + WeekDayHeaderGap
        val chrome = ShellPaddingV * 2 + bannerBlock + headerBlock
        val gridHeight = (LocalSize.current.height - chrome).coerceAtLeast((periods * 10).dp)
        val unit = gridHeight / periods

        Row(
            modifier = GlanceModifier.fillMaxWidth().height(WeekTitleHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "第 ${data.weekNum} 周",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textPrimary),
            )
            if (data.weekRangeText.isNotBlank()) {
                Spacer(GlanceModifier.defaultWeight())
                Text(
                    text = data.weekRangeText,
                    style = TextStyle(fontSize = 11.sp, color = textSecondary),
                )
            }
        }
        Spacer(GlanceModifier.height(WeekTitleGap))
        GlanceWeekHeader(data)
        Spacer(GlanceModifier.height(WeekDayHeaderGap))
        GlanceWeekGrid(data, periods, unit, gridHeight)
    }
}

@Composable
private fun GlanceWeekHeader(data: WeekWidgetData) {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = GlanceModifier.fillMaxWidth().height(WeekDayHeaderHeight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(GlanceModifier.width(WeekPeriodLabelWidth))
        (1..7).forEach { day ->
            val isToday = day == data.todayWeekday
            Column(
                modifier = GlanceModifier.defaultWeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = labels[day - 1],
                    style = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isToday) accent else textSecondary,
                    ),
                )
                Box(
                    modifier = GlanceModifier
                        .size(17.dp)
                        .cornerRadius(9.dp)
                        .background(if (isToday) accent else bgInner),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = data.weekDates.getOrNull(day - 1)?.day?.toString() ?: "",
                        style = TextStyle(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isToday) chipText else textPrimary,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun GlanceWeekGrid(data: WeekWidgetData, periods: Int, unit: Dp, gridHeight: Dp) {
    Row(modifier = GlanceModifier.fillMaxWidth().height(gridHeight)) {
        Column(modifier = GlanceModifier.width(WeekPeriodLabelWidth).fillMaxHeight()) {
            (1..periods).forEach { period ->
                Box(
                    modifier = GlanceModifier.fillMaxWidth().height(unit),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$period",
                        style = TextStyle(fontSize = 8.sp, color = textSecondary),
                    )
                }
            }
        }
        (1..7).forEach { day ->
            val isToday = day == data.todayWeekday
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .fillMaxHeight()
                    .cornerRadius(6.dp)
                    .background(if (isToday) todayBgSoft else bgInner)
                    .padding(horizontal = 1.dp),
            ) {
                var cursor = 1
                data.columnCourses.getOrNull(day - 1).orEmpty()
                    .sortedBy { it.startPeriod }
                    .forEach { course ->
                        val start = course.startPeriod
                        if (start < cursor || start > periods) return@forEach
                        val end = course.endPeriod.coerceAtMost(periods)
                        if (start > cursor) Spacer(GlanceModifier.height(unit * (start - cursor)))
                        GlanceWeekCourseChip(course, height = unit * (end - start + 1))
                        cursor = end + 1
                    }
            }
        }
    }
}

@Composable
private fun GlanceWeekCourseChip(course: GridCourse, height: Dp) {
    val lines = ((height.value - 6f) / 10f).toInt().coerceIn(1, 6)
    val showRoom = course.room.isNotBlank() && lines >= 4
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height)
            .padding(vertical = 1.dp),
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .cornerRadius(4.dp)
                .background(ColorProvider(course.color))
                .padding(horizontal = 2.dp, vertical = 2.dp),
        ) {
            Text(
                text = course.title,
                style = TextStyle(fontSize = 8.sp, fontWeight = FontWeight.Medium, color = chipText),
                maxLines = if (showRoom) 2 else lines,
            )
            if (showRoom) {
                Text(
                    text = course.room,
                    style = TextStyle(fontSize = 7.sp, color = chipText),
                    maxLines = lines - 2,
                )
            }
        }
    }
}

@Composable
private fun UnboundHint() {
    Text(
        text = "未绑定课表，点击打开森森课表",
        style = TextStyle(fontSize = 13.sp, color = textSecondary),
    )
}

@Composable
private fun GlanceWidgetShell(
    bannerText: String? = null,
    verticalPadding: Dp = ShellPaddingV,
    centerVertically: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val context = LocalContext.current
    val openApp = actionStartActivity(
        Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        },
    )
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(20.dp)
            .background(bgInner)
            .padding(horizontal = ShellPaddingH, vertical = verticalPadding)
            .clickable(openApp),
        verticalAlignment = if (centerVertically) Alignment.CenterVertically else Alignment.Top,
    ) {
        val banner = bannerText?.trim()
        if (!banner.isNullOrEmpty()) {
            GlanceCloudBanner(banner)
            Spacer(GlanceModifier.height(6.dp))
        }
        content()
    }
}

@Composable
private fun GlanceCloudBanner(text: String) {
    Box(
        modifier = GlanceModifier
            .fillMaxWidth()
            .cornerRadius(8.dp)
            .background(bannerBg)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = TextStyle(fontSize = 11.sp, color = bannerText, fontWeight = FontWeight.Medium),
            maxLines = 2,
        )
    }
}