package com.example.evaluacion_01

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import kotlin.random.Random

class SensorsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensors)

        val tvTempValor = findViewById<TextView>(R.id.tvTempValor)
        val tvHumedadValor = findViewById<TextView>(R.id.tvHumedadValor)
        val btnActualizarSensores = findViewById<Button>(R.id.btnActualizarSensores)

        btnActualizarSensores.setOnClickListener {
            // Generar valores simulados aleatorios para los sensores
            val nuevaTemp = String.format("%.1f", Random.nextDouble(15.0, 35.0)) + " °C"
            val nuevaHumedad = Random.nextInt(30, 90).toString() + " %"

            tvTempValor.text = nuevaTemp
            tvHumedadValor.text = nuevaHumedad

        }
    }
}