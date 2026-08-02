package com.example.gemmaapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.view.Gravity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Загружаем строку из C++ движка
        val bridge = LlamaBridge()
        val cppMessage = try {
            bridge.stringFromJNI()
        } catch (e: Exception) {
            "Ошибка JNI: ${e.message}"
        }

        // Рисуем экран кодом
        val tv = TextView(this).apply {
            text = cppMessage
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(0xFF00FF00.toInt()) // Ярко-зеленый текст
            setBackgroundColor(0xFF000000.toInt()) // Черный фон
        }
        
        setContentView(tv)
    }
}
