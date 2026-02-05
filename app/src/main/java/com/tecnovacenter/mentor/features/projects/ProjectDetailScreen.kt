package com.tecnovacenter.mentor.features.projects

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnovacenter.mentor.data.local.AppDatabase
import com.tecnovacenter.mentor.data.repository.ProjectRepository
import com.tecnovacenter.mentor.features.llm.LlmService
import com.tecnovacenter.mentor.data.ConversationMessage
import com.tecnovacenter.mentor.ui.theme.seed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(projectId: Long) {
    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Chat", "Presupuestos", "Notas")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Proyecto #$projectId") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = tabIndex == index,
                        onClick = { tabIndex = index },
                        text = { Text(title) })
                }
            }
            when (tabIndex) {
                0 -> ChatScreen(projectId = projectId)
                1 -> Text("Aquí irán los presupuestos")
                2 -> Text("Aquí irán las notas")
            }
        }
    }
}

@Composable
fun ChatScreen(projectId: Long) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = ProjectRepository(db.projectDao(), db.conversationMessageDao(), db.budgetDao())
    val llmService = LlmService(application)
    val factory = ViewModelFactory(application, repository, projectId, llmService)
    val viewModel: ProjectDetailViewModel = viewModel(factory = factory)

    val messages by viewModel.messages.collectAsState()
    val streamingResponse by viewModel.streamingResponse.collectAsState()
    val llmState by viewModel.llmUiState.collectAsState()
    var newMessage by remember { mutableStateOf("") }

    if (llmState.isDownloading) {
        // ... (La pantalla de descarga no cambia)
    } else if (llmState.isInitializing) {
        // ... (La pantalla de inicialización no cambia)
    } else if (llmState.initializationError != null) {
        // ... (La pantalla de error no cambia)
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp, vertical = 4.dp),
                reverseLayout = true
            ) {
                // Renderiza la respuesta de la IA en streaming
                if (streamingResponse.isNotBlank()) {
                    item {
                        ChatMessageItem(message = ConversationMessage(message = streamingResponse, isFromUser = false, projectId = projectId))
                    }
                }
                // Renderiza los mensajes ya guardados
                items(messages.reversed()) { message ->
                    ChatMessageItem(message = message)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newMessage,
                    onValueChange = { newMessage = it },
                    modifier = Modifier.weight(1f),
                    label = { Text("Escribe un mensaje...") },
                    enabled = !llmState.isGenerating
                )
                Button(
                    onClick = {
                        if (newMessage.isNotBlank()) {
                            viewModel.sendMessage(newMessage)
                            newMessage = ""
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = !llmState.isGenerating
                ) {
                    if (llmState.isGenerating) {
                        CircularProgressIndicator()
                    } else {
                        Text("Enviar")
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ConversationMessage) {
    val messageColor = if (message.isFromUser) seed else Color(0xFFF0F0F0)
    val textColor = if (message.isFromUser) Color.White else Color.Black
    val arrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = arrangement
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = messageColor)
        ) {
            Text(
                text = message.message,
                color = textColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
