package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.AppDockSize
import com.example.model.AppInfo
import com.example.ui.theme.AppleTvFocusBorder
import com.example.ui.theme.tvFocusable

@Composable
fun AppleTvDock(
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
            .padding(horizontal = 28.dp, vertical = 6.dp)
    ) {
        // Shelf Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "APLIKASI",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD1D5DB),
                letterSpacing = 1.sp
            )

            Text(
                text = "Tahan OK untuk opsi",
                fontSize = 11.sp,
                color = Color(0x70FFFFFF)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Frosted Glass Dock Tray
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0x351F2937))
                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 1. All Apps Launcher Tile
                AppleTvDockSpecialTile(
                    icon = Icons.Default.Apps,
                    label = "Semua App",
                    accentColor = Color.White,
                    dockSize = dockSize,
                    onClick = onAllAppsClick
                )

                // 2. Installed Apps
                apps.forEach { app ->
                    AppleTvDockAppTile(
                        app = app,
                        dockSize = dockSize,
                        accentColor = accentColor,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }

                // 3. System Settings Tile
                AppleTvDockSpecialTile(
                    icon = Icons.Default.Settings,
                    label = "Pengaturan",
                    accentColor = Color(0xFF94A3B8),
                    dockSize = dockSize,
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
private fun AppleTvDockAppTile(
    app: AppInfo,
    dockSize: AppDockSize,
    accentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit
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
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0x401E293B))
                .tvFocusable(
                    accentColor = AppleTvFocusBorder,
                    shape = RoundedCornerShape(18.dp),
                    scaleOnFocus = 1.15f,
                    elevationOnFocus = 14.dp,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
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
                            .size((dockSize.iconSizeDp * 0.74).dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Apps,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Discreet favorite star badge
            if (app.isFavorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFBBF24)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favorit",
                        tint = Color.Black,
                        modifier = Modifier.size(8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = app.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFF3F4F6),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppleTvDockSpecialTile(
    icon: ImageVector,
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
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.12f))
                .tvFocusable(
                    accentColor = AppleTvFocusBorder,
                    shape = RoundedCornerShape(18.dp),
                    scaleOnFocus = 1.15f,
                    elevationOnFocus = 14.dp,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size((dockSize.iconSizeDp * 0.48).dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
