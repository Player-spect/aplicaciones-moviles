package com.example.evaluacion_01.api

// package com.example.evaluacion_01

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL_AWS = "http://52.204.32.220/api/"

    // URL Base para los sensores
    private const val BASE_URL_SENSORES = "https://www.pnk.cl/"

    val apiServiceAws: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_AWS)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val apiServiceSensores: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL_SENSORES)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}