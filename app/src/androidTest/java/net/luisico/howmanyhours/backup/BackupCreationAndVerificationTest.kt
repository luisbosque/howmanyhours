package net.luisico.howmanyhours.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import net.luisico.howmanyhours.data.database.AppDatabase
import net.luisico.howmanyhours.data.entities.PeriodClose
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
 * Backup creation and verification tests focusing on:
 * - Backup file creation and validity
 * - Backup content accuracy
 * - Backup metadata correctness
 * - Data integrity after backup
 */
@RunWith(AndroidJUnit4::class)
class BackupCreationAndVerificationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: TimeTrackingRepository
    private lateinit var backupManager: BackupManager
    private lateinit var testBackupDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Create test database with same name as production for BackupManager compatibility
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

        // Create test backup directory
        testBackupDir = File(context.filesDir, "test_backups")
        if (!testBackupDir.exists()) {
            testBackupDir.mkdirs()
        }
    }

    @After
    fun teardown() {
        database.close()
        AppDatabase.clearInstance()
        context.deleteDatabase("howmanyhours_database")

        // Clean up test backups
        testBackupDir.listFiles()?.forEach { it.delete() }
        testBackupDir.delete()
    }

    // ============ Backup Creation Tests ============

    @Test
    fun testCreateBackupProducesValidFile() = runBlocking {
        // Insert test data
        val projectId = database.projectDao().insertProject(
            Project(name = "Test Project", isActive = true)
        )
        database.timeEntryDao().insertTimeEntry(
            TimeEntry(projectId = projectId, startTime = Date(), isRunning = true)
        )

        // Create backup
        val backupInfo = backupManager.createBackup(BackupType.MANUAL)

        assertNotNull("Backup should be created successfully", backupInfo)
        assertTrue("Backup file should exist", backupInfo!!.file.exists())
        assertTrue("Backup file should not be empty", backupInfo.file.length() > 0)
        assertEquals("Backup type should match", BackupType.MANUAL, backupInfo.type)
    }

    @Test
    fun testBackupContainsAllProjects() = runBlocking {
        // Create multiple projects
        database.projectDao().insertProject(Project(name = "Project 1", isActive = true))
        database.projectDao().insertProject(Project(name = "Project 2", isActive = false))
        database.projectDao().insertProject(Project(name = "Project 3", isActive = true))

        // Create backup
        val backupInfo = backupManager.createBackup(BackupType.MANUAL)
        assertNotNull("Backup should be created", backupInfo)

        // Verify backup contains all projects
        assertEquals("Backup should contain all 3 projects", 3, backupInfo!!.projectCount)
    }

    @Test
    fun testBackupContainsAllTimeEntries() = runBlocking {
        // Create project with multiple entries
        val projectId = database.projectDao().insertProject(Project(name = "Test", isActive = true))

        repeat(5) { i ->
            database.timeEntryDao().insertTimeEntry(
                TimeEntry(
                    projectId = projectId,
                    startTime = Date(System.currentTimeMillis() - (i * 60000L)),
                    endTime = Date(System.currentTimeMillis() - (i * 60000L) + 30000L),
                    isRunning = false
                )
            )
        }

        // Create backup
        val backupInfo = backupManager.createBackup(BackupType.MANUAL)
        assertNotNull("Backup should be created", backupInfo)

        // Verify backup contains all entries
        assertEquals("Backup should contain all 5 time entries", 5, backupInfo!!.entryCount)
    }

    @Test
    fun testBackupFileIsValidSQLiteDatabase() = runBlocking {
        val projectId = database.projectDao().insertProject(Project(name = "Test", isActive = true))
        database.timeEntryDao().insertTimeEntry(
            TimeEntry(projectId = projectId, startTime = Date(), isRunning = true)
        )

        val backupInfo = backupManager.createBackup(BackupType.MANUAL)
        assertNotNull("Backup should be created", backupInfo)

        // Try to open as SQLite database
        var isValidDatabase = false
        try {
            val backupDb = SQLiteDatabase.openDatabase(
                backupInfo!!.file.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            // Should be able to query sqlite_master
            val cursor = backupDb.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table'",
                null
            )

            val tableNames = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tableNames.add(cursor.getString(0))
            }
            cursor.close()
            backupDb.close()

            // Should have our core tables
            assertTrue("Backup should contain projects table", tableNames.contains("projects"))
            assertTrue("Backup should contain time_entries table", tableNames.contains("time_entries"))

            isValidDatabase = true
        } catch (e: Exception) {
            isValidDatabase = false
        }

        assertTrue("Backup file should be a valid SQLite database", isValidDatabase)
    }

    @Test
    fun testBackupCreationPerformance() = runBlocking {
        // Create realistic dataset
        val projectId = database.projectDao().insertProject(Project(name = "Performance Test", isActive = true))

        repeat(100) { i ->
            database.timeEntryDao().insertTimeEntry(
                TimeEntry(
                    projectId = projectId,
                    startTime = Date(System.currentTimeMillis() - (i * 60000L)),
                    endTime = Date(System.currentTimeMillis() - (i * 60000L) + 30000L),
                    isRunning = false
                )
            )
        }

        // Measure backup creation time
        val startTime = System.currentTimeMillis()
        val backupInfo = backupManager.createBackup(BackupType.MANUAL)
        val duration = System.currentTimeMillis() - startTime

        assertNotNull("Backup should be created", backupInfo)
        assertTrue("Backup creation should complete within 5 seconds (took ${duration}ms)", duration < 5000)
        assertEquals("Backup should contain all entries", 100, backupInfo!!.entryCount)
    }
}
