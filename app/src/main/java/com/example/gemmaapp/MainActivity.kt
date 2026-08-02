package com.example.gemmaapp

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {

    private val llamaBridge = LlamaBridge()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val textView = TextView(this).apply {
            text = "GemmaApp Ready"
            textSize = 20f
            setPadding(32, 32, 32, 32)
        }
        
        setContentView(textView)
    }
}
