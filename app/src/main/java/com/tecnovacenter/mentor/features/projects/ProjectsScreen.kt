package com.tecnovacenter.mentor.features.projects

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.tecnovacenter.mentor.data.local.AppDatabase
import com.tecnovacenter.mentor.data.repository.ProjectRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(navController: NavController, onLogout: () -> Unit) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val db = AppDatabase.getDatabase(context)
    val repository = ProjectRepository(db.projectDao(), db.conversationMessageDao(), db.budgetDao())

    // Asegúrate de que ViewModelFactory reciba todos los argumentos que necesita
    val factory = ViewModelFactory(application, repository)
    val viewModel: ProjectsViewModel = viewModel(factory = factory)

    val projects by viewModel.projects.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Proyectos") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.addProject("Nuevo Proyecto") }) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo proyecto")
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            items(projects) { project ->
                ListItem(
                    headlineContent = { Text(project.name) },
                    modifier = Modifier.clickable { navController.navigate("projectDetail/${project.id}") }
                )
            }
        }
    }
}
