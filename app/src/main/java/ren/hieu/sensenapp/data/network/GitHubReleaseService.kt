package ren.hieu.sensenapp.data.network

import com.squareup.moshi.Json
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

data class GitHubReleaseDto(
    @param:Json(name = "tag_name") val tagName: String = "",
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

data class GitHubReleaseAssetDto(
    val name: String = "",
    val state: String = "",
    @param:Json(name = "browser_download_url") val downloadUrl: String = "",
)

interface GitHubReleaseService {
    @Headers("Accept: application/vnd.github+json", "User-Agent: SenSenTimetable-Open")
    @GET("repos/Brick-web/sensen-timetable/releases/latest")
    suspend fun latestRelease(): Response<GitHubReleaseDto>
}
