package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.Wallpaper
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppDockSize
import com.example.model.LauncherSettings
import com.example.model.ThemeAccent
import com.example.model.WallpaperType
import com.example.ui.theme.AppleTvFocusBorder
import com.example.ui.theme.tvFocusable
import kotlinx.coroutines.delay

@Composable
fun LauncherSettingsDialog(
    settings: LauncherSettings,
    accentColor: Color,
    onSettingsChanged: (LauncherSettings) -> Unit,
    onSetDefaultLauncher: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    val initialFocusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(120)
        try {
            initialFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Fallback
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onSettingsChanged(
                settings.copy(
                    wallpaperType = WallpaperType.CUSTOM_PHOTO,
                    customWallpaperUri = uri.toString()
                )
            )
        }
    }

    val categories = listOf(
        Pair("Wallpaper", Icons.Default.Wallpaper),
        Pair("Warna Tema", Icons.Default.Palette),
        Pair("Ukuran Dok", Icons.Default.ViewAgenda),
        Pair("Lokasi Cuaca", Icons.Default.LocationCity),
        Pair("Launcher Default", Icons.Default.Home)
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Dimmed backdrop (non-clickable so TV remote does not get trapped)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center
        ) {
            // Apple TV Two-Column Settings Modal
            Box(
                modifier = Modifier
                    .width(780.dp)
                    .height(460.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color(0xF2111827))
                    .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(26.dp))
                    .padding(24.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pengaturan Aero TV",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Done / Close Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x35374151))
                                .tvFocusable(
                                    accentColor = AppleTvFocusBorder,
                                    shape = RoundedCornerShape(12.dp),
                                    scaleOnFocus = 1.08f,
                                    onClick = onDismiss
                                )
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Selesai",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Split View: Left Categories & Right Options
                    Row(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        // Left Column: Categories List (Up / Down remote navigable)
                        Column(
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x281F2937))
                                .padding(8.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            categories.forEachIndexed { index, (title, icon) ->
                                val isSelected = selectedCategoryIndex == index
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .tvFocusable(
                                            accentColor = AppleTvFocusBorder,
                                            shape = RoundedCornerShape(12.dp),
                                            scaleOnFocus = 1.04f,
                                            focusRequester = if (index == 0) initialFocusRequester else null,
                                            onClick = { selectedCategoryIndex = index }
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.Black else Color(0xFFD1D5DB),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = title,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Right Column: Options Panel for the selected category (Up/Down + OK navigable)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(18.dp))
                                .background(Color(0x351F2937))
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(18.dp))
                                .padding(16.dp)
                        ) {
                            when (selectedCategoryIndex) {
                                0 -> WallpaperSettingsSection(
                                    settings = settings,
                                    onSelectType = { type -> onSettingsChanged(settings.copy(wallpaperType = type)) },
                                    onPickPhoto = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onDimChange = { dim -> onSettingsChanged(settings.copy(dimLevel = dim)) },
                                    onToggleAnalogClock = { show -> onSettingsChanged(settings.copy(showAnalogClock = show)) }
                                )

                                1 -> ThemeAccentSection(
                                    currentAccent = settings.themeAccent,
                                    onSelectAccent = { accent -> onSettingsChanged(settings.copy(themeAccent = accent)) }
                                )

                                2 -> AppDockSizeSection(
                                    currentSize = settings.appDockSize,
                                    onSelectSize = { size -> onSettingsChanged(settings.copy(appDockSize = size)) }
                                )

                                3 -> WeatherLocationSection(
                                    selectedCity = settings.selectedCity,
                                    autoLocation = settings.useAutoLocation,
                                    onSelectCity = { city -> onSettingsChanged(settings.copy(selectedCity = city, useAutoLocation = false)) },
                                    onToggleAuto = { onSettingsChanged(settings.copy(useAutoLocation = !settings.useAutoLocation)) }
                                )

                                4 -> DefaultLauncherSection(
                                    onOpenDefaultSettings = onSetDefaultLauncher
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WallpaperSettingsSection(
    settings: LauncherSettings,
    onSelectType: (WallpaperType) -> Unit,
    onPickPhoto: () -> Unit,
    onDimChange: (Float) -> Unit,
    onToggleAnalogClock: (Boolean) -> Unit
) {
    val presets = listOf(
        Pair("Foto Keluarga (Referensi Default)", WallpaperType.FAMILY_PHOTO),
        Pair("Ambient Aurora", WallpaperType.AURORA_NIGHT),
        Pair("Deep Space Nebula", WallpaperType.DEEP_SPACE),
        Pair("Cyber Neon Ambient", WallpaperType.CYBER_NEON),
        Pair("OLED Pure Black (Ringan)", WallpaperType.OLED_BLACK)
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Pilihan Latar Belakang TV:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD1D5DB))

        presets.forEach { (label, type) ->
            val isSelected = settings.wallpaperType == type
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.18f) else Color(0x35374151))
                    .tvFocusable(
                        accentColor = AppleTvFocusBorder,
                        shape = RoundedCornerShape(12.dp),
                        scaleOnFocus = 1.04f,
                        onClick = { onSelectType(type) }
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Custom Photo Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x35374151))
                .tvFocusable(
                    accentColor = AppleTvFocusBorder,
                    shape = RoundedCornerShape(12.dp),
                    scaleOnFocus = 1.04f,
                    onClick = onPickPhoto
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Pilih Foto dari Flashdisk / TV Box", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("Gunakan foto pribadi sebagai wallpaper", fontSize = 10.sp, color = Color(0xFF9CA3AF))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Tingkat Keredupan Latar (Dim):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD1D5DB))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                Pair("15%", 0.15f),
                Pair("35%", 0.35f),
                Pair("55%", 0.55f),
                Pair("75%", 0.75f)
            ).forEach { (label, value) ->
                val isSelected = kotlin.math.abs(settings.dimLevel - value) < 0.1f
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color.White else Color(0x35374151))
                        .tvFocusable(
                            accentColor = AppleTvFocusBorder,
                            shape = RoundedCornerShape(10.dp),
                            scaleOnFocus = 1.08f,
                            onClick = { onDimChange(value) }
                        )
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text("Widget Jam Analog Header:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD1D5DB))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (settings.showAnalogClock) Color.White.copy(alpha = 0.18f) else Color(0x35374151))
                .tvFocusable(
                    accentColor = AppleTvFocusBorder,
                    shape = RoundedCornerShape(12.dp),
                    scaleOnFocus = 1.04f,
                    onClick = { onToggleAnalogClock(!settings.showAnalogClock) }
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Tampilkan Jam Analog Header", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                if (settings.showAnalogClock) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun ThemeAccentSection(
    currentAccent: ThemeAccent,
    onSelectAccent: (ThemeAccent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Pilih Warna Aksen Antarmuka:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD1D5DB))

        ThemeAccent.values().forEach { accent ->
            val isSelected = currentAccent == accent
            val color = Color(accent.hex)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.18f) else Color(0x35374151))
                    .tvFocusable(
                        accentColor = AppleTvFocusBorder,
                        shape = RoundedCornerShape(12.dp),
                        scaleOnFocus = 1.04f,
                        onClick = { onSelectAccent(accent) }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(color))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = accent.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDockSizeSection(
    currentSize: AppDockSize,
    onSelectSize: (AppDockSize) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Pilih Ukuran Baris Aplikasi Bawah:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD1D5DB))

        AppDockSize.values().forEach { size ->
            val isSelected = currentSize == size
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.18f) else Color(0x35374151))
                    .tvFocusable(
                        accentColor = AppleTvFocusBorder,
                        shape = RoundedCornerShape(12.dp),
                        scaleOnFocus = 1.04f,
                        onClick = { onSelectSize(size) }
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = size.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (size == AppDockSize.COMPACT_56) "Ukuran kompak ideal untuk TV Box B866F" else "Pilihan ukuran ikon",
                            fontSize = 11.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun WeatherLocationSection(
    selectedCity: String,
    autoLocation: Boolean,
    onSelectCity: (String) -> Unit,
    onToggleAuto: () -> Unit
) {
    val cities = listOf(
        "Jakarta (WIB)",
        "Surabaya (WIB)",
        "Bandung (WIB)",
        "Medan (WIB)",
        "Semarang (WIB)",
        "Yogyakarta (WIB)",
        "Makassar (WITA)",
        "Denpasar (WITA)"
    )

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Auto GPS Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (autoLocation) Color.White else Color(0x35374151))
                .tvFocusable(
                    accentColor = AppleTvFocusBorder,
                    shape = RoundedCornerShape(12.dp),
                    scaleOnFocus = 1.04f,
                    onClick = onToggleAuto
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Deteksi Lokasi Otomatis (GPS / Network)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (autoLocation) Color.Black else Color.White
                    )
                    Text(
                        text = if (autoLocation) "Sedang aktif mengikuti lokasi TV" else "Klik OK untuk mengaktifkan",
                        fontSize = 10.sp,
                        color = if (autoLocation) Color(0xFF4B5563) else Color(0xFF9CA3AF)
                    )
                }
                if (autoLocation) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text("Atau Pilih Kota Indonesia:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD1D5DB))

        cities.forEach { city ->
            val isSelected = !autoLocation && selectedCity == city
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.18f) else Color(0x35374151))
                    .tvFocusable(
                        accentColor = AppleTvFocusBorder,
                        shape = RoundedCornerShape(12.dp),
                        scaleOnFocus = 1.04f,
                        onClick = { onSelectCity(city) }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = city, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultLauncherSection(
    onOpenDefaultSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Jadikan Launcher Utama B866F", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)

        Text(
            text = "Langkah menetapkan Aero TV sebagai launcher utama:\n" +
                    "1. Tekan tombol OK pada tombol di bawah.\n" +
                    "2. Pada menu pengaturan Android TV, pilih 'Aplikasi Layar Utama' (Home App).\n" +
                    "3. Pilih 'Aero TV' dan centang 'Selalu' (Always).\n" +
                    "4. Selesai! Tombol Home remote Anda akan langsung membuka Aero TV.",
            fontSize = 11.sp,
            color = Color(0xFFD1D5DB),
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White)
                .tvFocusable(
                    accentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    scaleOnFocus = 1.05f,
                    elevationOnFocus = 10.dp,
                    onClick = onOpenDefaultSettings
                )
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Buka Pengaturan Home App Sistem",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
