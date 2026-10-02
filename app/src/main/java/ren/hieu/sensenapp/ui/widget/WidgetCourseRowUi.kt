package ren.hieu.sensenapp.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ren.hieu.sensenapp.data.model.DayCourseItem
import ren.hieu.sensenapp.domain.CourseTimeState
import ren.hieu.sensenapp.domain.TodayCourseGrouper
import ren.hieu.sensenapp.domain.WidgetCourseSlot
import ren.hieu.sensenapp.ui.theme.SenSenAccent
import ren.hieu.sensenapp.ui.theme.SenSenTextPrimary
import ren.hieu.sensenapp.ui.theme.SenSenTextSecondary
import java.time.LocalDateTime

val WidgetCurrentRowBg = Color(0xFFE8F1FE)
val WidgetPastText = Color(0xFFB8BEC8)
val WidgetPastBar = Color(0xFFD1D5DB)

@Composable
fun WidgetSlotRow(
    slot: WidgetCourseSlot,
    now: LocalDateTime,
    emptyHint: String,
    modifier: Modifier = Modifier,
) {
    if (slot.course == null) {
        WidgetEmptySlotRow(slot, now, emptyHint, modifier)
    } else {
        WidgetCourseRow(slot.course, now, modifier)
    }
}

@Composable
private fun WidgetEmptySlotRow(
    slot: WidgetCourseSlot,
    now: LocalDateTime,
    emptyHint: String,
    modifier: Modifier = Modifier,
) {
    val state = TodayCourseGrouper.slotTimeState(slot, now)
    val muted = state == CourseTimeState.PAST || slot.slotStart == "—"
    val textColor = if (muted) WidgetPastText else SenSenTextSecondary
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WidgetTimeColumn(
            start = slot.slotStart,
            end = slot.slotEnd,
            primaryColor = if (muted) WidgetPastText else SenSenTextPrimary,
            dashColor = if (muted) WidgetPastText else SenSenTextSecondary,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(WidgetPastBar),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = emptyHint,
            fontSize = 13.sp,
            color = textColor,
        )
    }
}

@Composable
fun WidgetCourseRow(
    item: DayCourseItem,
    now: LocalDateTime,
    modifier: Modifier = Modifier,
) {
    val state = TodayCourseGrouper.timeState(item, now)
    val isPast = state == CourseTimeState.PAST
    val isCurrent = state == CourseTimeState.CURRENT
    val titleColor = if (isPast) WidgetPastText else SenSenTextPrimary
    val metaColor = if (isPast) WidgetPastText else SenSenTextSecondary
    val barColor = when {
        isPast -> WidgetPastBar
        isCurrent -> SenSenAccent
        else -> SenSenAccent.copy(alpha = 0.55f)
    }
    val rowModifier = if (isCurrent) {
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(WidgetCurrentRowBg)
            .padding(vertical = 1.dp, horizontal = 2.dp)
    } else {
        modifier
    }
    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        WidgetTimeColumn(
            start = item.sectionStart,
            end = item.sectionEnd,
            primaryColor = titleColor,
            dashColor = metaColor,
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(barColor),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = item.event.name,
                fontSize = 15.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp,
            )
            Text(
                text = TodayCourseGrouper.metaLine(item),
                fontSize = 11.sp,
                color = metaColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 13.sp,
            )
        }
    }
}

@Composable
fun WidgetTimeColumn(
    start: String,
    end: String,
    primaryColor: Color,
    dashColor: Color,
) {
    Column(
        modifier = Modifier
            .width(34.dp)
            .height(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = start,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            lineHeight = 8.sp,
        )
        Text(
            text = end,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor,
            lineHeight = 8.sp,
        )
    }
}
