package ren.hieu.sensenapp.widget

import androidx.compose.runtime.Composable
import androidx.glance.unit.ColorProvider
import ren.hieu.sensenapp.R

/**
 * 小组件配色：通过 values / values-night 资源随系统浅色、深色（含定时深色）切换。
 */
object ScheduleWidgetGlanceColors {
    val bgOuter = ColorProvider(R.color.widget_bg_outer)
    val bgInner = ColorProvider(R.color.widget_bg_inner)
    val textPrimary = ColorProvider(R.color.widget_text_primary)
    val textSecondary = ColorProvider(R.color.widget_text_secondary)
    val accent = ColorProvider(R.color.widget_accent)
    val accentSoft = ColorProvider(R.color.widget_accent_soft)
    val textPast = ColorProvider(R.color.widget_text_past)
    val barPast = ColorProvider(R.color.widget_bar_past)
    val rowCurrentBg = ColorProvider(R.color.widget_row_current_bg)
    val gridLine = ColorProvider(R.color.widget_grid_line)
    val headerBg = ColorProvider(R.color.widget_header_bg)
    val todayBg = ColorProvider(R.color.widget_today_bg)
    val todayBgSoft = ColorProvider(R.color.widget_today_bg_soft)
    val chipText = ColorProvider(R.color.widget_chip_text)
    val bannerBg = ColorProvider(R.color.widget_banner_bg)
    val bannerText = ColorProvider(R.color.widget_banner_text)
    val titleBadgeBg = ColorProvider(R.color.widget_title_badge_bg)
}

/** 预留统一入口；主题色由 [ScheduleWidgetGlanceColors] 的资源夜模式解析。 */
@Composable
fun ScheduleGlanceTheme(content: @Composable () -> Unit) {
    content()
}
