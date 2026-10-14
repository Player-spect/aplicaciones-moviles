package com.example.evaluacion_01

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.evaluacion_01.api.RetrofitClient // Asegúrate de que apunte a tu Retrofit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditUserActivity : AppCompatActivity() {

    private var idUsuario: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_users)

        val etNombres = findViewById<EditText>(R.id.etEditNombres)
        val etApellidos = findViewById<EditText>(R.id.etEditApellidos)
        val etEmail = findViewById<EditText>(R.id.etEditEmail)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarCambios)

        // 1. Recibir los datos enviados desde la lista
        idUsuario = intent.getIntExtra("id_usuario", -1)
        etNombres.setText(intent.getStringExtra("nombres"))
        etApellidos.setText(intent.getStringExtra("apellidos"))
        etEmail.setText(intent.getStringExtra("email"))

        // 2. Lógica para guardar cambios
        btnGuardar.setOnClickListener {
            val nombres = etNombres.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val email = etEmail.text.toString().trim()

            // Validaciones (Requisito de la rúbrica)
            if (nombres.isEmpty() || apellidos.isEmpty() || email.isEmpty()) {
                mostrarAlerta("Campos vacíos", "Todos los campos son obligatorios.")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mostrarAlerta("Correo inválido", "Por favor, ingrese un formato de correo válido.")
                return@setOnClickListener
            }

            actualizarUsuarioEnAWS(nombres, apellidos, email)
        }
    }

    private fun actualizarUsuarioEnAWS(nombres: String, apellidos: String, email: String) {
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        pDialog.progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
        pDialog.titleText = "Guardando cambios..."
        pDialog.setCancelable(false)
        pDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Como tu ApiService espera un Map<String, String>, enviamos el ID convertido a String
                val requestParams = mapOf(
                    "id" to idUsuario.toString(),
                    "nombres" to nombres,
                    "apellidos" to apellidos,
                    "email" to email
                )

                val response = RetrofitClient.apiServiceAws.modificarUsuario(requestParams)

                withContext(Dispatchers.Main) {
                    pDialog.dismissWithAnimation()
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success) {
                            SweetAlertDialog(this@EditUserActivity, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("¡Actualizado!")
                                .setContentText("El usuario fue modificado exitosamente.")
                                .setConfirmButton("Aceptar") { sDialog ->
                                    sDialog.dismissWithAnimation()
                                    finish() // Cierra esta pantalla y vuelve a la lista
                                }
                                .show()
                        } else {
                            mostrarAlerta("Error al actualizar", body.message)
                        }
                    } else {
                        mostrarAlerta("Error del servidor", "El servidor no respondió correctamente.")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pDialog.dismissWithAnimation()
                    Log.e("EditUserActivity", "Error de red", e)
                    mostrarAlerta("Sin Conexión", "No se pudo conectar al servidor AWS.")
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