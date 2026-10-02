package ren.hieu.sensenapp.data.repository

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import ren.hieu.sensenapp.BuildConfig
import ren.hieu.sensenapp.data.model.AndroidUpdateConfigDto
import ren.hieu.sensenapp.data.model.AppRemoteConfigDto
import ren.hieu.sensenapp.data.network.ScheduleApiService
import ren.hieu.sensenapp.data.prefs.RemoteConfigStore
import ren.hieu.sensenapp.data.prefs.SessionStore
import ren.hieu.sensenapp.widget.updateAllScheduleWidgets

data class AppUpdatePrompt(
    val force: Boolean,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
)

class RemoteConfigRepository(
    private val appContext: Context,
    private val api: ScheduleApiService,
    private val sessionStore: SessionStore,
    private val remoteConfigStore: RemoteConfigStore,
    private val scheduleRepository: ScheduleRepository,
) {
    private val _updatePrompt = MutableStateFlow<AppUpdatePrompt?>(null)
    val updatePrompt: StateFlow<AppUpdatePrompt?> = _updatePrompt.asStateFlow()

    suspend fun sync(force: Boolean = false): Result<AppRemoteConfigDto?> = withContext(Dispatchers.IO) {
        val minIntervalMs = if (force) 0L else 30 * 60 * 1000L
        val elapsed = System.currentTimeMillis() - remoteConfigStore.lastFetchedEpochMs
        if (!force && elapsed < minIntervalMs && remoteConfigStore.load() != null) {
            evaluateUpdate(remoteConfigStore.load()!!)
            return@withContext Result.success(remoteConfigStore.load())
        }

        val school = sessionStore.school.orEmpty().ifBlank { "hieu" }
        try {
            val previousRevision = remoteConfigStore.load()?.revision
            val config = api.fetchAppConfig(school)
            remoteConfigStore.save(config)
            applyHolidayRevisionIfNeeded(config)
            if (previousRevision != config.revision) {
                updateAllScheduleWidgets(appContext)
            }
            evaluateUpdate(config)
            Result.success(config)
        } catch (e: Exception) {
            remoteConfigStore.load()?.let { evaluateUpdate(it) }
            Result.failure(e)
        }
    }

    fun cachedConfig(): AppRemoteConfigDto? = remoteConfigStore.load()

    fun dismissUpdatePrompt() {
        _updatePrompt.value = null
    }

    private suspend fun applyHolidayRevisionIfNeeded(config: AppRemoteConfigDto) {
        val revision = config.widget.holidayDataRevision.trim()
        if (revision.isEmpty()) return
        if (revision == remoteConfigStore.appliedHolidayRevision) return
        remoteConfigStore.appliedHolidayRevision = revision
        if (sessionStore.isLinked) {
            scheduleRepository.refreshLocalScheduleViews()
        } else {
            updateAllScheduleWidgets(appContext)
        }
    }

    private fun evaluateUpdate(config: AppRemoteConfigDto) {
        val android = config.android
        val current = BuildConfig.VERSION_CODE
        val needsForce = current < android.minVersionCode || (android.forceUpdate && current < android.latestVersionCode)
        val hasUpdate = current < android.latestVersionCode && android.apkUrl.isNotBlank()
        if (!needsForce && !hasUpdate) {
            _updatePrompt.value = null
            return
        }
        if (!needsForce && _updatePrompt.value != null) {
            return
        }
        _updatePrompt.value = AppUpdatePrompt(
            force = needsForce,
            versionName = android.latestVersionName.ifBlank { "新版本" },
            releaseNotes = android.releaseNotes.trim(),
            apkUrl = android.apkUrl,
        )
    }
}
