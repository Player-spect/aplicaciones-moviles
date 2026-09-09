package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class UserCRUD_Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_crud)

        val btnCrearUsuarioCrud = findViewById<Button>(R.id.btnCrearUsuarioCrud)
        val btnListarUsuarios = findViewById<Button>(R.id.btnListarUsuarios)

        // registrar usuarios
        btnCrearUsuarioCrud.setOnClickListener {
             val intent = Intent(this, RegisterUsersActivity::class.java)
             startActivity(intent)
        }

        // Administracion
        btnListarUsuarios.setOnClickListener {
            val intent = Intent(this, ListUsersActivity::class.java)
            startActivity(intent)
        }
    }
}