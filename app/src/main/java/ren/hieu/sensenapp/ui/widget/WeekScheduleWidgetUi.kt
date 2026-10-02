package ren.hieu.sensenapp.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ren.hieu.sensenapp.domain.GridCourse
import ren.hieu.sensenapp.ui.theme.SenSenAccent
import ren.hieu.sensenapp.ui.theme.SenSenGridLine
import ren.hieu.sensenapp.ui.theme.SenSenTextPrimary
import ren.hieu.sensenapp.ui.theme.SenSenTextSecondary
import ren.hieu.sensenapp.widget.WeekWidgetData

private val TodayColumnBg = Color(0xFFEEF4FF)
private val WeekHeaderCellHeight = 34.dp

@Composable
fun WeekScheduleWidgetCard(
    data: WeekWidgetData?,
    modifier: Modifier = Modifier,
) {
    WidgetShellCard(modifier = modifier) {
        if (data == null) {
            Text("未绑定课表", fontSize = 13.sp, color = SenSenTextSecondary)
            return@WidgetShellCard
        }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "第 ${data.weekNum} 周",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SenSenTextPrimary,
                )
                if (data.weekRangeText.isNotBlank()) {
                    Text(
                        text = data.weekRangeText,
                        fontSize = 11.sp,
                        color = SenSenTextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            WeekGridHeader(data)
            Spacer(Modifier.height(4.dp))
        WeekGridBody(data)
    }
}

@Composable
private fun WeekGridHeader(data: WeekWidgetData) {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFAFAFA))
            .padding(vertical = 6.dp),
    ) {
        (1..7).forEach { day ->
            val isToday = day == data.todayWeekday
            val dateCell = data.weekDates.getOrNull(day - 1)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(WeekHeaderCellHeight)
                    .padding(horizontal = 1.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isToday) TodayColumnBg else Color(0xFFFAFAFA)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = labels[day - 1],
                    fontSize = 10.sp,
                    color = if (isToday) SenSenAccent else SenSenTextSecondary,
                    fontWeight = FontWeight.Medium,
                )
                if (dateCell != null) {
                    Text(
                        text = "${dateCell.day}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isToday) SenSenAccent else SenSenTextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeekGridBody(data: WeekWidgetData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .border(1.dp, SenSenGridLine, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White),
    ) {
        (1..7).forEach { day ->
            val isToday = day == data.todayWeekday
            val dayCourses = data.columnCourses.getOrNull(day - 1).orEmpty()
                .sortedBy { it.startPeriod }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .then(
                        if (isToday) Modifier.background(TodayColumnBg.copy(alpha = 0.35f))
                        else Modifier,
                    )
                    .padding(horizontal = 2.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (dayCourses.isEmpty()) {
                    Text(
                        text = "—",
                        fontSize = 10.sp,
                        color = SenSenTextSecondary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    dayCourses.take(3).forEach { course ->
                        WeekCourseChip(course)
                        Spacer(Modifier.height(3.dp))
                    }
                    if (dayCourses.size > 3) {
                        Text(
                            text = "+${dayCourses.size - 3}",
                            fontSize = 9.sp,
                            color = SenSenTextSecondary,
                        )
                    }
                }
            }
            if (day < 7) {
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(132.dp)
                        .background(SenSenGridLine),
                )
            }
        }
    }
}

@Composable
private fun WeekCourseChip(course: GridCourse) {
    val chipColor = course.color.copy(alpha = 0.92f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(chipColor)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = course.title,
            fontSize = 8.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 9.sp,
            textAlign = TextAlign.Center,
        )
    }
}
