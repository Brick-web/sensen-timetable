package ren.hieu.sensenapp.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import ren.hieu.sensenapp.R

/** 用户在桌面确认固定小组件后的回调（部分 Launcher 会触发）。 */
class WidgetPinResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_WIDGET_PINNED) return
        Toast.makeText(context.applicationContext, R.string.widget_pin_success, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ACTION_WIDGET_PINNED = "ren.hieu.sensenapp.open.action.WIDGET_PINNED"
    }
}
