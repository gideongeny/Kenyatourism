package com.gideongeng.kenyatourism.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gideongeng.kenyatourism.data.WeatherData
import com.gideongeng.kenyatourism.data.fetchWeather

@Composable
fun WeatherWidget(lat: Double, lon: Double) {
    var weatherData by remember { mutableStateOf<WeatherData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(lat, lon) {
        isLoading = true
        weatherData = fetchWeather(lat, lon)
        isLoading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(16.dp))
                Text("Fetching weather...")
            } else {
                Text(
                    text = weatherData?.icon ?: "☀️", 
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Current Weather",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "${weatherData?.temp ?: "--"}°C • ${weatherData?.status ?: "Unknown"}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1565C0)
                    )
                }
            }
        }
    }
}
