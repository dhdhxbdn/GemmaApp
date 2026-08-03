package com.example.gemmaapp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private val chatList = mutableListOf<ChatSession>()
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var messageAdapter: MessageAdapter
    private var currentChat: ChatSession? = null
    private lateinit var tvCurrentModel: TextView
    private lateinit var btnAttachImage: ImageView
    private var selectedImageUri: Uri? = null
    
    private lateinit var llmManager: LlmManager

    private val selectModelLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { handleModelFileSelected(it) } }
    private val selectPhotoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedImageUri = it
            Toast.makeText(this, "Фото прикреплено!", Toast.LENGTH_SHORT).show()
            btnAttachImage.setColorFilter(0xFF00FF66.toInt())
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        llmManager = LlmManager(this)
        drawerLayout = findViewById(R.id.drawer_layout)
        tvCurrentModel = findViewById(R.id.tv_current_model)
        btnAttachImage = findViewById(R.id.btn_attach_image)

        chatAdapter = ChatAdapter(chatList, { selectChat(it); drawerLayout.closeDrawer(GravityCompat.END) }, { deleteChat(it) })
        val rvDrawerChats = findViewById<RecyclerView>(R.id.rv_drawer_chats)
        rvDrawerChats.layoutManager = LinearLayoutManager(this)
        rvDrawerChats.adapter = chatAdapter

        loadChatsFromStorage()
        if (chatList.isEmpty()) createNewChatSession("Чат 1") else selectChat(chatList[0])

        findViewById<TextView>(R.id.btn_drawer_new_chat).setOnClickListener { createNewChatSession("Чат ${chatList.size + 1}"); drawerLayout.closeDrawer(GravityCompat.END) }
        findViewById<TextView>(R.id.btn_drawer_add_model).setOnClickListener { drawerLayout.closeDrawer(GravityCompat.END); selectModelLauncher.launch(arrayOf("*/*")) }
        btnAttachImage.setOnClickListener { selectPhotoLauncher.launch("image/*") }

        findViewById<RecyclerView>(R.id.rv_messages).layoutManager = LinearLayoutManager(this)
        findViewById<ImageView>(R.id.btn_send).setOnClickListener {
            val etMessage = findViewById<EditText>(R.id.et_message)
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty() || selectedImageUri != null) { sendMessage(text); etMessage.setText("") }
        }
    }

    private fun handleModelFileSelected(uri: Uri) {
        var fileName = "model.bin"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) fileName = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
        }
        if (!fileName.lowercase().contains("gemma")) {
            Toast.makeText(this, "Поддерживаются только модели Gemma.", Toast.LENGTH_LONG).show()
            return
        }
        tvCurrentModel.text = "Загрузка: $fileName..."
        
        lifecycleScope.launch {
            Toast.makeText(this@MainActivity, "Копирование модели во внутреннюю память...", Toast.LENGTH_LONG).show()
            if (llmManager.loadModel(uri)) {
                tvCurrentModel.text = "Модель: $fileName"
                btnAttachImage.visibility = if (fileName.lowercase().let { it.contains("3") || it.contains("4b") || it.contains("vision") || it.contains("pali") }) View.VISIBLE else View.GONE
                Toast.makeText(this@MainActivity, "Модель готова!", Toast.LENGTH_SHORT).show()
            } else {
                tvCurrentModel.text = "Ошибка загрузки"
            }
        }
    }

    private fun createNewChatSession(title: String) {
        val newChat = ChatSession(id = System.currentTimeMillis().toString(), title = title)
        chatList.add(0, newChat)
        chatAdapter.notifyItemInserted(0)
        selectChat(newChat)
        saveChatsToStorage()
    }

    private fun selectChat(chat: ChatSession) {
        currentChat = chat
        messageAdapter = MessageAdapter(chat.messages)
        val rv = findViewById<RecyclerView>(R.id.rv_messages)
        rv.adapter = messageAdapter
        if (chat.messages.isNotEmpty()) rv.scrollToPosition(chat.messages.size - 1)
    }

    private fun deleteChat(chat: ChatSession) {
        if (chatList.remove(chat)) {
            chatAdapter.notifyDataSetChanged()
            if (currentChat == chat) { if (chatList.isNotEmpty()) selectChat(chatList[0]) else createNewChatSession("Чат 1") }
            saveChatsToStorage()
        }
    }

    private fun sendMessage(text: String) {
        val chat = currentChat ?: return
        val rv = findViewById<RecyclerView>(R.id.rv_messages)
        
        chat.messages.add(ChatMessage(text, true, selectedImageUri?.toString()))
        messageAdapter.notifyItemInserted(chat.messages.size - 1)
        rv.scrollToPosition(chat.messages.size - 1)
        
        selectedImageUri = null
        btnAttachImage.clearColorFilter()

        if (chat.title.startsWith("Чат ") && chat.messages.size == 1) {
            chat.title = if (text.length > 20) text.substring(0, 20) + "..." else text
            chatAdapter.notifyDataSetChanged()
        }
        
        if (!llmManager.isModelLoaded) {
            chat.messages.add(ChatMessage("Выберите модель через меню.", false))
            messageAdapter.notifyItemInserted(chat.messages.size - 1)
            rv.scrollToPosition(chat.messages.size - 1)
            return
        }

        val botIndex = chat.messages.size
        chat.messages.add(ChatMessage("Думает...", false))
        messageAdapter.notifyItemInserted(botIndex)
        rv.scrollToPosition(botIndex)

        lifecycleScope.launch {
            val response = llmManager.generateResponse(text.ifEmpty { "Опиши фото" })
            chat.messages[botIndex] = ChatMessage(response, false)
            messageAdapter.notifyItemChanged(botIndex)
            rv.scrollToPosition(botIndex)
            saveChatsToStorage()
        }
    }

    private fun saveChatsToStorage() {
        try {
            val arr = JSONArray()
            chatList.forEach { c ->
                val obj = JSONObject().put("id", c.id).put("title", c.title)
                val msgs = JSONArray()
                c.messages.forEach { m -> msgs.put(JSONObject().put("text", m.text).put("isUser", m.isUser).put("imageUri", m.imageUri ?: "")) }
                arr.put(obj.put("messages", msgs))
            }
            getSharedPreferences("gemma_prefs", Context.MODE_PRIVATE).edit().putString("chats_data", arr.toString()).apply()
        } catch (e: Exception) {}
    }

    private fun loadChatsFromStorage() {
        try {
            val str = getSharedPreferences("gemma_prefs", Context.MODE_PRIVATE).getString("chats_data", null) ?: return
            chatList.clear()
            val arr = JSONArray(str)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val chat = ChatSession(obj.getString("id"), obj.getString("title"))
                val msgs = obj.getJSONArray("messages")
                for (j in 0 until msgs.length()) {
                    val m = msgs.getJSONObject(j)
                    chat.messages.add(ChatMessage(m.getString("text"), m.getBoolean("isUser"), m.getString("imageUri").takeIf { it.isNotEmpty() }))
                }
                chatList.add(chat)
            }
            chatAdapter.notifyDataSetChanged()
        } catch (e: Exception) {}
    }
}
