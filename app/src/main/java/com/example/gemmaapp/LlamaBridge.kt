package com.example.gemmaapp

class LlamaBridge {
    external fun stringFromJNI(): String

    companion object {
        init {
            System.loadLibrary("llama_bridge")
        }
    }
}
