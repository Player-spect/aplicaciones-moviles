package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val btnCrudUsuario = findViewById<Button>(R.id.btnCrudUsuario)
        val btnDatosSensor = findViewById<Button>(R.id.btnDatosSensor)
        val btnDesarrollador = findViewById<Button>(R.id.btnDesarrollador)

        // CRUD
        btnCrudUsuario.setOnClickListener {
            val Intent = Intent(this, UserCRUD_Activity::class.java)
            startActivity(Intent)

        }

        // Sensores
        btnDatosSensor.setOnClickListener {
            val Intent = Intent(this, SensorsActivity::class.java)
            startActivity(Intent)
        }

        // Portafolio
        btnDesarrollador.setOnClickListener {
            val Intent = Intent(this, PortafolioActivity::class.java)
            startActivity(Intent)
        }
    }
}