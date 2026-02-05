package com.tecnovacenter.mentor.features.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tecnovacenter.mentor.data.repository.AuthRepository
import io.appwrite.models.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)
            val loggedIn = authRepository.login(email, password)
            if (loggedIn) {
                val licenseValid = authRepository.validateLicense()
                if (licenseValid) {
                    _authState.value = AuthState(isLoggedIn = true)
                } else {
                    _authState.value = AuthState(error = "Licencia inválida o caducada")
                    authRepository.logout()
                }
            } else {
                _authState.value = AuthState(error = "Email o contraseña incorrectos")
            }
        }
    }

    suspend fun getCurrentUser(): User<Map<String, Any>>? {
        return authRepository.getCurrentUser()
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
        }
    }

    companion object {
        fun Factory(context: Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
                        @Suppress("UNCHECKED_CAST")
                        return AuthViewModel(AuthRepository(context)) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }
    }
}
