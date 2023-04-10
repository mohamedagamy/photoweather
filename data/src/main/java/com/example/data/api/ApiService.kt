package com.example.data.api

import com.example.data.model.WeatherData
import retrofit2.http.*

interface ApiService {

    @GET("v1/current.json?key=e77df0fbe2b94a9e89f00352230904&q=cairo")
    //@GET("weather")
    suspend fun getCurrentWeather(): WeatherData
}
