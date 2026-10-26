package ren.hieu.sensenapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ren.hieu.sensenapp.widget.WidgetRefreshScheduler
import ren.hieu.sensenapp.widget.WidgetRefreshWorker

/** 重启、升级及手动改时后重新读取本地课表并恢复刷新链。 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action !in setOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED,
                Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED, Intent.ACTION_DATE_CHANGED,
                "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED")) return
        WidgetRefreshScheduler.schedulePeriodic(context)
        WidgetRefreshWorker.enqueue(context)
    }
}
