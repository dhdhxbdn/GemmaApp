package com.example.gemmaapp

data class Chat(val id: String, val title: String, val lastMessage: String)
data class Message(val text: String, val isUser: Boolean)
