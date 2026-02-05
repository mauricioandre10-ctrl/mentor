package com.tecnovacenter.mentor.data.repository

import com.tecnovacenter.mentor.data.ConversationMessage
import com.tecnovacenter.mentor.data.Project
import com.tecnovacenter.mentor.data.local.BudgetDao
import com.tecnovacenter.mentor.data.local.ConversationMessageDao
import com.tecnovacenter.mentor.data.local.ProjectDao
import kotlinx.coroutines.flow.Flow

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val conversationMessageDao: ConversationMessageDao,
    private val budgetDao: BudgetDao
) {

    // --- Project Methods ---
    fun getAllProjects(): Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun insertProject(project: Project) {
        projectDao.insertProject(project)
    }

    // --- Message Methods ---
    fun getMessagesForProject(projectId: Long): Flow<List<ConversationMessage>> {
        return conversationMessageDao.getMessagesForProject(projectId)
    }

    suspend fun insertMessage(message: ConversationMessage) {
        conversationMessageDao.insertMessage(message)
    }
}
