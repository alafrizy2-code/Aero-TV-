package com.example.ui.components

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.example.R
import com.example.model.LauncherSettings
import com.example.model.WallpaperType

@Composable
fun DynamicWallpaperView(
    settings: LauncherSettings,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wallpaper_anim")
    val animOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aurora_shift"
    )

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF070B14))) {
        when (settings.wallpaperType) {
            WallpaperType.FAMILY_PHOTO -> {
                Image(
                    painter = painterResource(id = R.drawable.custom_family_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            WallpaperType.AURORA_NIGHT -> {
                Image(
                    painter = painterResource(id = R.drawable.tv_ambient_bg_1793285759114),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            WallpaperType.DEEP_SPACE -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1E1B4B).copy(alpha = 0.8f),
                                Color(0xFF0F172A),
                                Color(0xFF030712)
                            ),
                            center = Offset(w * (0.3f + animOffset * 0.4f), h * 0.4f),
                            radius = w * 0.85f
                        )
                    )
                }
            }

            WallpaperType.CYBER_NEON -> {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0D1B2A),
                                Color(0xFF001233),
                                Color(0xFF10002B)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(w, h)
                        )
                    )
                }
            }

            WallpaperType.OLED_BLACK -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF05070A))
                )
            }

            WallpaperType.CUSTOM_PHOTO -> {
                if (!settings.customWallpaperUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = Uri.parse(settings.customWallpaperUri),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.custom_family_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Adjustable dim overlay for contrast and optimal text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = settings.dimLevel.coerceIn(0.05f, 0.75f)))
        )
    }
}
