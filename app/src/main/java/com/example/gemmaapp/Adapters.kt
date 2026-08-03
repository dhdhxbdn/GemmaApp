package com.example.gemmaapp

import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ChatMessage(val text: String, val isUser: Boolean, val imageUri: String? = null)
data class ChatSession(val id: String, var title: String, val messages: MutableList<ChatMessage> = mutableListOf())

class ChatAdapter(private val chats: List<ChatSession>, private val onChatClick: (ChatSession) -> Unit, private val onDeleteClick: (ChatSession) -> Unit) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {
    class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_chat_title)
        val btnDelete: ImageView = view.findViewById(R.id.btn_delete_chat)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ChatViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_drawer_chat, parent, false))
    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.tvTitle.text = chat.title
        holder.itemView.setOnClickListener { onChatClick(chat) }
        holder.btnDelete.setOnClickListener { onDeleteClick(chat) }
    }
    override fun getItemCount() = chats.size
}

class MessageAdapter(private val messages: List<ChatMessage>) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutBubble: LinearLayout = view.findViewById(R.id.layout_message_bubble)
        val tvText: TextView = view.findViewById(R.id.tv_message_text)
        val ivAttached: ImageView = view.findViewById(R.id.iv_attached_image)
        val rootLayout: LinearLayout = view as LinearLayout
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = messages[position]
        holder.tvText.text = msg.text
        if (!msg.imageUri.isNullOrEmpty()) {
            holder.ivAttached.visibility = View.VISIBLE
            try { holder.ivAttached.setImageURI(Uri.parse(msg.imageUri)) } catch (e: Exception) { holder.ivAttached.visibility = View.GONE }
        } else holder.ivAttached.visibility = View.GONE

        holder.rootLayout.gravity = if (msg.isUser) Gravity.END else Gravity.START
        holder.layoutBubble.setBackgroundResource(if (msg.isUser) R.drawable.bg_message_user else R.drawable.bg_message_bot)
    }
    override fun getItemCount() = messages.size
}
