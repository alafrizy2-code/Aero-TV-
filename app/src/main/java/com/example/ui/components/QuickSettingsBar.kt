package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.tvFocusable

@Composable
fun QuickSettingsBar(
    accentColor: Color,
    onVoiceSearchClick: () -> Unit,
    onWifiClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onDisplayClick: () -> Unit,
    onTvSettingsClick: () -> Unit,
    onCustomizerClick: () -> Unit,
    onSetDefaultLauncherClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Aero TV branding & Voice Search Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Brand Chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AERO TV",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }

            // Voice Search Trigger Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x3B1E293B))
                    .tvFocusable(
                        accentColor = accentColor,
                        shape = RoundedCornerShape(20.dp),
                        scaleOnFocus = 1.05f,
                        elevationOnFocus = 8.dp,
                        onClick = onVoiceSearchClick
                    )
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Pencarian Suara",
                        tint = accentColor,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = "Pencarian Cepat Suara...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFCBD5E1)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0x66FFFFFF),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Right: System Settings Shortcuts
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Wi-Fi
            SettingsShortcutIcon(
                icon = Icons.Default.Wifi,
                label = "Wi-Fi",
                accentColor = accentColor,
                onClick = onWifiClick
            )

            // Bluetooth
            SettingsShortcutIcon(
                icon = Icons.Default.Bluetooth,
                label = "Bluetooth",
                accentColor = accentColor,
                onClick = onBluetoothClick
            )

            // Display
            SettingsShortcutIcon(
                icon = Icons.Default.Tv,
                label = "Tampilan",
                accentColor = accentColor,
                onClick = onDisplayClick
            )

            // Theme & Customizer
            SettingsShortcutIcon(
                icon = Icons.Default.Palette,
                label = "Tema & Wallpaper",
                accentColor = accentColor,
                onClick = onCustomizerClick
            )

            // TV System Settings
            SettingsShortcutIcon(
                icon = Icons.Default.Settings,
                label = "Pengaturan TV",
                accentColor = accentColor,
                onClick = onTvSettingsClick
            )

            // Set Default Launcher Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.2f))
                    .tvFocusable(
                        accentColor = accentColor,
                        shape = RoundedCornerShape(10.dp),
                        scaleOnFocus = 1.08f,
                        elevationOnFocus = 6.dp,
                        onClick = onSetDefaultLauncherClick
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Launcher Default",
                        tint = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Jadikan Default",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsShortcutIcon(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color(0x3B1E293B))
            .tvFocusable(
                accentColor = accentColor,
                shape = CircleShape,
                scaleOnFocus = 1.15f,
                elevationOnFocus = 8.dp,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = Color(0xFFE2E8F0),
            modifier = Modifier.size(17.dp)
        )
    }
}
