package com.example.photoweather.ui
import androidx.camera.core.impl.MutableStateObservable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.common.Resource
import com.example.data.model.WeatherData
import com.example.photoweather.domain.usecase.getweather.GetWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherUseCase: GetWeatherUseCase,
) : ViewModel() {

    val state = MutableStateFlow<WeatherUiState>(Loading)
    init {
        getWeatherData()
    }

    fun getWeatherData() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            weatherUseCase().collect(::handleResponse)
        }
    }

    private suspend fun handleResponse(it: Resource<WeatherData>) = withContext(Dispatchers.Main) {
        when (it.status) {
            Resource.Status.LOADING -> state.value = Loading
            Resource.Status.SUCCESS -> state.value = WeatherUiStateReady(weather = it.data)
            Resource.Status.ERROR -> state.value =
                WeatherUiStateError(error = it.error?.data?.message)
        }
    }
}

sealed class WeatherUiState
data class WeatherUiStateReady(val weather: WeatherData?) : WeatherUiState()
object Loading : WeatherUiState()
class WeatherUiStateError(val error: String? = null) : WeatherUiState()