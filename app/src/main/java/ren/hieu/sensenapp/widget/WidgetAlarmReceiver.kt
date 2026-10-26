package ren.hieu.sensenapp.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** 持久任务负责磁盘读取和渲染，避免广播结束后协程被系统回收。 */
class WidgetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        WidgetRefreshWorker.enqueue(context)
    }
}
