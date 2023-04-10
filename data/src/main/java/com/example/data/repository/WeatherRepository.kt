package com.example.data.repository

import com.example.data.api.ApiService
import com.example.data.model.WeatherData
import dagger.hilt.android.scopes.ActivityRetainedScoped

import javax.inject.Inject

@ActivityRetainedScoped
class WeatherRepository @Inject constructor(
    private val apiService: ApiService,
) {
    suspend fun getCurrentWeather(): WeatherData = apiService.getCurrentWeather()
}