package com.howmanyhours.viewmodel

import com.howmanyhours.data.entities.Project
import com.howmanyhours.data.entities.TimeEntry
import org.junit.Test
import org.junit.Assert.*
import java.util.*

class TimeTrackingViewModelTest {

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
    fun `project should have correct properties`() {
        // Test Project entity
        assertEquals(1L, testProject.id)
        assertEquals("Test Project", testProject.name)
        assertTrue(testProject.isActive)
        assertNotNull(testProject.createdAt)
    }

    @Test
    fun `time entry should have correct properties`() {
        // Test TimeEntry entity
        assertEquals(1L, testTimeEntry.id)
        assertEquals(1L, testTimeEntry.projectId)
        assertTrue(testTimeEntry.isRunning)
        assertNull(testTimeEntry.endTime)
        assertNotNull(testTimeEntry.startTime)
    }

    @Test
    fun `time entry should calculate duration correctly`() {
        // Given
        val startTime = Date(1000000)
        val endTime = Date(1060000) // 1 minute later
        val entry = TimeEntry(
            id = 1L,
            projectId = 1L,
            startTime = startTime,
            endTime = endTime,
            isRunning = false
        )

        // When
        val duration = entry.getDurationInMinutes()

        // Then
        assertEquals(1L, duration) // Should be 1 minute
    }

    @Test
    fun `ui state should have correct defaults`() {
        // Given
        val uiState = TimeTrackingUiState()

        // Then
        assertNull(uiState.activeProject)
        assertNull(uiState.runningTimeEntry)
        assertFalse(uiState.isTracking)
        assertEquals(0L, uiState.monthlyHours)
        assertTrue(uiState.projectMonthlyHours.isEmpty())
        assertFalse(uiState.showSwitchConfirmation)
        assertNull(uiState.pendingProject)
    }
}