package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

class RecoveryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recovery)

        val etEmailRecuperar = findViewById<EditText>(R.id.etEmailRecuperar)
        val btnRecuperar = findViewById<Button>(R.id.btnRecuperar)

        btnRecuperar.setOnClickListener {
            val email = etEmailRecuperar.text.toString().trim()

            // Validar campo en blanco
            if (email.isEmpty()) {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Campo vacío")
                    .setContentText("Debe ingresar un email para continuar")
                    .show()
                return@setOnClickListener
            }

            // Validar formato de email
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Formato Inválido")
                    .setContentText("Ingrese un correo electrónico válido")
                    .show()
                return@setOnClickListener
            }

            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("Código Enviado")
                .setContentText("Revisa tu bandeja de entrada")
                .setConfirmButton("Ingresar Nueva Clave") { sDialog ->
                    sDialog.dismissWithAnimation()
                    val Intent = Intent(this, CreatePassword::class.java)
                    startActivity(Intent)
                }
                .show()
        }
    }
}