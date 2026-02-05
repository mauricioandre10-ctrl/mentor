package com.tecnovacenter.mentor.data.repository

import android.content.Context
import com.tecnovacenter.mentor.data.AppwriteConstants
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.models.User

class AuthRepository(context: Context) {

    private val client = Client(context)
        .setEndpoint(AppwriteConstants.ENDPOINT)
        .setProject(AppwriteConstants.PROJECT_ID)

    private val account = Account(client)

    suspend fun login(email: String, password: String): Boolean {
        return try {
            account.createEmailPasswordSession(email, password)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCurrentUser(): User<Map<String, Any>>? {
        return try {
            account.get()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun validateLicense(): Boolean {
        // En una futura implementación, llamaríamos a la función de Appwrite
        // Por ahora, simularemos que la licencia es siempre válida
        return true
    }

    suspend fun logout() {
        try {
            account.deleteSession("current")
        } catch (_: Exception) {}
    }
}
