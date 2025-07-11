package com.example.royalfreshapp.database

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.Dao

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY id ASC")
    fun getAllSchedules(): LiveData<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(schedule: ScheduleEntity): Long

    @Update
    suspend fun update(schedule: ScheduleEntity)

    @Delete
    suspend fun delete(schedule: ScheduleEntity)

    @Query("DELETE FROM schedules WHERE id = :scheduleId")
    suspend fun deleteById(scheduleId: Long)

    @Query("UPDATE schedules SET isOn = :isOn WHERE id = :scheduleId")
    suspend fun updateToggleState(scheduleId: Long, isOn: Boolean)
}
