package com.example.gemmaapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object ChatStorageManager {
    private const val FILE_NAME = "chats_data.json"

    fun saveChats(context: Context, chats: List<ChatSession>) {
        try {
            val rootArray = JSONArray()
            for (chat in chats) {
                val chatObj = JSONObject()
                chatObj.put("id", chat.id)
                chatObj.put("title", chat.title)
                chatObj.put("createdAt", chat.createdAt)

                val msgArray = JSONArray()
                for (msg in chat.messages) {
                    val msgObj = JSONObject()
                    msgObj.put("id", msg.id)
                    msgObj.put("text", msg.text)
                    msgObj.put("isUser", msg.isUser)
                    msgObj.put("timestamp", msg.timestamp)
                    msgArray.put(msgObj)
                }
                chatObj.put("messages", msgArray)
                rootArray.put(chatObj)
            }
            File(context.filesDir, FILE_NAME).writeText(rootArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadChats(context: Context): MutableList<ChatSession> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return mutableListOf()

        val list = mutableListOf<ChatSession>()
        try {
            val rootArray = JSONArray(file.readText())
            for (i in 0 until rootArray.length()) {
                val chatObj = rootArray.getJSONObject(i)
                val id = chatObj.getString("id")
                val title = chatObj.getString("title")
                val createdAt = chatObj.optLong("createdAt", System.currentTimeMillis())

                val messages = mutableListOf<ChatMessage>()
                val msgArray = chatObj.getJSONArray("messages")
                for (j in 0 until msgArray.length()) {
                    val msgObj = msgArray.getJSONObject(j)
                    messages.add(
                        ChatMessage(
                            id = msgObj.optString("id", UUID.randomUUID().toString()),
                            text = msgObj.getString("text"),
                            isUser = msgObj.getBoolean("isUser"),
                            timestamp = msgObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                list.add(ChatSession(id, title, messages, createdAt))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
