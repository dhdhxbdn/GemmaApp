package com.example.gemmaapp

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "Новый чат",
    val messages: List<ChatMessage> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)
