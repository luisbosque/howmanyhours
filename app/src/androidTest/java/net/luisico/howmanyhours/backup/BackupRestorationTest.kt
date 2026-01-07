package net.luisico.howmanyhours.backup

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
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
 * Backup restoration and rollback tests focusing on:
 * - Successful restoration of valid backups
 * - Automatic rollback on failure
 * - Data consistency after restoration
 * - Recovery from corrupted backups
 */
@RunWith(AndroidJUnit4::class)
class BackupRestorationTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: TimeTrackingRepository
    private lateinit var backupManager: BackupManager
    private lateinit var testBackupDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()

        // Use a persistent database for restore testing with production name
        database = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "howmanyhours_database"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()

        repository = TimeTrackingRepository(
            database.projectDao(),
            database.timeEntryDao(),
            database.periodCloseDao()
        )
        backupManager = BackupManager(context, repository)

        testBackupDir = File(context.filesDir, "test_restore_backups")
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

        // Clean up any temp files
        File(context.cacheDir, "pre_restore_backup.db").delete()
        File(context.cacheDir, "pre_restore_backup.db-wal").delete()
        File(context.cacheDir, "pre_restore_backup.db-shm").delete()
    }

    // ============ Successful Restoration Tests ============

    @Test
    fun testRestoreValidBackup() = runBlocking {
        // Create initial data
        val originalProjectId = database.projectDao().insertProject(
            Project(name = "Original Project", isActive = true)
        )
        database.timeEntryDao().insertTimeEntry(
            TimeEntry(projectId = originalProjectId, startTime = Date(), isRunning = true)
        )

        // Create backup
        val backupInfo = backupManager.createBackup(BackupType.MANUAL)
        assertNotNull("Backup should be created", backupInfo)

        // Modify database
        val newProjectId = database.projectDao().insertProject(
            Project(name = "New Project", isActive = true)
        )
        database.timeEntryDao().insertTimeEntry(
            TimeEntry(projectId = newProjectId, startTime = Date(), isRunning = true)
        )

        // Verify we have 2 projects
        val projectsBeforeRestore = database.projectDao().getAllProjects().first()
        assertEquals("Should have 2 projects before restore", 2, projectsBeforeRestore.size)

        // Restore backup
        val result = backupManager.restoreFromBackup(backupInfo!!)

        assertTrue("Restore should succeed", result is RestoreResult.Success)

        // Verify restored data
        val projectsAfterRestore = database.projectDao().getAllProjects().first()
        assertEquals("Should have 1 project after restore", 1, projectsAfterRestore.size)
        assertEquals("Project name should match", "Original Project", projectsAfterRestore[0].name)
    }

    @Test
    fun testRestorePreservesAllData() = runBlocking {
        // Create comprehensive data
        val project1Id = database.projectDao().insertProject(
            Project(name = "Project Alpha", isActive = true)
        )
        val project2Id = database.projectDao().insertProject(
            Project(name = "Project Beta", isActive = true)
        )

        database.timeEntryDao().insertTimeEntry(
            TimeEntry(projectId = project1Id, startTime = Date(System.currentTimeMillis() - 120000),
                      endTime = Date(System.currentTimeMillis() - 60000), isRunning = false, name = "Entry 1")
        )
        database.timeEntryDao().insertTimeEntry(
            TimeEntry(projectId = project2Id, startTime = Date(System.currentTimeMillis() - 60000),
                      endTime = Date(), isRunning = false, name = "Entry 2")
        )

        // Create backup
        val backupInfo = backupManager.createBackup(BackupType.MANUAL)
        assertNotNull("Backup should be created", backupInfo)

        // Clear database
        database.clearAllTables()

        // Verify empty
        assertEquals("Should be empty before restore", 0, database.projectDao().getAllProjects().first().size)

        // Restore
        val result = backupManager.restoreFromBackup(backupInfo!!)
        assertTrue("Restore should succeed", result is RestoreResult.Success)

        // Get fresh database instance
        val restoredDb = AppDatabase.getDatabase(context)

        // Verify all data restored
        val projects = restoredDb.projectDao().getAllProjects().first()
        assertEquals("Should restore 2 projects", 2, projects.size)

        val entries = restoredDb.timeEntryDao().getAllTimeEntries()
        assertEquals("Should restore 2 entries", 2, entries.size)
    }

    // ============ Rollback Tests ============

    @Test
    fun testRestoreCorruptedBackupTriggersRollback() = runBlocking {
        // Create valid current data
        val projectId = database.projectDao().insertProject(
            Project(name = "Current Project", isActive = true)
        )
        database.timeEntryDao().insertTimeEntry(
            TimeEntry(projectId = projectId, startTime = Date(), isRunning = true)
        )

        val currentProjects = database.projectDao().getAllProjects().first()
        assertEquals("Should have current project", 1, currentProjects.size)

        // Create corrupted backup file
        val corruptedFile = File(testBackupDir, "corrupted.db")
        corruptedFile.writeText("This is not a valid database!")

        val corruptedBackupInfo = BackupInfo(
            file = corruptedFile,
            timestamp = Date(),
            type = BackupType.MANUAL,
            sizeBytes = corruptedFile.length()
        )

        // Attempt restore
        val result = backupManager.restoreFromBackup(corruptedBackupInfo)

        // Should fail
        assertTrue("Restore of corrupted backup should fail", result is RestoreResult.Failed)

        // Verify original data still intact (rollback successful)
        val projectsAfterFailedRestore = database.projectDao().getAllProjects().first()
        assertEquals("Should still have original project after rollback", 1, projectsAfterFailedRestore.size)
        assertEquals("Original data should be intact", "Current Project", projectsAfterFailedRestore[0].name)
    }

    @Test
    fun testRestoreInvalidSchemaTriggersRollback() = runBlocking {
        // Create valid current data
        val projectId = database.projectDao().insertProject(
            Project(name = "Existing Project", isActive = true)
        )
        database.timeEntryDao().insertTimeEntry(
            TimeEntry(projectId = projectId, startTime = Date(), isRunning = false,
                      endTime = Date(), name = "Existing Entry")
        )

        // Create backup with invalid schema
        val invalidSchemaFile = File(testBackupDir, "invalid_schema.db")
        val db = SQLiteDatabase.openOrCreateDatabase(invalidSchemaFile, null)
        try {
            // Create wrong tables
            db.execSQL("CREATE TABLE wrong_table (id INTEGER, data TEXT)")
            db.version = 0
        } finally {
            db.close()
        }

        val invalidBackupInfo = BackupInfo(
            file = invalidSchemaFile,
            timestamp = Date(),
            type = BackupType.MANUAL,
            sizeBytes = invalidSchemaFile.length()
        )

        // Attempt restore
        val result = backupManager.restoreFromBackup(invalidBackupInfo)

        // Should fail
        assertTrue("Restore of invalid schema should fail", result is RestoreResult.Failed)

        // Verify original data intact
        val projectsAfter = database.projectDao().getAllProjects().first()
        assertEquals("Should have original project after rollback", 1, projectsAfter.size)

        val entriesAfter = database.timeEntryDao().getAllTimeEntries()
        assertEquals("Should have original entry after rollback", 1, entriesAfter.size)
        assertEquals("Entry data should be intact", "Existing Entry", entriesAfter[0].name)
    }

    // ============ Performance Tests ============

    @Test
    fun testRestoreLargeBackup() = runBlocking {
        // Create large dataset
        val projectId = database.projectDao().insertProject(
            Project(name = "Large Dataset Project", isActive = true)
        )

        // Add 200 entries
        repeat(200) { i ->
            database.timeEntryDao().insertTimeEntry(
                TimeEntry(
                    projectId = projectId,
                    startTime = Date(System.currentTimeMillis() - (i * 60000L)),
                    endTime = Date(System.currentTimeMillis() - (i * 60000L) + 30000L),
                    isRunning = false,
                    name = "Entry $i"
                )
            )
        }

        // Create backup
        val startBackup = System.currentTimeMillis()
        val backupInfo = backupManager.createBackup(BackupType.MANUAL)
        val backupDuration = System.currentTimeMillis() - startBackup

        assertNotNull("Large backup should be created", backupInfo)
        assertTrue("Backup of 200 entries should complete within 10s", backupDuration < 10000)

        // Clear database
        database.clearAllTables()

        // Restore
        val startRestore = System.currentTimeMillis()
        val result = backupManager.restoreFromBackup(backupInfo!!)
        val restoreDuration = System.currentTimeMillis() - startRestore

        assertTrue("Restore should succeed", result is RestoreResult.Success)
        assertTrue("Restore of 200 entries should complete within 10s", restoreDuration < 10000)

        // Verify all data restored - get fresh database instance after restore
        val restoredDb = AppDatabase.getDatabase(context)
        val entries = restoredDb.timeEntryDao().getAllTimeEntries()
        assertEquals("Should restore all 200 entries", 200, entries.size)
    }
}
