package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shop
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppInfo
import com.example.ui.theme.AppleTvFocusBorder
import com.example.ui.theme.SiriBlue
import com.example.ui.theme.SiriCyan
import com.example.ui.theme.SiriMagenta
import com.example.ui.theme.SiriPurple
import com.example.ui.theme.tvFocusable

@Composable
fun VoiceSearchOverlay(
    installedApps: List<AppInfo>,
    accentColor: Color,
    onAppSelect: (AppInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var queryText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!spokenMatches.isNullOrEmpty()) {
                queryText = spokenMatches[0]
            }
        }
    }

    fun startListening() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Siri: Katakan apa yang ingin Anda cari...")
            }
            isListening = true
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            isListening = false
        }
    }

    LaunchedEffect(Unit) {
        startListening()
    }

    val matchingApps = remember(queryText, installedApps) {
        if (queryText.isBlank()) emptyList()
        else installedApps.filter { it.label.contains(queryText, ignoreCase = true) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(660.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xE6161D2B))
                    .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(28.dp))
                    .clickable(enabled = false) {}
                    .padding(28.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        brush = Brush.sweepGradient(
                                            listOf(SiriCyan, SiriBlue, SiriPurple, SiriMagenta, SiriCyan)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Pencarian Suara Apple TV",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
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
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Apple TV Siri Glowing Orb
                    AppleTvSiriOrb(
                        isListening = isListening,
                        onOrbClick = { startListening() }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = if (isListening) "Siri mendengarkan suara Anda..." else "Katakan nama aplikasi, film, atau jadwal pertandingan",
                        fontSize = 13.sp,
                        color = if (isListening) SiriCyan else Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Text Input Fallback
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ketik atau ucapkan: YouTube, Vidio, Bola...", color = Color(0xFF6B7280), fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color(0x35FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .clickable { startListening() }
                                    .padding(7.dp)
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Bicara", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    )

                    // Results
                    if (matchingApps.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Aplikasi Ditemukan (${matchingApps.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF93C5FD),
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(matchingApps) { app ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x35374151))
                                        .tvFocusable(
                                            accentColor = AppleTvFocusBorder,
                                            shape = RoundedCornerShape(14.dp),
                                            scaleOnFocus = 1.08f,
                                            onClick = {
                                                onAppSelect(app)
                                                onDismiss()
                                            }
                                        )
                                        .padding(horizontal = 14.dp, vertical = 9.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Buka ${app.label}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Apple TV Quick Search Shortcuts
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AppleTvSearchActionTile(
                            label = "Cari di YouTube",
                            icon = Icons.Default.OndemandVideo,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                launchYouTubeSearch(context, queryText)
                                onDismiss()
                            }
                        )

                        AppleTvSearchActionTile(
                            label = "Google Play TV",
                            icon = Icons.Default.Shop,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                launchPlayStoreSearch(context, queryText)
                                onDismiss()
                            }
                        )

                        AppleTvSearchActionTile(
                            label = "Penelusuran Web",
                            icon = Icons.Default.Search,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                launchWebSearch(context, queryText)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppleTvSiriOrb(
    isListening: Boolean,
    onOrbClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "siri_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    val scalePulse by infiniteTransition.animateFloat(
        initialValue = if (isListening) 0.95f else 1f,
        targetValue = if (isListening) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_pulse"
    )

    Box(
        modifier = Modifier
            .size((72 * scalePulse).dp)
            .clip(CircleShape)
            .background(
                brush = Brush.sweepGradient(
                    listOf(SiriCyan, SiriBlue, SiriPurple, SiriMagenta, SiriCyan)
                )
            )
            .rotate(rotation)
            .tvFocusable(
                accentColor = Color.White,
                shape = CircleShape,
                scaleOnFocus = 1.15f,
                elevationOnFocus = 16.dp,
                onClick = onOrbClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(Color(0xD90B0F19)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Siri Voice",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun AppleTvSearchActionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x35374151))
            .tvFocusable(
                accentColor = AppleTvFocusBorder,
                shape = RoundedCornerShape(14.dp),
                scaleOnFocus = 1.05f,
                elevationOnFocus = 8.dp,
                onClick = onClick
            )
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

private fun launchYouTubeSearch(context: Context, query: String) {
    try {
        val q = if (query.isBlank()) "Sepak Bola Live" else query
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$q")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

private fun launchPlayStoreSearch(context: Context, query: String) {
    try {
        val q = if (query.isBlank()) "Android TV" else query
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$q")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}

private fun launchWebSearch(context: Context, query: String) {
    try {
        val q = if (query.isBlank()) "Berita Terkini" else query
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}
