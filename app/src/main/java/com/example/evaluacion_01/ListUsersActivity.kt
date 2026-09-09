package com.example.evaluacion_01

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import cn.pedant.SweetAlert.SweetAlertDialog

class ListUsersActivity : AppCompatActivity() {

    private lateinit var adapter: AdapterUser
    private lateinit var recyclerUsuarios: RecyclerView
    private lateinit var etBuscador: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list_users)

        etBuscador = findViewById(R.id.etBuscador)
        recyclerUsuarios = findViewById(R.id.recyclerUsuarios)
        recyclerUsuarios.layoutManager = LinearLayoutManager(this)

        // Configurar adaptador
        adapter = AdapterUser(
            db.listaUsuarios,
            onEditarClick = { usuario ->
                SweetAlertDialog(this, SweetAlertDialog.NORMAL_TYPE)
                    .setTitleText("Modificar Usuario")
                    .setContentText("Editando a ${usuario.nombres}")
                    .setConfirmButton("Aceptar") { it.dismissWithAnimation() }
                    .show()
            },
            onEliminarClick = { usuario ->
                // Confirmación con SweetAlert para eliminar
                SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("¿Eliminar usuario?")
                    .setContentText("Esta acción eliminará a ${usuario.nombres} del sistema.")
                    .setConfirmText("Sí, eliminar")
                    .setConfirmClickListener { sDialog ->
                        sDialog.dismissWithAnimation()
                        db.listaUsuarios.remove(usuario)
                        updateData("")
                        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("Eliminado")
                            .setContentText("El usuario ha sido borrado exitosamente.")
                            .show()
                    }
                    .setCancelButton("Cancelar") { it.dismissWithAnimation() }
                    .show()
            }
        )

        recyclerUsuarios.adapter = adapter

        // Buscador en tiempo real
        etBuscador.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateData(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updateData(filtro: String) {
        val filtrados = if (filtro.isEmpty()) {
            db.listaUsuarios
        } else {
            db.listaUsuarios.filter {
                it.nombres.contains(filtro, ignoreCase = true) ||
                it.apellidos.contains(filtro, ignoreCase = true)
            }
        }
        adapter.actualizarLista(filtrados)
    }

    override fun onResume() {
        super.onResume()
        updateData(etBuscador.text.toString())
    }
}