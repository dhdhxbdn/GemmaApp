package com.example.gemmaapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(private val messages: MutableList<Message>) :
    RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false) // Используем встроенный макет для простоты, настроим программно
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvText: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(message: Message) {
            tvText.text = message.text
            tvText.textSize = 15f
            tvText.setTextColor(Color.WHITE)
            tvText.setPadding(32, 24, 32, 24)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = if (message.isUser) Gravity.END else Gravity.START
                setMargins(16, 16, 16, 16)
            }
            tvText.layoutParams = params

            val bg = GradientDrawable().apply {
                cornerRadius = 32f
                if (message.isUser) {
                    setColor(Color.parseColor("#1B5E20")) // Темно-зеленый для юзера
                } else {
                    setColor(Color.parseColor("#000000")) // Черный с рамкой для AI
                    setStroke(3, Color.parseColor("#39FF14")) // Неоновая рамка
                }
            }
            tvText.background = bg
        }
    }
}
