package com.example.evaluacion_01

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog
import com.example.evaluacion_01.api.RetrofitClient
import com.example.evaluacion_01.api.UsuarioApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

class ListUsersActivity : AppCompatActivity() {

    private lateinit var adapter: AdapterUser
    private lateinit var recyclerUsuarios: RecyclerView
    private lateinit var etBuscador: EditText
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var networkHelper: NetworkHelper

    // Lista maestra cargada desde SQLite local (Offline-First)
    private var listaOriginal: List<UsuarioApi> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_users)

        dbHelper = DatabaseHelper(this)
        networkHelper = NetworkHelper(this)
        etBuscador = findViewById(R.id.etBuscador)
        recyclerUsuarios = findViewById(R.id.recyclerUsuarios)
        recyclerUsuarios.layoutManager = LinearLayoutManager(this)

        adapter = AdapterUser(
            emptyList(),
            onEditarClick = { usuario ->
                val intent = Intent(this, EditUserActivity::class.java)
                intent.putExtra("id_usuario", usuario.id)
                intent.putExtra("nombres", usuario.nombres)
                intent.putExtra("apellidos", usuario.apellidos)
                intent.putExtra("email", usuario.email)
                startActivity(intent)
            },
            onEliminarClick = { usuario ->
                confirmarEliminacion(usuario)
            }
        )

        recyclerUsuarios.adapter = adapter

        etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filtrarLista(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        // 1. Carga inmediata desde SQLite local para mostrar datos al instante
        cargarUsuariosDesdeLocal()

        // 2. Si hay internet, sincronizamos pendientes con AWS y luego refrescamos
        if (networkHelper.isNetworkAvailable()) {
            lifecycleScope.launch(Dispatchers.IO) {
                sincronizarPendientesConAws()
                withContext(Dispatchers.Main) {
                    cargarUsuariosDesdeLocal()
                }
                sincronizarConAwsYActualizarBackground()
            }
        }
    }

    // --- CARGAR DATOS DESDE SQLITE LOCAL ---
    private fun cargarUsuariosDesdeLocal() {
        val cursorValues = dbHelper.getAllLocalUsers()

        listaOriginal = cursorValues.map { cv ->
            UsuarioApi(
                id = cv.getAsInteger(DatabaseHelper.COLUMN_ID) ?: cv.getAsInteger(DatabaseHelper.COLUMN_LOCAL_ID) ?: 0,
                nombres = cv.getAsString(DatabaseHelper.COLUMN_NOMBRES) ?: "",
                apellidos = cv.getAsString(DatabaseHelper.COLUMN_APELLIDOS) ?: "",
                email = cv.getAsString(DatabaseHelper.COLUMN_EMAIL) ?: ""
            )
        }
        filtrarLista(etBuscador.text.toString())
    }

    private suspend fun sincronizarPendientesConAws() {
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
                        } else {
                            Log.e("ListUsersActivity", "Error al registrar en AWS: ${response.errorBody()?.string()}")
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
                Log.e("ListUsersActivity", "Error al sincronizar pendiente con AWS", e)
            }
        }
    }



    private suspend fun sincronizarConAwsYActualizarBackground() {
        try {
            val response = RetrofitClient.apiServiceAws.listarUsuarios()
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                if (body.success) {
                    val usuariosAws = body.data
                    val remoteIds = mutableListOf<Int>()

                    for (user in usuariosAws) {
                        remoteIds.add(user.id)
                        dbHelper.saveOrUpdateUserFromAws(
                            remoteId = user.id,
                            username = user.email.substringBefore("@"),
                            nombres = user.nombres,
                            apellidos = user.apellidos,
                            email = user.email,
                            passwordHash = "AWS_REMOTE",
                            createdAt = "2026-01-01 00:00:00"
                        )
                    }

                    // Eliminamos localmente los usuarios que ya no existen en AWS
                    dbHelper.cleanUpDeletedUsers(remoteIds)

                    withContext(Dispatchers.Main) {
                        cargarUsuariosDesdeLocal()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ListUsersActivity", "Error al sincronizar desde AWS", e)
        }
    }

    // --- ELIMINACIÓN OFFLINE ---
    private fun confirmarEliminacion(usuario: UsuarioApi) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("¿Eliminar usuario?")
            .setContentText("Esta acción eliminará a ${usuario.nombres}")
            .setConfirmText("Sí, eliminar")
            .setConfirmClickListener { sDialog ->
                sDialog.dismissWithAnimation()

                val db = dbHelper.writableDatabase
                val cursor = db.rawQuery(
                    "SELECT ${DatabaseHelper.COLUMN_LOCAL_ID} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.COLUMN_ID} = ? OR ${DatabaseHelper.COLUMN_EMAIL} = ?",
                    arrayOf(usuario.id.toString(), usuario.email)
                )
                if (cursor.moveToFirst()) {
                    val localId = cursor.getInt(0)
                    dbHelper.markUserAsDeletedOffline(localId)
                }
                cursor.close()
                db.close()

                SweetAlertDialog(this@ListUsersActivity, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("Eliminado")
                    .setContentText("El cambio se realizo con exito.")
                    .show()

                cargarUsuariosDesdeLocal()
            }
            .setCancelButton("Cancelar") { it.dismissWithAnimation() }
            .show()
    }

    private fun filtrarLista(filtro: String) {
        val query = removerAcentos(filtro).lowercase()
        val filtrados = if (query.isEmpty()) {
            listaOriginal
        } else {
            listaOriginal.filter {
                removerAcentos(it.nombres).lowercase().contains(query) ||
                removerAcentos(it.apellidos).lowercase().contains(query)
            }
        }
        adapter.actualizarLista(filtrados)
    }

    private fun removerAcentos(cadena: String): String {
        val normalizada = Normalizer.normalize(cadena, Normalizer.Form.NFD)
        return normalizada.replace("\\p{M}".toRegex(), "")
    }
}