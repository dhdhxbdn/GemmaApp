package com.example.gemmaapp

class LlamaBridge {
    companion object {
        init {
            try {
                System.loadLibrary("gemma")
            } catch (e: UnsatisfiedLinkError) {
                e.printStackTrace()
            }
        }
    }

    external fun initModel(modelPath: String): Long
    external fun generateResponse(contextPtr: Long, prompt: String): String
    external fun freeModel(contextPtr: Long)
}
