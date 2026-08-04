package com.example.gemmaapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LlmManager(private val context: Context) {
    private var isLoaded = false
    private var currentModelPath: String? = null

    suspend fun initModel(modelPath: String, gpuLayers: Int = 16): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            LlamaBridge.unloadModel()
            // Пробуем загрузить 16 слоев на GPU
            val success = LlamaBridge.loadModel(modelPath, gpuLayers)
            if (success) {
                isLoaded = true
                currentModelPath = modelPath
            } else {
                // Если GPU дал сбой, прогружаем целиком на CPU (0 слоев на GPU)
                val cpuSuccess = LlamaBridge.loadModel(modelPath, 0)
                if (cpuSuccess) {
                    isLoaded = true
                    currentModelPath = modelPath
                } else {
                    error("Не удалось инициализировать модель GGUF")
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
