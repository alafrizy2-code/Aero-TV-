package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.AppleTvFocusBorder
import com.example.ui.theme.tvFocusable
import kotlinx.coroutines.delay

@Composable
fun AppleTvControlCenter(
    onWifiClick: () -> Unit,
    onBluetoothClick: () -> Unit,
    onDisplayClick: () -> Unit,
    onTvSettingsClick: () -> Unit,
    onCustomizerClick: () -> Unit,
    onSetDefaultLauncherClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            firstItemFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Focus request fallback
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Outer non-clickable dimmed scrim to avoid stealing D-pad focus
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
        ) {
            // Slide-over Right Frosted Glass Panel
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(360.dp)
                    .background(Color(0xF0111827))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp))
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Pusat Kontrol",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x22FFFFFF))
                                    .tvFocusable(
                                        accentColor = AppleTvFocusBorder,
                                        shape = CircleShape,
                                        onClick = onDismiss
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Control Center Tiles (Up/Down D-pad Navigable)
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 1. Wi-Fi (Has initial FocusRequester)
                            ControlCenterTile(
                                icon = Icons.Default.Wifi,
                                title = "Jaringan & Wi-Fi",
                                subtitle = "Kelola koneksi internet TV",
                                focusRequester = firstItemFocusRequester,
                                onClick = {
                                    onWifiClick()
                                    onDismiss()
                                }
                            )

                            // 2. Bluetooth
                            ControlCenterTile(
                                icon = Icons.Default.Bluetooth,
                                title = "Bluetooth & Remot",
                                subtitle = "Pasangkan perangkat nirkabel",
                                onClick = {
                                    onBluetoothClick()
                                    onDismiss()
                                }
                            )

                            // 3. Display
                            ControlCenterTile(
                                icon = Icons.Default.Tv,
                                title = "Tampilan & Audio",
                                subtitle = "Pengaturan layar dan suara",
                                onClick = {
                                    onDisplayClick()
                                    onDismiss()
                                }
                            )

                            // 4. Customizer
                            ControlCenterTile(
                                icon = Icons.Default.Palette,
                                title = "Tema & Wallpaper",
                                subtitle = "Ganti latar & warna aksen",
                                onClick = {
                                    onDismiss()
                                    onCustomizerClick()
                                }
                            )

                            // 5. System Settings
                            ControlCenterTile(
                                icon = Icons.Default.Settings,
                                title = "Pengaturan TV Box",
                                subtitle = "Buka pengaturan lengkap OS 14",
                                onClick = {
                                    onTvSettingsClick()
                                    onDismiss()
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Bottom Launcher Default Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .tvFocusable(
                                accentColor = Color.White,
                                shape = RoundedCornerShape(16.dp),
                                scaleOnFocus = 1.05f,
                                elevationOnFocus = 10.dp,
                                onClick = {
                                    onSetDefaultLauncherClick()
                                    onDismiss()
                                }
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Jadikan Launcher Bawaan",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlCenterTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x35374151))
            .tvFocusable(
                accentColor = AppleTvFocusBorder,
                shape = RoundedCornerShape(14.dp),
                scaleOnFocus = 1.05f,
                elevationOnFocus = 8.dp,
                focusRequester = focusRequester,
                onClick = onClick
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
        }
    }
}
