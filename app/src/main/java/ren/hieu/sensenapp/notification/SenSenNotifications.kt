package ren.hieu.sensenapp.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ren.hieu.sensenapp.MainActivity
import ren.hieu.sensenapp.R

object SenSenNotifications {
    /** Bumped when channel vibration/importance changes; old channels are not updated by the OS. */
    const val CHANNEL_CLASS = "class_reminder_v2"
    private const val LEGACY_CHANNEL_CLASS = "class_reminder"
    private const val NOTIFICATION_ID_BASE = 7000
    private val reminderVibrationPattern = longArrayOf(0, 120, 80, 120, 80, 200)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel(LEGACY_CHANNEL_CLASS)
        val channel = NotificationChannel(
            CHANNEL_CLASS,
            context.getString(R.string.notification_channel_class),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_class_desc)
            enableVibration(true)
            vibrationPattern = reminderVibrationPattern
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun showClassReminder(
        context: Context,
        requestCode: Int,
        courseName: String,
        location: String,
        startLabel: String,
        minutesBefore: Int,
    ) {
        if (!canPost(context)) return
        ensureChannels(context.applicationContext)
        val app = context.applicationContext
        val open = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            app,
            requestCode,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val place = location.ifBlank { "地点待定" }
        val notification = NotificationCompat.Builder(app, CHANNEL_CLASS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("即将上课：$courseName")
            .setContentText("${minutesBefore} 分钟后（$startLabel 开始）· $place")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "${minutesBefore} 分钟后开始（$startLabel）\n$place",
                ),
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(reminderVibrationPattern)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            .build()
        NotificationManagerCompat.from(app).notify(NOTIFICATION_ID_BASE + requestCode % 500, notification)
    }
}
