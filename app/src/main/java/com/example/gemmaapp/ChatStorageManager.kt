package com.example.gemmaapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatMessage(
    val text: String,
    val isUser: Boolean
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage> = emptyList()
)

class ChatStorageManager(context: Context) {
    private val prefs = context.getSharedPreferences("gemma_chats", Context.MODE_PRIVATE)

    fun saveSession(session: ChatSession) {
        val sessions = loadSessions().toMutableList()
        val index = sessions.indexOfFirst { it.id == session.id }
        if (index != -1) {
            sessions[index] = session
        } else {
            sessions.add(0, session)
        }
        saveAll(sessions)
    }

    fun loadSessions(): List<ChatSession> {
        val jsonString = prefs.getString("sessions_key", null) ?: return emptyList()
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<ChatSession>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val id = obj.optString("id", UUID.randomUUID().toString())
                val title = obj.optString("title", "Чат")
                val msgArray = obj.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until msgArray.length()) {
                    val mObj = msgArray.getJSONObject(j)
                    messages.add(
                        ChatMessage(
                            text = mObj.optString("text", ""),
                            isUser = mObj.optBoolean("isUser", false)
                        )
                    )
                }
                list.add(ChatSession(id, title, messages))
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deleteSession(id: String) {
        val sessions = loadSessions().filterNot { it.id == id }
        saveAll(sessions)
    }

    private fun saveAll(sessions: List<ChatSession>) {
        val jsonArray = JSONArray()
        for (session in sessions) {
            val obj = JSONObject()
            obj.put("id", session.id)
            obj.put("title", session.title)
            val msgArray = JSONArray()
            for (msg in session.messages) {
                val mObj = JSONObject()
                mObj.put("text", msg.text)
                mObj.put("isUser", msg.isUser)
                msgArray.put(mObj)
            }
            obj.put("messages", msgArray)
            jsonArray.put(obj)
        }
        prefs.edit().putString("sessions_key", jsonArray.toString()).apply()
    }
}
