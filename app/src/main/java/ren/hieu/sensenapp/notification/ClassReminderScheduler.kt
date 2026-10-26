package ren.hieu.sensenapp.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ren.hieu.sensenapp.SenSenApplication
import ren.hieu.sensenapp.data.prefs.SettingsStore
import ren.hieu.sensenapp.domain.ClassReminderPlanner
import ren.hieu.sensenapp.domain.TodayCourseGrouper
import java.time.ZoneId

object ClassReminderScheduler {
    private const val MAX_ALARMS = 64
    private val mutex = Mutex()
    private const val PREFS = "class_reminder_alarms"
    private const val KEY_CODES = "scheduled_codes"

    fun canScheduleExact(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()

    suspend fun reschedule(context: Context) = withContext(Dispatchers.IO) { mutex.withLock {
        val app = context.applicationContext
        val settings = SettingsStore(app)
        if (!settings.classReminderEnabled) {
            cancelAll(app)
            return@withLock
        }
        val container = (app as? SenSenApplication)?.container ?: return@withLock
        val pair = container.scheduleRepository.getExportForWidget() ?: return@withLock
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
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val previous = prefs.getStringSet(KEY_CODES, emptySet()).orEmpty().toSet()
        val upcoming = triggers.take(MAX_ALARMS)
        val desired = upcoming.map { it.requestCode.toString() }.toSet()
        upcoming.forEach { trigger ->
            val intent = Intent(app, ClassReminderReceiver::class.java).apply {
                action = ClassReminderReceiver.ACTION_CLASS_REMINDER
                putExtra(ClassReminderReceiver.EXTRA_COURSE_NAME, trigger.courseName)
                putExtra(ClassReminderReceiver.EXTRA_LOCATION, trigger.location)
                putExtra(ClassReminderReceiver.EXTRA_START, trigger.startLabel)
                putExtra(ClassReminderReceiver.EXTRA_MINUTES_BEFORE, minutes)
                putExtra(ClassReminderReceiver.EXTRA_REQUEST_CODE, trigger.requestCode)
                putExtra(ClassReminderReceiver.EXTRA_START_EPOCH,
                    trigger.fireAt.plusMinutes(minutes.toLong()).atZone(TodayCourseGrouper.zone).toInstant().toEpochMilli())
            }
            val pending = PendingIntent.getBroadcast(
                app,
                trigger.requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val millis = trigger.fireAt.atZone(zone).toInstant().toEpochMilli()
            // 不先清空所有闹钟：后台刷新恰好撞上提醒时间时，不能撤销正在投递的提醒。
            try {
                if (canScheduleExact(app)) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
                }
            } catch (_: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pending)
            }
            prefs.edit().putLong("fire_${trigger.requestCode}", millis).apply()
        }
        // 已到点的广播留给接收器校验，避免重排吞掉刚到点的提醒。
        val now = System.currentTimeMillis()
        val retained = (previous - desired).filter { code ->
            val fireAt = prefs.getLong("fire_$code", 0)
            if (fireAt > now) { cancelCode(app, code.toInt()); false }
            else fireAt > now - 24 * 60 * 60 * 1000L
        }
        prefs.edit().putStringSet(KEY_CODES, desired + retained).apply()
    } }

    fun cancelAll(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val codes = prefs.getStringSet(KEY_CODES, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toMutableSet()
        // 兼容升级前未记录的三天闹钟；旧实现编号实际在 6000..9670。
        val today = LocalDate.now(TodayCourseGrouper.zone)
        for (offset in -3L..3L) {
            val day = today.plusDays(offset)
            for (section in 1..10) codes.add(6000 + day.dayOfYear % 400 * 10 + section)
        }
        codes.forEach { cancelCode(app, it) }
        prefs.edit().clear().apply()
    }

    private fun cancelCode(context: Context, code: Int) {
        val intent = Intent(context, ClassReminderReceiver::class.java).apply {
            action = ClassReminderReceiver.ACTION_CLASS_REMINDER
        }
        val pending = PendingIntent.getBroadcast(context, code, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE) ?: return
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pending)
        pending.cancel()
    }
}
