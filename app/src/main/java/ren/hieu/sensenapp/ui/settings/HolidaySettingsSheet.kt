package ren.hieu.sensenapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ren.hieu.sensenapp.data.model.HolidayMappingDto
import ren.hieu.sensenapp.ui.components.IosPrimaryButton
import ren.hieu.sensenapp.ui.components.iosPressable
import ren.hieu.sensenapp.ui.theme.IosBlue
import ren.hieu.sensenapp.ui.theme.IosCardBg
import ren.hieu.sensenapp.ui.theme.IosGroupedBg
import ren.hieu.sensenapp.ui.theme.IosLabel
import ren.hieu.sensenapp.ui.theme.IosRed
import ren.hieu.sensenapp.ui.theme.IosSecondaryLabel
import ren.hieu.sensenapp.ui.theme.IosSeparator
import ren.hieu.sensenapp.ui.theme.IosTertiaryLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HolidaySettingsSheet(
    mappings: List<HolidayMappingDto>,
    onAdd: (HolidayMappingDto) -> Unit,
    onRemove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var dateText by rememberSaveable { mutableStateOf("") }
    var isMakeup by rememberSaveable { mutableStateOf(false) }
    var makeupWeekday by rememberSaveable { mutableIntStateOf(1) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IosGroupedBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("假期与调休", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IosLabel)
            Text(
                "日期格式 yyyy-MM-dd。放假当天不显示课表；调休上课可选择按周几的课表显示。",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = IosSecondaryLabel,
                modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(IosCardBg)
                    .padding(16.dp),
            ) {
                Text("日期", fontSize = 13.sp, color = IosSecondaryLabel)
                BasicTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 17.sp, color = IosLabel),
                    cursorBrush = SolidColor(IosBlue),
                    decorationBox = { inner ->
                        if (dateText.isEmpty()) {
                            Text("2026-10-01", fontSize = 17.sp, color = IosTertiaryLabel)
                        }
                        inner()
                    },
                )
                Spacer(Modifier.height(12.dp))
                TypeToggle(
                    label = "放假",
                    selected = !isMakeup,
                    onClick = { isMakeup = false },
                )
                Spacer(Modifier.height(8.dp))
                TypeToggle(
                    label = "调休上课",
                    selected = isMakeup,
                    onClick = { isMakeup = true },
                )
                if (isMakeup) {
                    Spacer(Modifier.height(12.dp))
                    Text("按周几课表", fontSize = 13.sp, color = IosSecondaryLabel)
                    Row(
                        Modifier.padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        (1..7).forEach { day ->
                            val active = makeupWeekday == day
                            Text(
                                text = weekdayShort(day),
                                fontSize = 14.sp,
                                color = if (active) IosBlue else IosSecondaryLabel,
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) IosBlue.copy(alpha = 0.12f) else IosGroupedBg)
                                    .iosPressable { makeupWeekday = day }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            IosPrimaryButton(
                text = "添加规则",
                enabled = dateText.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
                loading = false,
                onClick = {
                    val mapping = if (isMakeup) {
                        HolidayMappingDto(
                            calendarDate = dateText.trim(),
                            kind = "work",
                            sourceWeekday = makeupWeekday,
                        )
                    } else {
                        HolidayMappingDto(
                            calendarDate = dateText.trim(),
                            kind = "off",
                            sourceWeekday = 0,
                        )
                    }
                    onAdd(mapping)
                    dateText = ""
                },
            )

            if (mappings.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text("已配置", fontSize = 13.sp, color = IosSecondaryLabel)
                Spacer(Modifier.height(8.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosCardBg),
                ) {
                    mappings.sortedBy { it.calendarDate }.forEachIndexed { index, item ->
                        if (index > 0) {
                            Spacer(
                                Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .background(IosSeparator),
                            )
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.calendarDate, fontSize = 17.sp, color = IosLabel)
                                Text(
                                    mappingSummary(item),
                                    fontSize = 13.sp,
                                    color = IosSecondaryLabel,
                                )
                            }
                            Text(
                                "删除",
                                fontSize = 15.sp,
                                color = IosRed,
                                modifier = Modifier.iosPressable { onRemove(item.calendarDate) },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "完成",
                fontSize = 17.sp,
                color = IosBlue,
                modifier = Modifier
                    .fillMaxWidth()
                    .iosPressable(onClick = onDismiss)
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TypeToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 16.sp,
        color = if (selected) IosBlue else IosLabel,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) IosBlue.copy(alpha = 0.1f) else IosGroupedBg)
            .iosPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    )
}

private fun weekdayShort(day: Int): String = when (day) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> day.toString()
}

private fun mappingSummary(item: HolidayMappingDto): String {
    val kind = item.kind.trim().lowercase()
    return if (kind == "off") {
        "放假"
    } else {
        "调休 · 按周${weekdayShort(item.sourceWeekday)}"
    }
}
