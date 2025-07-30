package com.howmanyhours.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.howmanyhours.data.dao.ProjectDao
import com.howmanyhours.data.dao.TimeEntryDao
import com.howmanyhours.data.entities.Project
import com.howmanyhours.data.entities.TimeEntry
import com.howmanyhours.utils.DateConverters

@Database(
    entities = [Project::class, TimeEntry::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun timeEntryDao(): TimeEntryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "howmanyhours_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}