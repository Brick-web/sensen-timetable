package ren.hieu.sensenapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AppRemoteConfigDto(
    @Json(name = "schema_version") val schemaVersion: Int = 1,
    val revision: String = "",
    val school: String = "",
    val android: AndroidUpdateConfigDto = AndroidUpdateConfigDto(),
    val widget: WidgetRemoteConfigDto = WidgetRemoteConfigDto(),
)

@JsonClass(generateAdapter = true)
data class AndroidUpdateConfigDto(
    @Json(name = "min_version_code") val minVersionCode: Int = 1,
    @Json(name = "latest_version_code") val latestVersionCode: Int = 1,
    @Json(name = "latest_version_name") val latestVersionName: String = "",
    @Json(name = "apk_url") val apkUrl: String = "",
    @Json(name = "release_notes") val releaseNotes: String = "",
    @Json(name = "force_update") val forceUpdate: Boolean = false,
)

@JsonClass(generateAdapter = true)
data class WidgetRemoteConfigDto(
    @Json(name = "banner_text") val bannerText: String = "",
    @Json(name = "banner_start") val bannerStart: String = "",
    @Json(name = "banner_end") val bannerEnd: String = "",
    @Json(name = "no_class_hint") val noClassHint: String = "",
    @Json(name = "holiday_data_revision") val holidayDataRevision: String = "",
)
