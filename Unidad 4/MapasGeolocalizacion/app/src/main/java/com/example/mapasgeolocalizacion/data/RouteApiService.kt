package com.example.mapasgeolocalizacion.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

//Modelos de datos para mapear el GeoJSON de respuesta
data class RouteResponse(val features: List<Feature>)
data class Feature(val geometry: Geometry)
data class Geometry(val coordinates: List<List<Double>>) //Retorna longitud, latitud

interface OpenRouteService {
    @GET("v2/directions/driving-car")
    suspend fun getDirections(
        @Query("api_key") apiKey: String,
        @Query("start", encoded = true) start: String, //longitud, latitud
        @Query("end", encoded = true) end: String      //longitud, latitud
    ): RouteResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.openrouteservice.org/"

    val apiService: OpenRouteService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouteService::class.java)
    }
}