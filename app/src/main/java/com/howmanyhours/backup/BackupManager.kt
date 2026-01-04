package com.howmanyhours.backup

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import androidx.documentfile.provider.DocumentFile
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
import java.util.TimeZone

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

data class BackupValidation(
    val isValid: Boolean,
    val backupVersion: Int,
    val currentVersion: Int,
    val requiresDestructiveMigration: Boolean,
    val message: String
)

sealed class RestoreResult {
    object Success : RestoreResult()
    data class Failed(val error: String) : RestoreResult()
}

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

        private val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }
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
                    BackupType.MONTHLY -> "monthly_backup_${SimpleDateFormat("yyyyMM", Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }.format(timestamp)}.db"
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
                    val backupInfo = getBackupInfo(backupFile, type, timestamp)

                    // Auto-export to external folder if enabled
                    if (backupInfo != null) {
                        autoExportBackupIfEnabled(backupInfo)
                    }

                    return@withContext backupInfo
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

    // Create backup by copying the actual Room database file
    private suspend fun createBackupByExport(backupFile: File): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath("howmanyhours_database")

                if (!dbFile.exists()) {
                    android.util.Log.e("BackupManager", "Database file does not exist")
                    return@withContext false
                }

                android.util.Log.d("BackupManager", "Creating backup by copying database file")

                // Use SQLite checkpoint to ensure all data is written to the main file
                // This doesn't close the database, so the app can keep using it
                val db = AppDatabase.getDatabase(context)
                try {
                    // Force a checkpoint to flush WAL to main database file
                    db.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
                    android.util.Log.d("BackupManager", "WAL checkpoint completed")
                } catch (e: Exception) {
                    android.util.Log.w("BackupManager", "WAL checkpoint failed, continuing anyway: ${e.message}")
                }

                // Copy the database file (database remains open and usable)
                dbFile.copyTo(backupFile, overwrite = true)

                android.util.Log.d("BackupManager", "Backup created successfully via file copy")
                true

            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Backup creation failed: ${e.message}", e)
                false
            }
        }
    }

    // Validate backup before restore
    suspend fun validateBackup(backupInfo: BackupInfo): BackupValidation {
        return withContext(Dispatchers.IO) {
            if (!backupInfo.file.exists()) {
                return@withContext BackupValidation(
                    isValid = false,
                    backupVersion = 0,
                    currentVersion = 4,
                    requiresDestructiveMigration = false,
                    message = "Backup file does not exist"
                )
            }

            var database: SQLiteDatabase? = null
            try {
                // Open backup database in read-only mode
                database = SQLiteDatabase.openDatabase(
                    backupInfo.file.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )

                // Get database version
                val backupVersion = database.version
                val currentVersion = 4 // Current app database version

                android.util.Log.d("BackupManager", "Backup version: $backupVersion, Current version: $currentVersion")

                // Check if backup is compatible
                when {
                    backupVersion == 0 -> {
                        // No version set - check if it has our tables (might be backup created by export)
                        val tablesCursor = database.rawQuery(
                            "SELECT name FROM sqlite_master WHERE type='table' AND (name='projects' OR name='time_entries')",
                            null
                        )
                        val hasOurTables = tablesCursor.use { cursor ->
                            var count = 0
                            while (cursor.moveToNext()) count++
                            count >= 2 // Should have both projects and time_entries tables
                        }

                        if (hasOurTables) {
                            // Valid backup created by our export function, just missing version number
                            // Treat as current version since export creates current schema
                            BackupValidation(
                                isValid = true,
                                backupVersion = currentVersion,
                                currentVersion = currentVersion,
                                requiresDestructiveMigration = false,
                                message = "Backup is compatible with current app version"
                            )
                        } else {
                            // Doesn't have our tables - truly corrupted
                            BackupValidation(
                                isValid = false,
                                backupVersion = backupVersion,
                                currentVersion = currentVersion,
                                requiresDestructiveMigration = false,
                                message = "Backup file appears to be corrupted or invalid"
                            )
                        }
                    }
                    backupVersion < 4 -> {
                        // Old schema - will be migrated during restore
                        BackupValidation(
                            isValid = true,
                            backupVersion = backupVersion,
                            currentVersion = currentVersion,
                            requiresDestructiveMigration = true,
                            message = "This backup is from an older version of the app (v$backupVersion). " +
                                    "It will be automatically upgraded to the current format during restore. " +
                                    "Your projects and time entries will be preserved. " +
                                    if (backupVersion < 3) {
                                        "Period history will be auto-generated from your existing time entries."
                                    } else {
                                        ""
                                    }
                        )
                    }
                    backupVersion > currentVersion -> {
                        // Future version - not compatible
                        BackupValidation(
                            isValid = false,
                            backupVersion = backupVersion,
                            currentVersion = currentVersion,
                            requiresDestructiveMigration = false,
                            message = "This backup is from a newer version of the app (v$backupVersion vs v$currentVersion). " +
                                    "Please update the app to restore this backup."
                        )
                    }
                    else -> {
                        // Compatible version (3 or 4)
                        BackupValidation(
                            isValid = true,
                            backupVersion = backupVersion,
                            currentVersion = currentVersion,
                            requiresDestructiveMigration = false,
                            message = "Backup is compatible with current app version"
                        )
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Failed to validate backup: ${e.message}", e)
                BackupValidation(
                    isValid = false,
                    backupVersion = 0,
                    currentVersion = 4,
                    requiresDestructiveMigration = false,
                    message = "Failed to read backup file: ${e.message}"
                )
            } finally {
                database?.close()
            }
        }
    }

    // Migrate backup to current version if needed
    private suspend fun migrateBackupToCurrentVersion(backupFile: File, fromVersion: Int): File? {
        return withContext(Dispatchers.IO) {
            if (fromVersion >= 4) {
                // Already current version, no migration needed
                return@withContext backupFile
            }

            try {
                // Create a temporary copy to migrate
                val tempFile = File(backupFile.parent, "${backupFile.name}.migrating")
                backupFile.copyTo(tempFile, overwrite = true)

                android.util.Log.d("BackupManager", "Migrating backup from version $fromVersion to version 4")

                var database: SQLiteDatabase? = null
                try {
                    database = SQLiteDatabase.openDatabase(
                        tempFile.absolutePath,
                        null,
                        SQLiteDatabase.OPEN_READWRITE
                    )

                    var currentVersion = fromVersion

                    // Migration 1→2: Add name column to time_entries
                    if (currentVersion == 1) {
                        android.util.Log.d("BackupManager", "Applying migration 1→2")
                        database.execSQL("ALTER TABLE time_entries ADD COLUMN name TEXT")
                        currentVersion = 2
                    }

                    // Migration 2→3: Add period tracking support
                    if (currentVersion == 2) {
                        android.util.Log.d("BackupManager", "Applying migration 2→3")

                        // Add periodMode column to projects
                        database.execSQL("ALTER TABLE projects ADD COLUMN periodMode TEXT NOT NULL DEFAULT 'monthly'")

                        // Create period_closes table
                        database.execSQL("""
                            CREATE TABLE period_closes (
                                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                                projectId INTEGER NOT NULL,
                                closeTime INTEGER NOT NULL,
                                isAutomatic INTEGER NOT NULL DEFAULT 0,
                                createdAt INTEGER NOT NULL,
                                FOREIGN KEY(projectId) REFERENCES projects(id) ON DELETE CASCADE
                            )
                        """)

                        // Create indices
                        database.execSQL("CREATE INDEX index_period_closes_projectId ON period_closes(projectId)")
                        database.execSQL("CREATE INDEX index_period_closes_closeTime ON period_closes(closeTime)")

                        // Initialize monthly closes for existing projects with historical data
                        database.execSQL("""
                            INSERT INTO period_closes (projectId, closeTime, isAutomatic, createdAt)
                            SELECT DISTINCT
                                projectId,
                                strftime('%s', date(startTime / 1000, 'unixepoch', 'start of month', '+1 month')) * 1000,
                                1,
                                strftime('%s', 'now') * 1000
                            FROM time_entries
                            WHERE startTime < strftime('%s', date('now', 'start of month')) * 1000
                            GROUP BY projectId, strftime('%Y-%m', startTime / 1000, 'unixepoch')
                            ORDER BY projectId, closeTime
                        """)

                        currentVersion = 3
                    }

                    // Migration 3→4: Add index on time_entries.projectId
                    if (currentVersion == 3) {
                        android.util.Log.d("BackupManager", "Applying migration 3→4")
                        database.execSQL("CREATE INDEX IF NOT EXISTS index_time_entries_projectId ON time_entries(projectId)")
                        currentVersion = 4
                    }

                    // Update database version
                    database.version = 4
                    android.util.Log.d("BackupManager", "Migration completed successfully to version 4")

                    return@withContext tempFile

                } finally {
                    database?.close()
                }

            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Backup migration failed: ${e.message}", e)
                null
            }
        }
    }

    // Auto-backup to external folder settings
    fun isAutoExportEnabled(): Boolean {
        return prefs.getBoolean("auto_export_enabled", false)
    }

    fun setAutoExportEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_export_enabled", enabled).apply()
    }

    fun getAutoExportFolderUri(): String? {
        return prefs.getString("auto_export_folder_uri", null)
    }

    fun setAutoExportFolderUri(uri: String?) {
        prefs.edit().putString("auto_export_folder_uri", uri).apply()
        if (uri != null) {
            // Persist permissions for the folder
            try {
                context.contentResolver.takePersistableUriPermission(
                    android.net.Uri.parse(uri),
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Failed to persist folder permissions: ${e.message}")
            }
        }
    }

    // Automatically export backup to external folder if enabled
    private suspend fun autoExportBackupIfEnabled(backupInfo: BackupInfo): Boolean {
        if (!isAutoExportEnabled()) {
            return true // Auto-export not enabled, consider it success
        }

        val folderUriString = getAutoExportFolderUri()
        if (folderUriString == null) {
            android.util.Log.w("BackupManager", "Auto-export enabled but no folder selected")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                val folderUri = android.net.Uri.parse(folderUriString)
                val documentUri = DocumentFile.fromTreeUri(context, folderUri)

                if (documentUri == null || !documentUri.canWrite()) {
                    android.util.Log.e("BackupManager", "Cannot write to auto-export folder")
                    return@withContext false
                }

                // Create file in the selected folder
                val fileName = backupInfo.file.name
                val mimeType = "application/octet-stream"
                val newFile = documentUri.createFile(mimeType, fileName)

                if (newFile == null) {
                    android.util.Log.e("BackupManager", "Failed to create file in auto-export folder")
                    return@withContext false
                }

                // Copy backup to the external folder
                context.contentResolver.openOutputStream(newFile.uri)?.use { outputStream ->
                    backupInfo.file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                android.util.Log.d("BackupManager", "Auto-exported backup to: ${newFile.uri}")
                true
            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Auto-export failed: ${e.message}", e)
                false
            }
        }
    }

    // Export backup to external file (user-chosen location)
    suspend fun exportBackupToUri(backupInfo: BackupInfo, destinationUri: android.net.Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                    backupInfo.file.inputStream().use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
                android.util.Log.d("BackupManager", "Backup exported to external location")
                true
            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Export failed: ${e.message}", e)
                false
            }
        }
    }

    // Import backup from external file
    suspend fun importBackupFromUri(sourceUri: android.net.Uri, backupName: String): BackupInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val importedFile = File(backupDir, backupName)

                context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                    importedFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                android.util.Log.d("BackupManager", "Backup imported from external location")

                // Get info about the imported backup
                val timestamp = Date()
                getBackupInfo(importedFile, BackupType.MANUAL, timestamp)

            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Import failed: ${e.message}", e)
                null
            }
        }
    }

    // Restore from backup - with migration support and rollback on failure
    suspend fun restoreFromBackup(backupInfo: BackupInfo): RestoreResult {
        return withContext(Dispatchers.IO) {
            val dbFile = context.getDatabasePath("howmanyhours_database")
            val walFile = File(dbFile.absolutePath + "-wal")
            val shmFile = File(dbFile.absolutePath + "-shm")
            val tempBackupOfCurrent = File(context.cacheDir, "pre_restore_backup.db")
            val tempWalBackup = File(context.cacheDir, "pre_restore_backup.db-wal")
            val tempShmBackup = File(context.cacheDir, "pre_restore_backup.db-shm")

            try {
                android.util.Log.d("BackupManager", "Starting safe restore from: ${backupInfo.file.name}")

                // Step 0: Validate and potentially migrate the backup
                val validation = validateBackup(backupInfo)

                if (!validation.isValid) {
                    return@withContext RestoreResult.Failed(validation.message)
                }

                val fileToRestore = if (validation.requiresDestructiveMigration) {
                    android.util.Log.d("BackupManager", "Backup requires migration from version ${validation.backupVersion}")
                    migrateBackupToCurrentVersion(backupInfo.file, validation.backupVersion)
                        ?: return@withContext RestoreResult.Failed("Failed to migrate backup")
                } else {
                    backupInfo.file
                }

                // Step 1: Close Room database completely
                try {
                    AppDatabase.getDatabase(context).close()
                    android.util.Log.d("BackupManager", "Room database closed for restore")
                } catch (e: Exception) {
                    android.util.Log.w("BackupManager", "Failed to close Room database: ${e.message}")
                }
                AppDatabase.clearInstance()
                Thread.sleep(500)

                // Step 2: CRITICAL - Backup current database for rollback
                try {
                    if (dbFile.exists()) {
                        dbFile.copyTo(tempBackupOfCurrent, overwrite = true)
                        android.util.Log.d("BackupManager", "Created rollback point")
                    }
                    if (walFile.exists()) {
                        walFile.copyTo(tempWalBackup, overwrite = true)
                    }
                    if (shmFile.exists()) {
                        shmFile.copyTo(tempShmBackup, overwrite = true)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("BackupManager", "Failed to create rollback point: ${e.message}")
                    return@withContext RestoreResult.Failed("Cannot create safety backup: ${e.message}")
                }

                // Step 3: Delete existing database files
                dbFile.delete()
                walFile.delete()
                shmFile.delete()
                android.util.Log.d("BackupManager", "Existing database deleted")

                // Step 4: Copy backup to main database location
                fileToRestore.copyTo(dbFile, overwrite = true)
                android.util.Log.d("BackupManager", "Backup copied to main database location")

                // Step 5: Clean up temporary migration file
                if (fileToRestore != backupInfo.file) {
                    fileToRestore.delete()
                }

                // Step 6: Try to open with Room (migrations will run here)
                try {
                    val db = AppDatabase.getDatabase(context)

                    // Step 7: Validate by attempting a query using the NEW database instance
                    // Don't use repository - it has DAOs from the old, deleted database!
                    db.projectDao().getAllProjects().first()

                    android.util.Log.d("BackupManager", "Restore successful, deleting rollback point")
                    tempBackupOfCurrent.delete()
                    tempWalBackup.delete()
                    tempShmBackup.delete()

                    return@withContext RestoreResult.Success

                } catch (e: Exception) {
                    // Step 8: ROLLBACK - Restore failed
                    android.util.Log.e("BackupManager", "Restore failed, rolling back: ${e.message}", e)

                    AppDatabase.clearInstance()
                    dbFile.delete()
                    walFile.delete()
                    shmFile.delete()

                    if (tempBackupOfCurrent.exists()) {
                        tempBackupOfCurrent.copyTo(dbFile, overwrite = true)
                    }
                    if (tempWalBackup.exists()) {
                        tempWalBackup.copyTo(walFile, overwrite = true)
                    }
                    if (tempShmBackup.exists()) {
                        tempShmBackup.copyTo(shmFile, overwrite = true)
                    }

                    // Reopen original database
                    AppDatabase.getDatabase(context)

                    return@withContext RestoreResult.Failed("Restore failed: ${e.message}. Rolled back to previous state.")
                }

            } catch (e: Exception) {
                android.util.Log.e("BackupManager", "Restore process failed: ${e.message}", e)

                // Emergency rollback if anything went wrong
                if (tempBackupOfCurrent.exists()) {
                    try {
                        AppDatabase.clearInstance()
                        dbFile.delete()
                        walFile.delete()
                        shmFile.delete()
                        tempBackupOfCurrent.copyTo(dbFile, overwrite = true)
                        if (tempWalBackup.exists()) tempWalBackup.copyTo(walFile, overwrite = true)
                        if (tempShmBackup.exists()) tempShmBackup.copyTo(shmFile, overwrite = true)
                        AppDatabase.getDatabase(context)
                    } catch (rollbackError: Exception) {
                        android.util.Log.e("BackupManager", "Emergency rollback failed: ${rollbackError.message}")
                    }
                }

                return@withContext RestoreResult.Failed(e.message ?: "Unknown error")

            } finally {
                // Clean up rollback files
                tempBackupOfCurrent.delete()
                tempWalBackup.delete()
                tempShmBackup.delete()
            }
        }
    }
}