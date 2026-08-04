package com.example.gemmaapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LlmManager(private val context: Context) {
    private var isLoaded = false
    private var currentModelPath: String? = null

    // gpuLayers = 16 (гибридный режим: 16 слоев на GPU, остальное на CPU)
    suspend fun initModel(modelPath: String, gpuLayers: Int = 16): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            LlamaBridge.unloadModel()
            // Загружаем модель с разгрузкой слоев на GPU
            val success = LlamaBridge.loadModel(modelPath, gpuLayers)
            if (success) {
                isLoaded = true
                currentModelPath = modelPath
            } else {
                // Если GPU выбьет ошибку, делаем фоллбэк на чистый CPU (0 слоев)
                val cpuFallback = LlamaBridge.loadModel(modelPath, 0)
                if (cpuFallback) {
                    isLoaded = true
                    currentModelPath = modelPath
                } else {
                    error("Не удалось загрузить GGUF модель ни на GPU, ни на CPU")
                }
            }
        }
    }

    suspend fun generateResponse(
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
