package ren.hieu.sensenapp.ui.curriculum

import ren.hieu.sensenapp.data.model.CourseEventDto

data class CourseEditorRequest(
    val weekday: Int,
    val startPeriod: Int,
    val weekNum: Int,
    val existing: CourseEventDto? = null,
)
