package com.example.model

data class WeatherInfo(
    val temperature: Int,
    val conditionText: String,
    val conditionCode: Int,
    val locationName: String,
    val humidity: Int,
    val windSpeed: Int,
    val isDay: Boolean = true,
    val lastUpdated: String = ""
)
