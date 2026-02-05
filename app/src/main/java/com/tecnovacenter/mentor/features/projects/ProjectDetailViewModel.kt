package com.tecnovacenter.mentor.features.projects

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tecnovacenter.mentor.data.ConversationMessage
import com.tecnovacenter.mentor.data.repository.ProjectRepository
import com.tecnovacenter.mentor.features.llm.LlmService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LlmUiState(
    val isInitializing: Boolean = true,
    val initializationError: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f
)

class ProjectDetailViewModel(
    application: Application,
    private val projectRepository: ProjectRepository,
    private val projectId: Long,
    private val llmService: LlmService
) : AndroidViewModel(application) {

    private val _llmUiState = MutableStateFlow(LlmUiState())
    val llmUiState: StateFlow<LlmUiState> = _llmUiState.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    val messages: StateFlow<List<ConversationMessage>> =
        projectRepository.getMessagesForProject(projectId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _llmUiState.update { it.copy(isInitializing = true, initializationError = null, isDownloading = true) }

            val modelUrl = "https://drive.google.com/uc?export=download&id=1sSNyr4jrRRBtK2fCxFhe6emSzw3GZGnN"
            val modelFileName = "gemma_model.task"

            val success = llmService.initializeModel(modelUrl, modelFileName) { progress ->
                _llmUiState.update { it.copy(downloadProgress = progress) }
            }

            _llmUiState.update {
                if (success) it.copy(isInitializing = false, isDownloading = false)
                else it.copy(
                    isInitializing = false,
                    isDownloading = false,
                    initializationError = "No se pudo descargar o inicializar el modelo."
                )
            }
        }
    }

    fun sendMessage(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userMessage = ConversationMessage(projectId = projectId, message = text, isFromUser = true)
            projectRepository.insertMessage(userMessage)

            val prompt = createPrompt(messages.value, text)
            val finalResponse = llmService.generateResponse(prompt)
            _aiResponse.value = finalResponse

            if (finalResponse.isNotBlank() && !finalResponse.startsWith("Error:")) {
                val aiMessage = ConversationMessage(projectId = projectId, message = finalResponse, isFromUser = false)
                projectRepository.insertMessage(aiMessage)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        llmService.close()
    }

    private fun createPrompt(history: List<ConversationMessage>, newMessage: String): String {
        val prompt = StringBuilder()
        history.forEach {
            val prefix = if (it.isFromUser) "user: " else "model: "
            prompt.append(prefix).append(it.message).append("\n")
        }
        prompt.append("user: ").append(newMessage).append("\nmodel: ")
        return prompt.toString()
    }
}
