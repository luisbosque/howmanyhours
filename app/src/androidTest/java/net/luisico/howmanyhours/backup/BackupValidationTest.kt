package net.luisico.howmanyhours.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.luisico.howmanyhours.data.database.AppDatabase
import net.luisico.howmanyhours.data.entities.Project
import net.luisico.howmanyhours.data.entities.TimeEntry
import net.luisico.howmanyhours.repository.TimeTrackingRepository
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Date

/**
 * Backup validation tests focusing on:
 * - Corruption detection
 * - Version compatibility checking
 * - Invalid backup file handling
 * - Schema validation
 */
@RunWith(AndroidJUnit4::class)
class BackupValidationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: TimeTrackingRepository
    private lateinit var backupManager: BackupManager
    private lateinit var testBackupDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "howmanyhours_database"
        )
            .allowMainThreadQueries()
            .build()

        repository = TimeTrackingRepository(
            database.projectDao(),
            database.timeEntryDao(),
            database.periodCloseDao()
        )
        backupManager = BackupManager(context, repository)

        testBackupDir = File(context.filesDir, "test_validation_backups")
        if (!testBackupDir.exists()) {
            testBackupDir.mkdirs()
        }
    }

    @After
    fun teardown() {
        database.close()
        AppDatabase.clearInstance()
        context.deleteDatabase("howmanyhours_database")

        testBackupDir.listFiles()?.forEach { it.delete() }
        testBackupDir.delete()
    }

    // ============ Corruption Detection Tests ============

    @Test
    fun testValidateNonExistentBackup() = runBlocking {
        val nonExistentFile = File(testBackupDir, "non_existent.db")
        val backupInfo = BackupInfo(
            file = nonExistentFile,
            timestamp = Date(),
            type = BackupType.MANUAL,
            sizeBytes = 0
        )

        val validation = backupManager.validateBackup(backupInfo)

        assertFalse("Non-existent backup should be invalid", validation.isValid)
        assertTrue(
            "Error message should mention file doesn't exist",
            validation.message.contains("does not exist")
        )
    }

    @Test
    fun testValidateCorruptedBackupFile() = runBlocking {
        // Create a file that's not a valid SQLite database
        val corruptedFile = File(testBackupDir, "corrupted.db")
        corruptedFile.writeText("This is not a SQLite database!")

        val backupInfo = BackupInfo(
            file = corruptedFile,
            timestamp = Date(),
            type = BackupType.MANUAL,
            sizeBytes = corruptedFile.length()
        )

        val validation = backupManager.validateBackup(backupInfo)

        assertFalse("Corrupted backup should be invalid", validation.isValid)
        assertTrue(
            "Error message should mention corruption or read failure",
            validation.message.contains("corrupted") || validation.message.contains("Failed to read")
        )
    }

    @Test
    fun testValidateEmptyBackupFile() = runBlocking {
        // Create empty file
        val emptyFile = File(testBackupDir, "empty.db")
        emptyFile.createNewFile()

        val backupInfo = BackupInfo(
            file = emptyFile,
            timestamp = Date(),
            type = BackupType.MANUAL,
            sizeBytes = 0
        )

        val validation = backupManager.validateBackup(backupInfo)

        assertFalse("Empty backup file should be invalid", validation.isValid)
    }

    @Test
    fun testValidateCurrentVersionBackup() = runBlocking {
        // Create valid backup with current schema version
        val validFile = File(testBackupDir, "valid_v4.db")
        val db = SQLiteDatabase.openOrCreateDatabase(validFile, null)
        try {
            // Create our schema
            db.execSQL("""
                CREATE TABLE projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    periodMode TEXT NOT NULL DEFAULT 'monthly'
                )
            """)
            db.execSQL("""
                CREATE TABLE time_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    startTime INTEGER NOT NULL,
                    endTime INTEGER,
                    isRunning INTEGER NOT NULL,
                    name TEXT,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                )
            """)
            db.execSQL("""
                CREATE TABLE period_closes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    closeTime INTEGER NOT NULL,
                    isAutomatic INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                )
            """)
            db.version = 4 // Current version
        } finally {
            db.close()
        }

        val backupInfo = BackupInfo(
            file = validFile,
            timestamp = Date(),
            type = BackupType.MANUAL,
            sizeBytes = validFile.length()
        )

        val validation = backupManager.validateBackup(backupInfo)

        assertTrue("Valid current version backup should pass validation", validation.isValid)
        assertEquals("Backup version should be 4", 4, validation.backupVersion)
        assertEquals("Current version should be 4", 4, validation.currentVersion)
        assertFalse(
            "Same version shouldn't require migration",
            validation.requiresDestructiveMigration
        )
    }

    @Test
    fun testValidateOldVersionBackup() = runBlocking {
        // Create backup with old schema version (version 1)
        val oldFile = File(testBackupDir, "old_v1.db")
        val db = SQLiteDatabase.openOrCreateDatabase(oldFile, null)
        try {
            // Create version 1 schema (without 'name' column in time_entries)
            db.execSQL("""
                CREATE TABLE projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    isActive INTEGER NOT NULL
                )
            """)
            db.execSQL("""
                CREATE TABLE time_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    startTime INTEGER NOT NULL,
                    endTime INTEGER,
                    isRunning INTEGER NOT NULL,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                )
            """)
            db.version = 1
        } finally {
            db.close()
        }

        val backupInfo = BackupInfo(
            file = oldFile,
            timestamp = Date(),
            type = BackupType.MANUAL,
            sizeBytes = oldFile.length()
        )

        val validation = backupManager.validateBackup(backupInfo)

        assertTrue("Old version backup should be valid (migratable)", validation.isValid)
        assertEquals("Backup version should be 1", 1, validation.backupVersion)
        assertEquals("Current version should be 4", 4, validation.currentVersion)
        assertTrue(
            "Old version should require migration",
            validation.requiresDestructiveMigration
        )
        assertTrue(
            "Message should mention older version",
            validation.message.contains("older version")
        )
    }

    @Test
    fun testValidateFutureVersionBackup() = runBlocking {
        // Create backup with future schema version
        val futureFile = File(testBackupDir, "future_v10.db")
        val db = SQLiteDatabase.openOrCreateDatabase(futureFile, null)
        try {
            db.execSQL("""
                CREATE TABLE projects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    createdAt INTEGER NOT NULL,
                    isActive INTEGER NOT NULL,
                    periodMode TEXT NOT NULL DEFAULT 'monthly'
                )
            """)
            db.execSQL("""
                CREATE TABLE time_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    projectId INTEGER NOT NULL,
                    startTime INTEGER NOT NULL,
                    endTime INTEGER,
                    isRunning INTEGER NOT NULL,
                    name TEXT,
                    FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                )
            """)
            db.version = 10 // Future version
        } finally {
            db.close()
        }

        val backupInfo = BackupInfo(
            file = futureFile,
            timestamp = Date(),
            type = BackupType.MANUAL,
            sizeBytes = futureFile.length()
        )

        val validation = backupManager.validateBackup(backupInfo)

        assertFalse("Future version backup should be invalid", validation.isValid)
        assertEquals("Backup version should be 10", 10, validation.backupVersion)
        assertEquals("Current version should be 4", 4, validation.currentVersion)
        assertTrue(
            "Message should mention newer version or update requirement",
            validation.message.contains("newer version") || validation.message.contains("update")
        )
    }
}
