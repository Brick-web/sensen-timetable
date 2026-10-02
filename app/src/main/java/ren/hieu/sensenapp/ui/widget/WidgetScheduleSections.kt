package ren.hieu.sensenapp.ui.widget

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ren.hieu.sensenapp.domain.TodayCourseSection
import ren.hieu.sensenapp.ui.theme.SenSenTextPrimary
import ren.hieu.sensenapp.ui.theme.SenSenTextSecondary
import java.time.LocalDateTime

/**
 * 日课表小组件正文（Compose 预览与 App 内展示共用）。
 * 桌面 Glance 小组件在 [ren.hieu.sensenapp.widget.GlanceScheduleWidgets] 中保持相同结构与样式。
 */
@Composable
fun WidgetScheduleSectionList(
    sections: List<TodayCourseSection>,
    emptyHint: String,
    now: LocalDateTime,
    sectionGap: androidx.compose.ui.unit.Dp = 8.dp,
    rowGap: androidx.compose.ui.unit.Dp = 4.dp,
) {
    sections.forEachIndexed { index, section ->
        if (index > 0) Spacer(Modifier.height(sectionGap))
        val title = section.title.trim()
        if (title.isNotEmpty()) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = SenSenTextPrimary,
            )
            Spacer(Modifier.height(6.dp))
        }
        when {
            section.slots.isNotEmpty() -> {
                section.slots.forEachIndexed { slotIndex, slot ->
                    WidgetSlotRow(slot, now, emptyHint, Modifier.fillMaxWidth())
                    if (slotIndex < section.slots.lastIndex) {
                        Spacer(Modifier.height(rowGap))
                    }
                }
            }
            section.items.isEmpty() -> {
                Text(text = emptyHint, fontSize = 13.sp, color = SenSenTextSecondary)
            }
            else -> {
                section.items.forEachIndexed { itemIndex, item ->
                    WidgetCourseRow(item, now, Modifier.fillMaxWidth())
                    if (itemIndex < section.items.lastIndex) {
                        Spacer(Modifier.height(rowGap))
                    }
                }
            }
        }
    }
}
