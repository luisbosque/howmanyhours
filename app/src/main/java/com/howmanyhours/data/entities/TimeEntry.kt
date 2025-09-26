package com.howmanyhours.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "time_entries",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TimeEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val projectId: Long,
    val startTime: Date,
    val endTime: Date? = null,
    val isRunning: Boolean = false,
    val name: String? = null
) {
    fun getDurationInMinutes(): Long {
        val end = endTime ?: Date()
        return (end.time - startTime.time) / (1000 * 60)
    }
}