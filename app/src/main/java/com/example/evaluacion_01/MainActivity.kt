package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
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

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlazar los elementos de la vista
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val tvRegister = findViewById<TextView>(R.id.tvRegistrarme)
        val tvOlvidasteClave = findViewById<TextView>(R.id.tvOlvidasteClave)

        // Configurar INGRESAR
        btnIngresar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validacion Campos vacíos
            if (email.isEmpty() || password.isEmpty()) {
                showErrorAlert("¡Error!", "Todos los campos son obligatorios")
                return@setOnClickListener
            }

            // Validacion Formato de Email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showErrorAlert("Formato Inválido", "Por favor ingresa un correo válido")
                return@setOnClickListener
            }

            // --- INICIO DE CONEXIÓN A AWS ---

            // Mostrar diálogo de carga
            val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
                titleText = "Conectando..."
                setCancelable(false)
                show()
            }

            // Ejecutar la petición en un hilo secundario (IO)
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val requestParams = mapOf(
                        "email" to email,
                        "password" to password
                    )

                    // Llamada a Retrofit usando el cliente que creamos
                    val response = RetrofitClient.apiServiceAws.login(requestParams)

                    // Volver al hilo principal para actualizar la pantalla
                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()

                        if (response.isSuccessful && response.body() != null) {
                            val loginResponse = response.body()!!

                            // Éxito: Mostrar mensaje y navegar al menú principal
                            SweetAlertDialog(this@MainActivity, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("¡Bienvenido!")
                                .setContentText(loginResponse.message)
                                .setConfirmClickListener { sDialog ->
                                    sDialog.dismissWithAnimation()
                                    val intent = Intent(this@MainActivity, MainMenuActivity::class.java)
                                    startActivity(intent)
                                    finish() // Cierra el login para que no pueda volver con el botón "Atrás"
                                }
                                .show()

                        } else {
                            // Extraer el mensaje de error exacto desde PHP (Ej: "La cuenta está inactiva")
                            val errorBody = response.errorBody()?.string()
                            var errorMsg = "Credenciales incorrectas"

                            if (errorBody != null) {
                                try {
                                    val jsonObject = JSONObject(errorBody)
                                    errorMsg = jsonObject.getString("message")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            // Usar tu función personalizada de error
                            showErrorAlert("Datos incorrectos", errorMsg)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()
                        Log.e("MainActivity", "Error de red", e)
                        showErrorAlert("Sin Conexión", "No se pudo conectar con el servidor AWS.")
                    }
                }
            }
        }

        // Registrar usuarios
        tvRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        // Password Recovery
        tvOlvidasteClave.setOnClickListener {
            val intent = Intent(this, RecoveryActivity::class.java)
            startActivity(intent)
        }
    }

    // Sweet_Alert
    private fun showErrorAlert(titulo: String, mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .show()
    }
}