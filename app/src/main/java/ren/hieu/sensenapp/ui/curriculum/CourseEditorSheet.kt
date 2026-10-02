package ren.hieu.sensenapp.ui.curriculum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ren.hieu.sensenapp.data.model.CourseEventDto
import ren.hieu.sensenapp.ui.components.IosPrimaryButton
import ren.hieu.sensenapp.ui.components.iosPressable
import ren.hieu.sensenapp.ui.theme.IosBlue
import ren.hieu.sensenapp.ui.theme.IosCardBg
import ren.hieu.sensenapp.ui.theme.IosFieldBg
import ren.hieu.sensenapp.ui.theme.IosGroupedBg
import ren.hieu.sensenapp.ui.theme.IosLabel
import ren.hieu.sensenapp.ui.theme.IosRed
import ren.hieu.sensenapp.ui.theme.IosSecondaryLabel
import ren.hieu.sensenapp.ui.theme.IosTertiaryLabel
import ren.hieu.sensenapp.ui.theme.SenSenAccent

private const val SELECTABLE_WEEKS = 20

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CourseEditorSheet(
    request: CourseEditorRequest,
    maxWeek: Int,
    onSave: (CourseEventDto) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val existing = request.existing
    val weekLimit = maxWeek.coerceIn(1, SELECTABLE_WEEKS).coerceAtMost(SELECTABLE_WEEKS)
    var name by rememberSaveable { mutableStateOf(existing?.name ?: "") }
    var teacher by rememberSaveable { mutableStateOf(existing?.teacher ?: "") }
    var location by rememberSaveable { mutableStateOf(existing?.location ?: "") }
    var startPeriod by rememberSaveable(request.startPeriod) {
        mutableIntStateOf(existing?.sections?.minOrNull() ?: request.startPeriod)
    }
    var endPeriod by rememberSaveable(request.startPeriod) {
        mutableIntStateOf(existing?.sections?.maxOrNull() ?: request.startPeriod)
    }
    var selectedWeeks by remember(request.existing?.id, request.weekNum) {
        mutableStateOf(
            existing?.weeks?.filter { it in 1..weekLimit }?.toSet()
                ?: setOf(request.weekNum.coerceIn(1, weekLimit)),
        )
    }

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
            Text(
                text = if (existing == null) "新增课程" else "编辑课程",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = IosLabel,
            )
            Text(
                text = "周${weekdayLabel(request.weekday)} · 第${request.weekNum}周",
                fontSize = 14.sp,
                color = IosSecondaryLabel,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            EditorField(label = "课程名称", value = name, onValueChange = { name = it })
            Spacer(Modifier.height(10.dp))
            EditorField(label = "教师", value = teacher, onValueChange = { teacher = it })
            Spacer(Modifier.height(10.dp))
            EditorField(label = "教室", value = location, onValueChange = { location = it })
            Spacer(Modifier.height(14.dp))
            PeriodRow(
                startPeriod = startPeriod,
                endPeriod = endPeriod,
                onStartChange = { startPeriod = it.coerceIn(1, 10) },
                onEndChange = { endPeriod = it.coerceIn(1, 10) },
            )
            Spacer(Modifier.height(14.dp))
            WeekSelectionBlock(
                weekLimit = weekLimit,
                selectedWeeks = selectedWeeks,
                onSelectionChange = { selectedWeeks = it },
            )
            Spacer(Modifier.height(20.dp))
            IosPrimaryButton(
                text = "保存",
                enabled = name.isNotBlank() && selectedWeeks.isNotEmpty(),
                loading = false,
                onClick = {
                    val start = startPeriod.coerceIn(1, 10)
                    val end = endPeriod.coerceIn(start, 10)
                    val sections = (start..end).toList()
                    val weeks = selectedWeeks.sorted()
                    val event = CourseEventDto(
                        id = existing?.id ?: "",
                        name = name.trim(),
                        teacher = teacher.trim(),
                        location = location.trim(),
                        weekday = request.weekday,
                        sections = sections,
                        weeks = weeks,
                        weekParity = "all",
                    )
                    onSave(event)
                },
            )
            if (onDelete != null) {
                Spacer(Modifier.height(8.dp))
                BoxAction(text = "删除课程", color = IosRed, onClick = onDelete)
            }
            Spacer(Modifier.height(8.dp))
            BoxAction(text = "取消", color = IosBlue, onClick = onDismiss)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WeekSelectionBlock(
    weekLimit: Int,
    selectedWeeks: Set<Int>,
    onSelectionChange: (Set<Int>) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IosCardBg)
            .padding(16.dp),
    ) {
        Text("上课周次", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = IosLabel)
        Text(
            "已选 ${selectedWeeks.size} 周",
            fontSize = 13.sp,
            color = IosSecondaryLabel,
            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickWeekChip("全选") {
                onSelectionChange((1..weekLimit).toSet())
            }
            QuickWeekChip("单周") {
                onSelectionChange((1..weekLimit).filter { it % 2 == 1 }.toSet())
            }
            QuickWeekChip("双周") {
                onSelectionChange((1..weekLimit).filter { it % 2 == 0 }.toSet())
            }
            QuickWeekChip("清空") {
                onSelectionChange(emptySet())
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            (1..weekLimit).forEach { week ->
                val active = selectedWeeks.contains(week)
                Text(
                    text = week.toString(),
                    fontSize = 14.sp,
                    color = if (active) Color.White else IosLabel,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) SenSenAccent else IosFieldBg)
                        .iosPressable {
                            val next = selectedWeeks.toMutableSet()
                            if (active) next.remove(week) else next.add(week)
                            onSelectionChange(next)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun QuickWeekChip(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 14.sp,
        color = IosBlue,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(IosGroupedBg)
            .iosPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IosCardBg)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(label, fontSize = 13.sp, color = IosSecondaryLabel)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            singleLine = true,
            textStyle = TextStyle(fontSize = 17.sp, color = IosLabel),
            cursorBrush = SolidColor(IosBlue),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text("请输入", fontSize = 17.sp, color = IosTertiaryLabel)
                }
                inner()
            },
        )
    }
}

@Composable
private fun PeriodRow(
    startPeriod: Int,
    endPeriod: Int,
    onStartChange: (Int) -> Unit,
    onEndChange: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(IosCardBg)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("节次", fontSize = 17.sp, color = IosLabel, modifier = Modifier.weight(1f))
        PeriodStepper(value = startPeriod, onChange = onStartChange)
        Text("—", modifier = Modifier.padding(horizontal = 8.dp), color = IosSecondaryLabel)
        PeriodStepper(value = endPeriod, onChange = onEndChange)
    }
}

@Composable
private fun PeriodStepper(value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−") { onChange((value - 1).coerceIn(1, 10)) }
        Text(
            text = value.toString(),
            fontSize = 17.sp,
            color = IosLabel,
            modifier = Modifier.padding(horizontal = 10.dp),
        )
        StepButton("+") { onChange((value + 1).coerceIn(1, 10)) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Text(
        text = label,
        fontSize = 20.sp,
        color = IosBlue,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(IosFieldBg)
            .iosPressable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    )
}

@Composable
private fun BoxAction(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 17.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .iosPressable(onClick = onClick)
            .padding(vertical = 14.dp),
        textAlign = TextAlign.Center,
    )
}

private fun weekdayLabel(weekday: Int): String = when (weekday) {
    1 -> "一"
    2 -> "二"
    3 -> "三"
    4 -> "四"
    5 -> "五"
    6 -> "六"
    7 -> "日"
    else -> weekday.toString()
}
