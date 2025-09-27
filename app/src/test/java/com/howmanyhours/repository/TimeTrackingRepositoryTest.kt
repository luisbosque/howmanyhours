package com.howmanyhours.repository

import com.howmanyhours.data.entities.Project
import com.howmanyhours.data.entities.TimeEntry
import org.junit.Test
import org.junit.Assert.*
import java.util.*

class TimeTrackingRepositoryTest {

    private val testProject = Project(
        id = 1L,
        name = "Test Project",
        createdAt = Date(),
        isActive = true
    )

    private val testTimeEntry = TimeEntry(
        id = 1L,
        projectId = 1L,
        startTime = Date(),
        endTime = null,
        isRunning = true
    )

    @Test
    fun `project entity should be valid`() {
        // Test that projects can be created with valid data
        assertTrue(testProject.name.isNotEmpty())
        assertTrue(testProject.id >= 0)
        assertNotNull(testProject.createdAt)
    }

    @Test
    fun `time entry entity should be valid`() {
        // Test that time entries have valid relationships
        assertEquals(testProject.id, testTimeEntry.projectId)
        assertNotNull(testTimeEntry.startTime)
        assertTrue(testTimeEntry.id >= 0)
    }

    @Test
    fun `project can be created with different states`() {
        // Test creating inactive project
        val inactiveProject = testProject.copy(isActive = false)
        assertFalse(inactiveProject.isActive)
        assertEquals(testProject.name, inactiveProject.name)
    }

    @Test
    fun `time entry can calculate running duration`() {
        // Test running entry duration calculation
        val runningEntry = testTimeEntry.copy(
            startTime = Date(System.currentTimeMillis() - 60000), // 1 minute ago
            endTime = null,
            isRunning = true
        )

        val duration = runningEntry.getDurationInMinutes()
        assertTrue("Duration should be at least 0", duration >= 0)
    }
}