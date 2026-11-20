package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AllAppsDialog
import com.example.ui.components.AppOptionsDialog
import com.example.ui.components.AppleTvControlCenter
import com.example.ui.components.DynamicWallpaperView
import com.example.ui.components.LauncherSettingsDialog
import com.example.ui.components.MatchDetailsDialog
import com.example.ui.components.FootballCenterWidget
import com.example.ui.components.PhotoReferenceHeader
import com.example.ui.components.VoiceSearchOverlay
import com.example.ui.theme.tvFocusable

@Composable
fun LauncherScreen(
    viewModel: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val weather by viewModel.weather.collectAsState()

    val selectedLeague by viewModel.selectedLeague.collectAsState()
    val currentLeagueSchedule by viewModel.currentLeagueSchedule.collectAsState()
    val isLoadingSchedule by viewModel.isLoadingSchedule.collectAsState()

    val showSettings by viewModel.showSettingsDialog.collectAsState()
    val showVoiceSearch by viewModel.showVoiceSearch.collectAsState()
    val showAllApps by viewModel.showAllAppsDialog.collectAsState()
    val showControlCenter by viewModel.showControlCenter.collectAsState()
    val selectedAppForOptions by viewModel.selectedAppForOptions.collectAsState()
    val selectedMatchForDetails by viewModel.selectedMatchForDetails.collectAsState()

    val accentColor = Color(settings.themeAccent.hex)

    // Back key handles dismissal smoothly: Schedule panel -> dialogs -> root
    BackHandler(enabled = true) {
        when {
            selectedLeague != null -> viewModel.closeLeagueSchedule()
            showControlCenter -> viewModel.showControlCenter.value = false
            showSettings -> viewModel.showSettingsDialog.value = false
            showVoiceSearch -> viewModel.showVoiceSearch.value = false
            showAllApps -> viewModel.showAllAppsDialog.value = false
            selectedAppForOptions != null -> viewModel.selectedAppForOptions.value = null
            selectedMatchForDetails != null -> viewModel.selectedMatchForDetails.value = null
            else -> {
                // At root, stay active
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Dynamic Wallpaper
        DynamicWallpaperView(settings = settings)

        // 2. Main TV Layout: Header + Middle Content (Football Center)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Section: Header (Greeting, Clock, Date, Weather, Analog Clock toggle, Top-Right Capsule)
            PhotoReferenceHeader(
                weather = weather,
                showAnalogClock = settings.showAnalogClock,
                onWifiClick = { viewModel.openWifiSettings() },
                onWallpaperClick = { viewModel.showSettingsDialog.value = true },
                onAllAppsClick = { viewModel.showAllAppsDialog.value = true },
                onSettingsClick = { viewModel.openTvSettings() },
                onWeatherClick = { viewModel.refreshWeather() }
            )

            // Middle Section: Slim Vertical Frosted Glass Football Center on the Left
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                FootballCenterWidget(
                    leagues = viewModel.targetLeagues,
                    selectedLeague = selectedLeague,
                    scheduleResult = currentLeagueSchedule,
                    isLoadingSchedule = isLoadingSchedule,
                    onSelectLeague = { league -> viewModel.selectLeague(league) },
                    onBackToLeagues = { viewModel.closeLeagueSchedule() },
                    onMatchClick = { match -> viewModel.selectedMatchForDetails.value = match }
                )
            }
        }

        // 3. Bottom-Right Corner: ONLY ONE "Aplikasi" Button opening the App Drawer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            ApplicationsButton(
                accentColor = accentColor,
                onClick = { viewModel.showAllAppsDialog.value = true }
            )
        }

        // 4. Modals & Dialogs (App Drawer, Settings, Control Center, etc.)
        if (showControlCenter) {
            AppleTvControlCenter(
                onWifiClick = { viewModel.openWifiSettings() },
                onBluetoothClick = { viewModel.openBluetoothSettings() },
                onDisplayClick = { viewModel.openDisplaySettings() },
                onTvSettingsClick = { viewModel.openTvSettings() },
                onCustomizerClick = { viewModel.showSettingsDialog.value = true },
                onSetDefaultLauncherClick = { viewModel.openDefaultHomeSettings() },
                onDismiss = { viewModel.showControlCenter.value = false }
            )
        }

        if (showVoiceSearch) {
            VoiceSearchOverlay(
                installedApps = apps,
                accentColor = accentColor,
                onAppSelect = { app -> viewModel.launchApp(app) },
                onDismiss = { viewModel.showVoiceSearch.value = false }
            )
        }

        if (showSettings) {
            LauncherSettingsDialog(
                settings = settings,
                accentColor = accentColor,
                onSettingsChanged = { newSettings -> viewModel.updateSettings(newSettings) },
                onSetDefaultLauncher = { viewModel.openDefaultHomeSettings() },
                onDismiss = { viewModel.showSettingsDialog.value = false }
            )
        }

        if (showAllApps) {
            AllAppsDialog(
                apps = apps,
                accentColor = accentColor,
                onAppClick = { app -> viewModel.launchApp(app) },
                onAppLongClick = { app -> viewModel.selectedAppForOptions.value = app },
                onDismiss = { viewModel.showAllAppsDialog.value = false }
            )
        }

        selectedAppForOptions?.let { app ->
            AppOptionsDialog(
                app = app,
                accentColor = accentColor,
                onOpenApp = { viewModel.launchApp(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onAppInfo = { viewModel.openAppInfo(it) },
                onUninstall = { viewModel.uninstallApp(it) },
                onDismiss = { viewModel.selectedAppForOptions.value = null }
            )
        }

        selectedMatchForDetails?.let { match ->
            MatchDetailsDialog(
                match = match,
                accentColor = accentColor,
                onDismiss = { viewModel.selectedMatchForDetails.value = null }
            )
        }
    }
}

@Composable
private fun ApplicationsButton(
    accentColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xC01E293B))
            .border(1.5.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
            .tvFocusable(
                accentColor = accentColor,
                shape = RoundedCornerShape(16.dp),
                scaleOnFocus = 1.08f,
                elevationOnFocus = 10.dp,
                onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Apps,
                contentDescription = "Aplikasi",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "Aplikasi",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}
