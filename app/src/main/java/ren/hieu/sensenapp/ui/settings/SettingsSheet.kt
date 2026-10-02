package ren.hieu.sensenapp.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ren.hieu.sensenapp.R
import ren.hieu.sensenapp.ui.ScheduleUiState
import ren.hieu.sensenapp.ui.components.AppBadge
import ren.hieu.sensenapp.ui.components.IosSwitch
import ren.hieu.sensenapp.ui.components.iosPressable
import ren.hieu.sensenapp.ui.theme.IosBlue
import ren.hieu.sensenapp.ui.theme.IosCardBg
import ren.hieu.sensenapp.ui.theme.IosChevron
import ren.hieu.sensenapp.ui.theme.IosFieldBg
import ren.hieu.sensenapp.ui.theme.IosGreen
import ren.hieu.sensenapp.ui.theme.IosGroupedBg
import ren.hieu.sensenapp.ui.theme.IosIndigo
import ren.hieu.sensenapp.ui.theme.IosLabel
import ren.hieu.sensenapp.ui.theme.IosOrange
import ren.hieu.sensenapp.ui.theme.IosPink
import ren.hieu.sensenapp.ui.theme.IosPurple
import ren.hieu.sensenapp.ui.theme.IosRed
import ren.hieu.sensenapp.ui.theme.IosSecondaryLabel
import ren.hieu.sensenapp.ui.theme.IosSeparator
import ren.hieu.sensenapp.ui.theme.IosTeal
import ren.hieu.sensenapp.widget.ScheduleWidgetKind
import ren.hieu.sensenapp.widget.WidgetPinHelper

private val GroupShape = RoundedCornerShape(12.dp)
private val IconSize = 29.dp
private val RowHorizontalPadding = 16.dp
private val SeparatorInset = RowHorizontalPadding + IconSize + 14.dp

private val cardStyleLabels = mapOf("classic" to "经典", "minimal" to "简约")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    uiState: ScheduleUiState,
    onOpenStyle: () -> Unit,
    onOpenHolidaySettings: () -> Unit,
    onOpenTermSettings: () -> Unit,
    onRefreshCloud: () -> Unit,
    onRefreshWidgets: () -> Unit,
    onClassReminderChanged: (Boolean) -> Unit,
    onHolidayAdjustmentsChanged: (Boolean) -> Unit,
    onOpenResetConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IosGroupedBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        dragHandle = { SheetGrabber() },
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding(),
        ) {
            SheetHeader(title = "设置", onDone = onDismiss)

            AccountCard(uiState)

            SectionHeader("课表")
            SettingsGroup {
                SettingsRow(
                    title = "卡片样式",
                    icon = SettingsGlyph.Cards,
                    iconColor = IosPurple,
                    value = cardStyleLabels[uiState.cardStyle],
                    showChevron = false,
                    onClick = { /* 卡片样式选择暂时关闭 */ },
                )
                RowSeparator()
                SettingsRow(
                    title = stringResource(R.string.class_reminder_title),
                    icon = SettingsGlyph.Bell,
                    iconColor = IosRed,
                    showChevron = false,
                    onClick = { onClassReminderChanged(!uiState.classReminderEnabled) },
                    trailing = {
                        IosSwitch(
                            checked = uiState.classReminderEnabled,
                            onCheckedChange = onClassReminderChanged,
                        )
                    },
                )
                RowSeparator()
                SettingsRow(
                    title = "调休与假期",
                    icon = SettingsGlyph.Calendar,
                    iconColor = IosOrange,
                    showChevron = false,
                    onClick = { onHolidayAdjustmentsChanged(!uiState.holidayAdjustmentsEnabled) },
                    trailing = {
                        IosSwitch(
                            checked = uiState.holidayAdjustmentsEnabled,
                            onCheckedChange = onHolidayAdjustmentsChanged,
                        )
                    },
                )
                RowSeparator()
                SettingsRow(
                    title = "配置假期与调休",
                    icon = SettingsGlyph.Calendar,
                    iconColor = IosTeal,
                    onClick = onOpenHolidaySettings,
                )
                RowSeparator()
                SettingsRow(
                    title = "第一周周一",
                    icon = SettingsGlyph.Calendar,
                    iconColor = IosIndigo,
                    value = uiState.export?.term?.firstMonday?.takeIf { it.isNotBlank() },
                    onClick = onOpenTermSettings,
                )
            }
            SectionFooter("开启后会在每节课开始前 15 分钟推送通知；假期规则保存在本机，不依赖云端")

            SectionHeader("桌面小组件")
            SettingsGroup {
                WidgetRow(
                    title = stringResource(R.string.widget_period_label),
                    subtitle = stringResource(R.string.widget_period_desc),
                    iconColor = IosTeal,
                    onClick = { WidgetPinHelper.requestPin(context, ScheduleWidgetKind.PERIOD) },
                )
                RowSeparator()
                WidgetRow(
                    title = stringResource(R.string.widget_full_day_label),
                    subtitle = stringResource(R.string.widget_full_day_desc),
                    iconColor = IosIndigo,
                    onClick = { WidgetPinHelper.requestPin(context, ScheduleWidgetKind.FULL_DAY) },
                )
                RowSeparator()
                WidgetRow(
                    title = stringResource(R.string.widget_week_label),
                    subtitle = stringResource(R.string.widget_week_desc),
                    iconColor = IosPink,
                    onClick = { WidgetPinHelper.requestPin(context, ScheduleWidgetKind.WEEK) },
                )
            }
            SectionFooter(stringResource(R.string.settings_widgets_hint))

            SectionHeader("其他")
            SettingsGroup {
                SettingsRow(
                    title = stringResource(R.string.check_update),
                    icon = SettingsGlyph.Download,
                    iconColor = IosGreen,
                    onClick = onRefreshCloud,
                )
                RowSeparator()
                SettingsRow(
                    title = "刷新小组件",
                    icon = SettingsGlyph.Widgets,
                    iconColor = IosTeal,
                    onClick = onRefreshWidgets,
                )
            }

            Spacer(Modifier.height(28.dp))
            SettingsGroup {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .iosPressable(onClick = onOpenResetConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("清空课表", fontSize = 17.sp, color = IosRed)
                }
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

@Composable
private fun SheetGrabber() {
    Box(
        Modifier
            .padding(top = 6.dp, bottom = 2.dp)
            .size(width = 36.dp, height = 5.dp)
            .clip(CircleShape)
            .background(Color(0x4D3C3C43)),
    )
}

@Composable
private fun SheetHeader(title: String, onDone: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = IosLabel,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .clip(RoundedCornerShape(8.dp))
                .iosPressable(onClick = onDone)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text("完成", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = IosBlue)
        }
    }
}

@Composable
private fun AccountCard(uiState: ScheduleUiState) {
    val export = uiState.export
    val courseCount = export?.events?.map { it.name }?.filter { it.isNotBlank() }?.distinct()?.size ?: 0
    val subtitle = buildList {
        add("本地课表")
        uiState.serverCurrentWeek?.let { add("当前第${it}周") }
        if (courseCount > 0) add("${courseCount}门课程")
    }.joinToString(" · ")
    SettingsGroup {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RowHorizontalPadding, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBadge(size = 56.dp)
            Column(
                Modifier
                    .padding(start = 14.dp)
                    .weight(1f),
            ) {
                Text(
                    export?.school?.takeIf { it.isNotBlank() } ?: "我的课表",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    subtitle,
                    fontSize = 13.sp,
                    color = IosSecondaryLabel,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        color = IosSecondaryLabel,
        modifier = Modifier.padding(start = 36.dp, end = 36.dp, top = 28.dp, bottom = 7.dp),
    )
}

@Composable
private fun SectionFooter(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        color = IosSecondaryLabel,
        modifier = Modifier.padding(start = 36.dp, end = 36.dp, top = 7.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(GroupShape)
            .background(IosCardBg),
        content = content,
    )
}

@Composable
private fun RowSeparator() {
    Box(
        Modifier
            .padding(start = SeparatorInset)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(IosSeparator),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    icon: SettingsGlyph,
    iconColor: Color,
    onClick: () -> Unit,
    value: String? = null,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .iosPressable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RowHorizontalPadding)
                .align(Alignment.CenterStart),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        GlyphTile(icon, iconColor)
        Text(
            title,
            fontSize = 17.sp,
            color = IosLabel,
            modifier = Modifier
                .padding(start = 14.dp)
                .weight(1f),
        )
        if (value != null) {
            Text(value, fontSize = 17.sp, color = IosSecondaryLabel)
        }
        trailing?.invoke()
        if (showChevron) {
            Chevron(Modifier.padding(start = 10.dp))
        }
        }
    }
}

@Composable
private fun WidgetRow(
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .iosPressable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = RowHorizontalPadding, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        GlyphTile(SettingsGlyph.Widgets, iconColor)
        Column(
            Modifier
                .padding(start = 14.dp, end = 12.dp)
                .weight(1f),
        ) {
            Text(title, fontSize = 17.sp, color = IosLabel)
            Text(
                subtitle,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = IosSecondaryLabel,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Box(
            Modifier
                .clip(CircleShape)
                .background(IosFieldBg)
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Text("添加", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = IosBlue)
        }
        }
    }
}

@Composable
private fun Chevron(modifier: Modifier = Modifier) {
    Canvas(modifier.size(width = 8.dp, height = 14.dp)) {
        val stroke = 2.dp.toPx()
        val path = Path().apply {
            moveTo(stroke / 2, stroke / 2)
            lineTo(size.width - stroke / 2, size.height / 2)
            lineTo(stroke / 2, size.height - stroke / 2)
        }
        drawPath(path, IosChevron, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private enum class SettingsGlyph { Cards, Bell, Widgets, Download, Calendar, Key }

@Composable
private fun GlyphTile(glyph: SettingsGlyph, color: Color) {
    Box(
        modifier = Modifier
            .size(IconSize)
            .clip(RoundedCornerShape(7.dp))
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(19.dp)) { drawGlyph(glyph) }
    }
}

private fun DrawScope.drawGlyph(glyph: SettingsGlyph) {
    val w = size.width
    val stroke = Stroke(width = w * 0.1f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(x: Float, y: Float) = Offset(x * w, y * w)
    val white = Color.White
    when (glyph) {
        SettingsGlyph.Cards -> {
            drawRoundRect(
                white.copy(alpha = 0.55f),
                topLeft = p(0.22f, 0.08f),
                size = Size(w * 0.56f, w * 0.2f),
                cornerRadius = CornerRadius(w * 0.08f),
            )
            drawRoundRect(
                white,
                topLeft = p(0.1f, 0.34f),
                size = Size(w * 0.8f, w * 0.56f),
                cornerRadius = CornerRadius(w * 0.12f),
            )
        }
        SettingsGlyph.Bell -> {
            val bell = Path().apply {
                moveTo(0.5f * w, 0.1f * w)
                cubicTo(0.3f * w, 0.1f * w, 0.24f * w, 0.28f * w, 0.24f * w, 0.44f * w)
                lineTo(0.24f * w, 0.6f * w)
                lineTo(0.14f * w, 0.74f * w)
                lineTo(0.86f * w, 0.74f * w)
                lineTo(0.76f * w, 0.6f * w)
                lineTo(0.76f * w, 0.44f * w)
                cubicTo(0.76f * w, 0.28f * w, 0.7f * w, 0.1f * w, 0.5f * w, 0.1f * w)
                close()
            }
            drawPath(bell, white)
            drawCircle(white, radius = w * 0.1f, center = p(0.5f, 0.84f))
        }
        SettingsGlyph.Widgets -> {
            val cell = w * 0.36f
            val gap = w * 0.1f
            val start = (w - cell * 2 - gap) / 2
            for (row in 0..1) for (col in 0..1) {
                drawRoundRect(
                    white,
                    topLeft = Offset(start + col * (cell + gap), start + row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(w * 0.09f),
                )
            }
        }
        SettingsGlyph.Download -> {
            drawLine(white, p(0.5f, 0.1f), p(0.5f, 0.6f), stroke.width, StrokeCap.Round)
            val head = Path().apply {
                moveTo(0.3f * w, 0.42f * w)
                lineTo(0.5f * w, 0.62f * w)
                lineTo(0.7f * w, 0.42f * w)
            }
            drawPath(head, white, style = stroke)
            val tray = Path().apply {
                moveTo(0.14f * w, 0.62f * w)
                lineTo(0.14f * w, 0.86f * w)
                lineTo(0.86f * w, 0.86f * w)
                lineTo(0.86f * w, 0.62f * w)
            }
            drawPath(tray, white, style = stroke)
        }
        SettingsGlyph.Calendar -> {
            drawRoundRect(
                white,
                topLeft = p(0.12f, 0.2f),
                size = Size(w * 0.76f, w * 0.68f),
                cornerRadius = CornerRadius(w * 0.12f),
                style = stroke,
            )
            drawLine(white, p(0.12f, 0.4f), p(0.88f, 0.4f), stroke.width)
            drawLine(white, p(0.34f, 0.08f), p(0.34f, 0.26f), stroke.width, StrokeCap.Round)
            drawLine(white, p(0.66f, 0.08f), p(0.66f, 0.26f), stroke.width, StrokeCap.Round)
            listOf(0.34f, 0.5f, 0.66f).forEach { x ->
                drawCircle(white, radius = w * 0.055f, center = p(x, 0.62f))
            }
        }
        SettingsGlyph.Key -> {
            drawCircle(white, radius = w * 0.17f, center = p(0.3f, 0.5f), style = stroke)
            drawLine(white, p(0.47f, 0.5f), p(0.9f, 0.5f), stroke.width, StrokeCap.Round)
            drawLine(white, p(0.74f, 0.5f), p(0.74f, 0.66f), stroke.width, StrokeCap.Round)
            drawLine(white, p(0.86f, 0.5f), p(0.86f, 0.62f), stroke.width, StrokeCap.Round)
        }
    }
}

private fun formatSyncTime(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return raw.replace('T', ' ').take(16)
}
