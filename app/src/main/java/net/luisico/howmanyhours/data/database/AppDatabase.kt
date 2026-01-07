package net.luisico.howmanyhours.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import net.luisico.howmanyhours.data.dao.PeriodCloseDao
import net.luisico.howmanyhours.data.dao.ProjectDao
import net.luisico.howmanyhours.data.dao.TimeEntryDao
import net.luisico.howmanyhours.data.entities.PeriodClose
import net.luisico.howmanyhours.data.entities.Project
import net.luisico.howmanyhours.data.entities.TimeEntry
import net.luisico.howmanyhours.utils.DateConverters

@Database(
    entities = [Project::class, TimeEntry::class, PeriodClose::class],
    version = 4,
    exportSchema = true
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun timeEntryDao(): TimeEntryDao
    abstract fun periodCloseDao(): PeriodCloseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2: Add name column to time_entries table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE time_entries ADD COLUMN name TEXT"
                )
            }
        }

        // Migration from version 2 to 3: Add period tracking support
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 1. Add periodMode column to projects table
                database.execSQL(
                    "ALTER TABLE projects ADD COLUMN periodMode TEXT NOT NULL DEFAULT 'monthly'"
                )

                // 2. Create period_closes table
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

                // 3. Create indices for efficient queries
                database.execSQL("CREATE INDEX index_period_closes_projectId ON period_closes(projectId)")
                database.execSQL("CREATE INDEX index_period_closes_closeTime ON period_closes(closeTime)")

                // 4. Initialize monthly closes for existing projects
                // For each project, create auto-close records at the end of each past month
                // that contains time entries (this preserves historical monthly data)
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
            }
        }

        // Migration from version 3 to 4: Add index on time_entries.projectId for performance
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add index on foreign key column for improved query performance
                // and faster cascading deletes
                database.execSQL("CREATE INDEX IF NOT EXISTS index_time_entries_projectId ON time_entries(projectId)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "howmanyhours_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                // No fallback - backups handle old schema versions via migration
                .build()
                INSTANCE = instance
                instance
            }
        }

        fun clearInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        /**
         * Emergency recovery: Forcefully delete the database and start fresh.
         * Use this only when the app is in an unrecoverable state.
         */
        fun emergencyReset(context: Context) {
            synchronized(this) {
                try {
                    // Close any existing instance
                    INSTANCE?.close()
                    INSTANCE = null

                    // Delete all database files
                    val dbFile = context.getDatabasePath("howmanyhours_database")
                    val walFile = java.io.File(dbFile.absolutePath + "-wal")
                    val shmFile = java.io.File(dbFile.absolutePath + "-shm")

                    dbFile.delete()
                    walFile.delete()
                    shmFile.delete()

                    android.util.Log.i("AppDatabase", "Emergency reset completed - database deleted")
                } catch (e: Exception) {
                    android.util.Log.e("AppDatabase", "Emergency reset failed: ${e.message}")
                }
            }
        }
    }
}