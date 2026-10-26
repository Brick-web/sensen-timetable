package ren.hieu.sensenapp.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test
import ren.hieu.sensenapp.data.network.GitHubReleaseAssetDto
import ren.hieu.sensenapp.data.network.GitHubReleaseDto

class OpenReleasePolicyTest {
    private val apkUrl = "https://github.com/Brick-web/sensen-timetable/releases/download/v1.0.2/open.apk"
    private val asset = GitHubReleaseAssetDto("open.apk", "uploaded", apkUrl)
    private val release = GitHubReleaseDto("v1.0.2", "更新说明", assets = listOf(asset))

    @Test fun usesOpenVersionSequence() {
        assertEquals(10000, OpenReleasePolicy.versionCode("v1.0.0"))
        assertEquals(10001, OpenReleasePolicy.versionCode("1.0.1"))
        assertEquals(10200, OpenReleasePolicy.versionCode("v1.2"))
        assertEquals(20000, OpenReleasePolicy.versionCode("v2.0.0"))
        listOf("v1.0.2-beta", "latest", "1.100.0", "9999999999.0.0", "0.0.0").forEach {
            assertNull(OpenReleasePolicy.versionCode(it))
        }
    }

    @Test fun rejectsOfficialAndUnrelatedDownloadLinks() {
        assertTrue(OpenReleasePolicy.isApkUrl(apkUrl))
        listOf(
            "https://saas.hieu.ren/sensen-timetable/download/app.apk",
            "https://www.hieu.ren/app.apk",
            "https://github.com/other/repo/releases/download/v1/app.apk",
            apkUrl.replace("github.com", "github.com.evil.example"),
            apkUrl.replace("https:", "http:"),
            apkUrl.replace("/v1.0.2/", "/../"),
            "$apkUrl?redirect=official",
            "javascript:alert(1)",
        ).forEach { assertFalse(it, OpenReleasePolicy.isApkUrl(it)) }
    }

    @Test fun usesGitHubAssetAndNotesWithoutForcingOfficialUpgrade() {
        val config = OpenReleasePolicy.updateConfig(release)
        assertEquals(10002, config.latestVersionCode)
        assertEquals("1.0.2", config.latestVersionName)
        assertEquals(apkUrl, config.apkUrl)
        assertEquals("更新说明", config.releaseNotes)
        assertFalse(config.forceUpdate)
        assertEquals(1, config.minVersionCode)
    }

    @Test fun rejectsDraftPrereleaseAndUnfinishedDownloads() {
        listOf(
            release.copy(draft = true), release.copy(prerelease = true),
            release.copy(tagName = "v1.0.2-rc1"), release.copy(assets = emptyList()),
            release.copy(assets = listOf(asset.copy(state = "new"))),
            release.copy(assets = listOf(asset.copy(downloadUrl = "https://saas.hieu.ren/app.apk"))),
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { OpenReleasePolicy.updateConfig(invalid) }
        }
    }

    @Test fun parsesGitHubResponseIncludingNullableNotesAndExtraFields() {
        val json = """{
            "tag_name":"v1.0.2", "body":null, "draft":false, "prerelease":false,
            "id":123, "assets":[{"name":"open.apk","state":"uploaded","size":12345,
            "browser_download_url":"$apkUrl"}]
        }"""
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val parsed = requireNotNull(moshi.adapter(GitHubReleaseDto::class.java).fromJson(json))
        val config = OpenReleasePolicy.updateConfig(parsed)
        assertEquals(apkUrl, config.apkUrl)
        assertEquals("", config.releaseNotes)
    }
}
