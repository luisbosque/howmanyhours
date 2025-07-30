package com.howmanyhours.data.dao

import androidx.room.*
import com.howmanyhours.data.entities.TimeEntry
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TimeEntryDao {
    @Query("SELECT * FROM time_entries WHERE projectId = :projectId ORDER BY startTime DESC")
    fun getTimeEntriesForProject(projectId: Long): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries WHERE isRunning = 1 LIMIT 1")
    suspend fun getRunningTimeEntry(): TimeEntry?

    @Query("SELECT * FROM time_entries WHERE projectId = :projectId AND isRunning = 1 LIMIT 1")
    suspend fun getRunningTimeEntryForProject(projectId: Long): TimeEntry?

    @Query("""
        SELECT * FROM time_entries 
        WHERE projectId = :projectId 
        AND startTime >= :monthStart 
        AND startTime < :monthEnd 
        ORDER BY startTime DESC
    """)
    fun getTimeEntriesForMonth(projectId: Long, monthStart: Date, monthEnd: Date): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries ORDER BY startTime DESC")
    suspend fun getAllTimeEntries(): List<TimeEntry>

    @Insert
    suspend fun insertTimeEntry(timeEntry: TimeEntry): Long

    @Update
    suspend fun updateTimeEntry(timeEntry: TimeEntry)

    @Delete
    suspend fun deleteTimeEntry(timeEntry: TimeEntry)

    @Query("UPDATE time_entries SET isRunning = 0, endTime = :endTime WHERE isRunning = 1")
    suspend fun stopAllRunningEntries(endTime: Date)

    @Query("""
        DELETE FROM time_entries 
        WHERE projectId = :projectId 
        AND startTime >= :monthStart 
        AND startTime < :monthEnd
    """)
    suspend fun deleteTimeEntriesForMonth(projectId: Long, monthStart: Date, monthEnd: Date)
}