package com.example.gemmaapp

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    private lateinit var llmManager: LlmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        llmManager = LlmManager(this)

        setContent {
            var messages by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
            var inputText by remember { mutableStateOf("") }
            var isLoading by remember { mutableStateOf(false) }
            var statusText by remember { mutableStateOf("ВЫБРАТЬ МОДЕЛЬ GGUF") }

            val filePicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent()
            ) { uri: Uri? ->
                uri?.let {
                    statusText = "ЗАГРУЗКА..."
                    isLoading = true
                    lifecycleScope.launch {
                        val file = withContext(Dispatchers.IO) { copyUriToCache(it) }
                        if (file != null) {
                            llmManager.initModel(file.absolutePath).fold(
                                onSuccess = {
                                    statusText = file.name
                                    isLoading = false
                                },
                                onFailure = { err ->
                                    statusText = "ОШИБКА ЗАГРУЗКИ"
                                    isLoading = false
                                    Toast.makeText(this@MainActivity, err.message, Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            statusText = "ОШИБКА ФАЙЛА"
                            isLoading = false
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { filePicker.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003300)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isLoading) "ЗАГРУЗКА..." else statusText,
                            color = Color(0xFF00FF66),
                            fontSize = 16.sp
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        reverseLayout = false
                    ) {
                        items(messages) { msg ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                contentAlignment = if (msg.second) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Surface(
                                    color = if (msg.second) Color(0xFF004D1A) else Color(0xFF1A1A1A),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = msg.first,
                                        color = Color.White,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Сообщение...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF00FF66),
                                unfocusedBorderColor = Color(0xFF003300),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && llmManager.isModelLoaded()) {
                                    val prompt = inputText
                                    messages = messages + Pair(prompt, true)
                                    inputText = ""
                                    
                                    var responseText = ""
                                    messages = messages + Pair("", false)
                                    val responseIndex = messages.size - 1

                                    lifecycleScope.launch {
                                        llmManager.generateResponse(prompt) { token ->
                                            responseText += token
                                            val updated = messages.toMutableList()
                                            updated[responseIndex] = Pair(responseText, false)
                                            messages = updated
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Text("➔", color = Color(0xFF00FF66), fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }

    private fun copyUriToCache(uri: Uri): File? {
        return try {
            val fileName = getFileName(uri) ?: "model.gguf"
            val cacheFile = File(cacheDir, fileName)
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            cacheFile
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) result = cursor.getString(index)
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        llmManager.close()
    }
}
