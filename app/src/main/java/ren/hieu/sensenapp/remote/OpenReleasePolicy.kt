package ren.hieu.sensenapp.remote

import ren.hieu.sensenapp.data.model.AndroidUpdateConfigDto
import ren.hieu.sensenapp.data.network.GitHubReleaseDto
import java.net.URI

/** Open releases have their own version sequence; official app_config is never an update source. */
object OpenReleasePolicy {
    const val RELEASES_URL = "https://github.com/Brick-web/sensen-timetable/releases/latest"
    private const val REPOSITORY_PATH = "/Brick-web/sensen-timetable/releases/download/"

    fun versionCode(tag: String): Int? {
        val match = Regex("^[vV]?(\\d+)\\.(\\d+)(?:\\.(\\d+))?$").matchEntire(tag.trim()) ?: return null
        val major = match.groupValues[1].toIntOrNull() ?: return null
        val minor = match.groupValues[2].toIntOrNull() ?: return null
        val patch = match.groupValues[3].ifEmpty { "0" }.toIntOrNull() ?: return null
        if (minor !in 0..99 || patch !in 0..99) return null
        val code = major.toLong() * 10000 + minor * 100 + patch
        return code.takeIf { it in 1..Int.MAX_VALUE.toLong() }?.toInt()
    }

    fun isApkUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host.equals("github.com", ignoreCase = true) &&
            uri.userInfo == null && uri.port == -1 && uri.query == null && uri.fragment == null &&
            uri.path.startsWith(REPOSITORY_PATH) && uri.path.endsWith(".apk", ignoreCase = true) &&
            uri.normalize().path == uri.path && uri.path.removePrefix(REPOSITORY_PATH).split('/').size == 2
    }.getOrDefault(false)

    fun updateConfig(release: GitHubReleaseDto): AndroidUpdateConfigDto {
        require(!release.draft && !release.prerelease) { "不是稳定发布版本" }
        val code = requireNotNull(versionCode(release.tagName)) { "无法识别开源版发布标签" }
        val apk = requireNotNull(release.assets.firstOrNull {
            it.state == "uploaded" && it.name.endsWith(".apk", ignoreCase = true) && isApkUrl(it.downloadUrl)
        }) { "最新发布尚未上传开源版 APK" }
        return AndroidUpdateConfigDto(
            minVersionCode = 1,
            latestVersionCode = code,
            latestVersionName = release.tagName.trim().removePrefix("v").removePrefix("V"),
            apkUrl = apk.downloadUrl,
            releaseNotes = release.body.orEmpty().trim(),
            forceUpdate = false,
        )
    }
}
