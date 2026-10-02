package ren.hieu.sensenapp.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
        updateAllScheduleWidgets(context)
        scheduleAlarms(context, export, holidays, applyHolidayAdjustments)
        schedulePeriodic(context)
        ClassReminderScheduler.reschedule(context)
    }

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(12, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun scheduleAlarms(
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
        triggers.take(8).forEachIndexed { index, trigger ->
            val pending = PendingIntent.getBroadcast(
                context,
                4000 + index,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val millis = trigger.atZone(zone).toInstant().toEpochMilli()
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
        }
    }
}
