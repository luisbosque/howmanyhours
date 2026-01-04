package com.howmanyhours.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.howmanyhours.data.entities.PeriodClose
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface PeriodCloseDao {
    @Query("SELECT * FROM period_closes WHERE projectId = :projectId ORDER BY closeTime ASC")
    fun getPeriodsForProject(projectId: Long): Flow<List<PeriodClose>>

    @Query("SELECT * FROM period_closes WHERE projectId = :projectId AND closeTime <= :timestamp ORDER BY closeTime DESC LIMIT 1")
    suspend fun getLastCloseBefore(projectId: Long, timestamp: Date): PeriodClose?

    @Query("SELECT * FROM period_closes WHERE projectId = :projectId AND closeTime > :timestamp ORDER BY closeTime ASC LIMIT 1")
    suspend fun getNextCloseAfter(projectId: Long, timestamp: Date): PeriodClose?

    @Query("SELECT * FROM period_closes WHERE projectId = :projectId AND closeTime = :closeTime AND isAutomatic = 1 LIMIT 1")
    suspend fun getAutoCloseAtTime(projectId: Long, closeTime: Date): PeriodClose?

    @Insert
    suspend fun insertPeriodClose(periodClose: PeriodClose): Long

    @Delete
    suspend fun deletePeriodClose(periodClose: PeriodClose)

    @Query("DELETE FROM period_closes WHERE projectId = :projectId AND closeTime > :afterTime AND isAutomatic = 1")
    suspend fun deleteAutoClosesAfter(projectId: Long, afterTime: Date)

    @Query("SELECT * FROM period_closes ORDER BY closeTime ASC")
    suspend fun getAllPeriodCloses(): List<PeriodClose>
}
