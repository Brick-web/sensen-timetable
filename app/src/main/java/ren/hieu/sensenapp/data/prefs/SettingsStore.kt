package ren.hieu.sensenapp.data.prefs

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("sensen_settings", Context.MODE_PRIVATE)

    var courseCardStyle: String
        get() = prefs.getString(KEY_CARD_STYLE, "classic") ?: "classic"
        set(value) = prefs.edit().putString(KEY_CARD_STYLE, value).apply()

    var classReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_CLASS_REMINDER, false)
        set(value) = prefs.edit().putBoolean(KEY_CLASS_REMINDER, value).apply()

    var classReminderMinutesBefore: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTES, 15).coerceIn(5, 60)
        set(value) = prefs.edit().putInt(KEY_REMINDER_MINUTES, value.coerceIn(5, 60)).apply()

    var holidayAdjustmentsEnabled: Boolean
        get() = prefs.getBoolean(KEY_HOLIDAY_ADJUSTMENTS, true)
        set(value) = prefs.edit().putBoolean(KEY_HOLIDAY_ADJUSTMENTS, value).apply()

    companion object {
        private const val KEY_CARD_STYLE = "course_card_style"
        private const val KEY_CLASS_REMINDER = "class_reminder_enabled"
        private const val KEY_REMINDER_MINUTES = "class_reminder_minutes"
        private const val KEY_HOLIDAY_ADJUSTMENTS = "holiday_adjustments_enabled"
    }
}
