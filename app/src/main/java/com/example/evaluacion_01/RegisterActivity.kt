package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.evaluacion_01.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etNombres = findViewById<EditText>(R.id.etNombres)
        val etApellidos = findViewById<EditText>(R.id.etApellidos)
        val etEmailReg = findViewById<EditText>(R.id.etEmailReg)
        val etClaveReg = findViewById<EditText>(R.id.etClaveReg)
        val etRepetirClaveReg = findViewById<EditText>(R.id.etRepetirClaveReg)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)

        btnRegistrar.setOnClickListener {
            val nombres = etNombres.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val email = etEmailReg.text.toString().trim()
            val clave = etClaveReg.text.toString().trim()
            val repetir = etRepetirClaveReg.text.toString().trim()

            // Campos obligatorios vacíos
            if (nombres.isEmpty() || apellidos.isEmpty() || email.isEmpty() || clave.isEmpty() || repetir.isEmpty()) {
                mostrarAlerta("Campos Obligatorios", "Por favor completa todos los campos del formulario.")
                return@setOnClickListener
            }

            // Formato de correo valido
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mostrarAlerta("Formato Inválido", "Ingresa una dirección de correo electrónico válida.")
                return@setOnClickListener
            }

            // 3. Coincidencia de contraseñas
            if (clave != repetir) {
                mostrarAlerta("Error de Contraseña", "Las contraseñas ingresadas no coinciden.")
                return@setOnClickListener
            }

            // 4. Robustez de la contraseña (>=8 caracteres, mayúscula, minúscula, número, especial)
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
                titleText = "Registrando..."
                setCancelable(false)
                show()
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Preparamos los datos para enviar al servidor
                    val requestParams = mapOf(
                        "nombres" to nombres,
                        "apellidos" to apellidos,
                        "email" to email,
                        "password" to clave
                    )

                    // Llamada al endpoint de registro
                    val response = RetrofitClient.apiServiceAws.registrarUsuario(requestParams)

                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()

                        if (response.isSuccessful && response.body() != null) {
                            // Registro exitoso en la base de datos
                            SweetAlertDialog(this@RegisterActivity, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("¡Registro Exitoso!")
                                .setContentText(response.body()!!.message)
                                .setConfirmButton("Ir al Login") { sDialog ->
                                    sDialog.dismissWithAnimation()
                                    finish()
                                }
                                .show()
                        } else {
                            var errorMsg = "No se pudo registrar el usuario."
                            val errorBody = response.errorBody()?.string()

                            if (errorBody != null) {
                                try {
                                    val jsonObject = JSONObject(errorBody)
                                    errorMsg = jsonObject.getString("message")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            mostrarAlerta("Error de Registro", errorMsg)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()
                        Log.e("RegisterActivity", "Error de red", e)
                        mostrarAlerta("Sin Conexión", "No se pudo conectar con el servidor AWS.")
                    }
                }
            }
        }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .show()
    }
}