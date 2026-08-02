package com.example.gemmaapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import android.view.Gravity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

class MainActivity : AppCompatActivity() {
    private lateinit var chatContainer: LinearLayout
    private lateinit var etMessage: EditText
    private lateinit var chatScroll: ScrollView
    private val bridge = LlamaBridge()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chatContainer = findViewById(R.id.chatContainer)
        etMessage = findViewById(R.id.etMessage)
        chatScroll = findViewById(R.id.chatScroll)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)

        // Стартовое сообщение от C++
        val sysInfo = try { bridge.stringFromJNI() } catch (e: Exception) { "Ошибка JNI" }
        addMessage(sysInfo, false)

        // Обработка кнопки "Отправить"
        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                addMessage(msg, true) // Сообщение пользователя
                etMessage.text.clear()
                
                // Имитация ответа нейросети
                chatContainer.postDelayed({
                    addMessage("Я готова к работе! Жду загрузки файла .gguf весов.", false)
                }, 500)
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(32, 24, 32, 24)
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = if (isUser) Gravity.END else Gravity.START
            setMargins(16, 16, 16, 16)
        }
        tv.layoutParams = params

        // Рисуем красивые пузыри сообщений
        val bg = GradientDrawable().apply {
            cornerRadius = 32f
            if (isUser) {
                setColor(Color.parseColor("#1B5E20")) // Темно-зеленый для юзера
            } else {
                setColor(Color.parseColor("#000000")) // Черный с рамкой для AI
                setStroke(3, Color.parseColor("#39FF14")) // Неоновая рамка
            }
        }
        tv.background = bg

        chatContainer.addView(tv)
        // Прокрутка вниз
        chatScroll.post { chatScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
