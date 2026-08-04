package com.example.gemmaapp

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    private var activePfd: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        llmManager = LlmManager(this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF090D16),
                    surface = Color(0xFF111827),
                    primary = Color(0xFF10B981)
                )
            ) {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen() {
        var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
        var inputText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var modelName by remember { mutableStateOf("Модель не выбрана") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        val filePicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { selectedUri ->
                isLoading = true
                errorMessage = null
                modelName = "Загрузка..."

                lifecycleScope.launch {
                    val filePath = withContext(Dispatchers.IO) { getFilePathFromUri(selectedUri) }
                    if (filePath != null) {
                        llmManager.initModel(filePath).fold(
                            onSuccess = {
                                modelName = getFileNameFromUri(selectedUri) ?: "GGUF Model"
                                isLoading = false
                            },
                            onFailure = { err ->
                                modelName = "Ошибка загрузки"
                                errorMessage = err.message ?: "Не удалось инициализировать модель"
                                isLoading = false
                            }
                        )
                    } else {
                        modelName = "Ошибка файла"
                        errorMessage = "Не удалось открыть доступ к файлу"
                        isLoading = false
                    }
                }
            }
        }

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF111827),
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Параметры и Инфо",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        HorizontalDivider(color = Color(0xFF1F2937))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Текущая модель:",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                        Text(
                            text = modelName,
                            color = Color(0xFF10B981),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Ошибка:",
                                color = Color(0xFFEF4444),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = errorMessage!!,
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { filePicker.launch("*/*") },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (isLoading) "Загрузка..." else "Выбрать файл .gguf",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Gemma AI", color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Text("☰", color = Color(0xFF10B981), fontSize = 24.sp)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color(0xFF090D16)
                        )
                    )
                },
                containerColor = Color(0xFF090D16)
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                            ) {
                                Surface(
                                    color = if (msg.isUser) Color(0xFF065F46) else Color(0xFF1F2937),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (msg.isUser) 16.dp else 4.dp,
                                        bottomEnd = if (msg.isUser) 4.dp else 16.dp
                                    ),
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (msg.isUser) "Вы" else "Gemma",
                                            color = if (msg.isUser) Color(0xFFA7F3D0) else Color(0xFF34D399),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = msg.text,
                                            color = Color.White,
                                            fontSize = 15.sp
                                        )
                                    }
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
                            placeholder = { Text("Сообщение...", color = Color(0xFF6B7280)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF374151),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF111827),
                                unfocusedContainerColor = Color(0xFF111827)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && llmManager.isModelLoaded()) {
                                    val prompt = inputText
                                    messages = messages + ChatMessage(text = prompt, isUser = true)
                                    inputText = ""

                                    val responseIndex = messages.size
                                    messages = messages + ChatMessage(text = "", isUser = false)

                                    lifecycleScope.launch {
                                        var currentText = ""
                                        llmManager.generateResponse(prompt) { token ->
                                            currentText += token
                                            val updated = messages.toMutableList()
                                            if (responseIndex < updated.size) {
                                                updated[responseIndex] = ChatMessage(text = currentText, isUser = false)
                                                messages = updated
                                            }
                                            scope.launch {
                                                listState.animateScrollToItem(messages.size - 1)
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color(0xFF10B981), CircleShape)
                        ) {
                            Text("➔", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    private fun getFilePathFromUri(uri: Uri): String? {
        return try {
            activePfd?.close()
            val pfd = contentResolver.openFileDescriptor(uri, "r")
            activePfd = pfd
            if (pfd != null) {
                val fdPath = "/proc/self/fd/${pfd.fd}"
                val fileTest = File(fdPath)
                if (fileTest.exists() && fileTest.canRead()) {
                    return fdPath
                }
            }
            val fileName = getFileNameFromUri(uri) ?: "model.gguf"
            val cacheFile = File(cacheDir, fileName)
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(cacheFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            cacheFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
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
        activePfd?.close()
        llmManager.close()
    }
}
