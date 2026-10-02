package ren.hieu.sensenapp.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import ren.hieu.sensenapp.data.repository.AppUpdatePrompt

@Composable
fun AppUpdateDialog(
    prompt: AppUpdatePrompt?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    if (prompt == null) return
    AlertDialog(
        onDismissRequest = {
            if (!prompt.force) onDismiss()
        },
        title = { Text("更新 ${prompt.versionName}") },
        text = if (prompt.releaseNotes.isNotBlank()) {
            { Text(prompt.releaseNotes) }
        } else {
            null
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val url = prompt.apkUrl.trim()
                    if (url.isNotEmpty()) {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            },
                        )
                    }
                    if (!prompt.force) onDismiss()
                },
            ) {
                Text("更新")
            }
        },
        dismissButton = if (!prompt.force) {
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
