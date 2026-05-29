package net.luisico.howmanyhours.data.dao

import androidx.room.*
import net.luisico.howmanyhours.data.entities.TimeEntry
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

    @Query("""
        SELECT * FROM time_entries
        WHERE projectId = :projectId
        AND startTime >= :periodStart
        AND startTime <= :periodEnd
        ORDER BY startTime DESC
    """)
    fun getTimeEntriesForPeriod(projectId: Long, periodStart: Date, periodEnd: Date): Flow<List<TimeEntry>>

    @Query("""
        SELECT * FROM time_entries
        WHERE projectId = :projectId
        AND startTime >= :lastCloseTime
        ORDER BY startTime DESC
    """)
    fun getTimeEntriesForCurrentPeriod(projectId: Long, lastCloseTime: Date): Flow<List<TimeEntry>>

    @Query("SELECT * FROM time_entries ORDER BY startTime DESC")
    suspend fun getAllTimeEntries(): List<TimeEntry>

    @Query("SELECT * FROM time_entries ORDER BY startTime ASC LIMIT :limit OFFSET :offset")
    suspend fun getTimeEntriesBatch(limit: Int, offset: Int): List<TimeEntry>

    @Query("SELECT COUNT(*) FROM time_entries")
    suspend fun getTimeEntriesCount(): Int

    @Insert
    suspend fun insertTimeEntry(timeEntry: TimeEntry): Long

    @Update
    suspend fun updateTimeEntry(timeEntry: TimeEntry)

    @Delete
    suspend fun deleteTimeEntry(timeEntry: TimeEntry)

    @Query("UPDATE time_entries SET isRunning = 0, endTime = :endTime WHERE isRunning = 1")
    suspend fun stopAllRunningEntries(endTime: Date)

    @Query("SELECT * FROM time_entries WHERE projectId = :projectId AND isRunning = 0 AND endTime IS NOT NULL ORDER BY endTime DESC LIMIT 1")
    suspend fun getMostRecentCompletedEntry(projectId: Long): TimeEntry?

    @Query("SELECT DISTINCT name FROM time_entries WHERE name IS NOT NULL AND startTime >= :since ORDER BY startTime DESC")
    suspend fun getRecentEntryNames(since: Date): List<String>

    // Only deletes entries that are both in the provided IDs and still flagged as paused — double safety filter
    @Query("DELETE FROM time_entries WHERE id IN (:ids) AND isPausedInterval = 1")
    suspend fun deletePausedIntervals(ids: List<Long>)

    // Clears the paused flag from completed session intervals (called on final stop)
    @Query("UPDATE time_entries SET isPausedInterval = 0 WHERE id IN (:ids)")
    suspend fun clearPausedIntervalFlags(ids: List<Long>)

    // Clears all orphaned paused flags on app start (entries from a session that ended abnormally)
    @Query("UPDATE time_entries SET isPausedInterval = 0 WHERE isPausedInterval = 1")
    suspend fun clearAllPausedIntervalFlags()
}