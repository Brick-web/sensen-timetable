package ren.hieu.sensenapp.widget

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CancellationException
import ren.hieu.sensenapp.SenSenApplication

class WidgetRefreshWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? SenSenApplication ?: return Result.success()
        return try {
            var failure: Exception? = null
            for (refresh in listOf<suspend () -> Unit>(
                { app.container.scheduleRepository.syncClassReminders() },
                { app.container.scheduleRepository.refreshAllWidgets() },
            )) {
                try { refresh() }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { failure = e }
            }
            failure?.let { throw it }
            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("WidgetRefresh", "Refresh failed; retrying", e)
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "widget_immediate_refresh", ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build(),
            )
        }
    }
}
