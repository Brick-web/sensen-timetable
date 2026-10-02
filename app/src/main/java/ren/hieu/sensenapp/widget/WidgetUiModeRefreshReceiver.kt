package ren.hieu.sensenapp.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 系统切换浅色 / 深色（含定时深色）时刷新小组件，使 [ColorProvider] day/night 立即生效。
 */
class WidgetUiModeRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_CONFIGURATION_CHANGED) return
        val app = context.applicationContext
        val night = app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getInt(KEY_NIGHT_MODE, -1)
        if (last == night) return
        prefs.edit().putInt(KEY_NIGHT_MODE, night).apply()
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                updateAllScheduleWidgets(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val PREFS = "schedule_widget_ui_mode"
        private const val KEY_NIGHT_MODE = "night_mode"
    }
}
