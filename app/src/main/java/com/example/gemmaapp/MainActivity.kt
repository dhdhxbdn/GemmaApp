package com.example.gemmaapp

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button

    private val messages = mutableListOf<ChatMessage>()
    private lateinit var messageAdapter: MessageAdapter
    private var llmInference: LlmInference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)

        messageAdapter = MessageAdapter(messages)
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = messageAdapter

        initLlmEngine()

        btnSend.setOnClickListener {
            val userText = etMessage.text.toString().trim()
            if (userText.isNotEmpty()) {
                sendMessage(userText)
            }
        }
    }

    private fun initLlmEngine() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val modelFile = File(filesDir, "gemma.bin")
                if (modelFile.exists()) {
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxTokens(1024)
                        .setTopK(40)
                        .setTemperature(0.8f)
                        .setRandomSeed(101)
                        .build()
                    llmInference = LlmInference.createFromOptions(this@MainActivity, options)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendMessage(userText: String) {
        val userMsg = ChatMessage(text = userText, isUser = true)
        messages.add(userMsg)
        messageAdapter.notifyItemInserted(messages.size - 1)
        rvMessages.scrollToPosition(messages.size - 1)
        etMessage.setText("")

        val botMsgIndex = messages.size
        val botMsg = ChatMessage(text = "Thinking...", isUser = false)
        messages.add(botMsg)
        messageAdapter.notifyItemInserted(botMsgIndex)
        rvMessages.scrollToPosition(botMsgIndex)

        lifecycleScope.launch(Dispatchers.IO) {
            val response = try {
                llmInference?.generateResponse(userText) ?: "Model not loaded. Please download gemma.bin."
            } catch (e: Exception) {
                "Error generating response: ${e.localizedMessage}"
            }

            withContext(Dispatchers.Main) {
                messages[botMsgIndex] = ChatMessage(text = response, isUser = false)
                messageAdapter.notifyItemChanged(botMsgIndex)
            }
        }
    }
}
