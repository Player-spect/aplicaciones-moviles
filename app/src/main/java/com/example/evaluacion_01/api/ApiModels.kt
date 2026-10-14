package com.example.evaluacion_01.api

// package com.example.evaluacion_01

// Modelo genérico para respuestas simples (ej. Registro, Recuperar, Eliminar)
data class BasicResponse(
    val success: Boolean,
    val message: String
)

// Modelo para la respuesta del Login
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val user: UserData?
)

data class UserData(
    val id: Int,
    val username: String,
    val email: String
)

// Modelo para listar usuarios
data class ListUsersResponse(
    val success: Boolean,
    val data: List<UsuarioApi>
)

data class UsuarioApi(
    val id: Int,
    val nombres: String,
    val apellidos: String,
    val email: String
)

// Modelo para los sensores (Ajusta los nombres según lo que devuelva www.pnk.cl)
data class SensorResponse(
    val temperatura: String,
    val humedad: String
)