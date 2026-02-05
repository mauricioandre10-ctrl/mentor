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
    val downloadProgress: Float = 0f,
    val isGenerating: Boolean = false // Nuevo estado para saber si está pensando
)

class ProjectDetailViewModel(
    application: Application,
    private val projectRepository: ProjectRepository,
    private val projectId: Long,
    private val llmService: LlmService
) : AndroidViewModel(application) {

    private val _llmUiState = MutableStateFlow(LlmUiState())
    val llmUiState: StateFlow<LlmUiState> = _llmUiState.asStateFlow()

    private val _streamingResponse = MutableStateFlow("")
    val streamingResponse: StateFlow<String> = _streamingResponse.asStateFlow()

    private val fullResponse = StringBuilder()

    val messages: StateFlow<List<ConversationMessage>> =
        projectRepository.getMessagesForProject(projectId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _llmUiState.update { it.copy(isInitializing = true, isDownloading = true) }

            val modelUrl = "https://huggingface.co/mauricio19/mentor-gemma-model/resolve/main/gemma_3n_e2b_it_int4.task?download=true"
            val modelFileName = "gemma_model.task"

            val success = llmService.initializeModel(modelUrl, modelFileName, 
                onDownloadProgress = { progress ->
                    _llmUiState.update { it.copy(downloadProgress = progress) }
                },
                onStreamResult = { partialResult, done ->
                    // Acumulamos el texto y lo emitimos
                    fullResponse.append(partialResult)
                    _streamingResponse.value = fullResponse.toString()

                    if (done) {
                        // Cuando termina, guardamos el mensaje completo y reseteamos
                        val finalMessage = fullResponse.toString()
                        if (finalMessage.isNotBlank()) {
                            viewModelScope.launch(Dispatchers.IO) {
                                projectRepository.insertMessage(
                                    ConversationMessage(projectId = projectId, message = finalMessage, isFromUser = false)
                                )
                            }
                        }
                        _llmUiState.update { it.copy(isGenerating = false) }
                        _streamingResponse.value = "" // Limpiamos la respuesta en streaming
                    }
                }
            )

            _llmUiState.update {
                if (success) it.copy(isInitializing = false, isDownloading = false)
                else it.copy(isInitializing = false, isDownloading = false, initializationError = "No se pudo inicializar el modelo.")
            }
        }
    }

    fun sendMessage(text: String) {
        if (llmUiState.value.isGenerating) return

        _llmUiState.update { it.copy(isGenerating = true) }
        fullResponse.clear()
        _streamingResponse.value = ""

        viewModelScope.launch(Dispatchers.IO) {
            val userMessage = ConversationMessage(projectId = projectId, message = text, isFromUser = true)
            projectRepository.insertMessage(userMessage)

            val prompt = createPrompt(messages.value, text)
            llmService.generateResponseAsync(prompt)
        }
    }

    override fun onCleared() {
        super.onCleared()
        llmService.close()
    }

    private fun createPrompt(history: List<ConversationMessage>, newMessage: String): String {
        // ... (La lógica del prompt no cambia)
    }
}
