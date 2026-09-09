package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val tvFechaHora = findViewById<TextView>(R.id.tvFechaHora)
        val btnCrudUsuario = findViewById<Button>(R.id.btnCrudUsuario)
        val btnDatosSensor = findViewById<Button>(R.id.btnDatosSensor)
        val btnDesarrollador = findViewById<Button>(R.id.btnDesarrollador)

        // Mostrar fecha y hora actual en tiempo real
        val formatoActual = SimpleDateFormat("dd-MM-yyyy, HH:mm:ss", Locale.getDefault())
        val fechaHoraStr = formatoActual.format(Date())
        tvFechaHora.text = "Fecha/Hora: $fechaHoraStr"

        // CRUD
        btnCrudUsuario.setOnClickListener {
            val Intent = Intent(this, UserCRUD_Activity::class.java)
            startActivity(Intent)

        }

        // Sensores
        btnDatosSensor.setOnClickListener {
        }

        // Portafolio
        btnDesarrollador.setOnClickListener {
        }
    }
}