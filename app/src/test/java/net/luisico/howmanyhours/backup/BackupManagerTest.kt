package net.luisico.howmanyhours.backup

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.util.*

/**
 * Basic BackupManager entity tests.
 * For comprehensive backup testing, see:
 * - BackupCreationAndVerificationTest
 * - BackupValidationTest
 * - BackupRestorationTest
 */
class BackupManagerTest {

    @Test
    fun `backup types should have correct values`() {
        // Test the enum values work correctly
        assertEquals("DAILY", BackupType.DAILY.name)
        assertEquals("MANUAL", BackupType.MANUAL.name)
        assertEquals("PRE_MIGRATION", BackupType.PRE_MIGRATION.name)
        assertEquals("MONTHLY", BackupType.MONTHLY.name)
        assertEquals("EMERGENCY", BackupType.EMERGENCY.name)
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

    @Test
    fun `backup validation should have correct properties`() {
        val validation = BackupValidation(
            isValid = true,
            backupVersion = 3,
            currentVersion = 4,
            requiresDestructiveMigration = true,
            message = "Test message"
        )

        assertTrue(validation.isValid)
        assertEquals(3, validation.backupVersion)
        assertEquals(4, validation.currentVersion)
        assertTrue(validation.requiresDestructiveMigration)
        assertEquals("Test message", validation.message)
    }

    @Test
    fun `restore result success type exists`() {
        val success = RestoreResult.Success
        assertNotNull(success)
    }

    @Test
    fun `restore result failed should contain error message`() {
        val failed = RestoreResult.Failed("Test error")
        assertTrue(failed.error.contains("Test error"))
    }
}
