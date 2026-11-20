package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppDockSize
import com.example.model.AppInfo
import com.example.ui.theme.tvFocusable

@Composable
fun BottomAppDock(
    apps: List<AppInfo>,
    dockSize: AppDockSize,
    accentColor: Color,
    onAppClick: (AppInfo) -> Unit,
    onAppLongClick: (AppInfo) -> Unit,
    onAllAppsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 6.dp)
    ) {
        // App Row Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Aplikasi",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${apps.size} terpasang",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            Text(
                text = "Tahan OK untuk opsi aplikasi",
                fontSize = 11.sp,
                color = Color(0x88CBD5E1)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Compact App Dock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 1. "Semua Aplikasi" Action Tile
            AppDockSpecialItem(
                icon = Icons.Default.Apps,
                label = "Semua App",
                accentColor = accentColor,
                dockSize = dockSize,
                onClick = onAllAppsClick
            )

            // 2. Installed & Favorite Apps
            apps.forEach { app ->
                AppDockItem(
                    app = app,
                    dockSize = dockSize,
                    accentColor = accentColor,
                    onClick = { onAppClick(app) },
                    onLongClick = { onAppLongClick(app) }
                )
            }

            // 3. Quick System Settings Tile at end of dock
            AppDockSpecialItem(
                icon = Icons.Default.Settings,
                label = "Pengaturan",
                accentColor = Color(0xFF64748B),
                dockSize = dockSize,
                onClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun AppDockItem(
    app: AppInfo,
    dockSize: AppDockSize,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isLongPressActive by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .width(dockSize.itemWidthDp.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(dockSize.iconSizeDp.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0x3B1E293B))
                .tvFocusable(
                    accentColor = accentColor,
                    shape = RoundedCornerShape(14.dp),
                    scaleOnFocus = 1.15f,
                    elevationOnFocus = 12.dp,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // App Icon
            if (app.icon != null) {
                val bitmap = remember(app.packageName) {
                    try {
                        app.icon.toBitmap(dockSize.iconSizeDp * 2, dockSize.iconSizeDp * 2)
                    } catch (e: Exception) {
                        null
                    }
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = app.label,
                        modifier = Modifier
                            .size((dockSize.iconSizeDp * 0.76).dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Favorite Star Badge
            if (app.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(2.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorit",
                        tint = Color.Black,
                        modifier = Modifier.size(9.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        // Compact Label
        Text(
            text = app.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFE2E8F0),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppDockSpecialItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    dockSize: AppDockSize,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(dockSize.itemWidthDp.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(dockSize.iconSizeDp.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(accentColor.copy(alpha = 0.22f))
                .tvFocusable(
                    accentColor = accentColor,
                    shape = RoundedCornerShape(14.dp),
                    scaleOnFocus = 1.15f,
                    elevationOnFocus = 12.dp,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size((dockSize.iconSizeDp * 0.52).dp)
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
