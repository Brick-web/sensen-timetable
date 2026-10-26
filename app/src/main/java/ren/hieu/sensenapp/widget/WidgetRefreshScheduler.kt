package ren.hieu.sensenapp.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ren.hieu.sensenapp.data.model.HolidayCalendarDto
import ren.hieu.sensenapp.data.model.ScheduleExportDto
import ren.hieu.sensenapp.domain.ScheduleCalculator
import ren.hieu.sensenapp.notification.ClassReminderScheduler
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object WidgetRefreshScheduler {
    private const val PERIODIC_WORK = "widget_periodic_refresh"
    private val zone = ZoneId.of("Asia/Shanghai")

    suspend fun scheduleAfterSync(
        context: Context,
        export: ScheduleExportDto,
        holidays: HolidayCalendarDto,
        applyHolidayAdjustments: Boolean = true,
    ) {
        scheduleAlarms(context, export, holidays, applyHolidayAdjustments)
        updateAllScheduleWidgets(context)
        schedulePeriodic(context)
        ClassReminderScheduler.reschedule(context)
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(30, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun scheduleAlarms(
        context: Context,
        export: ScheduleExportDto,
        holidays: HolidayCalendarDto,
        applyHolidayAdjustments: Boolean,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = LocalDateTime.now(zone)
        val triggers = ScheduleCalculator.refreshTriggerTimes(
            export,
            holidays,
            now,
            applyHolidayAdjustments,
        )
        val intent = Intent(context, WidgetAlarmReceiver::class.java)
        // 清除旧版本一次性安排的八个闹钟，改成每次触发后续排最近一个。
        for (index in 0 until 8) {
            val old = PendingIntent.getBroadcast(context, 4000 + index, intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (old != null) alarmManager.cancel(old)
        }
        val trigger = triggers.firstOrNull() ?: return
        val pending = PendingIntent.getBroadcast(context, 4000, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val millis = trigger.atZone(zone).toInstant().toEpochMilli()
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }
}
