package com.tecnovacenter.mentor.features.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class LlmService(private val context: Context) {

    private var llmInference: LlmInference? = null

    fun initializeModel(
        modelUrl: String,
        modelFileName: String,
        onDownloadProgress: (Float) -> Unit,
        onStreamResult: (partialResult: String, done: Boolean) -> Unit // Callback para el streaming
    ): Boolean {
        return try {
            val modelFile = File(context.filesDir, modelFileName)

            if (!modelFile.exists() || modelFile.length() < 1000000) {
                Log.d("LlmService", "Iniciando descarga desde: $modelUrl")
                downloadModel(modelUrl, modelFile, onDownloadProgress)
            }

            Log.d("LlmService", "Model exists=${modelFile.exists()} size=${modelFile.length()} path=${modelFile.absolutePath}")

            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .setResultListener { partialResult, done ->
                    // Cuando el modelo emite, llamamos al callback
                    onStreamResult(partialResult, done)
                }
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.d("LlmService", "✅ Engine inicializado OK")
            true
        } catch (e: Exception) {
            Log.e("LlmService", "❌ Error al inicializar modelo", e)
            val modelFile = File(context.filesDir, modelFileName)
            if (modelFile.exists()) {
                modelFile.delete()
            }
            false
        }
    }

    private fun downloadModel(url: String, file: File, onProgress: (Float) -> Unit) {
        // ... (La lógica de descarga no cambia)
    }

    // Ahora es asíncrono, no devuelve nada directamente
    fun generateResponseAsync(prompt: String) {
        try {
            llmInference?.generateResponseAsync(prompt)
        } catch (e: Exception) {
            Log.e("LlmService", "❌ Error generando respuesta async", e)
        }
    }

    fun close() {
        llmInference?.close()
        llmInference = null
    }
}
