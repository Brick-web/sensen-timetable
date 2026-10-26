package ren.hieu.sensenapp.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ren.hieu.sensenapp.BuildConfig
import ren.hieu.sensenapp.data.model.AppRemoteConfigDto
import ren.hieu.sensenapp.data.network.GitHubReleaseService
import ren.hieu.sensenapp.data.prefs.RemoteConfigStore
import ren.hieu.sensenapp.remote.OpenReleasePolicy
import retrofit2.HttpException

data class AppUpdatePrompt(
    val force: Boolean,
    val versionName: String,
    val releaseNotes: String,
    val apkUrl: String,
)

class RemoteConfigRepository(
    private val api: GitHubReleaseService,
    private val remoteConfigStore: RemoteConfigStore,
) {
    private val mutex = Mutex()
    private val _updatePrompt = MutableStateFlow<AppUpdatePrompt?>(null)
    val updatePrompt: StateFlow<AppUpdatePrompt?> = _updatePrompt.asStateFlow()

    suspend fun sync(force: Boolean = false, showPrompt: Boolean = true): Result<AppRemoteConfigDto?> =
        withContext(Dispatchers.IO) { mutex.withLock {
            if (!showPrompt) _updatePrompt.value = null
            val cached = remoteConfigStore.load()
            val elapsed = System.currentTimeMillis() - remoteConfigStore.lastFetchedEpochMs
            if (!force && elapsed in 0 until 30 * 60 * 1000L && cached != null) {
                if (showPrompt) evaluateUpdate(cached)
                return@withLock Result.success(cached)
            }
            try {
                val response = api.latestRelease()
                val config = when {
                    // No published release yet; never fall back to the official update channel.
                    response.code() == 404 -> AppRemoteConfigDto()
                    !response.isSuccessful -> throw HttpException(response)
                    else -> {
                        val release = requireNotNull(response.body()) { "GitHub 发布信息为空" }
                        AppRemoteConfigDto(revision = release.tagName, android = OpenReleasePolicy.updateConfig(release))
                    }
                }
                remoteConfigStore.save(config)
                if (showPrompt) evaluateUpdate(config)
                Result.success(config)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showPrompt) cached?.let(::evaluateUpdate)
                Result.failure(e)
            }
        } }

    fun cachedConfig(): AppRemoteConfigDto? = remoteConfigStore.load()

    fun dismissUpdatePrompt() {
        _updatePrompt.value = null
    }

    private fun evaluateUpdate(config: AppRemoteConfigDto) {
        val android = config.android
        _updatePrompt.value = if (BuildConfig.VERSION_CODE < android.latestVersionCode &&
            OpenReleasePolicy.isApkUrl(android.apkUrl)) {
            AppUpdatePrompt(false, android.latestVersionName, android.releaseNotes, android.apkUrl)
        } else null
    }
}
