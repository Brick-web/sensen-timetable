package ren.hieu.sensenapp.ui.widget

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import ren.hieu.sensenapp.data.model.TodaySchedule
import ren.hieu.sensenapp.domain.TodayCourseGrouper
import ren.hieu.sensenapp.domain.WidgetSectionMode
import ren.hieu.sensenapp.ui.theme.SenSenTextSecondary
import java.time.LocalDateTime

@Composable
fun TodayScheduleWidgetCard(
    today: TodaySchedule?,
    modifier: Modifier = Modifier,
    sectionMode: WidgetSectionMode = WidgetSectionMode.CURRENT_PERIOD,
    now: LocalDateTime = LocalDateTime.now(TodayCourseGrouper.zone),
) {
    WidgetShellCard(modifier = modifier) {
        if (today == null) {
            Text("未绑定课表", fontSize = 13.sp, color = SenSenTextSecondary)
            return@WidgetShellCard
        }
        val sections = TodayCourseGrouper.groupForDisplay(today.courses, sectionMode, now)
        val emptyHint = TodayCourseGrouper.emptySectionHint(sectionMode)
        WidgetScheduleSectionList(sections, emptyHint, now)
    }
}
