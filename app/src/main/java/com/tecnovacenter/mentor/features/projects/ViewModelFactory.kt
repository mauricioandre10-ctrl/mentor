package com.tecnovacenter.mentor.features.projects

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tecnovacenter.mentor.data.repository.ProjectRepository
import com.tecnovacenter.mentor.features.llm.LlmService

class ViewModelFactory(
    private val application: Application,
    private val projectRepository: ProjectRepository,
    private val projectId: Long? = null,
    private val llmService: LlmService? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectsViewModel(projectRepository) as T
        } else if (modelClass.isAssignableFrom(ProjectDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectDetailViewModel(
                application,
                projectRepository,
                projectId ?: throw IllegalArgumentException("projectId is required"),
                llmService ?: throw IllegalArgumentException("llmService is required")
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
