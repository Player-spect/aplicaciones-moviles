package com.example.evaluacion_01

data class Usuario(
    val id: Int,
    val nombres: String,
    val apellidos: String,
    val email: String,
    val clave: String
)

object db{
    val listaUsuarios = mutableListOf(
        Usuario(1, "test1", "test", "test1.t@example.com", "Admin@123"),
        Usuario(2, "test2", "test", "test2.t@example.com", "Clave#456")
    )
}