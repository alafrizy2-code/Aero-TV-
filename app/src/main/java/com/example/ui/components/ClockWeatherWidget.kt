package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WeatherInfo
import com.example.ui.theme.tvFocusable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun ClockWeatherWidget(
    weather: WeatherInfo,
    accentColor: Color,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentSeconds by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val secFormat = SimpleDateFormat(":ss", Locale.forLanguageTag("id-ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("id-ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }

        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentSeconds = secFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Digital Clock Section
        Column {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (currentTime.isEmpty()) "12:00" else currentTime,
                    fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = (-1).sp,
                    fontFamily = FontFamily.SansSerif
                )
                Text(
                    text = currentSeconds,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .padding(bottom = 14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "WIB",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
            Text(
                text = if (currentDate.isEmpty()) "Kamis, 29 Oktober 2026" else currentDate,
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFCBD5E1),
                letterSpacing = 0.5.sp
            )
        }

        // Real-Time Weather Card (Focusable with remote)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x381E293B))
                .tvFocusable(
                    accentColor = accentColor,
                    shape = RoundedCornerShape(16.dp),
                    scaleOnFocus = 1.05f,
                    onClick = onWeatherClick
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Weather icon badge
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val weatherIcon = when {
                        weather.conditionCode == 0 -> Icons.Default.WbSunny
                        weather.conditionCode in 1..3 -> Icons.Default.Cloud
                        weather.conditionCode in 51..82 -> Icons.Default.WaterDrop
                        weather.conditionCode >= 95 -> Icons.Default.Thunderstorm
                        else -> Icons.Default.Cloud
                    }
                    val iconTint = if (weather.conditionCode == 0) Color(0xFFFBBF24) else accentColor
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = "Cuaca",
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Temp and Condition
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${weather.temperature}°C",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = weather.conditionText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = weather.locationName,
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${weather.humidity}%",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Air,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${weather.windSpeed} km/j",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Segarkan cuaca",
                    tint = Color(0x66FFFFFF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
