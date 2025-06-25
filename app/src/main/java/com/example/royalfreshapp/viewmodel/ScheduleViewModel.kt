package com.example.royalfreshapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.example.royalfreshapp.database.ScheduleDatabase
import com.example.royalfreshapp.repository.ScheduleRepository
import com.example.royalfreshapp.ui.screens.ScheduleItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: ScheduleRepository
    val allSchedules: LiveData<List<ScheduleItem>>
    
    init {
        val scheduleDao = ScheduleDatabase.getDatabase(application).scheduleDao()
        repository = ScheduleRepository(scheduleDao)
        allSchedules = repository.allSchedules
    }
    
    // Insert a new schedule
    fun insert(scheduleItem: ScheduleItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.insert(scheduleItem)
    }
    
    // Update an existing schedule
    fun update(scheduleItem: ScheduleItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.update(scheduleItem)
    }
    
    // Delete a schedule
    fun delete(scheduleItem: ScheduleItem) = viewModelScope.launch(Dispatchers.IO) {
        repository.delete(scheduleItem)
    }
    
    // Update toggle state
    fun updateToggleState(scheduleItem: ScheduleItem, isOn: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        repository.updateToggleState(scheduleItem.id, isOn)
    }
}
