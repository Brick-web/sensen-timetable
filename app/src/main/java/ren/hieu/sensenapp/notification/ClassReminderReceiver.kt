package ren.hieu.sensenapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ren.hieu.sensenapp.data.prefs.SettingsStore

class ClassReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CLASS_REMINDER) return
        // 系统延迟到上课后才投递的旧提醒不再显示。
        val startEpoch = intent.getLongExtra(EXTRA_START_EPOCH, Long.MAX_VALUE)
        if (System.currentTimeMillis() >= startEpoch) {
            ren.hieu.sensenapp.widget.WidgetRefreshWorker.enqueue(context)
            return
        }
        val settings = SettingsStore(context)
        if (!settings.classReminderEnabled) return
        val name = intent.getStringExtra(EXTRA_COURSE_NAME) ?: return
        val location = intent.getStringExtra(EXTRA_LOCATION) ?: ""
        val start = intent.getStringExtra(EXTRA_START) ?: ""
        val minutes = intent.getIntExtra(EXTRA_MINUTES_BEFORE, settings.classReminderMinutesBefore)
        SenSenNotifications.showClassReminder(
            context = context,
            requestCode = intent.getIntExtra(EXTRA_REQUEST_CODE, 0),
            courseName = name,
            location = location,
            startLabel = start,
            minutesBefore = minutes,
        )
        ren.hieu.sensenapp.widget.WidgetRefreshWorker.enqueue(context)
    }

    companion object {
        const val ACTION_CLASS_REMINDER = "ren.hieu.sensenapp.CLASS_REMINDER"
        const val EXTRA_COURSE_NAME = "course_name"
        const val EXTRA_LOCATION = "location"
        const val EXTRA_START_EPOCH = "start_epoch"
        const val EXTRA_START = "start"
        const val EXTRA_MINUTES_BEFORE = "minutes_before"
        const val EXTRA_REQUEST_CODE = "request_code"
    }
}
