package ren.hieu.sensenapp.domain

import androidx.compose.ui.graphics.Color

object CurriculumPalette {
    private val classicColors = listOf(
        0xFF6d8fa3, 0xFF7a9a88, 0xFFa08f7e, 0xFF8f849e, 0xFF7d9170, 0xFF9a8490,
        0xFF688998, 0xFF8a9680, 0xFF9a8878, 0xFF7b8fa0, 0xFF8e82a0, 0xFF6f9589,
        0xFFa08678, 0xFF8a7d98, 0xFF6f8f9f, 0xFF7f9688, 0xFF9985a0, 0xFF8f9470,
    ).map { Color(it.toInt()) }

    fun hashString(str: String): Int {
        var hash = 0
        for (ch in str) {
            hash = ch.code + ((hash shl 5) - hash)
        }
        return kotlin.math.abs(hash)
    }

    fun classicColor(paletteIndex: Int): Color {
        return classicColors[paletteIndex.mod(classicColors.size)]
    }

    /**
     * 为每门课分配颜色。键一般为「课程名|教师」，同名同师同色；
     * 按名称排序后依次取色，尽量让不同课使用不同色相。
     */
    fun colorsForCourses(keys: Collection<String>): Map<String, Color> {
        val size = classicColors.size
        val distinct = keys.map { it.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        if (distinct.isEmpty()) return emptyMap()

        val usageCount = IntArray(size)
        return buildMap {
            for ((index, key) in distinct.withIndex()) {
                var idx = if (index < size) index else hashString(key) % size
                if (usageCount[idx] > 0) {
                    var best = idx
                    var minUse = usageCount[best]
                    for (candidate in 0 until size) {
                        if (usageCount[candidate] < minUse) {
                            best = candidate
                            minUse = usageCount[candidate]
                        }
                    }
                    idx = best
                }
                usageCount[idx]++
                put(key, classicColor(idx))
            }
        }
    }
}
