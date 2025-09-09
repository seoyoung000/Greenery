package com.example.greenery.ui.home.weatherAPI

data class WeatherResponse(
    val main: Main,
    val weather: List<Weather>
) {
    data class Main(val temp: Float)
    data class Weather(val description: String)
}

