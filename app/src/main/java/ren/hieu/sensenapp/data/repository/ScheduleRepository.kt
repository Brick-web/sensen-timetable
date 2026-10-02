package ren.hieu.sensenapp.data.repository

import android.content.Context
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ren.hieu.sensenapp.data.local.LocalScheduleDefaults
import ren.hieu.sensenapp.data.local.ScheduleCacheEntity
import ren.hieu.sensenapp.data.local.ScheduleDao
import ren.hieu.sensenapp.data.model.CourseEventDto
import ren.hieu.sensenapp.data.model.HolidayCalendarDto
import ren.hieu.sensenapp.data.model.RulesMetaDto
import ren.hieu.sensenapp.data.model.ScheduleExportDto
import ren.hieu.sensenapp.data.model.TodaySchedule
import ren.hieu.sensenapp.data.prefs.SessionStore
import ren.hieu.sensenapp.data.prefs.SettingsStore
import ren.hieu.sensenapp.domain.ScheduleCalculator
import ren.hieu.sensenapp.domain.TodayCourseGrouper
import ren.hieu.sensenapp.notification.ClassReminderScheduler
import ren.hieu.sensenapp.widget.WidgetRefreshScheduler
import ren.hieu.sensenapp.widget.updateAllScheduleWidgets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class ScheduleRepository(
    private val appContext: Context,
    private val dao: ScheduleDao,
    private val sessionStore: SessionStore,
    private val settingsStore: SettingsStore,
    private val moshi: Moshi,
) {
    private val _today = MutableStateFlow<TodaySchedule?>(null)
    val todaySchedule: Flow<TodaySchedule?> = _today.asStateFlow()

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    suspend fun ensureLocalSchedule(): Boolean = withContext(Dispatchers.IO) {
        if (dao.getCache() != null) {
            sessionStore.isLinked = true
            return@withContext loadFromCache()
        }
        val export = LocalScheduleDefaults.emptyExport()
        val holidays = LocalScheduleDefaults.emptyHolidays(export)
        persistCache(export, holidays)
        sessionStore.isLinked = true
        sessionStore.school = export.school
        _today.value = todayFrom(export, holidays)
        true
    }

    suspend fun hasLocalCache(): Boolean = dao.getCache() != null

    suspend fun loadFromCache(): Boolean = withContext(Dispatchers.IO) {
        val cache = dao.getCache() ?: return@withContext false
        val export = moshi.adapter(ScheduleExportDto::class.java).fromJson(cache.scheduleJson)
        val holidays = moshi.adapter(HolidayCalendarDto::class.java).fromJson(cache.holidaysJson)
        if (export == null || holidays == null) return@withContext false
        _today.value = todayFrom(export, holidays)
        true
    }

    private fun todayFrom(export: ScheduleExportDto, holidays: HolidayCalendarDto): TodaySchedule {
        return ScheduleCalculator.todaySchedule(
            export,
            holidays,
            applyHolidayAdjustments = settingsStore.holidayAdjustmentsEnabled,
        )
    }

    suspend fun saveScheduleExport(export: ScheduleExportDto): Result<Unit> = withContext(Dispatchers.IO) {
        val holidays = getHolidayCalendar() ?: LocalScheduleDefaults.emptyHolidays(export)
        runCatching { persistCache(export, holidays) }
    }

    suspend fun saveHolidayCalendar(holidays: HolidayCalendarDto): Result<Unit> = withContext(Dispatchers.IO) {
        val export = getScheduleExport()
            ?: return@withContext Result.failure(IllegalStateException("本地课表不存在"))
        val stamped = holidays.copy(
            rulesMeta = RulesMetaDto(
                updatedAt = LocalDate.now(TodayCourseGrouper.zone).format(dateFmt) + "T00:00:00+08:00",
            ),
        )
        runCatching { persistCache(export, stamped) }
    }

    suspend fun upsertCourseEvent(event: CourseEventDto): Result<Unit> = withContext(Dispatchers.IO) {
        val export = getScheduleExport()
            ?: return@withContext Result.failure(IllegalStateException("本地课表不存在"))
        val normalized = if (event.id.isBlank()) {
            event.copy(id = UUID.randomUUID().toString().replace("-", ""))
        } else {
            event
        }
        val events = export.events.toMutableList()
        val index = events.indexOfFirst { it.id == normalized.id }
        if (index >= 0) {
            events[index] = normalized
        } else {
            events.add(normalized)
        }
        saveScheduleExport(export.copy(events = events))
    }

    suspend fun updateTermFirstMonday(firstMonday: String): Result<Unit> = withContext(Dispatchers.IO) {
        val export = getScheduleExport()
            ?: return@withContext Result.failure(IllegalStateException("本地课表不存在"))
        val holidays = getHolidayCalendar()
            ?: LocalScheduleDefaults.emptyHolidays(export)
        val date = runCatching { java.time.LocalDate.parse(firstMonday.trim(), dateFmt) }.getOrNull()
            ?: return@withContext Result.failure(IllegalArgumentException("日期格式须为 yyyy-MM-dd"))
        val monday = if (date.dayOfWeek == java.time.DayOfWeek.MONDAY) {
            date
        } else {
            date.with(java.time.temporal.TemporalAdjusters.previous(java.time.DayOfWeek.MONDAY))
        }
        val mondayText = monday.format(dateFmt)
        val maxWeek = export.term.maxWeek.coerceIn(1, 30)
        val currentWeek = ScheduleCalculator.currentWeek(
            monday,
            java.time.LocalDate.now(TodayCourseGrouper.zone),
            maxWeek,
        ).coerceAtLeast(1)
        val updatedExport = export.copy(
            term = export.term.copy(
                firstMonday = mondayText,
                currentWeek = currentWeek,
            ),
        )
        val updatedHolidays = holidays.copy(
            term = holidays.term.copy(
                firstMonday = mondayText,
                maxWeek = maxWeek,
            ),
        )
        runCatching { persistCache(updatedExport, updatedHolidays) }
    }

    suspend fun deleteCourseEvent(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val export = getScheduleExport()
            ?: return@withContext Result.failure(IllegalStateException("本地课表不存在"))
        val events = export.events.filterNot { it.id == eventId }
        saveScheduleExport(export.copy(events = events))
    }

    suspend fun getHolidayCalendar(): HolidayCalendarDto? = withContext(Dispatchers.IO) {
        val cache = dao.getCache() ?: return@withContext null
        moshi.adapter(HolidayCalendarDto::class.java).fromJson(cache.holidaysJson)
    }

    suspend fun refreshLocalScheduleViews() = withContext(Dispatchers.IO) {
        loadFromCache()
        updateAllScheduleWidgets(appContext)
        val pair = getExportForWidget() ?: return@withContext
        WidgetRefreshScheduler.scheduleAfterSync(
            appContext,
            pair.first,
            pair.second,
            settingsStore.holidayAdjustmentsEnabled,
        )
    }

    suspend fun refreshAllWidgets() = withContext(Dispatchers.IO) {
        loadFromCache()
        updateAllScheduleWidgets(appContext)
    }

    suspend fun resetSchedule() = withContext(Dispatchers.IO) {
        dao.clear()
        sessionStore.clear()
        _today.value = null
        ClassReminderScheduler.cancelAll(appContext)
        ensureLocalSchedule()
    }

    suspend fun getScheduleExport(): ScheduleExportDto? = withContext(Dispatchers.IO) {
        val cache = dao.getCache() ?: return@withContext null
        moshi.adapter(ScheduleExportDto::class.java).fromJson(cache.scheduleJson)
    }

    suspend fun syncClassReminders() {
        ClassReminderScheduler.reschedule(appContext)
    }

    suspend fun getTodaySnapshot(): TodaySchedule? = withContext(Dispatchers.IO) {
        loadFromCache()
        _today.value
    }

    suspend fun getExportForWidget(): Pair<ScheduleExportDto, HolidayCalendarDto>? = withContext(Dispatchers.IO) {
        val cache = dao.getCache() ?: return@withContext null
        val export = moshi.adapter(ScheduleExportDto::class.java).fromJson(cache.scheduleJson)
        val holidays = moshi.adapter(HolidayCalendarDto::class.java).fromJson(cache.holidaysJson)
        if (export == null || holidays == null) null else export to holidays
    }

    private suspend fun persistCache(export: ScheduleExportDto, holidays: HolidayCalendarDto) {
        val scheduleJson = moshi.adapter(ScheduleExportDto::class.java).toJson(export)
        val holidaysJson = moshi.adapter(HolidayCalendarDto::class.java).toJson(holidays)
        dao.upsert(
            ScheduleCacheEntity(
                school = export.school.ifBlank { "local" },
                scheduleJson = scheduleJson,
                holidaysJson = holidaysJson,
                revision = export.source.revision.ifBlank { "local" },
                syncedAtEpochMs = System.currentTimeMillis(),
            ),
        )
        _today.value = todayFrom(export, holidays)
        WidgetRefreshScheduler.scheduleAfterSync(
            appContext,
            export,
            holidays,
            settingsStore.holidayAdjustmentsEnabled,
        )
        ClassReminderScheduler.reschedule(appContext)
    }
}
