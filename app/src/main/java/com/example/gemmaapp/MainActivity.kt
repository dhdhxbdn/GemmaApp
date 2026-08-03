package com.example.gemmaapp

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val btnBurger = findViewById<TextView>(R.id.btn_burger)
        val btnNewChat = findViewById<TextView>(R.id.btn_new_chat)
        val btnSend = findViewById<ImageView>(R.id.btn_send)
        val etMessage = findViewById<EditText>(R.id.et_message)

        // Открытие меню СПРАВА
        btnBurger.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        btnNewChat.setOnClickListener {
            // Новый чат
        }

        btnSend.setOnClickListener {
            val text = etMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                etMessage.setText("")
            }
        }
    }
}
