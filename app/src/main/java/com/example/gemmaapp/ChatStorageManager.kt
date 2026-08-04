package com.example.gemmaapp

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class ChatStorageManager(private val context: Context) {
    private val gson = Gson()
    private val file = File(context.filesDir, "chat_sessions.json")

    fun loadSessions(): List<ChatSession> {
        return try {
            if (!file.exists()) return emptyList()
            val json = file.readText()
            val type = object : TypeToken<List<ChatSession>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSessions(sessions: List<ChatSession>) {
        try {
            val json = gson.toJson(sessions)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSession(session: ChatSession) {
        val sessions = loadSessions().toMutableList()
        val index = sessions.indexOfFirst { it.id == session.id }
        if (index != -1) {
            sessions[index] = session
        } else {
            sessions.add(0, session)
        }
        saveSessions(sessions)
    }

    fun deleteSession(sessionId: String) {
        val sessions = loadSessions().filterNot { it.id == sessionId }
        saveSessions(sessions)
    }
}
