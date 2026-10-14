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

            // --- INICIO DE CONEXIÓN A AWS ---
            val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE).apply {
                progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
                titleText = "Enviando solicitud..."
                setCancelable(false)
                show()
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val requestParams = mapOf(
                        "email" to email
                    )

                    // Llamada al endpoint para solicitar el correo de recuperación
                    val response = RetrofitClient.apiServiceAws.solicitarRecuperacion(requestParams)

                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()

                        if (response.isSuccessful && response.body() != null) {

                            SweetAlertDialog(this@RecoveryActivity, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Código Enviado")
                                .setContentText(response.body()!!.message) // Mensaje desde PHP
                                .setConfirmButton("Ingresar Nueva Clave") { sDialog ->
                                    sDialog.dismissWithAnimation()

                                    val intent = Intent(this@RecoveryActivity, CreatePasswordActivity::class.java)
                                    // Pasamos el email a la siguiente Activity para saber a quién resetearle la clave
                                    intent.putExtra("email_usuario", email)
                                    startActivity(intent)
                                    finish()
                                }
                                .show()

                        } else {
                            // Extraer error del servidor (ej: Correo no encontrado)
                            var errorMsg = "No se pudo procesar la solicitud."
                            val errorBody = response.errorBody()?.string()

                            if (errorBody != null) {
                                try {
                                    val jsonObject = JSONObject(errorBody)
                                    errorMsg = jsonObject.getString("message")
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }

                            SweetAlertDialog(this@RecoveryActivity, SweetAlertDialog.ERROR_TYPE)
                                .setTitleText("Atención")
                                .setContentText(errorMsg)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pDialog.dismissWithAnimation()
                        Log.e("RecoveryActivity", "Error de red", e)
                        SweetAlertDialog(this@RecoveryActivity, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Sin Conexión")
                            .setContentText("No se pudo conectar con el servidor AWS.")
                            .show()
                    }
                }
            }
        }
    }
}