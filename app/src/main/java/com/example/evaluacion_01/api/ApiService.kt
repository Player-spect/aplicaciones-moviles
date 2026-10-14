package com.example.evaluacion_01.api

// package com.example.evaluacion_01

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT

interface ApiService {

    // --- MÓDULO AUTH ---
    @POST("auth/login.php")
    suspend fun login(@Body request: Map<String, String>): Response<LoginResponse>

    @POST("auth/solicitar_recuperacion.php")
    suspend fun solicitarRecuperacion(@Body request: Map<String, String>): Response<BasicResponse>

    @POST("auth/resetear_clave.php")
    suspend fun resetearClave(@Body request: Map<String, String>): Response<BasicResponse>

    // --- MÓDULO USUARIOS (CRUD) ---
    @POST("users/registro.php")
    suspend fun registrarUsuario(@Body request: Map<String, String>): Response<BasicResponse>

    @GET("users/listar.php")
    suspend fun listarUsuarios(): Response<ListUsersResponse>

    @PUT("users/modificar.php")
    @JvmSuppressWildcards
    suspend fun modificarUsuario(@Body request: Map<String, Any>): Response<BasicResponse>

    // Usamos @HTTP con DELETE porque Retrofit no permite @Body en @DELETE por defecto
    @HTTP(method = "DELETE", path = "users/eliminar.php", hasBody = true)
    suspend fun eliminarUsuario(@Body request: Map<String, Int>): Response<BasicResponse>

    // --- MÓDULO SENSORES ---
    // NOTA: Reemplaza "ruta_a_la_api_pnk" con la ruta real de www.pnk.cl
    @GET("ruta_a_la_api_pnk")
    suspend fun obtenerDatosSensores(): Response<SensorResponse>
}