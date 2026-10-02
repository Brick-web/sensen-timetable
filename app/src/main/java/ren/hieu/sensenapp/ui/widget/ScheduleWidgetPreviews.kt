package ren.hieu.sensenapp.ui.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ren.hieu.sensenapp.data.model.CourseEventDto
import ren.hieu.sensenapp.data.model.DayCourseItem
import ren.hieu.sensenapp.data.model.TodaySchedule
import ren.hieu.sensenapp.domain.GridCourse
import ren.hieu.sensenapp.domain.WeekDateCell
import ren.hieu.sensenapp.domain.WidgetSectionMode
import ren.hieu.sensenapp.ui.theme.SenSenSchoolTheme
import ren.hieu.sensenapp.widget.WeekWidgetData
import java.time.LocalDateTime

private fun sampleTodaySchedule() = TodaySchedule(
    dateLabel = "4月2日 星期三",
    weekLabel = "第 8 周",
    courses = listOf(
        DayCourseItem(
            event = CourseEventDto(
                name = "文献检索与论文写作",
                location = "实验实训楼北205(基础)",
                teacher = "杨敏",
                sections = listOf(1, 2),
            ),
            sectionStart = "08:00",
            sectionEnd = "09:40",
            sectionIndex = 1,
        ),
        DayCourseItem(
            event = CourseEventDto(
                name = "大学英语",
                location = "教学楼A101",
                teacher = "李老师",
                sections = listOf(3, 4),
            ),
            sectionStart = "10:00",
            sectionEnd = "11:40",
            sectionIndex = 3,
        ),
        DayCourseItem(
            event = CourseEventDto(
                name = "数据结构",
                location = "机房302",
                teacher = "王老师",
                sections = listOf(5, 6),
            ),
            sectionStart = "14:00",
            sectionEnd = "15:40",
            sectionIndex = 5,
        ),
    ),
    nextCourse = null,
    nextCountdownLabel = null,
)

/** 下午上课时间：上午课变灰，下午课高亮，晚上显示无课 */
private val previewAfternoon = LocalDateTime.of(2026, 4, 2, 14, 30)

@Preview(showBackground = true, widthDp = 360, name = "时段课表")
@Composable
private fun PeriodScheduleWidgetPreview() {
    SenSenSchoolTheme {
        TodayScheduleWidgetCard(
            today = sampleTodaySchedule(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            sectionMode = WidgetSectionMode.CURRENT_PERIOD,
            now = previewAfternoon,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "全天课表")
@Composable
private fun FullDayScheduleWidgetPreview() {
    SenSenSchoolTheme {
        TodayScheduleWidgetCard(
            today = sampleTodaySchedule(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            sectionMode = WidgetSectionMode.ALL,
            now = previewAfternoon,
        )
    }
}

@Preview(showBackground = true, widthDp = 360, name = "周课表")
@Composable
private fun WeekScheduleWidgetPreview() {
    SenSenSchoolTheme {
        WeekScheduleWidgetCard(
            data = WeekWidgetData(
                weekNum = 8,
                weekRangeText = "4/1 - 4/7",
                todayWeekday = 3,
                weekDates = listOf(
                    WeekDateCell(4, 1),
                    WeekDateCell(4, 2),
                    WeekDateCell(4, 3),
                    WeekDateCell(4, 4),
                    WeekDateCell(4, 5),
                    WeekDateCell(4, 6),
                    WeekDateCell(4, 7),
                ),
                columnCourses = listOf(
                    listOf(),
                    listOf(),
                    listOf(
                        GridCourse(
                            id = "1",
                            title = "文献检索与论文写作",
                            room = "北205",
                            teacher = "杨敏",
                            weekday = 3,
                            startPeriod = 1,
                            endPeriod = 2,
                            span = 2,
                            color = Color(0xFF2F7AF0),
                        ),
                        GridCourse(
                            id = "2",
                            title = "数据结构",
                            room = "302",
                            teacher = "王老师",
                            weekday = 3,
                            startPeriod = 5,
                            endPeriod = 6,
                            span = 2,
                            color = Color(0xFF10B981),
                        ),
                    ),
                    listOf(),
                    listOf(
                        GridCourse(
                            id = "3",
                            title = "大学英语",
                            room = "A101",
                            teacher = "李老师",
                            weekday = 5,
                            startPeriod = 3,
                            endPeriod = 4,
                            span = 2,
                            color = Color(0xFFF59E0B),
                        ),
                    ),
                    listOf(),
                    listOf(),
                ),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        )
    }
}
