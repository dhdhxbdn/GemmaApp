package com.example.gemmaapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatListActivity : AppCompatActivity() {
    private lateinit var recyclerViewChats: RecyclerView
    private lateinit var chatListAdapter: ChatListAdapter
    private val chats = listOf(
        Chat("1", "Новый чат", "Начни разговор!"),
        Chat("2", "Тестовый чат", "Я готова к работе...")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        recyclerViewChats = findViewById(R.id.recyclerViewChats)
        chatListAdapter = ChatListAdapter(chats) { chat ->
            // Навигация кMainActivity
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("CHAT_ID", chat.id)
            }
            startActivity(intent)
        }
        recyclerViewChats.adapter = chatListAdapter
        recyclerViewChats.layoutManager = LinearLayoutManager(this)
    }
}
