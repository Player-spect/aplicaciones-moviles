package com.example.evaluacion_01

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enlazar los elementos de la vista
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val tvRegistrarme = findViewById<TextView>(R.id.tvRegistrarme)
        val tvOlvidasteClave = findViewById<TextView>(R.id.tvOlvidasteClave)

        // Configurar INGRESAR
        btnIngresar.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Validacion Campos vacíos
            if (email.isEmpty() || password.isEmpty()) {
                show_error_alert("¡Error!", "Todos los campos son obligatorios")
                return@setOnClickListener
            }

            // Validacion Formato de Email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                show_error_alert("Formato Inválido", "Por favor ingresa un correo válido")
                return@setOnClickListener
            }

            // Si pasa las validaciones, simulamos el ingreso exitoso
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("¡Éxito!")
                .setContentText("Credenciales validadas correctamente")
                .setConfirmButton("Continuar") { sDialog ->
                    sDialog.dismissWithAnimation()
                    // Para despues

                }
                .show()
        }

        // 3. Configurar clics de los textos (Para Puntos 3, 4 y 5)
        tvRegistrarme.setOnClickListener {
            // Registrar usuarios
        }

        tvOlvidasteClave.setOnClickListener {
            // Recuperar contrasennia
        }
    }

    // Sweet_Alert
    private fun show_error_alert(titulo: String, mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .show()
    }
}