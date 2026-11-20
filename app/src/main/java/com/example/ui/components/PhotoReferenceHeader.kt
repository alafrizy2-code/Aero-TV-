package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WeatherInfo
import com.example.ui.theme.AppleTvFocusBorder
import com.example.ui.theme.tvFocusable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun PhotoReferenceHeader(
    weather: WeatherInfo,
    showAnalogClock: Boolean = true,
    onWifiClick: () -> Unit,
    onWallpaperClick: () -> Unit,
    onAllAppsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var greetingText by remember { mutableStateOf("SELAMAT PAGI") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.forLanguageTag("id-ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }

        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now).uppercase()

            val hour = Calendar.getInstance(TimeZone.getTimeZone("Asia/Jakarta")).get(Calendar.HOUR_OF_DAY)
            greetingText = when (hour) {
                in 4..10 -> "SELAMAT PAGI"
                in 11..14 -> "SELAMAT SIANG"
                in 15..18 -> "SELAMAT SORE"
                else -> "SELAMAT MALAM"
            }

            delay(1000)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        // Left Section: Greeting + Clock + Divider + Date/Weather + Analog Clock
        Column {
            // Greeting in Uppercase (e.g. SELAMAT PAGI)
            Text(
                text = greetingText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE2E8F0),
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Digital Clock (Large bold white)
                Text(
                    text = if (currentTime.isEmpty()) "00:41" else currentTime,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.SansSerif,
                    letterSpacing = (-1).sp
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Vertical Divider Line
                Box(
                    modifier = Modifier
                        .height(38.dp)
                        .width(1.5.dp)
                        .background(Color(0x55FFFFFF))
                )

                Spacer(modifier = Modifier.width(16.dp))

                // Date & Weather Block
                Column {
                    // Date (e.g. JUMAT 30 OKTOBER)
                    Text(
                        text = if (currentDate.isEmpty()) "JUMAT 30 OKTOBER" else currentDate,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Weather Row (e.g. 24°C Overcast · Medan)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .tvFocusable(
                                accentColor = AppleTvFocusBorder,
                                shape = RoundedCornerShape(6.dp),
                                scaleOnFocus = 1.05f,
                                onClick = onWeatherClick
                            )
                            .padding(vertical = 2.dp, horizontal = 2.dp)
                    ) {
                        val weatherIcon = when {
                            weather.conditionCode == 0 -> Icons.Default.WbSunny
                            weather.conditionCode in 1..3 -> Icons.Default.Cloud
                            weather.conditionCode in 51..82 -> Icons.Default.WaterDrop
                            weather.conditionCode >= 95 -> Icons.Default.Thunderstorm
                            else -> Icons.Default.Cloud
                        }
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${weather.temperature}°C ${weather.conditionText} · ${weather.locationName.substringBefore(" (")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCBD5E1)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Decorative Analog Clock Icon Badge
                if (showAnalogClock) {
                    DecorativeAnalogClockBadge()
                }
            }
        }

        // Top-Right Section: 4 Quick Action Icons in Floating Pill Capsule
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x401E293B))
                .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. Wi-Fi
                TopQuickIconButton(
                    icon = Icons.Default.Wifi,
                    contentDescription = "Wi-Fi",
                    onClick = onWifiClick
                )

                // 2. Wallpaper / Galeri
                TopQuickIconButton(
                    icon = Icons.Default.Image,
                    contentDescription = "Wallpaper",
                    onClick = onWallpaperClick
                )

                // 3. All Apps / Grid
                TopQuickIconButton(
                    icon = Icons.Default.Apps,
                    contentDescription = "Semua Aplikasi",
                    onClick = onAllAppsClick
                )

                // 4. Settings
                TopQuickIconButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
private fun TopQuickIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.Transparent)
            .tvFocusable(
                accentColor = Color.White,
                shape = CircleShape,
                scaleOnFocus = 1.15f,
                elevationOnFocus = 8.dp,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun DecorativeAnalogClockBadge() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x331E293B))
            .border(1.5.dp, Color(0x66FFFFFF), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            // Center pin
            drawCircle(color = Color.White, radius = 2.dp.toPx(), center = center)
            // Hour hand (pointing to ~12 / 1)
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(center.x + 3.dp.toPx(), center.y - 7.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Minute hand (pointing to ~8 / 40 min)
            drawLine(
                color = Color.White,
                start = center,
                end = Offset(center.x - 7.dp.toPx(), center.y + 4.dp.toPx()),
                strokeWidth = 1.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
