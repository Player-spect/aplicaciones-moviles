package com.example.evaluacion_01

import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RegisterUsersActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_users)

        val etNombres = findViewById<EditText>(R.id.etCrudNombres)
        val etApellidos = findViewById<EditText>(R.id.etCrudApellidos)
        val etEmail = findViewById<EditText>(R.id.etCrudEmail)
        val etClave = findViewById<EditText>(R.id.etCrudClave)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarCrud)

        btnGuardar.setOnClickListener {
            val nombres = etNombres.text.toString().trim()
            val apellidos = etApellidos.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val clave = etClave.text.toString().trim()

            if (nombres.isEmpty() || apellidos.isEmpty() || email.isEmpty() || clave.isEmpty()) {
                mostrarAlerta("Campos vacíos", "Todos los campos son obligatorios.")
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mostrarAlerta("Correo inválido", "Ingrese un formato de correo correcto.")
                return@setOnClickListener
            }

            if (!isClaveRobusta(clave)) {
                mostrarAlerta(
                    "Contraseña débil",
                    "La contraseña debe tener al menos 8 caracteres, incluir una mayúscula, una minúscula, un número y un carácter especial."
                )
                return@setOnClickListener
            }

            // Generar username basado en el email
            val username = email.substringBefore("@")
            val createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            val dbHelper = DatabaseHelper(this@RegisterUsersActivity)
            dbHelper.insertUserOffline(
                username = username,
                nombres = nombres,
                apellidos = apellidos,
                email = email,
                passwordHash = clave,
                createdAt = createdAt
            )

            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("¡Guardado!")
                .setContentText("El usuario se ha registrado con éxito.")
                .setConfirmButton("Aceptar") { sDialog ->
                    sDialog.dismissWithAnimation()
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

    private fun isClaveRobusta(clave: String): Boolean {
        // Al menos 8 caracteres, al menos un dígito, al menos una minúscula, al menos una mayúscula y al menos un carácter especial
        val regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#\$%^&+=!])(?=\\S+$).{8,}$".toRegex()
        return regex.matches(clave)
    }
}