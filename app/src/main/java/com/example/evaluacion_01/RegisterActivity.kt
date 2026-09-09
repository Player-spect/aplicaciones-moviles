package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

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

            // 5. Registro exitoso (simulado sin base de datos por ahora)
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("¡Registro Exitoso!")
                .setContentText("Usuario registrado correctamente.")
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
