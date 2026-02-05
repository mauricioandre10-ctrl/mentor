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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LlmUiState(
    val isInitializing: Boolean = true,
    val initializationError: String? = null,
    val isDownloading: Boolean = false,
    val downloadProgress: Float = 0f,
    val isGenerating: Boolean = false
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

    // La única lista de mensajes para la UI
    val uiMessages: StateFlow<List<ConversationMessage>> = combine(
        projectRepository.getMessagesForProject(projectId), // 1. Mensajes de la BD
        _streamingResponse // 2. El mensaje en streaming
    ) { messagesFromDb, streamingText ->
        val combinedList = messagesFromDb.toMutableList()
        if (streamingText.isNotBlank()) {
            // Añade el mensaje en streaming como un item temporal
            combinedList.add(
                ConversationMessage(projectId = projectId, message = streamingText, isFromUser = false)
            )
        }
        combinedList
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val fullResponse = StringBuilder()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _llmUiState.update { it.copy(isInitializing = true, isDownloading = true) }

            val modelUrl = "https://huggingface.co/mauricio19/mentor-gemma-model/resolve/main/gemma_3n_e2b_it_int4.task?download=true"
            val modelFileName = "gemma_model.task"

            val success = llmService.initializeModel(modelUrl, modelFileName) { progress ->
                _llmUiState.update { it.copy(downloadProgress = progress) }
            }

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
        _streamingResponse.value = " " // Inicia con un espacio para que el combine lo detecte

        viewModelScope.launch(Dispatchers.IO) {
            val userMessage = ConversationMessage(projectId = projectId, message = text, isFromUser = true)
            projectRepository.insertMessage(userMessage)

            val prompt = createPrompt(uiMessages.value, text)
            
            llmService.generateResponseAsync(prompt) { partialResult, done ->
                viewModelScope.launch(Dispatchers.Main) {
                    fullResponse.append(partialResult)
                    _streamingResponse.value = fullResponse.toString()

                    if (done) {
                        val finalMessage = fullResponse.toString().trim()
                        if (finalMessage.isNotBlank()) {
                            viewModelScope.launch(Dispatchers.IO) {
                                projectRepository.insertMessage(
                                    ConversationMessage(projectId = projectId, message = finalMessage, isFromUser = false)
                                )
                            }
                        }
                        _llmUiState.update { it.copy(isGenerating = false) }
                        _streamingResponse.value = "" // Limpia para eliminar el item temporal
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        llmService.close()
    }

    private fun createPrompt(history: List<ConversationMessage>, newMessage: String): String {
        val SYSTEM_PROMPT = """
Eres Mentor, un asistente experto en la creación de presupuestos para proyectos de construcción y remodelación. 
Tu función es ayudar al usuario a definir con precisión su proyecto, haciendo preguntas claras y relevantes para entender el alcance, materiales, plazos y cualquier otro detalle necesario.

Sigue estas reglas:
- Sé amable, profesional y paciente.
- Formula una sola pregunta por mensaje para mantener el diálogo ordenado.
- Evita respuestas largas o con información innecesaria.
- Usa un tono claro y profesional, sin tecnicismos excesivos.
- Si el usuario no da suficiente información, pídesela de forma específica.
- Una vez tengas los datos suficientes, genera un presupuesto estructurado, conciso y bien organizado.

Objetivo final: entregar un presupuesto comprensible, detallado y útil para el usuario.
"""


        val prompt = StringBuilder(SYSTEM_PROMPT)

        history.takeLast(10).forEach { // Limita el historial a los últimos 10 mensajes
            val prefix = if (it.isFromUser) "user: " else "model: "
            prompt.append(prefix).append(it.message).append("\n")
        }

        prompt.append("user: ".plus(newMessage)).append("\nmodel: ")
        return prompt.toString()
    }
}
