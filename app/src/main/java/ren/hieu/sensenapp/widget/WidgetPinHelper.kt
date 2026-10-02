package ren.hieu.sensenapp.widget

import android.app.AlertDialog
import android.app.AppOpsManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.widget.Toast
import ren.hieu.sensenapp.R

enum class ScheduleWidgetKind {
    PERIOD,
    FULL_DAY,
    WEEK,
}

object WidgetPinHelper {
    private const val PIN_CALLBACK_BASE = 5000

    // MIUI / HyperOS 私有权限「桌面快捷方式」的 AppOps 编号
    private const val MIUI_OP_INSTALL_SHORTCUT = 10017

    /**
     * 必须在 App 处于前台时调用：国产 ROM 会丢弃后台应用发起的固定请求，
     * 确认弹窗由桌面绘制并覆盖在当前 App 之上。
     */
    fun requestPin(context: Context, kind: ScheduleWidgetKind) {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        if (!manager.isRequestPinAppWidgetSupported) {
            Toast.makeText(context, R.string.widget_pin_not_supported, Toast.LENGTH_LONG).show()
            return
        }
        if (isXiaomi() && !isMiuiShortcutPermissionGranted(appContext)) {
            showPermissionDialog(context)
            return
        }

        val component = when (kind) {
            ScheduleWidgetKind.PERIOD -> ComponentName(appContext, PeriodScheduleGlanceReceiver::class.java)
            ScheduleWidgetKind.FULL_DAY -> ComponentName(appContext, FullDayScheduleGlanceReceiver::class.java)
            ScheduleWidgetKind.WEEK -> ComponentName(appContext, WeekScheduleGlanceReceiver::class.java)
        }
        val callback = PendingIntent.getBroadcast(
            appContext,
            PIN_CALLBACK_BASE + kind.ordinal,
            Intent(appContext, WidgetPinResultReceiver::class.java).apply {
                action = WidgetPinResultReceiver.ACTION_WIDGET_PINNED
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val accepted = runCatching { manager.requestPinAppWidget(component, null, callback) }
            .getOrDefault(false)
        if (!accepted) {
            showPermissionDialog(context)
        } else if (isDomesticRom()) {
            Toast.makeText(context, R.string.widget_pin_added_hint, Toast.LENGTH_LONG).show()
        }
    }

    private fun showPermissionDialog(context: Context) {
        if (context.findActivity() == null) {
            Toast.makeText(context, R.string.widget_pin_permission_message, Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(context)
            .setTitle(R.string.widget_pin_permission_title)
            .setMessage(R.string.widget_pin_permission_message)
            .setPositiveButton(R.string.widget_pin_permission_go) { _, _ -> openPermissionSettings(context) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun openPermissionSettings(context: Context) {
        val pkg = context.packageName
        val candidates = buildList {
            if (isXiaomi()) {
                add(
                    Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                        setClassName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.permissions.PermissionsEditorActivity",
                        )
                        putExtra("extra_pkgname", pkg)
                    },
                )
            }
            add(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null)))
        }
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isSuccess) return
        }
    }

    private fun isMiuiShortcutPermissionGranted(context: Context): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        return runCatching {
            val method = AppOpsManager::class.java.getMethod(
                "checkOpNoThrow",
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                String::class.java,
            )
            val mode = method.invoke(ops, MIUI_OP_INSTALL_SHORTCUT, Process.myUid(), context.packageName) as Int
            mode == AppOpsManager.MODE_ALLOWED
        }.getOrDefault(true)
    }

    private fun isXiaomi(): Boolean {
        val brand = Build.MANUFACTURER.lowercase()
        return brand.contains("xiaomi") || brand.contains("redmi") || brand.contains("poco")
    }

    private fun isDomesticRom(): Boolean {
        val brand = Build.MANUFACTURER.lowercase()
        return listOf("xiaomi", "redmi", "oppo", "realme", "oneplus", "vivo", "iqoo", "huawei", "honor", "meizu")
            .any { brand.contains(it) }
    }

    private fun Context.findActivity(): android.app.Activity? {
        var ctx: Context? = this
        while (ctx is ContextWrapper) {
            if (ctx is android.app.Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}
