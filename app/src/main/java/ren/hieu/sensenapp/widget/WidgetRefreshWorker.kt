package ren.hieu.sensenapp.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ren.hieu.sensenapp.SenSenApplication

class WidgetRefreshWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? SenSenApplication ?: return Result.success()
        app.container.scheduleRepository.loadFromCache()
        updateAllScheduleWidgets(applicationContext)
        return Result.success()
    }
}
