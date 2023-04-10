package com.example.photoweather.domain.usecase.getweather

import com.example.data.common.Resource
import com.example.data.model.WeatherData
import com.example.data.repository.WeatherRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class GetWeatherUseCase @Inject constructor(
    private val repository: WeatherRepository,
    private val defaultDispatcher: CoroutineDispatcher
) {

    private var weatherData = WeatherData()

    suspend operator fun invoke(): Flow<Resource<WeatherData>> =
        flow<Resource<WeatherData>> {
            try {
                emit(Resource.loading())
                weatherData = repository.getCurrentWeather()
                emit(Resource.success(weatherData))
            } catch (e: Throwable) {
                emit(Resource.error(e))
            }
        }.flowOn(defaultDispatcher)

}

