package net.luisico.howmanyhours.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import net.luisico.howmanyhours.data.entities.Project
import net.luisico.howmanyhours.data.entities.TimeEntry
import net.luisico.howmanyhours.repository.Period
import net.luisico.howmanyhours.repository.TimeTrackingRepository
import net.luisico.howmanyhours.services.TimeTrackingNotificationService
import net.luisico.howmanyhours.backup.BackupManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import java.util.*
import java.util.TimeZone

class TimeTrackingViewModel(
    private val repository: TimeTrackingRepository,
    private val context: Context
) : ViewModel() {

    // Public access for backup functionality
    internal val backupRepository: TimeTrackingRepository get() = repository

    // Backup manager
    private val backupManager = BackupManager(context, repository)

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

        // Check and create automatic backup if needed
        checkAutoBackup()

        // Check monthly auto-close for all projects in monthly mode
        viewModelScope.launch {
            projects.first().filter { it.periodMode == "monthly" }.forEach { project ->
                repository.checkMonthlyAutoClose(project.id)
            }
        }
    }

    private fun loadActiveProject() {
        viewModelScope.launch {
            val activeProject = repository.getActiveProject()
            _uiState.update { it.copy(activeProject = activeProject) }

            activeProject?.let { project ->
                loadCurrentPeriod(project.id)
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
            val calendar = Calendar.getInstance(TimeZone.getDefault())
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

    fun forceRefreshAfterRestore() {
        viewModelScope.launch {
            // Complete refresh after database restore
            // Reset UI state to initial values
            _uiState.update {
                TimeTrackingUiState() // Reset to empty state
            }

            // Cancel all running jobs to prevent conflicts
            timerJob?.cancel()
            monthlyHoursJob?.cancel()

            // Reload everything from the restored database
            loadActiveProject()
            loadRunningTimeEntry()
            refreshAllProjectsMonthlyHours()
            startTimerIfNeeded()

            // Force refresh to ensure UI is up to date
            refreshCurrentState()
        }
    }

    private fun checkAutoBackup() {
        viewModelScope.launch {
            try {
                backupManager.checkAndCreateBackupIfNeeded()
            } catch (e: Exception) {
                Log.w(TAG, "Auto backup failed: ${e.message}")
            }
        }
    }

    private fun loadMonthlyHours(projectId: Long) {
        // Cancel any existing monthly hours job to prevent interference
        monthlyHoursJob?.cancel()

        val calendar = Calendar.getInstance(TimeZone.getDefault())
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
            val calendar = Calendar.getInstance(TimeZone.getDefault())
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
                _uiState.update { it.copy(activeProject = null, runningTimeEntry = null, monthlyHours = 0) }
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
                loadCurrentPeriod(project.id)
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
            
            // Update period hours after stopping - with a small delay to ensure DB is updated
            val activeProject = _uiState.value.activeProject
            if (activeProject != null) {
                delay(100) // Small delay to ensure database write completes
                loadCurrentPeriod(activeProject.id)
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
            loadCurrentPeriod(project.id) // Load current period hours

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

    suspend fun exportToCsvToStream(outputStream: java.io.OutputStream) {
        val projects = repository.getAllProjects().first()
        val writer = outputStream.bufferedWriter()

        try {
            // Write CSV header
            writer.write("Project,Entry Name,Start Time,End Time,Duration (minutes)\n")

            // Process entries in batches of 1000 to avoid loading everything into memory
            val batchSize = 1000
            var offset = 0
            var batch: List<net.luisico.howmanyhours.data.entities.TimeEntry>

            do {
                batch = repository.getTimeEntriesBatch(batchSize, offset)

                for (entry in batch) {
                    val project = projects.find { it.id == entry.projectId }
                    // Properly escape quotes in entry names for CSV
                    val entryName = entry.name?.let {
                        "\"${it.replace("\"", "\"\"")}\""  // Escape quotes by doubling them
                    } ?: ""

                    writer.write(
                        "${project?.name ?: "Unknown"},$entryName,${entry.startTime},${entry.endTime ?: "Running"},${entry.getDurationInMinutes()}\n"
                    )
                }

                offset += batchSize
            } while (batch.size == batchSize)  // Continue while we got a full batch

            writer.flush()
        } finally {
            writer.close()
        }
    }

    fun addTimeEntry(projectId: Long, hoursInMinutes: Long, name: String? = null) {
        viewModelScope.launch {
            repository.addTimeEntry(projectId, hoursInMinutes, name)

            // Refresh the period hours display
            if (_uiState.value.activeProject?.id == projectId) {
                loadCurrentPeriod(projectId)
            }
            loadAllProjectsMonthlyHours() // Refresh all projects' monthly hours
        }
    }

    // Period management methods

    fun loadCurrentPeriod(projectId: Long) {
        viewModelScope.launch {
            try {
                val period = repository.getCurrentPeriod(projectId)
                _uiState.update { it.copy(currentPeriod = period) }

                // Load hours for this specific period (not calendar month)
                loadCurrentPeriodHours(projectId, period)

                // Also check for monthly auto-close
                repository.checkMonthlyAutoClose(projectId)
            } catch (e: Exception) {
                Log.e(TAG, "Error loading current period", e)
            }
        }
    }

    private fun loadCurrentPeriodHours(@Suppress("UNUSED_PARAMETER") projectId: Long, period: Period) {
        viewModelScope.launch {
            try {
                repository.getEntriesForPeriod(period).collect { entries ->
                    val totalMinutes = entries.sumOf { it.getDurationInMinutes() }
                    _uiState.update { it.copy(monthlyHours = totalMinutes) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading period hours", e)
            }
        }
    }

    fun loadAllPeriods(projectId: Long) {
        viewModelScope.launch {
            try {
                val periods = repository.getAllPeriods(projectId)
                _uiState.update { it.copy(allPeriods = periods) }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading all periods", e)
            }
        }
    }

    fun selectPeriod(period: Period) {
        viewModelScope.launch {
            repository.getEntriesForPeriod(period).collect { entries ->
                _uiState.update {
                    it.copy(
                        selectedPeriod = period,
                        periodEntries = entries
                    )
                }
            }
        }
    }

    fun closeCurrentPeriod(projectId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                // Check if there's active tracking
                val runningEntry = _uiState.value.runningTimeEntry
                if (runningEntry != null && runningEntry.projectId == projectId) {
                    // Stop tracking before closing period
                    stopTracking()
                }

                repository.closePeriod(projectId)
                loadCurrentPeriod(projectId)
                loadAllPeriods(projectId)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing period", e)
                onError(e.message ?: "Failed to close period")
            }
        }
    }

    fun changePeriodMode(projectId: Long, newMode: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                repository.changePeriodMode(projectId, newMode)
                loadCurrentPeriod(projectId)
                loadAllPeriods(projectId)
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Error changing period mode", e)
                onError(e.message ?: "Failed to change period mode")
            }
        }
    }

    fun getProjectById(projectId: Long): Flow<Project?> {
        return flow {
            emit(repository.getProjectById(projectId))
        }
    }

    fun updateRunningEntryName(newName: String) {
        viewModelScope.launch {
            val runningEntry = _uiState.value.runningTimeEntry
            if (runningEntry != null) {
                repository.updateEntryName(runningEntry.id, newName)
                // Reload the running entry to update UI
                val updatedEntry = repository.getRunningTimeEntry()
                _uiState.update { it.copy(runningTimeEntry = updatedEntry) }
            }
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
    val pendingProject: Project? = null,
    val currentPeriod: Period? = null,
    val allPeriods: List<Period> = emptyList(),
    val selectedPeriod: Period? = null,
    val periodEntries: List<TimeEntry> = emptyList()
)