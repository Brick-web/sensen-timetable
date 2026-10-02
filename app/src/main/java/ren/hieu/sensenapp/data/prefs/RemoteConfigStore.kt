package ren.hieu.sensenapp.data.prefs

import android.content.Context
import com.squareup.moshi.Moshi
import ren.hieu.sensenapp.data.model.AppRemoteConfigDto
import ren.hieu.sensenapp.domain.TodayCourseGrouper
import java.time.LocalDate

class RemoteConfigStore(
    context: Context,
    private val moshi: Moshi,
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val adapter = moshi.adapter(AppRemoteConfigDto::class.java)

    var lastFetchedEpochMs: Long
        get() = prefs.getLong(KEY_FETCHED_AT, 0L)
        private set(value) = prefs.edit().putLong(KEY_FETCHED_AT, value).apply()

    var appliedHolidayRevision: String
        get() = prefs.getString(KEY_HOLIDAY_REVISION, "") ?: ""
        set(value) = prefs.edit().putString(KEY_HOLIDAY_REVISION, value).apply()

    fun save(config: AppRemoteConfigDto) {
        prefs.edit()
            .putString(KEY_JSON, adapter.toJson(config))
            .putLong(KEY_FETCHED_AT, System.currentTimeMillis())
            .apply()
    }

    fun load(): AppRemoteConfigDto? {
        val json = prefs.getString(KEY_JSON, null) ?: return null
        return adapter.fromJson(json)
    }

    fun activeBanner(now: LocalDate = LocalDate.now(TodayCourseGrouper.zone)): String? {
        val widget = load()?.widget ?: return null
        val text = widget.bannerText.trim()
        if (text.isEmpty()) return null
        if (!isDateInRange(now, widget.bannerStart, widget.bannerEnd)) return null
        return text
    }

    fun activeNoClassHint(now: LocalDate = LocalDate.now(TodayCourseGrouper.zone)): String? {
        val widget = load()?.widget ?: return null
        val text = widget.noClassHint.trim()
        if (text.isEmpty()) return null
        if (!isDateInRange(now, widget.bannerStart, widget.bannerEnd)) return null
        return text
    }

    fun holidayDataRevision(): String {
        return load()?.widget?.holidayDataRevision?.trim() ?: ""
    }

    private fun isDateInRange(today: LocalDate, start: String, end: String): Boolean {
        val startDate = start.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) }
        val endDate = end.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) }
        if (startDate != null && today.isBefore(startDate)) return false
        if (endDate != null && today.isAfter(endDate)) return false
        return true
    }

    companion object {
        private const val PREFS = "sensen_remote_config"
        private const val KEY_JSON = "config_json"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val KEY_HOLIDAY_REVISION = "holiday_revision_applied"
    }
}
