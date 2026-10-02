package ren.hieu.sensenapp

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ren.hieu.sensenapp.data.AppContainer
import ren.hieu.sensenapp.notification.SenSenNotifications
import ren.hieu.sensenapp.remote.RemoteConfigWorker

class SenSenApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SenSenNotifications.ensureChannels(this)
        RemoteConfigWorker.schedule(this)
        appScope.launch {
            container.remoteConfigRepository.sync(force = false)
        }
    }
}
