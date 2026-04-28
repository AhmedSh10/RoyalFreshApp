package com.example.royalfreshapp.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.royalfreshapp.ui.screens.ScheduleItem

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timeRange: String,
    val frequency: String,
    val deviceId: String,
    val workingTime: Int,
    val pauseTime: Int,
    val isOn: Boolean
) {
    // Convert Entity to UI model
    fun toScheduleItem(): ScheduleItem {
        return ScheduleItem(
            id = id,
            timeRange = timeRange,
            frequency = frequency,
            deviceId = deviceId,
            workingTime = workingTime,
            pauseTime = pauseTime,
            isOn = isOn
        )
    }

    companion object {
        // Convert UI model to Entity
        fun fromScheduleItem(scheduleItem: ScheduleItem): ScheduleEntity {
            return ScheduleEntity(
                id = scheduleItem.id,
                timeRange = scheduleItem.timeRange,
                frequency = scheduleItem.frequency,
                deviceId = scheduleItem.deviceId,
                workingTime = scheduleItem.workingTime,
                pauseTime = scheduleItem.pauseTime,
                isOn = scheduleItem.isOn
            )
        }
    }
}
