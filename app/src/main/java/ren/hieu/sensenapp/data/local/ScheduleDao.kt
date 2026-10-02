package ren.hieu.sensenapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule_cache WHERE id = 1 LIMIT 1")
    suspend fun getCache(): ScheduleCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ScheduleCacheEntity)

    @Query("DELETE FROM schedule_cache")
    suspend fun clear()
}
