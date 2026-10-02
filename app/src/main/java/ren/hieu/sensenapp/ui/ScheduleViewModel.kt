package ren.hieu.sensenapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ren.hieu.sensenapp.BuildConfig
import ren.hieu.sensenapp.data.model.CourseEventDto
import ren.hieu.sensenapp.data.model.HolidayCalendarDto
import ren.hieu.sensenapp.data.model.HolidayMappingDto
import ren.hieu.sensenapp.data.model.ScheduleExportDto
import ren.hieu.sensenapp.data.prefs.SettingsStore
import ren.hieu.sensenapp.data.repository.RemoteConfigRepository
import ren.hieu.sensenapp.data.repository.ScheduleRepository
import ren.hieu.sensenapp.domain.CurriculumWeekBuilder
import ren.hieu.sensenapp.domain.GridCourse
import ren.hieu.sensenapp.ui.curriculum.CourseEditorRequest

data class ScheduleUiState(
    val isLinked: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val export: ScheduleExportDto? = null,
    val currentWeek: Int = 1,
    val serverCurrentWeek: Int? = null,
    val weekRangeText: String = "",
    val weekCourses: List<GridCourse> = emptyList(),
    val weekDates: List<ren.hieu.sensenapp.domain.WeekDateCell> = emptyList(),
    val maxWeek: Int = 17,
    val showWeekPicker: Boolean = false,
    val showStyleSheet: Boolean = false,
    val courseEditor: CourseEditorRequest? = null,
    val showSettingsSheet: Boolean = false,
    val showHolidaySettings: Boolean = false,
    val showTermSettings: Boolean = false,
    val showResetConfirm: Boolean = false,
    val classReminderEnabled: Boolean = false,
    val holidayAdjustmentsEnabled: Boolean = true,
    val holidays: HolidayCalendarDto? = null,
    val settingsSyncAction: SettingsSyncAction? = null,
    val settingsResultDialog: SettingsResultDialog? = null,
    val cardStyle: String = "classic",
)

class ScheduleViewModel(
    private val repository: ScheduleRepository,
    private val remoteConfigRepository: RemoteConfigRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val updatePrompt = remoteConfigRepository.updatePrompt
    private val _uiState = MutableStateFlow(
        ScheduleUiState(
            cardStyle = settingsStore.courseCardStyle,
            classReminderEnabled = settingsStore.classReminderEnabled,
            holidayAdjustmentsEnabled = settingsStore.holidayAdjustmentsEnabled,
        ),
    )
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { remoteConfigRepository.sync(force = false) }
        viewModelScope.launch {
            repository.ensureLocalSchedule()
            reloadFromCacheInternal(showLoading = true)
        }
    }

    fun dismissUpdatePrompt() = remoteConfigRepository.dismissUpdatePrompt()

    fun dismissSettingsResultDialog() {
        _uiState.update { it.copy(settingsResultDialog = null) }
    }

    fun refreshRemoteConfig() {
        viewModelScope.launch {
            _uiState.update { it.copy(settingsSyncAction = SettingsSyncAction.CheckUpdate) }
            val result = remoteConfigRepository.sync(force = true)
            reloadFromCacheInternal(showLoading = false)
            val dialog = if (result.isFailure) {
                SettingsResultDialog(
                    title = "检查更新",
                    message = result.exceptionOrNull()?.message ?: "网络异常，请稍后重试",
                )
            } else {
                val android = remoteConfigRepository.cachedConfig()?.android
                val current = BuildConfig.VERSION_CODE
                val latestCode = android?.latestVersionCode ?: current
                val apkUrl = android?.apkUrl?.trim().orEmpty()
                val latestName = android?.latestVersionName?.takeIf { it.isNotBlank() } ?: "新版本"
                if (current < latestCode && apkUrl.isNotEmpty()) {
                    val notes = android?.releaseNotes?.trim().orEmpty()
                    val body = buildString {
                        append("发现新版本 $latestName")
                        if (notes.isNotBlank()) {
                            append("\n\n")
                            append(notes)
                        }
                    }
                    SettingsResultDialog(
                        title = "发现新版本",
                        message = body,
                        confirmLabel = "去更新",
                        confirmUrl = apkUrl,
                    )
                } else {
                    SettingsResultDialog(
                        title = "检查更新",
                        message = "当前已是最新版本",
                    )
                }
            }
            _uiState.update {
                it.copy(settingsSyncAction = null, settingsResultDialog = dialog)
            }
        }
    }

    fun reloadFromCache() {
        viewModelScope.launch { reloadFromCacheInternal(showLoading = true) }
    }

    private suspend fun reloadFromCacheInternal(showLoading: Boolean) {
        if (showLoading) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        }
        repository.loadFromCache()
        val export = repository.getScheduleExport()
        val holidays = repository.getHolidayCalendar()
        if (export == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isLinked = false,
                    export = null,
                    holidays = null,
                )
            }
            return
        }
        applyExport(export, holidays)
        _uiState.update { it.copy(isLoading = false, isLinked = true, holidays = holidays) }
    }

    private fun applyExport(export: ScheduleExportDto, holidays: HolidayCalendarDto?) {
        val maxWeek = export.term.maxWeek.coerceIn(1, 30)
        val serverWeek = export.term.currentWeek
        val week = when {
            serverWeek > 0 -> serverWeek.coerceIn(1, maxWeek)
            else -> _uiState.value.currentWeek.coerceIn(1, maxWeek)
        }
        refreshWeek(export, week, serverWeek.takeIf { it > 0 }, holidays)
    }

    private fun refreshWeek(
        export: ScheduleExportDto,
        week: Int,
        serverWeek: Int?,
        holidays: HolidayCalendarDto? = _uiState.value.holidays,
    ) {
        val dates = CurriculumWeekBuilder.weekDates(export.term.firstMonday, week)
        _uiState.update {
            it.copy(
                export = export,
                currentWeek = week,
                serverCurrentWeek = serverWeek,
                maxWeek = export.term.maxWeek.coerceIn(1, 30),
                weekCourses = CurriculumWeekBuilder.buildWeekCourses(export, week),
                weekDates = dates,
                weekRangeText = CurriculumWeekBuilder.weekRangeText(dates),
                holidays = holidays,
            )
        }
    }

    fun setWeek(week: Int) {
        val export = _uiState.value.export ?: return
        val w = week.coerceIn(1, _uiState.value.maxWeek)
        if (w == _uiState.value.currentWeek) {
            _uiState.update { it.copy(showWeekPicker = false) }
            return
        }
        refreshWeek(export, w, _uiState.value.serverCurrentWeek)
        _uiState.update { it.copy(showWeekPicker = false) }
    }

    fun changeWeekByDelta(delta: Int) {
        val state = _uiState.value
        val next = state.currentWeek + delta
        if (next < 1 || next > state.maxWeek) return
        setWeek(next)
    }

    fun openWeekPicker() = _uiState.update { it.copy(showWeekPicker = true) }
    fun closeWeekPicker() = _uiState.update { it.copy(showWeekPicker = false) }
    fun openStyleSheet() = _uiState.update { it.copy(showStyleSheet = true) }
    fun closeStyleSheet() = _uiState.update { it.copy(showStyleSheet = false) }

    fun openCourseDetail(course: GridCourse) {
        val export = _uiState.value.export
        val existing = export?.events?.firstOrNull { it.id == course.id }
        _uiState.update {
            it.copy(
                courseEditor = CourseEditorRequest(
                    weekday = course.weekday,
                    startPeriod = course.startPeriod,
                    weekNum = it.currentWeek,
                    existing = existing,
                ),
            )
        }
    }

    fun openBlankCourseEditor(weekday: Int, period: Int) {
        _uiState.update {
            it.copy(
                courseEditor = CourseEditorRequest(
                    weekday = weekday,
                    startPeriod = period,
                    weekNum = it.currentWeek,
                ),
            )
        }
    }

    fun closeCourseEditor() = _uiState.update { it.copy(courseEditor = null) }

    fun saveCourse(event: CourseEventDto) {
        viewModelScope.launch {
            val result = repository.upsertCourseEvent(event)
            if (result.isSuccess) {
                reloadFromCacheInternal(showLoading = false)
                closeCourseEditor()
            } else {
                _uiState.update {
                    it.copy(
                        settingsResultDialog = SettingsResultDialog(
                            title = "保存失败",
                            message = result.exceptionOrNull()?.message ?: "请稍后重试",
                        ),
                    )
                }
            }
        }
    }

    fun deleteCourse(eventId: String) {
        viewModelScope.launch {
            repository.deleteCourseEvent(eventId)
            reloadFromCacheInternal(showLoading = false)
            closeCourseEditor()
        }
    }

    fun openSettingsSheet() = _uiState.update { it.copy(showSettingsSheet = true, errorMessage = null) }
    fun closeSettingsSheet() = _uiState.update { it.copy(showSettingsSheet = false) }
    fun openHolidaySettings() = _uiState.update { it.copy(showHolidaySettings = true) }
    fun closeHolidaySettings() = _uiState.update { it.copy(showHolidaySettings = false) }
    fun openTermSettings() = _uiState.update { it.copy(showTermSettings = true) }
    fun closeTermSettings() = _uiState.update { it.copy(showTermSettings = false) }

    fun saveTermFirstMonday(date: String) {
        viewModelScope.launch {
            val result = repository.updateTermFirstMonday(date)
            if (result.isSuccess) {
                closeTermSettings()
                reloadFromCacheInternal(showLoading = false)
            } else {
                _uiState.update {
                    it.copy(
                        settingsResultDialog = SettingsResultDialog(
                            title = "保存失败",
                            message = result.exceptionOrNull()?.message ?: "请检查日期格式",
                        ),
                    )
                }
            }
        }
    }
    fun openResetConfirm() = _uiState.update { it.copy(showResetConfirm = true) }
    fun closeResetConfirm() = _uiState.update { it.copy(showResetConfirm = false) }

    fun openStyleFromSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(showSettingsSheet = false) }
            delay(280)
            _uiState.update { it.copy(showStyleSheet = true) }
        }
    }

    fun closeWidgetSheet() = Unit

    fun setClassReminderEnabled(enabled: Boolean) {
        settingsStore.classReminderEnabled = enabled
        _uiState.update { it.copy(classReminderEnabled = enabled) }
        viewModelScope.launch {
            repository.syncClassReminders()
        }
    }

    fun setHolidayAdjustmentsEnabled(enabled: Boolean) {
        settingsStore.holidayAdjustmentsEnabled = enabled
        _uiState.update { it.copy(holidayAdjustmentsEnabled = enabled) }
        viewModelScope.launch {
            repository.refreshLocalScheduleViews()
        }
    }

    fun refreshWidgets() {
        viewModelScope.launch {
            _uiState.update { it.copy(settingsSyncAction = SettingsSyncAction.RefreshWidgets) }
            repository.refreshAllWidgets()
            _uiState.update {
                it.copy(
                    settingsSyncAction = null,
                    settingsResultDialog = SettingsResultDialog(
                        title = "刷新小组件",
                        message = "桌面小组件已更新",
                    ),
                )
            }
        }
    }

    fun addHolidayMapping(mapping: HolidayMappingDto) {
        viewModelScope.launch {
            val holidays = repository.getHolidayCalendar() ?: return@launch
            val list = holidays.holidayMappings
                .filterNot { it.calendarDate == mapping.calendarDate }
                .plus(mapping)
            repository.saveHolidayCalendar(holidays.copy(holidayMappings = list))
            reloadFromCacheInternal(showLoading = false)
        }
    }

    fun removeHolidayMapping(calendarDate: String) {
        viewModelScope.launch {
            val holidays = repository.getHolidayCalendar() ?: return@launch
            val list = holidays.holidayMappings.filterNot { it.calendarDate == calendarDate }
            repository.saveHolidayCalendar(holidays.copy(holidayMappings = list))
            reloadFromCacheInternal(showLoading = false)
        }
    }

    fun resetSchedule() {
        viewModelScope.launch {
            repository.resetSchedule()
            closeSettingsSheet()
            closeResetConfirm()
            reloadFromCacheInternal(showLoading = false)
        }
    }

    fun selectCardStyle(style: String) {
        settingsStore.courseCardStyle = style
        _uiState.update { it.copy(cardStyle = style, showStyleSheet = false, showSettingsSheet = false) }
    }

    class Factory(
        private val repository: ScheduleRepository,
        private val remoteConfigRepository: RemoteConfigRepository,
        private val settingsStore: SettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScheduleViewModel(repository, remoteConfigRepository, settingsStore) as T
        }
    }
}
