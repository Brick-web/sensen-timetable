package ren.hieu.sensenapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ren.hieu.sensenapp.ui.components.IosPrimaryButton
import ren.hieu.sensenapp.ui.components.iosPressable
import ren.hieu.sensenapp.ui.theme.IosBlue
import ren.hieu.sensenapp.ui.theme.IosCardBg
import ren.hieu.sensenapp.ui.theme.IosGroupedBg
import ren.hieu.sensenapp.ui.theme.IosLabel
import ren.hieu.sensenapp.ui.theme.IosSecondaryLabel
import ren.hieu.sensenapp.ui.theme.IosTertiaryLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermSettingsSheet(
    currentFirstMonday: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var dateText by rememberSaveable(currentFirstMonday) { mutableStateOf(currentFirstMonday) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = IosGroupedBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text("学期开始", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = IosLabel)
            Text(
                "设置第 1 周周一的日期（yyyy-MM-dd）。若不是周一，将自动取该日所在周的周一。",
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
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text("第一周周一", fontSize = 13.sp, color = IosSecondaryLabel)
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
                            Text("2026-02-24", fontSize = 17.sp, color = IosTertiaryLabel)
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
            IosPrimaryButton(
                text = "保存",
                enabled = dateText.matches(Regex("\\d{4}-\\d{2}-\\d{2}")),
                loading = false,
                onClick = { onSave(dateText.trim()) },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "取消",
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
