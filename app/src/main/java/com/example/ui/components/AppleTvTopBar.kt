package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleTvFocusBorder
import com.example.ui.theme.SiriBlue
import com.example.ui.theme.SiriCyan
import com.example.ui.theme.SiriMagenta
import com.example.ui.theme.SiriPurple
import com.example.ui.theme.tvFocusable

@Composable
fun AppleTvTopBar(
    currentCategory: String,
    onCategorySelect: (String) -> Unit,
    onVoiceSearchClick: () -> Unit,
    onControlCenterClick: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val categories = listOf("Tonton Sekarang", "Jadwal Bola", "Aplikasi")

    // Subtle Siri glow pulse
    val siriTransition = rememberInfiniteTransition(label = "siri_glow")
    val siriPulse by siriTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "siri_pulse"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Apple TV Brand Badge
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(end = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Tv,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AERO",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = " tv",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Light,
                    color = Color(0xFFD1D5DB)
                )
            }
        }

        // Center: Floating Frosted Glass Category Pill Bar
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0x351F2937))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(22.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = currentCategory == category
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .tvFocusable(
                                accentColor = AppleTvFocusBorder,
                                shape = RoundedCornerShape(18.dp),
                                scaleOnFocus = 1.08f,
                                elevationOnFocus = 8.dp,
                                onClick = { onCategorySelect(category) }
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = category,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.Black else Color(0xFFD1D5DB)
                        )
                    }
                }
            }
        }

        // Right: Siri Voice Search & Control Center
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Siri Voice Search Button (Apple TV Siri iridescent gradient)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                SiriBlue.copy(alpha = 0.35f * siriPulse),
                                SiriPurple.copy(alpha = 0.4f * siriPulse),
                                SiriMagenta.copy(alpha = 0.35f * siriPulse)
                            )
                        )
                    )
                    .border(1.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp))
                    .tvFocusable(
                        accentColor = Color.White,
                        shape = RoundedCornerShape(20.dp),
                        scaleOnFocus = 1.08f,
                        elevationOnFocus = 10.dp,
                        onClick = onVoiceSearchClick
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Pencarian Suara Siri",
                        tint = SiriCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Cari",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }

            // Apple TV Control Center Button (Settings & Network)
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x351F2937))
                    .border(1.dp, Color(0x22FFFFFF), CircleShape)
                    .tvFocusable(
                        accentColor = Color.White,
                        shape = CircleShape,
                        scaleOnFocus = 1.15f,
                        elevationOnFocus = 8.dp,
                        onClick = onControlCenterClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pusat Kontrol",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
