package ren.hieu.sensenapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import ren.hieu.sensenapp.ui.components.AppBadge
import ren.hieu.sensenapp.ui.components.IosPrimaryButton
import ren.hieu.sensenapp.ui.components.iosPressable
import ren.hieu.sensenapp.ui.theme.IosBlue
import ren.hieu.sensenapp.ui.theme.IosCardBg
import ren.hieu.sensenapp.ui.theme.IosFieldBg
import ren.hieu.sensenapp.ui.theme.IosLabel
import ren.hieu.sensenapp.ui.theme.IosRed
import ren.hieu.sensenapp.ui.theme.IosSecondaryLabel
import ren.hieu.sensenapp.ui.theme.IosSeparator
import ren.hieu.sensenapp.ui.theme.IosTertiaryLabel

private const val BIND_CODE_LENGTH = 32

private fun sanitizeBindCode(raw: String): String =
    raw.filter { ch -> ch.isDigit() || ch in 'a'..'f' || ch in 'A'..'F' }.take(BIND_CODE_LENGTH)

@Composable
fun BindScreen(
    uiState: ScheduleUiState,
    onBind: (String) -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IosCardBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        AppBadge(size = 84.dp)
        Spacer(Modifier.height(20.dp))
        Text(
            text = "森森课表",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = IosLabel,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "输入绑定码，把课表同步到这台设备",
            fontSize = 15.sp,
            color = IosSecondaryLabel,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        BindCodeField(code = code, onCodeChange = { code = it }, enabled = !uiState.isLoading)
        BindErrorText(uiState.errorMessage)
        Spacer(Modifier.weight(1.4f))
        IosPrimaryButton(
            text = "绑定并同步",
            enabled = code.length == BIND_CODE_LENGTH,
            loading = uiState.isLoading,
            onClick = { onBind(code) },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BindCodeField(
    code: String,
    onCodeChange: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(IosFieldBg)
                .padding(start = 14.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicTextField(
                value = code,
                onValueChange = { onCodeChange(sanitizeBindCode(it)) },
                enabled = enabled,
                modifier = Modifier.weight(1f),
                singleLine = true,
                cursorBrush = SolidColor(IosBlue),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    color = IosLabel,
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Ascii,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Done,
                ),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (code.isEmpty()) {
                            Text("32 位绑定码", fontSize = 15.sp, color = IosTertiaryLabel)
                        }
                        inner()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .iosPressable(enabled = enabled) {
                        scope.launch {
                            val text = clipboard.getClipEntry()
                                ?.clipData
                                ?.takeIf { it.itemCount > 0 }
                                ?.getItemAt(0)
                                ?.text
                                ?.toString()
                                .orEmpty()
                            if (text.isNotBlank()) onCodeChange(sanitizeBindCode(text))
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Text("粘贴", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = IosBlue)
            }
        }
        Text(
            text = "${code.length}/$BIND_CODE_LENGTH",
            fontSize = 12.sp,
            color = IosTertiaryLabel,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, end = 4.dp),
        )
    }
}

@Composable
private fun BindErrorText(message: String?) {
    if (message.isNullOrBlank()) return
    Text(
        text = message,
        color = IosRed,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
    )
}

@Composable
fun BindCodeDialog(
    uiState: ScheduleUiState,
    onBind: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    Dialog(
        onDismissRequest = { if (!uiState.isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(IosCardBg)
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppBadge(size = 52.dp)
            Spacer(Modifier.height(14.dp))
            Text(
                text = if (uiState.isLinked) "更换绑定码" else "绑定课表",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = IosLabel,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (uiState.isLinked) {
                    "绑定新的课表后，当前设备上的课表会被替换"
                } else {
                    "输入绑定码，把课表同步到这台设备"
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = IosSecondaryLabel,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            BindCodeField(code = code, onCodeChange = { code = it }, enabled = !uiState.isLoading)
            BindErrorText(uiState.errorMessage)
            Spacer(Modifier.height(16.dp))
            IosPrimaryButton(
                text = "绑定",
                enabled = code.length == BIND_CODE_LENGTH,
                loading = uiState.isLoading,
                onClick = { onBind(code) },
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .iosPressable(enabled = !uiState.isLoading, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("取消", fontSize = 17.sp, color = IosBlue)
            }
        }
    }
}

@Composable
fun UnbindConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    "解除绑定？",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = IosLabel,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "将清除本机的课表数据和已安排的课前提醒，之后可随时使用绑定码重新绑定。",
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
                AlertAction(
                    text = "取消",
                    color = IosBlue,
                    bold = true,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(IosSeparator),
                )
                AlertAction(
                    text = "解除绑定",
                    color = IosRed,
                    bold = false,
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AlertAction(
    text: String,
    color: Color,
    bold: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .iosPressable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            fontSize = 17.sp,
            color = color,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
