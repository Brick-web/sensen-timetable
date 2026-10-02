package ren.hieu.sensenapp.data

import android.content.Context
import ren.hieu.sensenapp.data.local.ScheduleDatabase
import ren.hieu.sensenapp.data.network.ApiClient
import ren.hieu.sensenapp.data.prefs.RemoteConfigStore
import ren.hieu.sensenapp.data.prefs.SessionStore
import ren.hieu.sensenapp.data.prefs.SettingsStore
import ren.hieu.sensenapp.data.repository.RemoteConfigRepository
import ren.hieu.sensenapp.data.repository.ScheduleRepository

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val sessionStore = SessionStore(appContext)
    val settingsStore = SettingsStore(appContext)
    val remoteConfigStore = RemoteConfigStore(appContext, ApiClient.moshi)
    private val database = ScheduleDatabase.get(appContext)
    private val api = ApiClient.scheduleService

    val scheduleRepository = ScheduleRepository(
        appContext = appContext,
        dao = database.scheduleDao(),
        sessionStore = sessionStore,
        settingsStore = settingsStore,
        moshi = ApiClient.moshi,
    )

    val remoteConfigRepository = RemoteConfigRepository(
        appContext = appContext,
        api = api,
        sessionStore = sessionStore,
        remoteConfigStore = remoteConfigStore,
        scheduleRepository = scheduleRepository,
    )
}
