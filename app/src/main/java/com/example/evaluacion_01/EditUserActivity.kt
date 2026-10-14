package com.example.evaluacion_01

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.evaluacion_01.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EditUserActivity : AppCompatActivity() {

    private lateinit var etNombres: EditText
    private lateinit var etApellidos: EditText
    private lateinit var etEmail: EditText
    private lateinit var btnGuardar: Button
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var networkHelper: NetworkHelper

    private var usuarioId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user) // Asegúrate de que coincida con tu XML de edición

        dbHelper = DatabaseHelper(this)
        networkHelper = NetworkHelper(this)

        etNombres = findViewById(R.id.etEditNombres) // Ajusta los IDs según tu XML
        etApellidos = findViewById(R.id.etEditApellidos)
        etEmail = findViewById(R.id.etEditEmail)
        btnGuardar = findViewById(R.id.btnGuardarCambios)

        // Recibimos los datos pasados desde el intent del adaptador
        usuarioId = intent.getIntExtra("id_usuario", 0)
        etNombres.setText(intent.getStringExtra("nombres") ?: "")
        etApellidos.setText(intent.getStringExtra("apellidos") ?: "")
        etEmail.setText(intent.getStringExtra("email") ?: "")

        btnGuardar.setOnClickListener {
            guardarCambiosOfflineFirst()
        }
    }

    private fun guardarCambiosOfflineFirst() {
        val nuevosNombres = etNombres.text.toString().trim()
        val nuevosApellidos = etApellidos.text.toString().trim()
        val nuevoEmail = etEmail.text.toString().trim()

        if (nuevosNombres.isEmpty() || nuevosApellidos.isEmpty() || nuevoEmail.isEmpty()) {
            SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                .setTitleText("Campos vacíos")
                .setContentText("Por favor completa todos los campos.")
                .show()
            return
        }

        // 1. Guardamos el cambio en SQLite localmente (Actualiza estado a 2 o mantiene 1)
        dbHelper.updateUserOffline(usuarioId, nuevosNombres, nuevosApellidos, nuevoEmail, isRemote = true)

        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
            .setTitleText("Guardado con exito")
            .setContentText("Los cambios se han realizado con exito.")
            .setConfirmClickListener {
                it.dismissWithAnimation()
                finish() // Regresar a la lista
            }
            .show()

        // 2. Si hay red, intentamos sincronizar de inmediato con AWS
        if (networkHelper.isNetworkAvailable()) {
            sincronizarPendientesConAws()
        }
    }

    private fun sincronizarPendientesConAws() {
        lifecycleScope.launch(Dispatchers.IO) {
            val pendientes = dbHelper.getPendingSyncUsers()
            for (cv in pendientes) {
                val syncStatus = cv.getAsInteger(DatabaseHelper.COLUMN_SYNC_STATUS)
                val remoteId = cv.getAsInteger(DatabaseHelper.COLUMN_ID)
                val localId = cv.getAsInteger(DatabaseHelper.COLUMN_LOCAL_ID)
                val nombres = cv.getAsString(DatabaseHelper.COLUMN_NOMBRES)
                val apellidos = cv.getAsString(DatabaseHelper.COLUMN_APELLIDOS)
                val email = cv.getAsString(DatabaseHelper.COLUMN_EMAIL)
                val passwordHash = cv.getAsString(DatabaseHelper.COLUMN_PASSWORD_HASH)

                try {
                    when (syncStatus) {
                        1 -> { // Pendiente de Insertar
                            val requestMap = mapOf(
                                "nombres" to (nombres ?: ""),
                                "apellidos" to (apellidos ?: ""),
                                "email" to (email ?: ""),
                                "password" to (passwordHash ?: "")
                            )
                            val response = RetrofitClient.apiServiceAws.registrarUsuario(requestMap)
                            if (response.isSuccessful && response.body()?.success == true) {
                                dbHelper.updateSyncStatusAfterSync(localId, remoteId = 0, newStatus = 0)
                            }
                        }
                        2 -> { // Pendiente de Actualizar
                            if (remoteId != null && remoteId > 0) {
                                val requestMap = mapOf(
                                    "id" to remoteId,
                                    "nombres" to (nombres ?: ""),
                                    "apellidos" to (apellidos ?: ""),
                                    "email" to (email ?: "")
                                )
                                val response = RetrofitClient.apiServiceAws.modificarUsuario(requestMap)
                                if (response.isSuccessful && response.body()?.success == true) {
                                    dbHelper.updateSyncStatusAfterSync(localId, remoteId, newStatus = 0)
                                }
                            }
                        }
                        3 -> { // Pendiente de Eliminar
                            if (remoteId != null && remoteId > 0) {
                                val requestMap = mapOf("id" to remoteId)
                                RetrofitClient.apiServiceAws.eliminarUsuario(requestMap)
                                val db = dbHelper.writableDatabase
                                db.delete(DatabaseHelper.TABLE_USERS, "${DatabaseHelper.COLUMN_LOCAL_ID} = ?", arrayOf(localId.toString()))
                                db.close()
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Si falla la red de fondo, se reintentará en el próximo ciclo
                }
            }
        }
    }
}