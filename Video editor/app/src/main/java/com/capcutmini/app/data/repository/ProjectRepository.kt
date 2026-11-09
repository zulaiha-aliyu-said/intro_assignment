package com.capcutmini.app.data.repository

import com.capcutmini.app.data.local.ProjectDao
import com.capcutmini.app.data.local.ProjectEntity
import com.capcutmini.app.media.model.EditDocument
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao
) {
    fun observeProjects(): Flow<List<ProjectEntity>> = projectDao.observeAll()

    suspend fun getProject(id: Long): ProjectEntity? = projectDao.getById(id)

    suspend fun createProject(
        title: String,
        sourceUri: String,
        durationMs: Long = 0L,
        editDocument: EditDocument = EditDocument()
    ): Long {
        val now = System.currentTimeMillis()
        return projectDao.insert(
            ProjectEntity(
                title = title,
                sourceUri = sourceUri,
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                durationMs = durationMs,
                editJson = editDocument.toJson()
            )
        )
    }

    suspend fun updateProject(entity: ProjectEntity) {
        projectDao.update(entity.copy(updatedAtEpochMs = System.currentTimeMillis()))
    }

    suspend fun saveEditDocument(projectId: Long, document: EditDocument, durationMs: Long) {
        val existing = projectDao.getById(projectId) ?: return
        projectDao.update(
            existing.copy(
                title = document.projectTitle.ifBlank { existing.title },
                sourceUri = document.clips.firstOrNull()?.uri ?: existing.sourceUri,
                durationMs = durationMs,
                editJson = document.toJson(),
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteProject(project: ProjectEntity) {
        projectDao.delete(project)
    }
}
