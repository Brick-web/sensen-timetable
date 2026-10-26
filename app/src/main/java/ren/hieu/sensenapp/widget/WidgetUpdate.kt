package ren.hieu.sensenapp.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.currentState
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import kotlinx.coroutines.CancellationException

private val refreshVersion = longPreferencesKey("schedule_refresh_version")

/** 更新持久状态，让正在运行的 Glance 会话也能观察到刷新。 */
suspend fun updateAllScheduleWidgets(context: Context) {
    val manager = GlanceAppWidgetManager(context)
    var failure: Exception? = null
    for (widget in listOf(PeriodScheduleGlanceWidget(), FullDayScheduleGlanceWidget(), WeekScheduleGlanceWidget())) {
        for (id in manager.getGlanceIds(widget.javaClass)) {
            try {
                updateAppWidgetState(context, id) { it[refreshVersion] = (it[refreshVersion] ?: 0L) + 1 }
                widget.update(context, id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failure = e
            }
        }
    }
    failure?.let { throw it }
}

private data class WidgetSnapshot<T>(val version: Long?, val data: T, val presentation: WidgetPresentation)

@Composable
internal fun <T> RefreshingWidgetContent(
    context: Context,
    initial: T,
    initialPresentation: WidgetPresentation,
    load: suspend () -> T,
    content: @Composable (T, WidgetPresentation) -> Unit,
) {
    val version = currentState<Preferences>()[refreshVersion]
    val snapshot by produceState(WidgetSnapshot(null, initial, initialPresentation), version) {
        value = WidgetSnapshot(version, load(), loadWidgetPresentation(context))
    }
    // 即使课表内容未变，也要重算当前时段与课程进行状态。
    key(snapshot) { content(snapshot.data, snapshot.presentation) }
}
