package ren.hieu.sensenapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_cache")
data class ScheduleCacheEntity(
    @PrimaryKey val id: Int = 1,
    val school: String,
    val scheduleJson: String,
    val holidaysJson: String,
    val revision: String,
    val syncedAtEpochMs: Long,
)
