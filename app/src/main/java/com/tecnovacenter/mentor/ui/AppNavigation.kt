package com.tecnovacenter.mentor.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.tecnovacenter.mentor.features.auth.AuthViewModel
import com.tecnovacenter.mentor.features.auth.LoginScreen
import com.tecnovacenter.mentor.features.projects.ProjectDetailScreen
import com.tecnovacenter.mentor.features.projects.ProjectsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory(context))

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val user = authViewModel.getCurrentUser()
        startDestination = if (user != null) "projects" else "login"
    }

    if (startDestination != null) {
        NavHost(navController = navController, startDestination = startDestination!!) {
            composable("login") {
                LoginScreen(onLoginSuccess = {
                    navController.navigate("projects") {
                        popUpTo("login") { inclusive = true }
                    }
                })
            }
            composable("projects") {
                ProjectsScreen(navController = navController, onLogout = {
                    authViewModel.logout()
                    navController.navigate("login") {
                        popUpTo("projects") { inclusive = true }
                    }
                })
            }
            composable(
                route = "projectDetail/{projectId}",
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) {
                val projectId = it.arguments?.getLong("projectId") ?: 0
                ProjectDetailScreen(projectId = projectId)
            }
        }
    } else {
        // Muestra un indicador de carga mientras se determina la ruta de inicio
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
