package com.example.evaluacion_01

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.evaluacion_01.api.UsuarioApi // Importa tu modelo de Retrofit

class AdapterUser(
    private var listaUsuarios: List<UsuarioApi>, // Cambiado a UsuarioApi
    private val onEditarClick: (UsuarioApi) -> Unit, // Cambiado a UsuarioApi
    private val onEliminarClick: (UsuarioApi) -> Unit // Cambiado a UsuarioApi
) : RecyclerView.Adapter<AdapterUser.UsuarioViewHolder>() {

    class UsuarioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombreCompleto: TextView = view.findViewById(R.id.tvNombreCompleto)
        val tvEmailItem: TextView = view.findViewById(R.id.tvEmailItem)
        val btnEditar: ImageView = view.findViewById(R.id.btnEditar)
        val btnEliminar: ImageView = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsuarioViewHolder {
        // Usa tu propio diseño XML (usuario.xml)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.usuario, parent, false)
        return UsuarioViewHolder(view)
    }

    override fun onBindViewHolder(holder: UsuarioViewHolder, position: Int) {
        val usuario = listaUsuarios[position]
        holder.tvNombreCompleto.text = "${usuario.nombres} ${usuario.apellidos}"
        holder.tvEmailItem.text = usuario.email

        holder.btnEditar.setOnClickListener { onEditarClick(usuario) }
        holder.btnEliminar.setOnClickListener { onEliminarClick(usuario) }
    }

    override fun getItemCount(): Int = listaUsuarios.size

    fun actualizarLista(nuevaLista: List<UsuarioApi>) { // Cambiado a UsuarioApi
        listaUsuarios = nuevaLista
        notifyDataSetChanged()
    }
}