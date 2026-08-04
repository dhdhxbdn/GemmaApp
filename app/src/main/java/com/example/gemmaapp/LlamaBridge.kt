package com.example.gemmaapp

object LlamaBridge {
    init {
        System.loadLibrary("llama_bridge")
    }

    @JvmStatic
    external fun loadModel(modelPath: String, gpuLayers: Int): Boolean

    @JvmStatic
    external fun unloadModel()

    @JvmStatic
    external fun generate(prompt: String, onToken: (String) -> Unit)
}
