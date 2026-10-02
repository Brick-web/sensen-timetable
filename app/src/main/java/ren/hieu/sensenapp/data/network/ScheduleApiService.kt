package ren.hieu.sensenapp.data.network

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import ren.hieu.sensenapp.data.model.AppRemoteConfigDto
import ren.hieu.sensenapp.data.model.HolidayCalendarDto
import ren.hieu.sensenapp.data.model.ScheduleExportDto

interface ScheduleApiService {
    @GET("schedule.php")
    suspend fun fetchSchedule(
        @Header("X-Sensen-Bind-Code") bindCode: String,
    ): ScheduleExportDto

    @GET("holidays.php")
    suspend fun fetchHolidays(
        @Query("school") school: String,
    ): HolidayCalendarDto

    @GET("app_config.php")
    suspend fun fetchAppConfig(
        @Query("school") school: String,
    ): AppRemoteConfigDto
}
