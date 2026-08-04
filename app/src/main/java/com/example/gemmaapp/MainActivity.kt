package com.example.gemmaapp

import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
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
    private lateinit var storageManager: ChatStorageManager
    private var activePfd: ParcelFileDescriptor? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        llmManager = LlmManager(this)
        storageManager = ChatStorageManager(this)

        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainScreen() {
        var sessions by remember { mutableStateOf(storageManager.loadSessions()) }
        var currentSession by remember {
            mutableStateOf(
                sessions.firstOrNull() ?: ChatSession(title = "Новый чат")
            )
        }
        var inputText by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var modelName by remember { mutableStateOf("gemma") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()

        val greenAccent = Color(0xFF00FF66)

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
                                modelName = "Ошибка"
                                errorMessage = err.message ?: "Ошибка инициализации"
                                isLoading = false
                            }
                        )
                    } else {
                        modelName = "Ошибка"
                        errorMessage = "Не удалось открыть файл"
                        isLoading = false
                    }
                }
            }
        }

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        ModalDrawerSheet(
                            drawerContainerColor = Color(0xFF0D0D0D),
                            modifier = Modifier.width(310.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp)
                            ) {
                                Text(
                                    text = "Меню & Чаты",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Button(
                                    onClick = {
                                        val newSess = ChatSession(title = "Новый чат")
                                        currentSession = newSess
                                        scope.launch { drawerState.close() }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.5.dp, greenAccent, RoundedCornerShape(12.dp)),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.Transparent,
                                        contentColor = greenAccent
                                    )
                                ) {
                                    Text("+ Новый чат", fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { filePicker.launch("*/*") },
                                    enabled = !isLoading,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = greenAccent,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isLoading) "Загрузка..." else "Загрузить модель .gguf",
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "Модель: $modelName",
                                    color = if (llmManager.isModelLoaded()) greenAccent else Color.Gray,
                                    fontSize = 13.sp
                                )

                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage!!,
                                        color = Color.Red,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = Color(0xFF222222)
                                )

                                Text(
                                    text = "История чатов",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(sessions) { sess ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (sess.id == currentSession.id) Color(0xFF1E2923) else Color.Transparent,
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    currentSession = sess
                                                    scope.launch { drawerState.close() }
                                                }
                                                .padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = sess.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "✕",
                                                color = Color.Gray,
                                                fontSize = 14.sp,
                                                modifier = Modifier
                                                    .clickable {
                                                        storageManager.deleteSession(sess.id)
                                                        sessions = storageManager.loadSessions()
                                                        if (currentSession.id == sess.id) {
                                                            currentSession = sessions.firstOrNull() ?: ChatSession(title = "Новый чат")
                                                        }
                                                    }
                                                    .padding(start = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ) {

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .border(1.5.dp, greenAccent, RoundedCornerShape(24.dp))
                                        .clickable {
                                            scope.launch { drawerState.open() }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = modelName,
                                        color = greenAccent,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(currentSession.messages) { msg ->
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        Surface(
                                            color = if (msg.isUser) Color(0xFF003818) else Color(0xFF121212),
                                            shape = RoundedCornerShape(16.dp),
                                            border = if (!msg.isUser) androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF222222)) else null,
                                            modifier = Modifier.widthIn(max = 300.dp)
                                        ) {
                                            Text(
                                                text = msg.text,
                                                color = Color.White,
                                                fontSize = 15.sp,
                                                modifier = Modifier.padding(12.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    placeholder = {
                                        Text("Сообщение...", color = greenAccent.copy(alpha = 0.5f))
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = greenAccent,
                                        unfocusedBorderColor = greenAccent,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color.Black,
                                        unfocusedContainerColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(28.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .border(1.5.dp, greenAccent, CircleShape)
                                        .background(Color.Black, CircleShape)
                                        .clickable {
                                            if (inputText.isNotBlank() && llmManager.isModelLoaded()) {
                                                val prompt = inputText
                                                inputText = ""

                                                val userMsg = ChatMessage(text = prompt, isUser = true)
                                                var updatedMessages = currentSession.messages + userMsg

                                                var updatedTitle = currentSession.title
                                                if (updatedTitle == "Новый чат") {
                                                    updatedTitle = if (prompt.length > 20) prompt.take(20) + "..." else prompt
                                                }

                                                val aiMsg = ChatMessage(text = "", isUser = false)
                                                val aiMsgIndex = updatedMessages.size
                                                updatedMessages = updatedMessages + aiMsg

                                                currentSession = currentSession.copy(
                                                    title = updatedTitle,
                                                    messages = updatedMessages
                                                )

                                                lifecycleScope.launch {
                                                    var currentResponse = ""
                                                    llmManager.generateResponse(prompt) { token ->
                                                        currentResponse += token
                                                        val listCopy = currentSession.messages.toMutableList()
                                                        if (aiMsgIndex < listCopy.size) {
                                                            listCopy[aiMsgIndex] = ChatMessage(text = currentResponse, isUser = false)
                                                            currentSession = currentSession.copy(messages = listCopy)
                                                            storageManager.saveSession(currentSession)
                                                            sessions = storageManager.loadSessions()
                                                        }
                                                        scope.launch {
                                                            listState.animateScrollToItem(currentSession.messages.size - 1)
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.size(22.dp)) {
                                        val center = Offset(size.width / 2, size.height / 2)
                                        drawCircle(
                                            color = greenAccent,
                                            radius = size.minDimension / 2,
                                            style = Stroke(width = 2.dp.toPx())
                                        )
                                        drawCircle(
                                            color = greenAccent,
                                            radius = size.minDimension / 5
                                        )
                                    }
                                }
                            }
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
