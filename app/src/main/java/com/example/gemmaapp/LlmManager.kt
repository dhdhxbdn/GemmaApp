package com.example.gemmaapp

import android.content.Context
import android.net.Uri
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class LlmManager(private val context: Context) {
    private var llmInference: LlmInference? = null
    var isModelLoaded = false
        private set

    suspend fun loadModel(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, "current_model.bin")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(modelFile).use { output -> input.copyTo(output) }
            }
            val options = LlmInference.LlmInferenceOptions.builder()
                    .setPreferredBackend(LlmInference.Backend.CPU)
                .setModelPath(modelFile.absolutePath)
                .setMaxTokens(1024)
                .build()
            
            llmInference?.close()
            llmInference = LlmInference.createFromOptions(context, options)
            isModelLoaded = true
            return@withContext true
        } catch (e: Exception) {
            e.printStackTrace()
            isModelLoaded = false
            return@withContext false
        }
    }

    suspend fun generateResponse(prompt: String): String = withContext(Dispatchers.IO) {
        if (!isModelLoaded || llmInference == null) return@withContext "Модель не загружена."
        try {
            return@withContext llmInference!!.generateResponse(prompt)
        } catch (e: Exception) {
            return@withContext "Ошибка: ${e.message}"
        }
    }
}
