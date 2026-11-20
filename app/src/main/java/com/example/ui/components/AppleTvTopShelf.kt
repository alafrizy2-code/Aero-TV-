package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsSoccer
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FootballMatch
import com.example.model.MatchStatus
import com.example.model.WeatherInfo
import com.example.ui.theme.AppleTvFocusBorder
import com.example.ui.theme.tvFocusable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun AppleTvTopShelf(
    featuredMatch: FootballMatch?,
    weather: WeatherInfo,
    accentColor: Color,
    onMatchClick: (FootballMatch) -> Unit,
    onRefreshMatches: () -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("id-ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }
        val dateFormat = SimpleDateFormat("EEEE, d MMMM", Locale.forLanguageTag("id-ID")).apply {
            timeZone = TimeZone.getTimeZone("Asia/Jakarta")
        }

        while (true) {
            val now = Date()
            currentTime = timeFormat.format(now)
            currentDate = dateFormat.format(now)
            delay(1000)
        }
    }

    // Live pulse
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(horizontal = 28.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0x401E293B),
                        Color(0x2B0F172A),
                        Color(0x3B1E293B)
                    )
                )
            )
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Hero Section: Featured Top European Football Match
            if (featuredMatch != null) {
                Column(
                    modifier = Modifier.weight(1.3f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Badge: League & Status
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = featuredMatch.leagueName.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        if (featuredMatch.status == MatchStatus.LIVE) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = pulseAlpha))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "LIVE ${featuredMatch.liveMinute ?: ""}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Text(
                                text = "KICK-OFF ${featuredMatch.matchTimeWib}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    // Main Match Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${featuredMatch.homeTeam}  vs  ${featuredMatch.awayTeam}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (featuredMatch.homeScore != null && featuredMatch.awayScore != null) {
                            Spacer(modifier = Modifier.width(12.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "${featuredMatch.homeScore} - ${featuredMatch.awayScore}",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Action buttons in Apple TV style
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Watch / Live Score button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .tvFocusable(
                                    accentColor = AppleTvFocusBorder,
                                    shape = RoundedCornerShape(12.dp),
                                    scaleOnFocus = 1.06f,
                                    elevationOnFocus = 8.dp,
                                    onClick = { onMatchClick(featuredMatch) }
                                )
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (featuredMatch.status == MatchStatus.LIVE) "Buka Skor Langsung" else "Detail Pertandingan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }

                        // Refresh schedule
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x351F2937))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                                .tvFocusable(
                                    accentColor = Color.White,
                                    shape = RoundedCornerShape(12.dp),
                                    scaleOnFocus = 1.06f,
                                    elevationOnFocus = 6.dp,
                                    onClick = onRefreshMatches
                                )
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Segarkan",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Hero Section: Apple TV Elegant Clock & Weather Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x35111827))
                    .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
                    .tvFocusable(
                        accentColor = Color.White,
                        shape = RoundedCornerShape(18.dp),
                        scaleOnFocus = 1.05f,
                        elevationOnFocus = 10.dp,
                        onClick = onWeatherClick
                    )
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    // Clock
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (currentTime.isEmpty()) "12:00" else currentTime,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "WIB",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = if (currentDate.isEmpty()) "Kamis, 29 Oktober" else currentDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Weather Summary Capsule
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            tint = if (weather.conditionCode == 0) Color(0xFFFBBF24) else Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )

                        Text(
                            text = "${weather.temperature}°C",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = weather.conditionText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCBD5E1)
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = weather.locationName,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}
