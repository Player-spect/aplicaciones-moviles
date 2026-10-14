package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.evaluacion_01.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserCRUD_Activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_crud)

        val btnCrearUsuarioCrud = findViewById<Button>(R.id.btnCrearUsuarioCrud)
        val btnListarUsuarios = findViewById<Button>(R.id.btnListarUsuarios)

        // registrar usuarios
        btnCrearUsuarioCrud.setOnClickListener {
             val intent = Intent(this, RegisterUsersActivity::class.java)
             startActivity(intent)
        }

        // Administracion
        btnListarUsuarios.setOnClickListener {
            val intent = Intent(this, ListUsersActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        
        val networkHelper = NetworkHelper(this)
        val dbHelper = DatabaseHelper(this)

        // Si hay internet, sincronizamos pendientes con AWS 
        if (networkHelper.isNetworkAvailable()) {
            lifecycleScope.launch(Dispatchers.IO) {
                sincronizarPendientesConAws(dbHelper)
            }
        }
    }

    private suspend fun sincronizarPendientesConAws(dbHelper: DatabaseHelper) {
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
                Log.e("UserCRUD_Activity", "Error al sincronizar pendiente con AWS", e)
            }
        }
    }
}