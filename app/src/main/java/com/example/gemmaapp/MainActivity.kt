package com.example.gemmaapp

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

    private lateinit var tvStatus: TextView
    private lateinit var btnSelectModel: Button
    private lateinit var btnChatList: Button
    private lateinit var btnNewChat: Button
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
    ) { uri: Uri? ->
        uri?.let { loadModelFromUri(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnSelectModel = findViewById(R.id.btnSelectModel)
        btnChatList = findViewById(R.id.btnChatList)
        btnNewChat = findViewById(R.id.btnNewChat)
        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        messageAdapter = MessageAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = messageAdapter

        loadChatsAndSelectLatest()

        btnSelectModel.setOnClickListener {
            selectModelLauncher.launch("*/*")
        }

        btnChatList.setOnClickListener {
            showChatSelectionDialog()
        }

        btnNewChat.setOnClickListener {
            createNewChat()
        }

        btnSend.setOnClickListener {
            val userText = etMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                sendMessage(userText)
            }
        }

        checkSavedModel()
    }

    private fun loadChatsAndSelectLatest() {
        allChats = ChatStorageManager.loadChats(this)
        if (allChats.isEmpty()) {
            createNewChat()
        } else {
            switchChat(allChats.first())
        }
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
        if (messages.isNotEmpty()) {
            rvMessages.scrollToPosition(messages.size - 1)
        }
    }

    private fun showChatSelectionDialog() {
        if (allChats.isEmpty()) return

        val titles = allChats.map { it.title }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Выберите чат")
            .setItems(titles) { _, which ->
                switchChat(allChats[which])
            }
            .setPositiveButton("Удалить текущий") { _, _ ->
                currentChat?.let { chat ->
                    allChats.remove(chat)
                    ChatStorageManager.saveChats(this, allChats)
                    loadChatsAndSelectLatest()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun checkSavedModel() {
        val modelFile = File(filesDir, "selected_model.bin")
        if (modelFile.exists() && modelFile.length() > 0) {
            tvStatus.text = "Статус: Загрузка ранее выбранной модели..."
            initLlmEngine(modelFile)
        } else {
            tvStatus.text = "Статус: Нажмите 'Модель' и выберите файл Gemma"
        }
    }

    private fun loadModelFromUri(uri: Uri) {
        tvStatus.text = "Статус: Копирование модели..."
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val destFile = File(filesDir, "selected_model.bin")
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    tvStatus.text = "Статус: Инициализация Gemma..."
                }

                initLlmEngine(destFile)
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvStatus.text = "Ошибка копирования: ${e.localizedMessage}"
                }
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
                    .setRandomSeed(101)
                    .build()

                llmInference?.close()
                llmInference = LlmInference.createFromOptions(this@MainActivity, options)

                withContext(Dispatchers.Main) {
                    tvStatus.text = "Статус: Модель готова!"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvStatus.text = "Ошибка: ${e.localizedMessage}"
                    Toast.makeText(
                        this@MainActivity,
                        "MediaPipe поддерживает .bin/.task форматы",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun sendMessage(userText: String) {
        val chat = currentChat ?: return

        val userMsg = ChatMessage(text = userText, isUser = true)
        messages.add(userMsg)
        chat.messages.add(userMsg)
        if (chat.messages.size == 1) {
            chat.title = if (userText.length > 20) userText.take(20) + "..." else userText
        }
        messageAdapter.notifyItemInserted(messages.size - 1)
        rvMessages.scrollToPosition(messages.size - 1)
        etMessage.setText("")
        ChatStorageManager.saveChats(this, allChats)

        val botMsgIndex = messages.size
        val botMsg = ChatMessage(text = "Думаю...", isUser = false)
        messages.add(botMsg)
        messageAdapter.notifyItemInserted(botMsgIndex)
        rvMessages.scrollToPosition(botMsgIndex)

        lifecycleScope.launch(Dispatchers.IO) {
            val response = try {
                if (llmInference != null) {
                    llmInference?.generateResponse(userText) ?: "Ошибка генерации"
                } else {
                    "Модель не загружена! Нажмите 'Модель' вверху экрана."
                }
            } catch (e: Exception) {
                "Ошибка генерации: ${e.localizedMessage}"
            }

            withContext(Dispatchers.Main) {
                val finalBotMsg = ChatMessage(text = response, isUser = false)
                messages[botMsgIndex] = finalBotMsg
                if (botMsgIndex < chat.messages.size) {
                    chat.messages[botMsgIndex] = finalBotMsg
                } else {
                    chat.messages.add(finalBotMsg)
                }
                messageAdapter.notifyItemChanged(botMsgIndex)
                ChatStorageManager.saveChats(this@MainActivity, allChats)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        llmInference?.close()
    }
}
