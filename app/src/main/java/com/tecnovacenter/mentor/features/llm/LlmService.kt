package com.tecnovacenter.mentor.features.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.ProgressListener
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class LlmService(private val context: Context) {

    private var llmInference: LlmInference? = null

    fun initializeModel(modelUrl: String, modelFileName: String, onDownloadProgress: (Float) -> Unit): Boolean {
        return try {
            val modelFile = File(context.filesDir, modelFileName)

            if (!modelFile.exists() || modelFile.length() < 1000000) {
                Log.d("LlmService", "Iniciando descarga desde: $modelUrl")
                downloadModel(modelUrl, modelFile, onDownloadProgress)
            }

            Log.d("LlmService", "Model exists=${modelFile.exists()} size=${modelFile.length()} path=${modelFile.absolutePath}")

            // 1. Opciones del Motor Base (sin listener)
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024) // Máximo total (entrada + salida)
                .setMaxTopK(40)
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            Log.d("LlmService", "✅ Engine inicializado OK")
            true
        } catch (e: Exception) {
            Log.e("LlmService", "❌ Error al inicializar modelo", e)
            val modelFile = File(context.filesDir, modelFileName)
            if (modelFile.exists()) modelFile.delete()
            false
        }
    }

    fun generateResponseAsync(
        prompt: String,
        onResult: (partialResult: String, done: Boolean) -> Unit
    ) {
        val engine = llmInference ?: throw IllegalStateException("LlmInference no inicializado.")

        try {
            // 2. Opciones de la Sesión
            val sessionOptions = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(40)
                .setTemperature(0.8f)
                .build()

            // 3. Crear la sesión para esta consulta
            val session = LlmInferenceSession.createFromOptions(engine, sessionOptions)

            // 4. Añadir el prompt ANTES de llamar a la generación
            session.addQueryChunk(prompt)

            // 5. Iniciar la generación en streaming con el listener correcto
            session.generateResponseAsync(ProgressListener { partialResult, done ->
                // Llamamos al callback con los dos parámetros correctos
                onResult(partialResult, done)
            })

        } catch(e: Exception) {
            Log.e("LlmService", "❌ Error al iniciar la generación en streaming", e)
            onResult("Error: ${e.message}", true) // Notificamos del error y finalizamos
        }
    }

    private fun downloadModel(url: String, file: File, onProgress: (Float) -> Unit) {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connect()

        if (connection.responseCode in 200..299) {
            val fileSize = connection.contentLength.toFloat()
            connection.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(4 * 1024) // Buffer de 4KB
                    var downloadedBytes = 0L
                    while (true) {
                        val bytesRead = input.read(buffer)
                        if (bytesRead == -1) break
                        output.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        if (fileSize > 0) {
                            onProgress(downloadedBytes / fileSize)
                        }
                    }
                }
            }
        } else {
            throw java.io.IOException("Respuesta del servidor inesperada: ${connection.responseCode}")
        }
        connection.disconnect()
    }

    fun close() {
        llmInference?.close()
    }
}
