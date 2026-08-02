package com.example.gemmaapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.view.Gravity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val bridge = LlamaBridge()
        val cppMessage = try {
            bridge.stringFromJNI()
        } catch (e: Exception) {
            "Ошибка JNI: ${e.message}"
        }

        val tv = TextView(this).apply {
            text = cppMessage
            textSize = 14f // Уменьшили шрифт
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32) // Добавили отступы
            setTextColor(0xFF00FF00.toInt())
            setBackgroundColor(0xFF000000.toInt())
        }
        
        setContentView(tv)
    }
}
