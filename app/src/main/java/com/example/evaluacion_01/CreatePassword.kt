package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

class CreatePassword : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_password)

        val etNuevaClave = findViewById<EditText>(R.id.etNuevaClave)
        val etRepetirClave = findViewById<EditText>(R.id.etRepetirClave)
        val btnCrear = findViewById<Button>(R.id.btnCrear)

        btnCrear.setOnClickListener {
            val clave = etNuevaClave.text.toString().trim()
            val repetir = etRepetirClave.text.toString().trim()

            // 1. Validar campos vacíos
            if (clave.isEmpty() || repetir.isEmpty()) {
                mostrarAlerta("Campos vacíos", "Debe completar ambos campos de contraseña.")
                return@setOnClickListener
            }

            // 2. Validar que ambas contraseñas coincidan
            if (clave != repetir) {
                mostrarAlerta("Error de coincidencia", "Las contraseñas ingresadas no coinciden.")
                return@setOnClickListener
            }

            // 3. Validar robustez mediante Regex
            // Requisitos: >=8 caracteres, 1 mayúscula, 1 minúscula, 1 número, 1 carácter especial
            val passwordRegex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!_-]).{8,}$".toRegex()

            if (!clave.matches(passwordRegex)) {
                mostrarAlerta(
                    "Contraseña Débil",
                    "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial."
                )
                return@setOnClickListener
            }

            // 4. Éxito al actualizar
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("¡Contraseña Creada!")
                .setContentText("Su contraseña se ha actualizado correctamente.")
                .setConfirmButton("Ir al Login") { sDialog ->
                    sDialog.dismissWithAnimation()
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                }
                .show()
        }
    }

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .show()
    }
}