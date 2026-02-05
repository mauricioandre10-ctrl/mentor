package com.tecnovacenter.mentor.features.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.net.URL

class LlmService(private val context: Context) {

    private var llmInference: LlmInference? = null

    fun initializeModel(modelUrl: String, modelFileName: String, onDownloadProgress: (Float) -> Unit): Boolean {
        return try {
            val modelFile = File(context.filesDir, modelFileName)

            if (!modelFile.exists()) {
                Log.d("LlmService", "El modelo no existe. Iniciando descarga desde: $modelUrl")
                downloadModel(modelUrl, modelFile, onDownloadProgress)
            }

            Log.d("LlmService", "Model exists=${modelFile.exists()} size=${modelFile.length()} path=${modelFile.absolutePath}")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(512)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.d("LlmService", "✅ Engine inicializado OK")
            true
        } catch (e: Exception) {
            Log.e("LlmService", "❌ Error al inicializar modelo", e)
            false
        }
    }

    private fun downloadModel(url: String, file: File, onProgress: (Float) -> Unit) {
        URL(url).openStream().use { input ->
            file.outputStream().use { output ->
                input.copyTo(output) // En una app real, se haría con seguimiento de progreso
            }
        }
    }

    fun generateResponse(prompt: String): String {
        if (llmInference == null) {
            return "Error: El motor de IA no está inicializado."
        }

        return try {
            llmInference?.generateResponse(prompt) ?: "Error: Respuesta nula del modelo."
        } catch (e: Exception) {
            Log.e("LlmService", "❌ Error generando respuesta", e)
            "Error: ${e.message}"
        }
    }

    fun close() {
        llmInference?.close()
        llmInference = null
    }
}
