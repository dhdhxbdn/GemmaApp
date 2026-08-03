package com.example.gemmaapp

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var tvStatus: TextView
    private lateinit var btnNewChat: Button
    private lateinit var btnSelectModel: Button
    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private var allChats = mutableListOf<ChatSession>()
    private var currentChat: ChatSession? = null
    private val messages = mutableListOf<ChatMessage>()
    private lateinit var messageAdapter: MessageAdapter
    private var llmInference: LlmInference? = null

    private val selectModelLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { loadModelFromUri(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        tvStatus = findViewById(R.id.tvStatus)
        btnNewChat = findViewById(R.id.btnNewChat)
        btnSelectModel = findViewById(R.id.btnSelectModel)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        messageAdapter = MessageAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = messageAdapter

        btnNewChat.setOnClickListener {
            createNewChat()
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        btnSelectModel.setOnClickListener {
            selectModelLauncher.launch("*/*")
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        btnSend.setOnClickListener {
            val txt = etMessage.text.toString().trim()
            if (txt.isNotEmpty()) sendMessage(txt)
        }

        loadChatsAndSelectLatest()
        checkSavedModel()
    }

    private fun loadChatsAndSelectLatest() {
        allChats = ChatStorageManager.loadChats(this)
        if (allChats.isEmpty()) createNewChat() else switchChat(allChats.first())
    }

    private fun createNewChat() {
        val newChat = ChatSession(title = "Чат ${allChats.size + 1}")
        allChats.add(0, newChat)
        ChatStorageManager.saveChats(this, allChats)
        switchChat(newChat)
    }

    private fun switchChat(chat: ChatSession) {
        currentChat = chat
        messages.clear()
        messages.addAll(chat.messages)
        messageAdapter.notifyDataSetChanged()
        if (messages.isNotEmpty()) rvMessages.scrollToPosition(messages.size - 1)
    }

    private fun checkSavedModel() {
        val modelFile = File(filesDir, "selected_model.bin")
        if (modelFile.exists() && modelFile.length() > 0) {
            initLlmEngine(modelFile)
        }
    }

    private fun loadModelFromUri(uri: Uri) {
        tvStatus.text = "ЗАГРУЗКА..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dest = File(filesDir, "selected_model.bin")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { input.copyTo(it) }
                }
                initLlmEngine(dest)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { tvStatus.text = "GEMMA AI" }
            }
        }
    }

    private fun initLlmEngine(modelFile: File) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile.absolutePath)
                    .setMaxTokens(1024)
                    .setTopK(40)
                    .setTemperature(0.8f)
                    .build()
                llmInference?.close()
                llmInference = LlmInference.createFromOptions(this@MainActivity, options)
                withContext(Dispatchers.Main) { tvStatus.text = "GEMMA AI (ГОТОВА)" }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvStatus.text = "GEMMA AI"
                    Toast.makeText(this@MainActivity, "Ошибка формата модели", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessage(userText: String) {
        val chat = currentChat ?: return
        val userMsg = ChatMessage(text = userText, isUser = true)
        messages.add(userMsg)
        chat.messages.add(userMsg)
        messageAdapter.notifyItemInserted(messages.size - 1)
        rvMessages.scrollToPosition(messages.size - 1)
        etMessage.setText("")
        ChatStorageManager.saveChats(this, allChats)

        val botIndex = messages.size
        val botMsg = ChatMessage(text = "...", isUser = false)
        messages.add(botMsg)
        messageAdapter.notifyItemInserted(botIndex)
        rvMessages.scrollToPosition(botIndex)

        lifecycleScope.launch(Dispatchers.IO) {
            val response = try {
                llmInference?.generateResponse(userText) ?: "..."
            } catch (e: Exception) { "..." }

            withContext(Dispatchers.Main) {
                val finalMsg = ChatMessage(text = response, isUser = false)
                messages[botIndex] = finalMsg
                if (botIndex < chat.messages.size) chat.messages[botIndex] = finalMsg else chat.messages.add(finalMsg)
                messageAdapter.notifyItemChanged(botIndex)
                ChatStorageManager.saveChats(this@MainActivity, allChats)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llmInference?.close()
    }
}
