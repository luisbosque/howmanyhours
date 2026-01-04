package com.howmanyhours.repository

import com.howmanyhours.data.dao.PeriodCloseDao
import com.howmanyhours.data.dao.ProjectDao
import com.howmanyhours.data.dao.TimeEntryDao
import com.howmanyhours.data.entities.PeriodClose
import com.howmanyhours.data.entities.Project
import com.howmanyhours.data.entities.TimeEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import java.util.TimeZone

data class Period(
    val projectId: Long,
    val startTime: Date,
    val endTime: Date,
    val isCurrent: Boolean,
    val closeRecord: PeriodClose? = null
) {
    fun getLabel(): String {
        return if (closeRecord?.isAutomatic == true) {
            val fmt = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            fmt.timeZone = TimeZone.getDefault()
            fmt.format(startTime)
        } else {
            val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            fmt.timeZone = TimeZone.getDefault()
            "${fmt.format(startTime)} - ${fmt.format(endTime)}"
        }
    }
}

class TimeTrackingRepository(
    private val projectDao: ProjectDao,
    private val timeEntryDao: TimeEntryDao,
    private val periodCloseDao: PeriodCloseDao
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
        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(year, month, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val monthStart = calendar.time

        calendar.add(Calendar.MONTH, 1)
        val monthEnd = calendar.time

        return timeEntryDao.getTimeEntriesForMonth(projectId, monthStart, monthEnd)
    }

    suspend fun getAllTimeEntries(): List<TimeEntry> = timeEntryDao.getAllTimeEntries()

    suspend fun getTimeEntriesBatch(limit: Int, offset: Int): List<TimeEntry> =
        timeEntryDao.getTimeEntriesBatch(limit, offset)

    suspend fun getTimeEntriesCount(): Int = timeEntryDao.getTimeEntriesCount()

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

    suspend fun addTimeEntry(projectId: Long, hoursInMinutes: Long, name: String? = null) {
        if (hoursInMinutes > 0) {
            val now = Date()
            val timeEntry = TimeEntry(
                projectId = projectId,
                startTime = now,
                endTime = now,
                isRunning = false,
                name = name
            )
            // Calculate the end time to represent the duration
            val endTime = Date(now.time + (hoursInMinutes * 60 * 1000))
            val finalEntry = timeEntry.copy(endTime = endTime)
            timeEntryDao.insertTimeEntry(finalEntry)
        }
    }

    // Period management methods

    suspend fun getCurrentPeriod(projectId: Long): Period {
        val project = getProjectById(projectId) ?: throw IllegalArgumentException("Project not found")
        val lastClose = periodCloseDao.getLastCloseBefore(projectId, Date())

        val periodStart = if (lastClose != null) {
            Date(lastClose.closeTime.time + 1)
        } else {
            project.createdAt
        }

        val periodEnd = Date()

        return Period(
            projectId = projectId,
            startTime = periodStart,
            endTime = periodEnd,
            isCurrent = true
        )
    }

    suspend fun getAllPeriods(projectId: Long): List<Period> {
        val project = getProjectById(projectId) ?: throw IllegalArgumentException("Project not found")
        val closes = periodCloseDao.getPeriodsForProject(projectId).first()

        val periods = mutableListOf<Period>()
        var currentStart = project.createdAt

        for (close in closes) {
            periods.add(Period(
                projectId = projectId,
                startTime = currentStart,
                endTime = close.closeTime,
                isCurrent = false,
                closeRecord = close
            ))
            currentStart = Date(close.closeTime.time + 1)
        }

        periods.add(Period(
            projectId = projectId,
            startTime = currentStart,
            endTime = Date(),
            isCurrent = true
        ))

        return periods
    }

    suspend fun closePeriod(projectId: Long) {
        val project = getProjectById(projectId) ?: throw IllegalArgumentException("Project not found")
        if (project.periodMode != "manual") {
            throw IllegalStateException("Can only manually close periods in manual mode")
        }

        val closeTime = Date()
        val periodClose = PeriodClose(
            projectId = projectId,
            closeTime = closeTime,
            isAutomatic = false
        )
        periodCloseDao.insertPeriodClose(periodClose)
    }

    suspend fun checkMonthlyAutoClose(projectId: Long) {
        val project = getProjectById(projectId) ?: return
        if (project.periodMode != "monthly") return

        val calendar = Calendar.getInstance(TimeZone.getDefault())
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val currentMonthStart = calendar.time

        // Only create a close for the current month if the project was created before this month
        // This prevents creating a backwards period for projects created during the current month
        if (project.createdAt >= currentMonthStart) {
            // Project was created in the current month, no close needed yet
            return
        }

        val existingClose = periodCloseDao.getAutoCloseAtTime(projectId, currentMonthStart)
        if (existingClose == null) {
            val periodClose = PeriodClose(
                projectId = projectId,
                closeTime = currentMonthStart,
                isAutomatic = true
            )
            periodCloseDao.insertPeriodClose(periodClose)
        }
    }

    suspend fun changePeriodMode(projectId: Long, newMode: String) {
        val project = getProjectById(projectId) ?: throw IllegalArgumentException("Project not found")
        val updatedProject = project.copy(periodMode = newMode)
        updateProject(updatedProject)
    }

    fun getEntriesForPeriod(period: Period): Flow<List<TimeEntry>> {
        return if (period.isCurrent) {
            timeEntryDao.getTimeEntriesForCurrentPeriod(period.projectId, period.startTime)
        } else {
            timeEntryDao.getTimeEntriesForPeriod(period.projectId, period.startTime, period.endTime)
        }
    }

    suspend fun updateEntryName(entryId: Long, newName: String) {
        val entry = timeEntryDao.getAllTimeEntries().find { it.id == entryId }
        entry?.let {
            val updatedEntry = it.copy(name = newName.trim().ifEmpty { null })
            timeEntryDao.updateTimeEntry(updatedEntry)
        }
    }

    suspend fun getAllPeriodCloses(): List<PeriodClose> {
        return periodCloseDao.getAllPeriodCloses()
    }
}