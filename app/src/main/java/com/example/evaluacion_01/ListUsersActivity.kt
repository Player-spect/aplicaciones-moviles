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
import com.example.evaluacion_01.api.RetrofitClient // Ajusta si es necesario
import com.example.evaluacion_01.api.UsuarioApi // Ajusta si es necesario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer

class ListUsersActivity : AppCompatActivity() {

    private lateinit var adapter: AdapterUser
    private lateinit var recyclerUsuarios: RecyclerView
    private lateinit var etBuscador: EditText

    // Lista maestra que guarda los datos originales descargados de AWS
    private var listaOriginal: List<UsuarioApi> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_users) // Asegúrate de que este es tu XML

        etBuscador = findViewById(R.id.etBuscador)
        recyclerUsuarios = findViewById(R.id.recyclerUsuarios)
        recyclerUsuarios.layoutManager = LinearLayoutManager(this)

        // Inicializar el adaptador vacío
        adapter = AdapterUser(
            emptyList(),
            onEditarClick = { usuario ->
                // TODO: Aquí abriremos la pantalla de Modificar más adelante
               // Abrimos la pantalla de edición y le pasamos los datos actuales
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

        // Buscador en tiempo real (ignora mayúsculas y acentos)
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
        obtenerUsuariosDeAWS()
    }

    private fun obtenerUsuariosDeAWS() {
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        pDialog.progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
        pDialog.titleText = "Cargando usuarios..."
        pDialog.setCancelable(false)
        pDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiServiceAws.listarUsuarios()

                withContext(Dispatchers.Main) {
                    pDialog.dismissWithAnimation()
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success) {
                            listaOriginal = body.data
                            filtrarLista(etBuscador.text.toString()) // Aplicar filtro si ya había texto
                        } else {
                            mostrarAlerta("Error", "No se pudieron cargar los usuarios.")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pDialog.dismissWithAnimation()
                    Log.e("ListUsersActivity", "Error de red", e)
                    mostrarAlerta("Sin Conexión", "No se pudo conectar al servidor.")
                }
            }
        }
    }

    private fun confirmarEliminacion(usuario: UsuarioApi) {
        SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
            .setTitleText("¿Eliminar usuario?")
            .setContentText("Esta acción eliminará a ${usuario.nombres} del sistema de forma permanente.")
            .setConfirmText("Sí, eliminar")
            .setConfirmClickListener { sDialog ->
                sDialog.dismissWithAnimation()
                eliminarUsuarioDeAWS(usuario.id)
            }
            .setCancelButton("Cancelar") { it.dismissWithAnimation() }
            .show()
    }

    private fun eliminarUsuarioDeAWS(idUsuario: Int) {
        val pDialog = SweetAlertDialog(this, SweetAlertDialog.PROGRESS_TYPE)
        pDialog.progressHelper.barColor = android.graphics.Color.parseColor("#A5DC86")
        pDialog.titleText = "Eliminando..."
        pDialog.setCancelable(false)
        pDialog.show()

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val requestParams = mapOf("id" to idUsuario)
                val response = RetrofitClient.apiServiceAws.eliminarUsuario(requestParams)

                withContext(Dispatchers.Main) {
                    pDialog.dismissWithAnimation()
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        if (body.success) {
                            SweetAlertDialog(this@ListUsersActivity, SweetAlertDialog.SUCCESS_TYPE)
                                .setTitleText("Eliminado")
                                .setContentText(body.message)
                                .show()
                            obtenerUsuariosDeAWS() // Refrescar la lista después de borrar
                        } else {
                            mostrarAlerta("Error", body.message)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pDialog.dismissWithAnimation()
                    mostrarAlerta("Sin Conexión", "No se pudo eliminar el usuario.")
                }
            }
        }
    }

    // --- LÓGICA DE FILTRADO Y ACENTOS ---

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

    private fun mostrarAlerta(titulo: String, mensaje: String) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(titulo)
            .setContentText(mensaje)
            .show()
    }
}