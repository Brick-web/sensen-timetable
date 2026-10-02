package ren.hieu.sensenapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ren.hieu.sensenapp.ui.theme.IosLabel
import ren.hieu.sensenapp.ui.theme.SenSenAccent

enum class SettingsSyncAction {
    CheckUpdate,
    RefreshHolidays,
    RefreshWidgets,
}

data class SettingsResultDialog(
    val title: String,
    val message: String,
    val confirmLabel: String? = null,
    val confirmUrl: String? = null,
)

@Composable
fun SettingsSyncOverlay(action: SettingsSyncAction?) {
    if (action == null) return
    val label = when (action) {
        SettingsSyncAction.CheckUpdate -> "正在检查更新…"
        SettingsSyncAction.RefreshHolidays -> "正在刷新调休…"
        SettingsSyncAction.RefreshWidgets -> "正在刷新小组件…"
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(14.dp))
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CircularProgressIndicator(color = SenSenAccent)
            Spacer(Modifier.height(16.dp))
            Text(label, fontSize = 15.sp, color = IosLabel, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun SettingsResultDialogHost(
    dialog: SettingsResultDialog?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    if (dialog == null) return
    val confirmLabel = dialog.confirmLabel
    val confirmUrl = dialog.confirmUrl?.trim().orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialog.title) },
        text = { Text(dialog.message) },
        confirmButton = {
            if (confirmLabel != null && confirmUrl.isNotEmpty()) {
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(confirmUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                        onDismiss()
                    },
                ) {
                    Text(confirmLabel)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("好")
                }
            }
        },
        dismissButton = if (confirmLabel != null && confirmUrl.isNotEmpty()) {
            {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        } else {
            null
        },
    )
}
