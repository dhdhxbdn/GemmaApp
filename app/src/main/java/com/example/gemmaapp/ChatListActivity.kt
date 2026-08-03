package com.example.gemmaapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChatListActivity : AppCompatActivity() {

    private lateinit var rvChats: RecyclerView
    private lateinit var btnNewChat: Button
    private lateinit var chatAdapter: ChatAdapter
    private val chatList = mutableListOf<ChatSession>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_list)

        rvChats = findViewById(R.id.rvChats)
        btnNewChat = findViewById(R.id.btnNewChat)

        chatAdapter = ChatAdapter(chatList) { chat ->
            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("CHAT_ID", chat.id)
            }
            startActivity(intent)
        }

        rvChats.layoutManager = LinearLayoutManager(this)
        rvChats.adapter = chatAdapter

        btnNewChat.setOnClickListener {
            val newChat = ChatSession(title = "Chat ${chatList.size + 1}")
            chatList.add(0, newChat)
            chatAdapter.notifyItemInserted(0)
            rvChats.scrollToPosition(0)

            val intent = Intent(this, MainActivity::class.java).apply {
                putExtra("CHAT_ID", newChat.id)
            }
            startActivity(intent)
        }
    }
}
