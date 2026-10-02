package ren.hieu.sensenapp.remote

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ren.hieu.sensenapp.SenSenApplication
import java.util.concurrent.TimeUnit

class RemoteConfigWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? SenSenApplication ?: return Result.success()
        return try {
            app.container.remoteConfigRepository.sync(force = false)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "remote_config_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RemoteConfigWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }
    }
}
