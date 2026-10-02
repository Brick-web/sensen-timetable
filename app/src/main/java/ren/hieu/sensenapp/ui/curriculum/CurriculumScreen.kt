package ren.hieu.sensenapp.ui.curriculum

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import ren.hieu.sensenapp.data.model.HolidayCalendarDto
import ren.hieu.sensenapp.data.model.ScheduleExportDto
import ren.hieu.sensenapp.domain.CurriculumWeekBuilder
import ren.hieu.sensenapp.domain.GridCourse
import ren.hieu.sensenapp.domain.ScheduleCalculator
import ren.hieu.sensenapp.domain.WeekDateCell
import ren.hieu.sensenapp.ui.ScheduleUiState
import ren.hieu.sensenapp.ui.components.iosPressable
import ren.hieu.sensenapp.ui.settings.HolidaySettingsSheet
import ren.hieu.sensenapp.ui.settings.SettingsSheet
import ren.hieu.sensenapp.ui.settings.TermSettingsSheet
import ren.hieu.sensenapp.ui.theme.IosBlue
import ren.hieu.sensenapp.ui.theme.IosCardBg
import ren.hieu.sensenapp.ui.theme.IosGroupedBg
import ren.hieu.sensenapp.ui.theme.IosLabel
import ren.hieu.sensenapp.ui.theme.IosRed
import ren.hieu.sensenapp.ui.theme.IosSecondaryLabel
import ren.hieu.sensenapp.ui.theme.IosSeparator
import ren.hieu.sensenapp.ui.theme.SenSenAccent
import ren.hieu.sensenapp.ui.theme.SenSenBg
import ren.hieu.sensenapp.ui.theme.SenSenDayHeaderBg
import ren.hieu.sensenapp.ui.theme.SenSenGridLine
import ren.hieu.sensenapp.ui.theme.SenSenTextMuted
import ren.hieu.sensenapp.ui.theme.SenSenTextPrimary
import ren.hieu.sensenapp.ui.theme.SenSenTextSecondary
import ren.hieu.sensenapp.ui.theme.SenSenToolbarBg
import ren.hieu.sensenapp.ui.theme.SenSenToolbarBorder

private val HolidayDateOff = Color(0xFFE53935)
private val HolidayDateMakeup = Color(0xFFF57C00)

private val periods = listOf(
    PeriodRow(1, "08:00", "08:45"),
    PeriodRow(2, "08:55", "09:40"),
    PeriodRow(3, "10:00", "10:45"),
    PeriodRow(4, "10:55", "11:40"),
    PeriodRow(5, "14:00", "14:45"),
    PeriodRow(6, "14:55", "15:40"),
    PeriodRow(7, "16:00", "16:45"),
    PeriodRow(8, "16:55", "17:40"),
    PeriodRow(9, "19:00", "19:45"),
    PeriodRow(10, "19:55", "20:40"),
)

private data class PeriodRow(val index: Int, val start: String, val end: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumScreen(
    uiState: ScheduleUiState,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenWeekPicker: () -> Unit,
    onCloseWeekPicker: () -> Unit,
    onSelectWeek: (Int) -> Unit,
    onCloseStyleSheet: () -> Unit,
    onSelectStyle: (String) -> Unit,
    onCourseClick: (GridCourse) -> Unit,
    onBlankCellClick: (weekday: Int, period: Int) -> Unit,
    onSaveCourse: (ren.hieu.sensenapp.data.model.CourseEventDto) -> Unit,
    onDeleteCourse: (String) -> Unit,
    onCloseCourseEditor: () -> Unit,
    onOpenSettings: () -> Unit,
    onCloseSettings: () -> Unit,
    onOpenStyleFromSettings: () -> Unit,
    onOpenHolidaySettings: () -> Unit,
    onCloseHolidaySettings: () -> Unit,
    onOpenTermSettings: () -> Unit,
    onCloseTermSettings: () -> Unit,
    onSaveTermFirstMonday: (String) -> Unit,
    onAddHolidayMapping: (ren.hieu.sensenapp.data.model.HolidayMappingDto) -> Unit,
    onRemoveHolidayMapping: (String) -> Unit,
    onOpenResetConfirm: () -> Unit,
    onCloseResetConfirm: () -> Unit,
    onResetSchedule: () -> Unit,
    onRefreshCloud: () -> Unit,
    onRefreshWidgets: () -> Unit,
    onHolidayAdjustmentsChanged: (Boolean) -> Unit,
    onClassReminderChanged: (Boolean) -> Unit,
    onWeekFromPager: (Int) -> Unit,
) {
    val export = uiState.export
    if (export == null && uiState.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = SenSenAccent)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SenSenBg),
    ) {
        CurriculumToolbar(
            currentWeek = uiState.currentWeek,
            weekRangeText = uiState.weekRangeText,
            onPrevWeek = onPrevWeek,
            onNextWeek = onNextWeek,
            onOpenWeekPicker = onOpenWeekPicker,
            onOpenSettings = onOpenSettings,
        )

        if (uiState.isLoading) {
            LinearLoadingBar()
        }

        val pagerState = rememberPagerState(
            initialPage = (uiState.currentWeek - 1).coerceAtLeast(0),
            pageCount = { uiState.maxWeek },
        )

        LaunchedEffect(uiState.currentWeek) {
            val target = uiState.currentWeek - 1
            if (pagerState.currentPage != target) {
                pagerState.animateScrollToPage(target)
            }
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page -> onWeekFromPager(page + 1) }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            val weekNum = page + 1
            val dates = export?.let {
                CurriculumWeekBuilder.weekDates(it.term.firstMonday, weekNum)
            } ?: emptyList()
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val headerHeight = 44.dp
                val rowHeight = (maxHeight - headerHeight).coerceAtLeast(32.dp) / 10
                WeekTimetable(
                    export = export,
                    weekNum = weekNum,
                    holidays = uiState.holidays,
                    applyHolidayAdjustments = uiState.holidayAdjustmentsEnabled,
                    weekDates = dates,
                    headerHeight = headerHeight,
                    rowHeight = rowHeight,
                    onCourseClick = onCourseClick,
                    onBlankCellClick = { weekday, period -> onBlankCellClick(weekday, period) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

    if (uiState.showWeekPicker) {
        WeekPickerSheet(
            weeks = (1..uiState.maxWeek).toList(),
            currentWeek = uiState.currentWeek,
            serverWeek = uiState.serverCurrentWeek,
            onSelect = onSelectWeek,
            onDismiss = onCloseWeekPicker,
        )
    }

    if (uiState.showStyleSheet) {
        StyleSheet(
            current = uiState.cardStyle,
            onSelect = onSelectStyle,
            onDismiss = onCloseStyleSheet,
        )
    }

    uiState.courseEditor?.let { request ->
        CourseEditorSheet(
            request = request,
            maxWeek = uiState.maxWeek,
            onSave = onSaveCourse,
            onDelete = request.existing?.id?.let { id -> { onDeleteCourse(id) } },
            onDismiss = onCloseCourseEditor,
        )
    }

    if (uiState.showSettingsSheet) {
        SettingsSheet(
            uiState = uiState,
            onOpenStyle = onOpenStyleFromSettings,
            onOpenHolidaySettings = onOpenHolidaySettings,
            onOpenTermSettings = onOpenTermSettings,
            onRefreshCloud = onRefreshCloud,
            onRefreshWidgets = onRefreshWidgets,
            onClassReminderChanged = onClassReminderChanged,
            onHolidayAdjustmentsChanged = onHolidayAdjustmentsChanged,
            onOpenResetConfirm = onOpenResetConfirm,
            onDismiss = onCloseSettings,
        )
    }

    if (uiState.showHolidaySettings) {
        HolidaySettingsSheet(
            mappings = uiState.holidays?.holidayMappings ?: emptyList(),
            onAdd = onAddHolidayMapping,
            onRemove = onRemoveHolidayMapping,
            onDismiss = onCloseHolidaySettings,
        )
    }

    if (uiState.showTermSettings) {
        TermSettingsSheet(
            currentFirstMonday = uiState.export?.term?.firstMonday ?: "",
            onSave = onSaveTermFirstMonday,
            onDismiss = onCloseTermSettings,
        )
    }

    if (uiState.showResetConfirm) {
        ResetScheduleConfirmDialog(onConfirm = onResetSchedule, onDismiss = onCloseResetConfirm)
    }
}

@Composable
private fun LinearLoadingBar() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(SenSenAccent.copy(alpha = 0.2f)),
    )
}

@Composable
private fun CurriculumToolbar(
    currentWeek: Int,
    weekRangeText: String,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenWeekPicker: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SenSenToolbarBg)
            .border(width = 1.dp, color = SenSenToolbarBorder)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToolbarIconButton(text = "‹", onClick = onPrevWeek)
                Column(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable(onClick = onOpenWeekPicker),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "第${currentWeek}周",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SenSenTextPrimary,
                        textAlign = TextAlign.Center,
                    )
                    if (weekRangeText.isNotBlank()) {
                        Text(
                            text = weekRangeText,
                            fontSize = 11.sp,
                            color = SenSenTextSecondary,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                ToolbarIconButton(text = "›", onClick = onNextWeek)
            }
            ToolbarIconButton(text = "⚙", onClick = onOpenSettings)
        }
    }
}

@Composable
private fun ToolbarIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(36.dp)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, fontSize = 22.sp, color = SenSenTextMuted, fontWeight = FontWeight.Light)
    }
}

@Composable
private fun WeekTimetable(
    export: ScheduleExportDto?,
    weekNum: Int,
    holidays: HolidayCalendarDto?,
    applyHolidayAdjustments: Boolean,
    weekDates: List<WeekDateCell>,
    headerHeight: androidx.compose.ui.unit.Dp,
    rowHeight: androidx.compose.ui.unit.Dp,
    onCourseClick: (GridCourse) -> Unit,
    onBlankCellClick: (weekday: Int, period: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timeColWidth = 34.dp
    val weekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Row(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .width(timeColWidth)
                .fillMaxHeight()
                .background(SenSenBg),
        ) {
            Box(
                Modifier
                    .height(headerHeight)
                    .fillMaxWidth()
                    .border(width = 0.5.dp, color = SenSenGridLine),
            )
            periods.forEach { p ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .border(width = 0.5.dp, color = SenSenGridLine),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(p.start, fontSize = 9.sp, color = SenSenTextSecondary, lineHeight = 12.sp)
                    Text("-", fontSize = 8.sp, color = SenSenTextSecondary, lineHeight = 10.sp)
                    Text(p.end, fontSize = 9.sp, color = SenSenTextSecondary, lineHeight = 12.sp)
                }
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .fillMaxWidth(),
        ) {
            (1..7).forEach { day ->
                val dayIndex = day - 1
                val dateCell = weekDates.getOrNull(dayIndex)
                val columnCourses = if (export != null) {
                    CurriculumWeekBuilder.coursesForColumn(
                        export = export,
                        weekNum = weekNum,
                        columnWeekday = day,
                        holidays = holidays,
                        applyHolidayAdjustments = applyHolidayAdjustments,
                    )
                } else {
                    emptyList()
                }
                val holidayHighlight = if (export != null) {
                    CurriculumWeekBuilder.columnHolidayHighlight(
                        export = export,
                        weekNum = weekNum,
                        columnWeekday = day,
                        holidays = holidays,
                        applyHolidayAdjustments = applyHolidayAdjustments,
                    )
                } else {
                    ScheduleCalculator.DayHolidayHighlight.None
                }
                DayColumn(
                    dayLabel = weekdayLabels[dayIndex],
                    weekday = day,
                    dateCell = dateCell,
                    courses = columnCourses,
                    holidayHighlight = holidayHighlight,
                    headerHeight = headerHeight,
                    rowHeight = rowHeight,
                    onCourseClick = onCourseClick,
                    onBlankCellClick = onBlankCellClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DayColumn(
    dayLabel: String,
    weekday: Int,
    dateCell: WeekDateCell?,
    courses: List<GridCourse>,
    holidayHighlight: ScheduleCalculator.DayHolidayHighlight,
    headerHeight: androidx.compose.ui.unit.Dp,
    rowHeight: androidx.compose.ui.unit.Dp,
    onCourseClick: (GridCourse) -> Unit,
    onBlankCellClick: (weekday: Int, period: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val occupiedPeriods = courses.flatMap { course ->
        (course.startPeriod..course.endPeriod).toList()
    }.toSet()
    val cellInset = 2.dp
    val dateColor = when (holidayHighlight) {
        ScheduleCalculator.DayHolidayHighlight.Off -> HolidayDateOff
        ScheduleCalculator.DayHolidayHighlight.MakeupWork -> HolidayDateMakeup
        ScheduleCalculator.DayHolidayHighlight.None -> SenSenTextSecondary
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .border(width = 0.5.dp, color = SenSenGridLine),
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .height(headerHeight)
                    .fillMaxWidth()
                    .background(SenSenDayHeaderBg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("周$dayLabel", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = SenSenTextPrimary)
                if (dateCell != null) {
                    Text(
                        "${dateCell.month}/${dateCell.day}",
                        fontSize = 11.sp,
                        color = dateColor,
                        fontWeight = if (holidayHighlight != ScheduleCalculator.DayHolidayHighlight.None) {
                            FontWeight.SemiBold
                        } else {
                            FontWeight.Normal
                        },
                        lineHeight = 12.sp,
                    )
                }
            }
            repeat(10) { index ->
                val period = index + 1
                val clickable = !occupiedPeriods.contains(period)
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .then(
                            if (clickable) {
                                Modifier.clickable { onBlankCellClick(weekday, period) }
                            } else {
                                Modifier
                            },
                        )
                        .border(width = 0.5.dp, color = SenSenGridLine),
                )
            }
        }

        courses.forEach { course ->
            val top = headerHeight + rowHeight * (course.startPeriod - 1) + cellInset
            val height = rowHeight * course.span - cellInset * 2
            Box(
                modifier = Modifier
                    .padding(horizontal = cellInset)
                    .offset(y = top)
                    .height(height.coerceAtLeast(0.dp))
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(course.color)
                    .clickable { onCourseClick(course) }
                    .padding(horizontal = 2.5.dp, vertical = 4.dp),
            ) {
                CourseCardContent(title = course.title, room = course.room)
            }
        }
    }
}

private val cardTitleStyle = TextStyle(
    color = Color.White,
    fontSize = 12.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight.SemiBold,
    letterSpacing = 0.sp,
    lineBreak = LineBreak.Simple,
)

private val cardRoomStyle = TextStyle(
    color = Color.White.copy(alpha = 0.92f),
    lineHeight = 1.18.em,
    letterSpacing = 0.sp,
    lineBreak = LineBreak.Simple,
)

/**
 * 地点优先布局：先按"总高度 - 一行标题"测量地点（字号在 8~10.5sp 间自动缩放以完整放下），
 * 标题再使用剩余高度，放不下时省略。
 */
@Composable
private fun CourseCardContent(title: String, room: String) {
    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            BasicText(
                text = title,
                style = cardTitleStyle,
                overflow = TextOverflow.Ellipsis,
            )
            if (room.isNotBlank()) {
                BasicText(
                    text = room,
                    style = cardRoomStyle,
                    overflow = TextOverflow.Ellipsis,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 8.sp,
                        maxFontSize = 10.5.sp,
                        stepSize = 0.5.sp,
                    ),
                )
            }
        },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val maxHeight = constraints.maxHeight
        val gap = 2.dp.roundToPx()
        val titleLine = cardTitleStyle.lineHeight.roundToPx()
        val roomPlaceable = measurables.getOrNull(1)?.measure(
            loose.copy(maxHeight = (maxHeight - titleLine - gap).coerceAtLeast(0)),
        )
        val roomBlock = roomPlaceable?.let { it.height + gap } ?: 0
        val titlePlaceable = measurables[0].measure(
            loose.copy(maxHeight = (maxHeight - roomBlock).coerceAtLeast(titleLine.coerceAtMost(maxHeight))),
        )
        layout(constraints.maxWidth, maxHeight) {
            titlePlaceable.place(0, 0)
            roomPlaceable?.place(0, titlePlaceable.height + gap)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekPickerSheet(
    weeks: List<Int>,
    currentWeek: Int,
    serverWeek: Int?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text("选择周次", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            weeks.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { week ->
                        val active = week == currentWeek
                        val isServer = week == serverWeek
                        val shape = RoundedCornerShape(10.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(shape)
                                .background(if (active) SenSenAccent else SenSenDayHeaderBg)
                                .border(
                                    width = if (isServer) 2.dp else 0.dp,
                                    color = SenSenAccent,
                                    shape = shape,
                                )
                                .clickable { onSelect(week) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "第${week}周",
                                fontSize = 13.sp,
                                color = if (active) Color.White else SenSenTextPrimary,
                            )
                        }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyleSheet(
    current: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val options = listOf(
        "classic" to "经典",
        "minimal" to "简约",
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IosGroupedBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Text(
                "卡片样式",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = IosLabel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center,
            )
            Column(
                Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(IosCardBg),
            ) {
                options.forEachIndexed { index, (id, label) ->
                    if (index > 0) {
                        Box(
                            Modifier
                                .padding(start = 16.dp)
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(IosSeparator),
                        )
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .iosPressable { onSelect(id) }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(label, fontSize = 17.sp, color = IosLabel, modifier = Modifier.weight(1f))
                        if (current == id) Checkmark()
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun Checkmark() {
    Canvas(Modifier.size(width = 16.dp, height = 12.dp)) {
        val stroke = 2.2.dp.toPx()
        val path = Path().apply {
            moveTo(stroke / 2, size.height * 0.55f)
            lineTo(size.width * 0.38f, size.height - stroke / 2)
            lineTo(size.width - stroke / 2, stroke / 2)
        }
        drawPath(path, IosBlue, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun ResetScheduleConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(IosCardBg),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "清空课表？",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosLabel,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "将删除本机所有课程与假期配置，并恢复为空白课表。",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = IosSecondaryLabel,
                    textAlign = TextAlign.Center,
                )
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(IosSeparator),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .iosPressable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("取消", fontSize = 17.sp, color = IosBlue, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(IosSeparator),
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .iosPressable(onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("清空", fontSize = 17.sp, color = IosRed)
                }
            }
        }
    }
}

