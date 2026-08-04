package com.example.gemmaapp

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

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
                val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                
                val msgArray = obj.optJSONArray("messages") ?: JSONArray()
                val messages = mutableListOf<ChatMessage>()
                for (j in 0 until msgArray.length()) {
                    val mObj = msgArray.getJSONObject(j)
                    messages.add(
                        ChatMessage(
                            id = mObj.optString("id", UUID.randomUUID().toString()),
                            text = mObj.optString("text", ""),
                            isUser = mObj.optBoolean("isUser", false),
                            timestamp = mObj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                list.add(ChatSession(id = id, title = title, messages = messages, createdAt = createdAt, timestamp = timestamp))
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
            obj.put("createdAt", session.createdAt)
            obj.put("timestamp", session.timestamp)
            
            val msgArray = JSONArray()
            for (msg in session.messages) {
                val mObj = JSONObject()
                mObj.put("id", msg.id)
                mObj.put("text", msg.text)
                mObj.put("isUser", msg.isUser)
                mObj.put("timestamp", msg.timestamp)
                msgArray.put(mObj)
            }
            obj.put("messages", msgArray)
            jsonArray.put(obj)
        }
        prefs.edit().putString("sessions_key", jsonArray.toString()).apply()
    }
}
