package com.example.gemmaapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {
    private lateinit var recyclerViewMessages: RecyclerView
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var etMessage: EditText
    private val messages = mutableListOf<Message>()
    private val bridge = LlamaBridge()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages)
        etMessage = findViewById(R.id.etMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        // Кнопка назад возвращает в список чатов
        btnBack.setOnClickListener { finish() }

        messageAdapter = MessageAdapter(messages)
        recyclerViewMessages.adapter = messageAdapter
        recyclerViewMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }

        // Обработка кнопки "Отправить"
        btnSend.setOnClickListener {
            val msg = etMessage.text.toString().trim()
            if (msg.isNotEmpty()) {
                addMessage(msg, true)
                etMessage.text.clear()
                
                // Имитация ответа
                recyclerViewMessages.postDelayed({
                    addMessage("Интерфейс DeepSeek активирован! Жду веса модели.", false)
                }, 500)
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean) {
        messageAdapter.addMessage(Message(text, isUser))
        recyclerViewMessages.scrollToPosition(messages.size - 1)
    }
}
