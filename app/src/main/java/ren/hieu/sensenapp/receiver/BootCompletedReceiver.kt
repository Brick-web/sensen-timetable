package ren.hieu.sensenapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ren.hieu.sensenapp.notification.ClassReminderScheduler
import ren.hieu.sensenapp.widget.WidgetRefreshScheduler

class BootCompletedReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        scope.launch {
            try {
                val appContext = context.applicationContext
                val container = (appContext as? ren.hieu.sensenapp.SenSenApplication)?.container
                val pair = container?.scheduleRepository?.getExportForWidget()
                if (pair != null) {
                    WidgetRefreshScheduler.scheduleAfterSync(appContext, pair.first, pair.second)
                }
                ClassReminderScheduler.reschedule(appContext)
                container?.remoteConfigRepository?.sync(force = true)
            } finally {
                pending.finish()
            }
        }
    }
}
