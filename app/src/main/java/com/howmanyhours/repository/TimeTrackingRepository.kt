package com.howmanyhours.repository

import com.howmanyhours.data.dao.ProjectDao
import com.howmanyhours.data.dao.TimeEntryDao
import com.howmanyhours.data.entities.Project
import com.howmanyhours.data.entities.TimeEntry
import kotlinx.coroutines.flow.Flow
import java.util.*
import java.util.Calendar

class TimeTrackingRepository(
    private val projectDao: ProjectDao,
    private val timeEntryDao: TimeEntryDao
) {
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun getProjectById(projectId: Long): Project? = projectDao.getProjectById(projectId)

    suspend fun getActiveProject(): Project? = projectDao.getActiveProject()

    suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)

    suspend fun updateProject(project: Project) = projectDao.updateProject(project)

    suspend fun deleteProject(project: Project) = projectDao.deleteProject(project)

    suspend fun activateProject(projectId: Long) {
        projectDao.deactivateAllProjects()
        projectDao.activateProject(projectId)
    }

    fun getTimeEntriesForProject(projectId: Long): Flow<List<TimeEntry>> = 
        timeEntryDao.getTimeEntriesForProject(projectId)

    suspend fun getRunningTimeEntry(): TimeEntry? = timeEntryDao.getRunningTimeEntry()

    suspend fun getRunningTimeEntryForProject(projectId: Long): TimeEntry? = 
        timeEntryDao.getRunningTimeEntryForProject(projectId)

    fun getTimeEntriesForMonth(projectId: Long, year: Int, month: Int): Flow<List<TimeEntry>> {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.time

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.time

        return timeEntryDao.getTimeEntriesForMonth(projectId, monthStart, monthEnd)
    }

    suspend fun getAllTimeEntries(): List<TimeEntry> = timeEntryDao.getAllTimeEntries()

    suspend fun startTracking(projectId: Long): TimeEntry {
        stopAllTracking()
        activateProject(projectId)
        
        val timeEntry = TimeEntry(
            projectId = projectId,
            startTime = Date(),
            isRunning = true
        )
        val id = timeEntryDao.insertTimeEntry(timeEntry)
        return timeEntry.copy(id = id)
    }

    suspend fun stopTracking(): TimeEntry? {
        val runningEntry = getRunningTimeEntry()
        if (runningEntry != null) {
            val updatedEntry = runningEntry.copy(
                endTime = Date(),
                isRunning = false
            )
            timeEntryDao.updateTimeEntry(updatedEntry)
            return updatedEntry
        }
        return null
    }

    suspend fun discardTracking(): TimeEntry? {
        val runningEntry = getRunningTimeEntry()
        if (runningEntry != null) {
            timeEntryDao.deleteTimeEntry(runningEntry)
            return runningEntry
        }
        return null
    }

    private suspend fun stopAllTracking() {
        timeEntryDao.stopAllRunningEntries(Date())
    }

    suspend fun getTotalHoursForMonth(projectId: Long, year: Int, month: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.time

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.time

        val entries = timeEntryDao.getTimeEntriesForMonth(projectId, monthStart, monthEnd)
        // Since we need the actual data, we'll collect it
        // In a real app, you might want to create a suspend function for this
        return 0 // Placeholder - will be calculated in the ViewModel
    }

    suspend fun setMonthlyHours(projectId: Long, year: Int, month: Int, newHoursInMinutes: Long) {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.time

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.time

        // Delete all existing entries for this month and project
        timeEntryDao.deleteTimeEntriesForMonth(projectId, monthStart, monthEnd)

        // Create a new entry with the specified hours if > 0
        if (newHoursInMinutes > 0) {
            val now = Date()
            val timeEntry = TimeEntry(
                projectId = projectId,
                startTime = now,
                endTime = now,
                isRunning = false
            )
            // We need to manually set the duration by calculating the end time
            val endTime = Date(now.time + (newHoursInMinutes * 60 * 1000))
            val finalEntry = timeEntry.copy(endTime = endTime)
            timeEntryDao.insertTimeEntry(finalEntry)
        }
    }

    suspend fun addTimeEntry(projectId: Long, hoursInMinutes: Long) {
        if (hoursInMinutes > 0) {
            val now = Date()
            val timeEntry = TimeEntry(
                projectId = projectId,
                startTime = now,
                endTime = now,
                isRunning = false
            )
            // Calculate the end time to represent the duration
            val endTime = Date(now.time + (hoursInMinutes * 60 * 1000))
            val finalEntry = timeEntry.copy(endTime = endTime)
            timeEntryDao.insertTimeEntry(finalEntry)
        }
    }
}