package com.example.royalfreshapp.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.royalfreshapp.database.ScheduleDao
import com.example.royalfreshapp.database.ScheduleEntity
import com.example.royalfreshapp.ui.screens.ScheduleItem

class ScheduleRepository(private val scheduleDao: ScheduleDao) {
    
    // Get all schedules as LiveData of UI models
    val allSchedules: LiveData<List<ScheduleItem>> = scheduleDao.getAllSchedules().map { entities ->
        entities.map { it.toScheduleItem() }
    }
    
    // Insert a new schedule
    suspend fun insert(scheduleItem: ScheduleItem): Long {
        val entity = ScheduleEntity.fromScheduleItem(scheduleItem)
        return scheduleDao.insert(entity)
    }
    
    // Update an existing schedule
    suspend fun update(scheduleItem: ScheduleItem) {
        val entity = ScheduleEntity.fromScheduleItem(scheduleItem)
        scheduleDao.update(entity)
    }
    
    // Delete a schedule
    suspend fun delete(scheduleItem: ScheduleItem) {
        val entity = ScheduleEntity.fromScheduleItem(scheduleItem)
        scheduleDao.delete(entity)
    }
    
    // Delete a schedule by ID
    suspend fun deleteById(scheduleId: Long) {
        scheduleDao.deleteById(scheduleId)
    }
    
    // Update toggle state
    suspend fun updateToggleState(scheduleId: Long, isOn: Boolean) {
        scheduleDao.updateToggleState(scheduleId, isOn)
    }
}
