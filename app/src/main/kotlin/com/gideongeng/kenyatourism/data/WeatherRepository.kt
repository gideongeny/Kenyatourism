package com.gideongeng.kenyatourism.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class WeatherData(val temp: Int, val status: String, val icon: String)

suspend fun fetchWeather(lat: Double, lon: Double): WeatherData = withContext(Dispatchers.IO) {
    try {
        val urlStr = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current_weather=true"
        val connection = URL(urlStr).openConnection() as HttpURLConnection
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        val current = json.getJSONObject("current_weather")
        val temp = current.getDouble("temperature").toInt()
        val code = current.getInt("weathercode")
        
        val (status, icon) = when (code) {
            0 -> "Clear" to "☀️"
            1, 2, 3 -> "Partly Cloudy" to "⛅"
            45, 48 -> "Foggy" to "🌫️"
            51, 53, 55 -> "Drizzle" to "🌦️"
            61, 63, 65 -> "Rain" to "🌧️"
            71, 73, 75 -> "Snow" to "❄️"
            80, 81, 82 -> "Showers" to "🌦️"
            95, 96, 99 -> "Thunderstorm" to "⛈️"
            else -> "Fine" to "☀️"
        }
        
        WeatherData(temp, status, icon)
    } catch (e: Exception) {
        WeatherData(25, "Partly Cloudy", "⛅") // Fallback
    }
}
