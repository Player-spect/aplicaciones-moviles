package com.example.evaluacion_01

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import cn.pedant.SweetAlert.SweetAlertDialog
import kotlin.random.Random

// Importaciones comentadas para cuando actives Retrofit
// import androidx.lifecycle.lifecycleScope
// import kotlinx.coroutines.Dispatchers
// import kotlinx.coroutines.launch
// import kotlinx.coroutines.withContext

class SensorsActivity : AppCompatActivity() {

    // Variables para la linterna física
    private var isLinternaOn = false
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensors)

        val tvTempValor = findViewById<TextView>(R.id.tvTempValor)
        val tvHumedadValor = findViewById<TextView>(R.id.tvHumedadValor)
        val btnActualizarSensores = findViewById<Button>(R.id.btnActualizarSensores)

        // --- INICIALIZACIÓN DE ACTUADOR (LINTERNA) ---
        val cvLinterna = findViewById<CardView>(R.id.cvLinterna)
        val ivLinterna = findViewById<ImageView>(R.id.ivLinterna)
        val tvLinternaEstado = findViewById<TextView>(R.id.tvLinternaEstado)

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList[0]
        } catch (e: CameraAccessException) {
            Log.e("SensorsActivity", "Error al acceder a la cámara", e)
        } catch (e: ArrayIndexOutOfBoundsException) {
            Log.e("SensorsActivity", "El dispositivo no tiene cámaras", e)
        }

        cvLinterna.setOnClickListener {
            if (cameraId != null) {
                try {
                    isLinternaOn = !isLinternaOn
                    cameraManager.setTorchMode(cameraId!!, isLinternaOn)

                    if (isLinternaOn) {
                        tvLinternaEstado.text = "Linterna: ON"
                        tvLinternaEstado.setTextColor(android.graphics.Color.parseColor("#FFC107"))
                        // ivLinterna.setImageResource(R.drawable.tu_icono_encendido)
                    } else {
                        tvLinternaEstado.text = "Linterna: OFF"
                        tvLinternaEstado.setTextColor(getColor(R.color.bg_gris_80))
                        // ivLinterna.setImageResource(R.drawable.tu_icono_apagado)
                    }
                } catch (e: CameraAccessException) {
                    mostrarAlerta("Error", "No se pudo usar el flash del dispositivo.")
                }
            } else {
                mostrarAlerta("Error", "No se detectó flash en el dispositivo.")
            }
        }

        btnActualizarSensores.setOnClickListener {
            // --- CÓDIGO TEMPORAL: Valores simulados ---
            val nuevaTemp = String.format("%.1f", Random.nextDouble(15.0, 35.0)) + " °C"
            val nuevaHumedad = Random.nextInt(30, 90).toString() + " %"

            tvTempValor.text = nuevaTemp
            tvHumedadValor.text = nuevaHumedad

            /*
            // --- TODO: DESCOMENTAR CUANDO TENGA LAS RUTAS ---
            val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
                titleText = "Consultando pnk.cl..."
                setCancelable(false)
                show()
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = RetrofitClient.apiServiceSensores.obtenerDatosSensores()

                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()

                        if (response.isSuccessful && response.body() != null) {
                            val datosSensor = response.body()!!
                            tvTempValor.text = "${datosSensor.temperatura} °C"
                            tvHumedadValor.text = "${datosSensor.humedad} %"
                        } else {
                            mostrarAlerta("Error de lectura", "No se pudieron interpretar los datos.")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()
                        Log.e("SensorsActivity", "Error de conexión", e)
                        mostrarAlerta("Sin Conexión", "No se pudo conectar con pnk.cl.")
                    }
                }
            }
            */
        }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .show()
    }
}