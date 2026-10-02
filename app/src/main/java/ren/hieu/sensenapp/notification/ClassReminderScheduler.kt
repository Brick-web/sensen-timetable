package ren.hieu.sensenapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ren.hieu.sensenapp.SenSenApplication
import ren.hieu.sensenapp.data.prefs.SettingsStore
import ren.hieu.sensenapp.domain.ClassReminderPlanner
import ren.hieu.sensenapp.domain.TodayCourseGrouper
import java.time.ZoneId

object ClassReminderScheduler {
    private const val MAX_ALARMS = 24
    private const val REQUEST_CODE_MIN = 6000
    private const val REQUEST_CODE_MAX = 6099

    suspend fun reschedule(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        cancelAll(app)
        val settings = SettingsStore(app)
        if (!settings.classReminderEnabled) return@withContext
        val container = (app as? SenSenApplication)?.container ?: return@withContext
        val pair = container.scheduleRepository.getExportForWidget() ?: return@withContext
        val (export, holidays) = pair
        val minutes = settings.classReminderMinutesBefore
        val triggers = ClassReminderPlanner.upcoming(
            export,
            holidays,
            minutesBefore = minutes,
            applyHolidayAdjustments = settings.holidayAdjustmentsEnabled,
        )
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val zone: ZoneId = TodayCourseGrouper.zone
        triggers.take(MAX_ALARMS).forEach { trigger ->
            val intent = Intent(app, ClassReminderReceiver::class.java).apply {
                action = ClassReminderReceiver.ACTION_CLASS_REMINDER
                putExtra(ClassReminderReceiver.EXTRA_COURSE_NAME, trigger.courseName)
                putExtra(ClassReminderReceiver.EXTRA_LOCATION, trigger.location)
                putExtra(ClassReminderReceiver.EXTRA_START, trigger.startLabel)
                putExtra(ClassReminderReceiver.EXTRA_MINUTES_BEFORE, minutes)
                putExtra(ClassReminderReceiver.EXTRA_REQUEST_CODE, trigger.requestCode)
            }
            val pending = PendingIntent.getBroadcast(
                app,
                trigger.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val millis = trigger.fireAt.atZone(zone).toInstant().toEpochMilli()
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }

    fun cancelAll(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (code in REQUEST_CODE_MIN..REQUEST_CODE_MAX) {
            val intent = Intent(app, ClassReminderReceiver::class.java).apply {
                action = ClassReminderReceiver.ACTION_CLASS_REMINDER
            }
            val pending = PendingIntent.getBroadcast(
                app,
                code,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }
}
