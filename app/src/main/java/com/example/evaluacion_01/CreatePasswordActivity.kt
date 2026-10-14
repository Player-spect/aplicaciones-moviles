package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.evaluacion_01.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

class CreatePasswordActivity : AppCompatActivity() {

    private var emailUsuario: String = ""
    private var temporizador: CountDownTimer? = null // Variable para controlar el timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_password)

        // Recuperar el correo enviado desde RecoveryActivity
        emailUsuario = intent.getStringExtra("email_usuario") ?: ""

        val etCodigoVerificacion = findViewById<EditText>(R.id.etCodigoVerificacion)
        val tvTemporizador = findViewById<TextView>(R.id.tvTemporizador)
        val etNuevaClave = findViewById<EditText>(R.id.etNuevaClave)
        val etRepetirClave = findViewById<EditText>(R.id.etRepetirClave)
        val btnCrear = findViewById<Button>(R.id.btnCrear)

        // Iniciar la cuenta regresiva visual de 15 minutos
        iniciarTemporizador(tvTemporizador, btnCrear)

        btnCrear.setOnClickListener {
            val token = etCodigoVerificacion.text.toString().trim()
            val clave = etNuevaClave.text.toString().trim()
            val repetir = etRepetirClave.text.toString().trim()

            // 1. Validar correo no nulo
            if (emailUsuario.isEmpty()) {
                mostrarAlerta("Error", "No se encontró el correo asociado a la recuperación.")
                return@setOnClickListener
            }

            // 2. Validar campos vacíos
            if (token.isEmpty() || clave.isEmpty() || repetir.isEmpty()) {
                mostrarAlerta("Campos vacíos", "Debe completar todos los campos, incluyendo el código.")
                return@setOnClickListener
            }

            // 3. Validar que ambas contraseñas coincidan
            if (clave != repetir) {
                mostrarAlerta("Error de coincidencia", "Las contraseñas ingresadas no coinciden.")
                return@setOnClickListener
            }

            // 4. Validar robustez mediante Regex
            val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_\\-]).{8,}$".toRegex()
            if (!clave.matches(passwordRegex)) {
                mostrarAlerta(
                    "Contraseña Débil",
                    "Debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial."
                )
                return@setOnClickListener
            }

            // --- INICIO DE CONEXIÓN A AWS ---
            val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
                titleText = "Actualizando..."
                setCancelable(false)
                show()
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val requestParams = mapOf(
                        "email" to emailUsuario,
                        "token" to token,
                        "new_password" to clave
                    )

                    val response = RetrofitClient.apiServiceAws.resetearClave(requestParams)

                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()

                        if (response.isSuccessful && response.body() != null) {
                            // Éxito al actualizar
                            temporizador?.cancel() // Detenemos el timer si tuvo éxito

                            SweetAlertDialog(this@CreatePasswordActivity, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("¡Contraseña Creada!")
                                .setContentText(response.body()!!.message)
                                .setConfirmButton("Ir al Login") { sDialog ->
                                    sDialog.dismissWithAnimation()
                                    val intent = Intent(this@CreatePasswordActivity, MainActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .show()
                        } else {
                            var errorMsg = "No se pudo actualizar la contraseña."
                            val errorBody = response.errorBody()?.string()

                            if (errorBody != null) {
                                try {
                                    val jsonObject = JSONObject(errorBody)
                                    errorMsg = jsonObject.getString("message")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            mostrarAlerta("Error de Verificación", errorMsg)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()
                        Log.e("CreatePasswordActivity", "Error de red", e)
                        mostrarAlerta("Sin Conexión", "No se pudo conectar con el servidor AWS.")
                    }
                }
            }
        }
    }

    // Función para manejar la cuenta regresiva visual
    private fun iniciarTemporizador(tvTemporizador: TextView, btnCrear: Button) {
        val tiempoMilisegundos: Long = 15 * 60 * 1000

        temporizador = object : CountDownTimer(tiempoMilisegundos, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Formatear el tiempo en Minutos:Segundos
                val minutos = (millisUntilFinished / 1000) / 60
                val segundos = (millisUntilFinished / 1000) % 60
                val tiempoFormateado = String.format(Locale.getDefault(), "%02d:%02d", minutos, segundos)

                tvTemporizador.text = "El código expira en: $tiempoFormateado"
            }

            override fun onFinish() {
                tvTemporizador.text = "El código ha expirado"
                tvTemporizador.setTextColor(android.graphics.Color.RED)
                btnCrear.isEnabled = false
                btnCrear.alpha = 0.5f // Hacer que el botón se vea gris/transparente
            }
        }.start()
    }

    // Evitar fugas de memoria destruyendo el temporizador si el usuario sale de la pantalla
    override fun onDestroy() {
        super.onDestroy()
        temporizador?.cancel()
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .show()
    }
}