package com.example.gemmaapp

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

data class ChatSession(val id: String, var title: String)

class ChatAdapter(
    private val chats: List<ChatSession>,
    private val onChatClick: (ChatSession) -> Unit
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_chat_title)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_drawer_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.tvTitle.text = chat.title
        holder.itemView.setOnClickListener { onChatClick(chat) }
    }

    override fun getItemCount() = chats.size
}

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val chatList = mutableListOf<ChatSession>()
    private lateinit var chatAdapter: ChatAdapter
    private var currentChatId: String = ""

    private val selectModelLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            val path = it.path ?: "выбранный файл"
            Toast.makeText(this, "Модель выбрана: $path", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val btnNewChat = findViewById<TextView>(R.id.btn_drawer_new_chat)
        val btnAddModel = findViewById<TextView>(R.id.btn_drawer_add_model)
        val btnSend = findViewById<ImageView>(R.id.btn_send)
        val etMessage = findViewById<EditText>(R.id.et_message)
        val rvDrawerChats = findViewById<RecyclerView>(R.id.rv_drawer_chats)

        // Инициализация истории чатов в шторке
        chatAdapter = ChatAdapter(chatList) { chat ->
            currentChatId = chat.id
            drawerLayout.closeDrawer(GravityCompat.END)
            Toast.makeText(this, "Открыт: ${chat.title}", Toast.LENGTH_SHORT).show()
        }
        rvDrawerChats.layoutManager = LinearLayoutManager(this)
        rvDrawerChats.adapter = chatAdapter

        // Создаем первый чат по умолчанию при запуске
        createNewChatSession("Чат 1")

        // Кнопка: Новый чат
        btnNewChat.setOnClickListener {
            val newIndex = chatList.size + 1
            createNewChatSession("Чат $newIndex")
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        // Кнопка: Выбрать модель из загрузок/памяти
        btnAddModel.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
            selectModelLauncher.launch(arrayOf("*/*"))
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                etMessage.setText("")
            }
        }
    }

    private fun createNewChatSession(title: String) {
        val newChat = ChatSession(System.currentTimeMillis().toString(), title)
        chatList.add(0, newChat)
        currentChatId = newChat.id
        chatAdapter.notifyDataSetChanged()
    }
}
