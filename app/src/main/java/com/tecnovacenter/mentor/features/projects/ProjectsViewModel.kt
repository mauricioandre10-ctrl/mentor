package com.tecnovacenter.mentor.features.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tecnovacenter.mentor.data.Project
import com.tecnovacenter.mentor.data.repository.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(private val projectRepository: ProjectRepository) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.getAllProjects()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addProject(projectName: String) {
        viewModelScope.launch {
            projectRepository.insertProject(Project(name = projectName))
        }
    }
}
