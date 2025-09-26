package com.howmanyhours.backup

import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import com.howmanyhours.data.database.AppDatabase
import com.howmanyhours.repository.TimeTrackingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

data class BackupInfo(
    val file: File,
    val timestamp: Date,
    val type: BackupType,
    val sizeBytes: Long,
    val projectCount: Int = 0,
    val entryCount: Int = 0,
    val lastEntryDate: Date? = null
)

data class BackupStats(
    val projectCount: Int,
    val entryCount: Int,
    val lastEntryDate: Date?
)

enum class BackupType {
    DAILY,
    PRE_MIGRATION,
    MANUAL,
    MONTHLY,
    EMERGENCY
}

class BackupManager(
    private val context: Context,
    private val repository: TimeTrackingRepository
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
    private val backupDir = File(context.filesDir, "backups")

    companion object {
        private const val PREF_AUTO_BACKUP_ENABLED = "auto_backup_enabled"
        private const val PREF_LAST_BACKUP_TIME = "last_backup_time"
        private const val PREF_ENTRY_COUNT_AT_LAST_BACKUP = "entry_count_at_last_backup"

        private const val MAX_DAILY_BACKUPS = 7
        private const val MAX_MONTHLY_BACKUPS = 6

        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    }

    init {
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
    }

    // Settings
    var isAutoBackupEnabled: Boolean
        get() = prefs.getBoolean(PREF_AUTO_BACKUP_ENABLED, true)
        set(value) = prefs.edit().putBoolean(PREF_AUTO_BACKUP_ENABLED, value).apply()

    private var lastBackupTime: Long
        get() = prefs.getLong(PREF_LAST_BACKUP_TIME, 0L)
        set(value) = prefs.edit().putLong(PREF_LAST_BACKUP_TIME, value).apply()

    private var entryCountAtLastBackup: Int
        get() = prefs.getInt(PREF_ENTRY_COUNT_AT_LAST_BACKUP, 0)
        set(value) = prefs.edit().putInt(PREF_ENTRY_COUNT_AT_LAST_BACKUP, value).apply()

    // Check if backup is needed
    suspend fun checkAndCreateBackupIfNeeded(): Boolean {
        if (!isAutoBackupEnabled) return false

        return withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val daysSinceLastBackup = (now - lastBackupTime) / (24 * 60 * 60 * 1000)

            // Daily backup check
            if (daysSinceLastBackup >= 1) {
                createBackup(BackupType.DAILY)
                return@withContext true
            }

            // Entry count trigger check
            val currentEntryCount = repository.getAllTimeEntries().size
            if (currentEntryCount - entryCountAtLastBackup >= 50) {
                createBackup(BackupType.DAILY)
                return@withContext true
            }

            return@withContext false
        }
    }

    // Create backup
    suspend fun createBackup(type: BackupType): BackupInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = Date()
                val fileName = when (type) {
                    BackupType.DAILY -> "daily_backup_${dateFormat.format(timestamp)}.db"
                    BackupType.PRE_MIGRATION -> "pre_migration_${dateFormat.format(timestamp)}.db"
                    BackupType.MANUAL -> "manual_backup_${dateFormat.format(timestamp)}.db"
                    BackupType.MONTHLY -> "monthly_backup_${SimpleDateFormat("yyyyMM", Locale.getDefault()).format(timestamp)}.db"
                    BackupType.EMERGENCY -> "emergency_backup_latest.db"
                }

                val backupFile = File(backupDir, fileName)
                val dbFile = context.getDatabasePath("howmanyhours_database")
                val walFile = File(dbFile.absolutePath + "-wal")
                val shmFile = File(dbFile.absolutePath + "-shm")

                android.util.Log.d("BackupManager", "Database files check:")
                android.util.Log.d("BackupManager", "  Main DB: ${dbFile.absolutePath}, exists: ${dbFile.exists()}, size: ${if (dbFile.exists()) dbFile.length() else "N/A"}")
                android.util.Log.d("BackupManager", "  WAL file: ${walFile.absolutePath}, exists: ${walFile.exists()}, size: ${if (walFile.exists()) walFile.length() else "N/A"}")
                android.util.Log.d("BackupManager", "  SHM file: ${shmFile.absolutePath}, exists: ${shmFile.exists()}, size: ${if (shmFile.exists()) shmFile.length() else "N/A"}")

                // Verify current database content before backup
                try {
                    val currentEntries = repository.getAllTimeEntries()
                    android.util.Log.d("BackupManager", "Current database content: ${currentEntries.size} entries")
                } catch (e: Exception) {
                    android.util.Log.w("BackupManager", "Failed to verify current database content: ${e.message}")
                }

                // Instead of copying the database file, let's export the data properly
                android.util.Log.d("BackupManager", "Creating backup by exporting current data")

                if (createBackupByExport(backupFile)) {
                    android.util.Log.d("BackupManager", "Backup created by data export: ${backupFile.absolutePath}, size: ${backupFile.length()}")

                    // Reopen the database to ensure it's available for the app
                    try {
                        AppDatabase.getDatabase(context)
                        android.util.Log.d("BackupManager", "Room database reopened")
                    } catch (e: Exception) {
                        android.util.Log.w("BackupManager", "Failed to reopen database: ${e.message}")
                    }

                    // Immediately verify the backup contains the expected data
                    val verificationStats = readBackupStats(backupFile)
                    android.util.Log.d("BackupManager", "Backup verification: ${verificationStats.projectCount} projects, ${verificationStats.entryCount} entries")

                    // Update tracking
                    lastBackupTime = System.currentTimeMillis()
                    entryCountAtLastBackup = repository.getAllTimeEntries().size

                    // Cleanup old backups
                    cleanupOldBackups(type)

                    // Get backup info
                    return@withContext getBackupInfo(backupFile, type, timestamp)
                } else {
                    android.util.Log.w("BackupManager", "Source database file does not exist: ${dbFile.absolutePath}")
                }

                null
            } catch (e: IOException) {
                null
            }
        }
    }

    // Get all available backups
    suspend fun getAvailableBackups(): List<BackupInfo> {
        return withContext(Dispatchers.IO) {
            if (!backupDir.exists()) return@withContext emptyList()

            backupDir.listFiles { file -> file.name.endsWith(".db") }
                ?.mapNotNull { file ->
                    val type = when {
                        file.name.startsWith("daily_") -> BackupType.DAILY
                        file.name.startsWith("pre_migration_") -> BackupType.PRE_MIGRATION
                        file.name.startsWith("manual_") -> BackupType.MANUAL
                        file.name.startsWith("monthly_") -> BackupType.MONTHLY
                        file.name.startsWith("emergency_") -> BackupType.EMERGENCY
                        else -> BackupType.DAILY
                    }

                    val timestamp = extractTimestampFromFileName(file.name) ?: Date(file.lastModified())
                    getBackupInfo(file, type, timestamp)
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
        }
    }

    // Get backup info with project count and last entry by reading the backup file
    private suspend fun getBackupInfo(file: File, type: BackupType, timestamp: Date): BackupInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val backupStats = readBackupStats(file)
                BackupInfo(
                    file = file,
                    timestamp = timestamp,
                    type = type,
                    sizeBytes = file.length(),
                    projectCount = backupStats.projectCount,
                    entryCount = backupStats.entryCount,
                    lastEntryDate = backupStats.lastEntryDate
                )
            } catch (e: Exception) {
                // If we can't read the backup, return basic info
                BackupInfo(
                    file = file,
                    timestamp = timestamp,
                    type = type,
                    sizeBytes = file.length(),
                    projectCount = 0,
                    entryCount = 0,
                    lastEntryDate = null
                )
            }
        }
    }

    // Read statistics from a backup database file
    private fun readBackupStats(backupFile: File): BackupStats {
        if (!backupFile.exists()) {
            android.util.Log.w("BackupManager", "Backup file does not exist: ${backupFile.absolutePath}")
            return BackupStats(0, 0, null)
        }

        var database: SQLiteDatabase? = null
        return try {
            database = SQLiteDatabase.openDatabase(
                backupFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            // First, check what tables exist
            val tablesCursor = database.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' AND name NOT LIKE 'room_%'",
                null
            )

            val tableNames = mutableListOf<String>()
            tablesCursor.use { cursor ->
                while (cursor.moveToNext()) {
                    val tableName = cursor.getString(0)
                    tableNames.add(tableName)
                    android.util.Log.d("BackupManager", "Found table: $tableName")
                }
            }

            android.util.Log.d("BackupManager", "All tables found: $tableNames")

            // If no tables found, this might not be our database
            if (tableNames.isEmpty()) {
                android.util.Log.w("BackupManager", "No tables found in backup database")
                return BackupStats(0, 0, null)
            }

            // Count projects
            val projectCount = if ("projects" in tableNames) {
                database.rawQuery("SELECT COUNT(*) FROM projects", null).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val count = cursor.getInt(0)
                        android.util.Log.d("BackupManager", "Project count: $count")
                        count
                    } else 0
                }
            } else {
                android.util.Log.w("BackupManager", "Projects table not found")
                0
            }

            // Count time entries and get last entry date
            val (entryCount, lastEntryDate) = if ("time_entries" in tableNames) {
                database.rawQuery(
                    "SELECT COUNT(*), MAX(startTime) FROM time_entries",
                    null
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        val count = cursor.getInt(0)
                        val lastEntryTimestamp = cursor.getLong(1)
                        val lastDate = if (lastEntryTimestamp > 0) Date(lastEntryTimestamp) else null
                        android.util.Log.d("BackupManager", "Entry count: $count, last entry timestamp: $lastEntryTimestamp")
                        Pair(count, lastDate)
                    } else {
                        android.util.Log.w("BackupManager", "No data in time_entries table")
                        Pair(0, null)
                    }
                }
            } else {
                android.util.Log.w("BackupManager", "Time_entries table not found")
                Pair(0, null)
            }

            android.util.Log.d("BackupManager", "Final stats - Projects: $projectCount, Entries: $entryCount")
            BackupStats(projectCount, entryCount, lastEntryDate)

        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Failed to read backup stats: ${e.message}", e)
            BackupStats(0, 0, null)
        } finally {
            database?.close()
        }
    }

    // Extract timestamp from filename
    private fun extractTimestampFromFileName(fileName: String): Date? {
        return try {
            val timestampPart = fileName.substringAfter("_").substringBefore(".db")
            if (timestampPart.length == 15) { // yyyyMMdd_HHmmss format
                dateFormat.parse(timestampPart)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Get total backup storage size
    suspend fun getTotalBackupSize(): Long {
        return withContext(Dispatchers.IO) {
            if (!backupDir.exists()) return@withContext 0L

            backupDir.listFiles { file -> file.name.endsWith(".db") }
                ?.sumOf { it.length() }
                ?: 0L
        }
    }

    // Clean up old backups
    private fun cleanupOldBackups(excludeType: BackupType) {
        if (!backupDir.exists()) return

        // Clean daily backups (keep last 7)
        val dailyBackups = backupDir.listFiles { file ->
            file.name.startsWith("daily_") && file.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() }

        dailyBackups?.drop(MAX_DAILY_BACKUPS)?.forEach { it.delete() }

        // Clean monthly backups (keep last 6)
        val monthlyBackups = backupDir.listFiles { file ->
            file.name.startsWith("monthly_") && file.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() }

        monthlyBackups?.drop(MAX_MONTHLY_BACKUPS)?.forEach { it.delete() }
    }

    // Delete all backups (cleanup function)
    suspend fun deleteAllBackups(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                backupDir.listFiles { file -> file.name.endsWith(".db") }
                    ?.forEach { it.delete() }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    // Create backup by exporting all current data to a new database file
    private suspend fun createBackupByExport(backupFile: File): Boolean {
        return try {
            // Get all current data from repository
            val projects = repository.getAllProjects().first()
            val entries = repository.getAllTimeEntries()

            android.util.Log.d("BackupManager", "Exporting ${projects.size} projects and ${entries.size} entries")

            // Create a new database file
            var backupDb: SQLiteDatabase? = null
            try {
                backupDb = SQLiteDatabase.openOrCreateDatabase(backupFile, null)

                // Create tables
                backupDb.execSQL("""
                    CREATE TABLE projects (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        isActive INTEGER NOT NULL
                    )
                """.trimIndent())

                backupDb.execSQL("""
                    CREATE TABLE time_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        projectId INTEGER NOT NULL,
                        startTime INTEGER NOT NULL,
                        endTime INTEGER,
                        isRunning INTEGER NOT NULL,
                        name TEXT,
                        FOREIGN KEY (projectId) REFERENCES projects(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                android.util.Log.d("BackupManager", "Tables created in backup database")

                // Insert projects
                val projectInsertStmt = backupDb.compileStatement(
                    "INSERT INTO projects (id, name, createdAt, isActive) VALUES (?, ?, ?, ?)"
                )

                projects.forEach { project ->
                    projectInsertStmt.bindLong(1, project.id)
                    projectInsertStmt.bindString(2, project.name)
                    projectInsertStmt.bindLong(3, project.createdAt.time)
                    projectInsertStmt.bindLong(4, if (project.isActive) 1 else 0)
                    projectInsertStmt.executeInsert()
                }
                projectInsertStmt.close()

                // Insert time entries
                val entryInsertStmt = backupDb.compileStatement(
                    "INSERT INTO time_entries (id, projectId, startTime, endTime, isRunning, name) VALUES (?, ?, ?, ?, ?, ?)"
                )

                entries.forEach { entry ->
                    entryInsertStmt.bindLong(1, entry.id)
                    entryInsertStmt.bindLong(2, entry.projectId)
                    entryInsertStmt.bindLong(3, entry.startTime.time)
                    if (entry.endTime != null) {
                        entryInsertStmt.bindLong(4, entry.endTime.time)
                    } else {
                        entryInsertStmt.bindNull(4)
                    }
                    entryInsertStmt.bindLong(5, if (entry.isRunning) 1 else 0)
                    if (entry.name != null) {
                        entryInsertStmt.bindString(6, entry.name)
                    } else {
                        entryInsertStmt.bindNull(6)
                    }
                    entryInsertStmt.executeInsert()
                }
                entryInsertStmt.close()

                android.util.Log.d("BackupManager", "Data export completed successfully")
                true

            } finally {
                backupDb?.close()
            }

        } catch (e: Exception) {
            android.util.Log.e("BackupManager", "Backup export failed: ${e.message}", e)
            false
        }
    }

    // Restore from backup - simple replacement approach
    suspend fun restoreFromBackup(backupInfo: BackupInfo): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("BackupManager", "Starting restore from: ${backupInfo.file.name}")

                val dbFile = context.getDatabasePath("howmanyhours_database")
                val walFile = File(dbFile.absolutePath + "-wal")
                val shmFile = File(dbFile.absolutePath + "-shm")

                // Step 1: Close Room database completely
                try {
                    val appDatabase = AppDatabase.getDatabase(context)
                    appDatabase.close()
                    android.util.Log.d("BackupManager", "Room database closed for restore")
                } catch (e: Exception) {
                    android.util.Log.w("BackupManager", "Failed to close Room database: ${e.message}")
                }

                // Give some time for cleanup
                Thread.sleep(500)

                // Step 2: Delete existing database files
                if (dbFile.exists()) {
                    val deleted = dbFile.delete()
                    android.util.Log.d("BackupManager", "Main DB deleted: $deleted")
                }
                if (walFile.exists()) {
                    val deleted = walFile.delete()
                    android.util.Log.d("BackupManager", "WAL file deleted: $deleted")
                }
                if (shmFile.exists()) {
                    val deleted = shmFile.delete()
                    android.util.Log.d("BackupManager", "SHM file deleted: $deleted")
                }

                // Step 3: Copy backup to main database location
                backupInfo.file.copyTo(dbFile, overwrite = true)
                android.util.Log.d("BackupManager", "Backup copied to main database location")

                // Step 4: Clear Room instance to force recreation
                AppDatabase.clearInstance()
                android.util.Log.d("BackupManager", "Room instance cleared")

                // Step 5: Test that we can reopen the database
                try {
                    AppDatabase.getDatabase(context)
                    android.util.Log.d("BackupManager", "Database reopened successfully after restore")
                } catch (e: Exception) {
                    android.util.Log.e("BackupManager", "Failed to reopen database after restore: ${e.message}")
                    return@withContext false
                }

                android.util.Log.d("BackupManager", "Restore completed successfully")
                true

            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Restore failed: ${e.message}", e)
                false
            }
        }
    }
}