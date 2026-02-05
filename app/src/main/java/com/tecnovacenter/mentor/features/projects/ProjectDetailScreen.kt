package com.tecnovacenter.mentor.features.projects

import android.app.Application
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tecnovacenter.mentor.data.local.AppDatabase
import com.tecnovacenter.mentor.data.repository.ProjectRepository
import com.tecnovacenter.mentor.features.llm.LlmService

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
    val aiResponse by viewModel.aiResponse.collectAsState()
    val llmState by viewModel.llmUiState.collectAsState()
    var newMessage by remember { mutableStateOf("") }

    if (llmState.isDownloading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Descargando modelo de IA...")
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(progress = llmState.downloadProgress)
            }
        }
    } else if (llmState.isInitializing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
            Text("Inicializando motor de IA...")
        }
    } else if (llmState.initializationError != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(llmState.initializationError!!)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).padding(8.dp),
                reverseLayout = true
            ) {
                if (aiResponse.isNotBlank()) {
                    item { Text("IA: $aiResponse") }
                }
                items(messages.reversed()) { message ->
                    val prefix = if (message.isFromUser) "Tú:" else "IA:"
                    Text("$prefix ${message.message}")
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
                    enabled = true
                )
                Button(
                    onClick = {
                        if (newMessage.isNotBlank()) {
                            viewModel.sendMessage(newMessage)
                            newMessage = ""
                        }
                    },
                    modifier = Modifier.padding(start = 8.dp),
                    enabled = true
                ) {
                    Text("Enviar")
                }
            }
        }
    }
}
