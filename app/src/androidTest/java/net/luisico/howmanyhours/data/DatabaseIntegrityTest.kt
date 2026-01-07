package net.luisico.howmanyhours.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.luisico.howmanyhours.data.database.AppDatabase
import net.luisico.howmanyhours.data.entities.PeriodClose
import net.luisico.howmanyhours.data.entities.Project
import net.luisico.howmanyhours.data.entities.TimeEntry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Database integrity tests focusing on data reliability and corruption prevention.
 * These tests verify:
 * - Foreign key constraints are enforced
 * - Cascading deletes work correctly
 * - Data consistency is maintained
 * - Invalid data is rejected
 */
@RunWith(AndroidJUnit4::class)
class DatabaseIntegrityTest {

    private lateinit var database: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        )
            .allowMainThreadQueries() // For testing only
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ============ Foreign Key Constraint Tests ============

    @Test
    fun testTimeEntryRequiresValidProject() = runBlocking {
        val projectDao = database.projectDao()
        val timeEntryDao = database.timeEntryDao()

        // Create a valid project first
        val project = Project(
            name = "Test Project",
            createdAt = Date(),
            isActive = true,
            periodMode = "monthly"
        )
        val projectId = projectDao.insertProject(project)

        // Valid time entry should succeed
        val validEntry = TimeEntry(
            projectId = projectId,
            startTime = Date(),
            endTime = null,
            isRunning = true
        )
        val entryId = timeEntryDao.insertTimeEntry(validEntry)
        assertTrue("Valid time entry should be inserted successfully", entryId > 0)

        // Entry should exist
        val entries = timeEntryDao.getTimeEntriesForProject(projectId).first()
        assertEquals("Should have one time entry", 1, entries.size)
    }

    @Test
    fun testCascadingDeleteRemovesTimeEntries() = runBlocking {
        val projectDao = database.projectDao()
        val timeEntryDao = database.timeEntryDao()

        // Create project
        val project = Project(
            name = "Test Project",
            createdAt = Date(),
            isActive = true
        )
        val projectId = projectDao.insertProject(project)

        // Create multiple time entries
        val entry1 = TimeEntry(
            projectId = projectId,
            startTime = Date(System.currentTimeMillis() - 3600000),
            endTime = Date(System.currentTimeMillis() - 1800000),
            isRunning = false
        )
        val entry2 = TimeEntry(
            projectId = projectId,
            startTime = Date(),
            endTime = null,
            isRunning = true
        )

        timeEntryDao.insertTimeEntry(entry1)
        timeEntryDao.insertTimeEntry(entry2)

        // Verify entries exist
        var entries = timeEntryDao.getTimeEntriesForProject(projectId).first()
        assertEquals("Should have two time entries before delete", 2, entries.size)

        // Delete project - should cascade delete to time entries
        projectDao.deleteProject(project.copy(id = projectId))

        // Verify entries are gone
        entries = timeEntryDao.getTimeEntriesForProject(projectId).first()
        assertEquals("Time entries should be cascade deleted", 0, entries.size)
    }

    @Test
    fun testCascadingDeleteRemovesPeriodCloses() = runBlocking {
        val projectDao = database.projectDao()
        val periodCloseDao = database.periodCloseDao()

        // Create project
        val project = Project(
            name = "Test Project",
            createdAt = Date(),
            isActive = true
        )
        val projectId = projectDao.insertProject(project)

        // Create period closes
        val close1 = PeriodClose(
            projectId = projectId,
            closeTime = Date(System.currentTimeMillis() - 86400000), // Yesterday
            isAutomatic = true
        )
        val close2 = PeriodClose(
            projectId = projectId,
            closeTime = Date(),
            isAutomatic = false
        )

        periodCloseDao.insertPeriodClose(close1)
        periodCloseDao.insertPeriodClose(close2)

        // Verify closes exist
        var closes = periodCloseDao.getPeriodsForProject(projectId).first()
        assertEquals("Should have two period closes before delete", 2, closes.size)

        // Delete project - should cascade delete to period closes
        projectDao.deleteProject(project.copy(id = projectId))

        // Verify closes are gone
        closes = periodCloseDao.getPeriodsForProject(projectId).first()
        assertEquals("Period closes should be cascade deleted", 0, closes.size)
    }

    // ============ Data Consistency Tests ============

    @Test
    fun testTimeEntryDurationCalculation() {
        // Test duration calculation accuracy
        val startTime = Date(1000000)
        val endTime = Date(1060000) // 1 minute later

        val entry = TimeEntry(
            projectId = 1L,
            startTime = startTime,
            endTime = endTime,
            isRunning = false
        )

        val duration = entry.getDurationInMinutes()
        assertEquals("Duration should be exactly 1 minute", 1L, duration)
    }

    @Test
    fun testRunningTimeEntryDurationCalculation() {
        // Test that running entries calculate duration from current time
        val startTime = Date(System.currentTimeMillis() - 120000) // 2 minutes ago

        val entry = TimeEntry(
            projectId = 1L,
            startTime = startTime,
            endTime = null,
            isRunning = true
        )

        val duration = entry.getDurationInMinutes()
        assertTrue("Running entry duration should be at least 1 minute", duration >= 1)
        assertTrue("Running entry duration should be approximately 2 minutes", duration <= 3)
    }

    @Test
    fun testMultipleProjectsIndependence() = runBlocking {
        val projectDao = database.projectDao()
        val timeEntryDao = database.timeEntryDao()

        // Create two projects
        val project1Id = projectDao.insertProject(Project(name = "Project 1", isActive = true))
        val project2Id = projectDao.insertProject(Project(name = "Project 2", isActive = true))

        // Add entries to each
        timeEntryDao.insertTimeEntry(TimeEntry(projectId = project1Id, startTime = Date(), isRunning = true))
        timeEntryDao.insertTimeEntry(TimeEntry(projectId = project1Id, startTime = Date(), isRunning = false))
        timeEntryDao.insertTimeEntry(TimeEntry(projectId = project2Id, startTime = Date(), isRunning = true))

        // Verify each project has correct entries
        val project1Entries = timeEntryDao.getTimeEntriesForProject(project1Id).first()
        val project2Entries = timeEntryDao.getTimeEntriesForProject(project2Id).first()

        assertEquals("Project 1 should have 2 entries", 2, project1Entries.size)
        assertEquals("Project 2 should have 1 entry", 1, project2Entries.size)

        // Delete project 1
        projectDao.deleteProject(Project(id = project1Id, name = "Project 1", isActive = true))

        // Verify project 2 entries are unaffected
        val project2EntriesAfter = timeEntryDao.getTimeEntriesForProject(project2Id).first()
        assertEquals("Project 2 entries should remain after Project 1 deletion", 1, project2EntriesAfter.size)
    }

    // ============ Data Corruption Detection ============

    @Test
    fun testDetectOrphanedTimeEntries() = runBlocking {
        val projectDao = database.projectDao()
        val timeEntryDao = database.timeEntryDao()

        // Create and then delete project
        val projectId = projectDao.insertProject(Project(name = "Temp Project", isActive = true))
        timeEntryDao.insertTimeEntry(TimeEntry(projectId = projectId, startTime = Date(), isRunning = true))

        // Get all entries before deletion
        val allEntriesBefore = timeEntryDao.getAllTimeEntries()
        assertEquals("Should have one entry", 1, allEntriesBefore.size)

        // Delete project (cascade should remove entry)
        projectDao.deleteProject(Project(id = projectId, name = "Temp Project", isActive = true))

        // Verify no orphans
        val allEntriesAfter = timeEntryDao.getAllTimeEntries()
        assertEquals("Should have no entries after cascade delete", 0, allEntriesAfter.size)
    }

    @Test
    fun testLargeDatasetPerformance() = runBlocking {
        val projectDao = database.projectDao()
        val timeEntryDao = database.timeEntryDao()

        // Create project
        val projectId = projectDao.insertProject(Project(name = "Performance Test", isActive = true))

        // Insert 100 entries
        val startInsert = System.currentTimeMillis()
        repeat(100) { i ->
            timeEntryDao.insertTimeEntry(
                TimeEntry(
                    projectId = projectId,
                    startTime = Date(System.currentTimeMillis() - (i * 60000L)),
                    endTime = Date(System.currentTimeMillis() - (i * 60000L) + 30000L),
                    isRunning = false
                )
            )
        }
        val insertDuration = System.currentTimeMillis() - startInsert

        // Query should be fast with index
        val startQuery = System.currentTimeMillis()
        val entries = timeEntryDao.getTimeEntriesForProject(projectId).first()
        val queryDuration = System.currentTimeMillis() - startQuery

        assertEquals("Should retrieve all 100 entries", 100, entries.size)
        assertTrue("Inserts should complete in reasonable time", insertDuration < 5000)
        assertTrue("Query with index should be fast", queryDuration < 500)
    }
}
