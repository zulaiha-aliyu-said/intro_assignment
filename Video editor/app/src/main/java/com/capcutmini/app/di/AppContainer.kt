package com.capcutmini.app.di

import android.content.Context
import com.capcutmini.app.data.local.AppDatabase
import com.capcutmini.app.data.repository.ProjectRepository

/**
 * Lightweight manual DI — no Hilt/Koin, keeps the milestone free of extra frameworks.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    val projectRepository: ProjectRepository = ProjectRepository(database.projectDao())
}
