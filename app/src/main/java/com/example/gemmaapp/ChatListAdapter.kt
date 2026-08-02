package com.example.gemmaapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatListAdapter(private val chats: List<Chat>, private val onItemClick: (Chat) -> Unit) :
    RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chats[position]
        holder.bind(chat, onItemClick)
    }

    override fun getItemCount(): Int = chats.size

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTitle: TextView = itemView.findViewById(R.id.tvChatTitle)
        private val tvLastMessage: TextView = itemView.findViewById(R.id.tvLastMessage)

        fun bind(chat: Chat, onItemClick: (Chat) -> Unit) {
            tvTitle.text = chat.title
            tvLastMessage.text = chat.lastMessage
            itemView.setOnClickListener { onItemClick(chat) }
        }
    }
}
