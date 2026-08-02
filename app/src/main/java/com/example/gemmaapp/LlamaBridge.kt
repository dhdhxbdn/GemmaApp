package com.example.gemmaapp

class LlamaBridge {
    external fun stringFromJNI(): String

    companion {
        init {
            System.loadLibrary("llama_bridge")
        }
    }
}
