package com.howmanyhours.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howmanyhours.data.entities.Project
import com.howmanyhours.data.entities.TimeEntry
import com.howmanyhours.repository.TimeTrackingRepository
import com.howmanyhours.services.TimeTrackingNotificationService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import java.util.*

class TimeTrackingViewModel(
    private val repository: TimeTrackingRepository,
    private val context: Context
) : ViewModel() {
    
    companion object {
        private const val TAG = "TimeTrackingViewModel"
    }
    
    private val _uiState = MutableStateFlow(TimeTrackingUiState())
    val uiState: StateFlow<TimeTrackingUiState> = _uiState.asStateFlow()

    val projects = repository.getAllProjects()
    
    private var timerJob: Job? = null
    private var monthlyHoursJob: Job? = null

    init {
        loadActiveProject()
        loadRunningTimeEntry()
        refreshAllProjectsMonthlyHours()
        startTimerIfNeeded()
        
        // Force immediate refresh to ensure UI is up to date
        refreshCurrentState()
    }

    private fun loadActiveProject() {
        viewModelScope.launch {
            val activeProject = repository.getActiveProject()
            _uiState.update { it.copy(activeProject = activeProject) }
            
            activeProject?.let { project ->
                loadMonthlyHours(project.id)
            }
        }
    }

    private fun loadRunningTimeEntry() {
        viewModelScope.launch {
            val runningEntry = repository.getRunningTimeEntry()
            _uiState.update { 
                it.copy(
                    runningTimeEntry = runningEntry,
                    isTracking = runningEntry?.isRunning == true
                ) 
            }
            
            // If we have a running entry, make sure the time is up to date
            if (runningEntry?.isRunning == true) {
                updateCurrentRunTime()
            }
        }
    }
    
    private fun startTimerIfNeeded() {
        viewModelScope.launch {
            val runningEntry = repository.getRunningTimeEntry()
            if (runningEntry?.isRunning == true) {
                // Update the UI state immediately with current data
                _uiState.update { 
                    it.copy(
                        runningTimeEntry = runningEntry,
                        isTracking = true
                    )
                }
                
                startTimer()
                
                // Resume notification service if tracking was active
                val activeProject = _uiState.value.activeProject
                if (activeProject != null) {
                    TimeTrackingNotificationService.startService(
                        context,
                        activeProject.name,
                        runningEntry.startTime.time
                    )
                }
            }
        }
    }
    
    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            // Immediate update when starting timer
            updateCurrentRunTime()
            updateMonthlyHours()
            
            while (_uiState.value.isTracking) {
                delay(10000) // Update every 10 seconds instead of 60
                updateCurrentRunTime()
                updateMonthlyHours()
            }
        }
    }
    
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    private suspend fun updateCurrentRunTime() {
        val runningEntry = repository.getRunningTimeEntry()
        if (runningEntry?.isRunning == true) {
            _uiState.update { it.copy(runningTimeEntry = runningEntry) }
        }
    }
    
    private suspend fun updateMonthlyHours() {
        val activeProject = _uiState.value.activeProject
        if (activeProject != null) {
            loadMonthlyHours(activeProject.id)
            // Also refresh all projects monthly hours to keep them in sync
            refreshAllProjectsMonthlyHours()
        }
    }
    
    private fun refreshAllProjectsMonthlyHours() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            
            val currentProjects = projects.first()
            val monthlyHoursMap = mutableMapOf<Long, Long>()
            
            for (project in currentProjects) {
                try {
                    val entries = repository.getTimeEntriesForMonth(project.id, currentYear, currentMonth).first()
                    val totalMinutes = entries.sumOf { it.getDurationInMinutes() }
                    monthlyHoursMap[project.id] = totalMinutes
                } catch (e: Exception) {
                    monthlyHoursMap[project.id] = 0L
                }
            }
            
            _uiState.update { it.copy(projectMonthlyHours = monthlyHoursMap.toMap()) }
        }
    }

    private fun refreshCurrentState() {
        viewModelScope.launch {
            updateCurrentRunTime()
            updateMonthlyHours()
        }
    }

    fun forceRefresh() {
        refreshCurrentState()
        refreshAllProjectsMonthlyHours()
    }

    private fun loadMonthlyHours(projectId: Long) {
        // Cancel any existing monthly hours job to prevent interference
        monthlyHoursJob?.cancel()
        
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH)

        monthlyHoursJob = viewModelScope.launch {
            repository.getTimeEntriesForMonth(projectId, currentYear, currentMonth)
                .collect { entries ->
                    // Only update if this is still the active project
                    if (_uiState.value.activeProject?.id == projectId) {
                        val totalMinutes = entries.sumOf { it.getDurationInMinutes() }
                        _uiState.update { it.copy(monthlyHours = totalMinutes) }
                    }
                }
        }
    }

    private fun loadAllProjectsMonthlyHours() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)

            projects.collect { projectList ->
                // Build the monthly hours map properly for all projects
                val monthlyHoursMap = mutableMapOf<Long, Long>()
                
                // Process each project synchronously to avoid race conditions
                for (project in projectList) {
                    try {
                        val entries = repository.getTimeEntriesForMonth(project.id, currentYear, currentMonth).first()
                        val totalMinutes = entries.sumOf { it.getDurationInMinutes() }
                        monthlyHoursMap[project.id] = totalMinutes
                    } catch (e: Exception) {
                        // If there's an error loading entries for a project, set to 0
                        monthlyHoursMap[project.id] = 0L
                    }
                }
                
                // Update UI state once with all project hours
                _uiState.update { it.copy(projectMonthlyHours = monthlyHoursMap.toMap()) }
            }
        }
    }

    fun createProject(name: String) {
        viewModelScope.launch {
            val project = Project(name = name)
            repository.insertProject(project)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_uiState.value.activeProject?.id == project.id) {
                _uiState.update { it.copy(activeProject = null, runningTimeEntry = null) }
            }
        }
    }

    fun startTracking(project: Project) {
        viewModelScope.launch {
            val currentRunning = repository.getRunningTimeEntry()
            if (currentRunning != null && currentRunning.projectId != project.id) {
                _uiState.update { it.copy(showSwitchConfirmation = true, pendingProject = project) }
            } else {
                val timeEntry = repository.startTracking(project.id)
                _uiState.update { 
                    it.copy(
                        activeProject = project,
                        runningTimeEntry = timeEntry,
                        isTracking = true
                    )
                }
                loadMonthlyHours(project.id)
                startTimer()
                
                // Start notification service
                Log.d(TAG, "Starting notification service for project: ${project.name}, startTime: ${timeEntry.startTime.time}")
                TimeTrackingNotificationService.startService(
                    context,
                    project.name,
                    timeEntry.startTime.time
                )
            }
        }
    }

    fun stopTracking() {
        viewModelScope.launch {
            repository.stopTracking()
            stopTimer()
            _uiState.update { 
                it.copy(
                    runningTimeEntry = null,
                    isTracking = false
                )
            }
            
            // Stop notification service
            Log.d(TAG, "Stopping notification service")
            TimeTrackingNotificationService.stopService(context)
            
            // Update monthly hours after stopping - with a small delay to ensure DB is updated
            val activeProject = _uiState.value.activeProject
            if (activeProject != null) {
                delay(100) // Small delay to ensure database write completes
                loadMonthlyHours(activeProject.id)
                refreshAllProjectsMonthlyHours() // Refresh all projects' monthly hours
            }
        }
    }

    fun discardTracking() {
        viewModelScope.launch {
            repository.discardTracking()
            stopTimer()
            _uiState.update { 
                it.copy(
                    runningTimeEntry = null,
                    isTracking = false
                )
            }
            
            // Stop notification service
            Log.d(TAG, "Stopping notification service")
            TimeTrackingNotificationService.stopService(context)
            
            // Monthly hours don't need to be updated since entry was discarded
        }
    }

    fun selectProject(project: Project) {
        viewModelScope.launch {
            // Cancel any existing monthly hours job immediately
            monthlyHoursJob?.cancel()
            
            repository.activateProject(project.id)
            _uiState.update { it.copy(activeProject = project, monthlyHours = 0) } // Reset to 0 immediately
            
            // Small delay to ensure state is properly updated before loading new data
            delay(50)
            loadMonthlyHours(project.id) // Then load correct hours
            
            // Force refresh to ensure current tracking time is up to date
            refreshCurrentState()
        }
    }

    fun switchProject(newProject: Project) {
        viewModelScope.launch {
            val currentRunning = repository.getRunningTimeEntry()
            if (currentRunning != null) {
                _uiState.update { it.copy(showSwitchConfirmation = true, pendingProject = newProject) }
            } else {
                startTracking(newProject)
            }
        }
    }

    fun confirmSwitchProject() {
        viewModelScope.launch {
            val pendingProject = _uiState.value.pendingProject
            if (pendingProject != null) {
                stopTimer()
                repository.stopTracking()
                
                // Stop current notification service
                TimeTrackingNotificationService.stopService(context)
                
                startTracking(pendingProject)
                _uiState.update { 
                    it.copy(
                        showSwitchConfirmation = false,
                        pendingProject = null
                    )
                }
            }
        }
    }

    fun discardAndSwitchProject() {
        viewModelScope.launch {
            val pendingProject = _uiState.value.pendingProject
            if (pendingProject != null) {
                stopTimer()
                repository.discardTracking()
                
                // Stop current notification service
                TimeTrackingNotificationService.stopService(context)
                
                startTracking(pendingProject)
                _uiState.update { 
                    it.copy(
                        showSwitchConfirmation = false,
                        pendingProject = null
                    )
                }
            }
        }
    }

    fun cancelSwitchProject() {
        _uiState.update { 
            it.copy(
                showSwitchConfirmation = false,
                pendingProject = null
            )
        }
    }

    suspend fun exportToCsv(): String {
        val projects = repository.getAllProjects().first()
        val allEntries = repository.getAllTimeEntries()
        
        val csv = StringBuilder()
        csv.appendLine("Project,Start Time,End Time,Duration (minutes)")
        
        for (entry in allEntries) {
            val project = projects.find { it.id == entry.projectId }
            csv.appendLine(
                "${project?.name ?: "Unknown"},${entry.startTime},${entry.endTime ?: "Running"},${entry.getDurationInMinutes()}"
            )
        }
        
        return csv.toString()
    }

    fun setMonthlyHours(projectId: Long, newHoursInMinutes: Long) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            
            repository.setMonthlyHours(projectId, currentYear, currentMonth, newHoursInMinutes)
            
            // Refresh the monthly hours display
            if (_uiState.value.activeProject?.id == projectId) {
                loadMonthlyHours(projectId)
            }
            loadAllProjectsMonthlyHours() // Refresh all projects' monthly hours
        }
    }

    fun addTimeEntry(projectId: Long, hoursInMinutes: Long) {
        viewModelScope.launch {
            repository.addTimeEntry(projectId, hoursInMinutes)
            
            // Refresh the monthly hours display
            if (_uiState.value.activeProject?.id == projectId) {
                loadMonthlyHours(projectId)
            }
            loadAllProjectsMonthlyHours() // Refresh all projects' monthly hours
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTimer()
        monthlyHoursJob?.cancel()
    }

    class Factory(
        private val repository: TimeTrackingRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimeTrackingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TimeTrackingViewModel(repository, context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class TimeTrackingUiState(
    val activeProject: Project? = null,
    val runningTimeEntry: TimeEntry? = null,
    val isTracking: Boolean = false,
    val monthlyHours: Long = 0,
    val projectMonthlyHours: Map<Long, Long> = emptyMap(),
    val showSwitchConfirmation: Boolean = false,
    val pendingProject: Project? = null
)