package com.example.gemmaapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LlmManager(private val context: Context) {
    private var isLoaded = false
    private var currentModelPath: String? = null

    suspend fun initModel(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            LlamaBridge.unloadModel()
            val success = LlamaBridge.loadModel(modelPath)
            if (success) {
                isLoaded = true
                currentModelPath = modelPath
            } else {
                error("Не удалось загрузить GGUF модель через llama.cpp")
            }
        }
    }

    suspend fn generateResponse(
        prompt: String,
        onToken: (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isLoaded) error("Модель не загружена")
            val fullResponse = StringBuilder()
            LlamaBridge.generate(prompt) { token ->
                fullResponse.append(token)
                onToken(token)
            }
            fullResponse.toString()
        }
    }

    fun close() {
        LlamaBridge.unloadModel()
        isLoaded = false
    }

    fun isModelLoaded(): Boolean = isLoaded
}
