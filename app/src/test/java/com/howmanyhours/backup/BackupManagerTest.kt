package com.howmanyhours.backup

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.howmanyhours.data.entities.Project
import com.howmanyhours.data.entities.TimeEntry
import com.howmanyhours.repository.TimeTrackingRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.rules.TemporaryFolder
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*
import java.io.File
import java.util.*

@ExperimentalCoroutinesApi
class BackupManagerTest {

    @Test
    fun `backup types should have correct display names`() {
        // Test the enum values work correctly
        assertEquals("daily", BackupType.DAILY.name.lowercase())
        assertEquals("manual", BackupType.MANUAL.name.lowercase())
        assertEquals("pre_migration", BackupType.PRE_MIGRATION.name.lowercase())
    }

    @Test
    fun `backup info should store correct data`() {
        // Given
        val testFile = File("test.db")
        val testDate = Date()
        val backupInfo = BackupInfo(
            file = testFile,
            timestamp = testDate,
            type = BackupType.MANUAL,
            sizeBytes = 1024L,
            projectCount = 5,
            entryCount = 10,
            lastEntryDate = testDate
        )

        // Then
        assertEquals(testFile, backupInfo.file)
        assertEquals(testDate, backupInfo.timestamp)
        assertEquals(BackupType.MANUAL, backupInfo.type)
        assertEquals(1024L, backupInfo.sizeBytes)
        assertEquals(5, backupInfo.projectCount)
        assertEquals(10, backupInfo.entryCount)
        assertEquals(testDate, backupInfo.lastEntryDate)
    }

}