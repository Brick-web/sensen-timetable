package ren.hieu.sensenapp.widget

import ren.hieu.sensenapp.domain.GridCourse
import ren.hieu.sensenapp.domain.WeekDateCell

data class WeekWidgetData(
    val weekNum: Int,
    val weekRangeText: String,
    /** index 0 = 周一 … 6 = 周日，已应用调休映射 */
    val columnCourses: List<List<GridCourse>>,
    val weekDates: List<WeekDateCell>,
    /** 1=周一 … 7=周日，用于高亮今天列 */
    val todayWeekday: Int,
)
