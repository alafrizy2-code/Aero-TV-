package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
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
import com.example.model.AppInfo
import com.example.ui.theme.AppleTvFocusBorder
import com.example.ui.theme.tvFocusable
import kotlinx.coroutines.delay

@Composable
fun AppOptionsDialog(
    app: AppInfo,
    accentColor: Color,
    onOpenApp: (AppInfo) -> Unit,
    onToggleFavorite: (AppInfo) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    onUninstall: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val firstFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100)
        try {
            firstFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Fallback
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(420.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xF0111827))
                    .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(22.dp))
                    .padding(24.dp)
            ) {
                Column {
                    // Header with App Title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = app.label,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = app.packageName,
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(0x22FFFFFF))
                                .tvFocusable(
                                    accentColor = AppleTvFocusBorder,
                                    shape = CircleShape,
                                    onClick = onDismiss
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Actions list
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // 1. Open
                        AppOptionButton(
                            label = "Buka Aplikasi",
                            icon = Icons.Default.PlayArrow,
                            accentColor = Color.White,
                            focusRequester = firstFocusRequester,
                            onClick = {
                                onOpenApp(app)
                                onDismiss()
                            }
                        )

                        // 2. Favorite toggle
                        AppOptionButton(
                            label = if (app.isFavorite) "Hapus dari Favorit" else "Sematkan ke Favorit",
                            icon = if (app.isFavorite) Icons.Default.StarOutline else Icons.Default.Star,
                            accentColor = if (app.isFavorite) Color(0xFFFBBF24) else Color.White,
                            onClick = {
                                onToggleFavorite(app)
                                onDismiss()
                            }
                        )

                        // 3. App Info
                        AppOptionButton(
                            label = "Info Aplikasi (Pengaturan TV)",
                            icon = Icons.Default.Info,
                            accentColor = Color(0xFF38BDF8),
                            onClick = {
                                onAppInfo(app)
                                onDismiss()
                            }
                        )

                        // 4. Uninstall (if not system app)
                        if (!app.isSystemApp) {
                            AppOptionButton(
                                label = "Copot Pemasangan (Uninstall)",
                                icon = Icons.Default.Delete,
                                accentColor = Color(0xFFEF4444),
                                onClick = {
                                    onUninstall(app)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppOptionButton(
    label: String,
    icon: ImageVector,
    accentColor: Color,
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
                scaleOnFocus = 1.04f,
                elevationOnFocus = 8.dp,
                focusRequester = focusRequester,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}
