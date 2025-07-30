package com.howmanyhours.data.dao

import androidx.room.*
import com.howmanyhours.data.entities.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY name ASC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: Long): Project?

    @Query("SELECT * FROM projects WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveProject(): Project?

    @Insert
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("UPDATE projects SET isActive = 0")
    suspend fun deactivateAllProjects()

    @Query("UPDATE projects SET isActive = 1 WHERE id = :projectId")
    suspend fun activateProject(projectId: Long)
}