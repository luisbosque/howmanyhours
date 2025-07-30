package com.howmanyhours.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.howmanyhours.data.entities.Project
import com.howmanyhours.data.entities.TimeEntry
import com.howmanyhours.repository.TimeTrackingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import java.util.*

class TimeTrackingViewModel(private val repository: TimeTrackingRepository) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TimeTrackingUiState())
    val uiState: StateFlow<TimeTrackingUiState> = _uiState.asStateFlow()

    val projects = repository.getAllProjects()
    
    private var timerJob: Job? = null
    private var monthlyHoursJob: Job? = null

    init {
        loadActiveProject()
        loadRunningTimeEntry()
        startTimerIfNeeded()
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
        }
    }
    
    private fun startTimerIfNeeded() {
        viewModelScope.launch {
            val runningEntry = repository.getRunningTimeEntry()
            if (runningEntry?.isRunning == true) {
                startTimer()
            }
        }
    }
    
    private fun startTimer() {
        stopTimer()
        timerJob = viewModelScope.launch {
            while (_uiState.value.isTracking) {
                updateCurrentRunTime()
                updateMonthlyHours()
                delay(60000) // Update every minute
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
        }
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
            // Update monthly hours after stopping - with a small delay to ensure DB is updated
            val activeProject = _uiState.value.activeProject
            if (activeProject != null) {
                delay(100) // Small delay to ensure database write completes
                loadMonthlyHours(activeProject.id)
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
        }
    }

    fun addTimeEntry(projectId: Long, hoursInMinutes: Long) {
        viewModelScope.launch {
            repository.addTimeEntry(projectId, hoursInMinutes)
            
            // Refresh the monthly hours display
            if (_uiState.value.activeProject?.id == projectId) {
                loadMonthlyHours(projectId)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopTimer()
        monthlyHoursJob?.cancel()
    }

    class Factory(private val repository: TimeTrackingRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TimeTrackingViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return TimeTrackingViewModel(repository) as T
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
    val showSwitchConfirmation: Boolean = false,
    val pendingProject: Project? = null
)