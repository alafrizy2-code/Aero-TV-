package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppManager
import com.example.data.FootballScheduleRepository
import com.example.data.PreferencesRepository
import com.example.data.WeatherRepository
import com.example.data.LeagueScheduleResult
import com.example.model.AppInfo
import com.example.model.FootballMatch
import com.example.model.LauncherSettings
import com.example.model.LeagueInfo
import com.example.model.WeatherInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LauncherViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application)
    private val appManager = AppManager(application)
    private val weatherRepo = WeatherRepository(application)
    private val footballRepo = FootballScheduleRepository()

    val settings: StateFlow<LauncherSettings> = prefsRepo.settings
    val favorites: StateFlow<Set<String>> = prefsRepo.favorites

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _weather = MutableStateFlow(
        WeatherInfo(
            temperature = 30,
            conditionText = "Cerah Berawan",
            conditionCode = 2,
            locationName = "Jakarta (WIB)",
            humidity = 70,
            windSpeed = 12,
            isDay = true,
            lastUpdated = "12:00 WIB"
        )
    )
    val weather: StateFlow<WeatherInfo> = _weather.asStateFlow()

    private val _footballMatches = MutableStateFlow<List<FootballMatch>>(emptyList())
    val footballMatches: StateFlow<List<FootballMatch>> = _footballMatches.asStateFlow()

    private val _isRefreshingWeather = MutableStateFlow(false)
    val isRefreshingWeather: StateFlow<Boolean> = _isRefreshingWeather.asStateFlow()

    private val _isRefreshingMatches = MutableStateFlow(false)
    val isRefreshingMatches: StateFlow<Boolean> = _isRefreshingMatches.asStateFlow()

    // Football Center 4 Leagues State
    val targetLeagues: List<LeagueInfo> = footballRepo.getTargetLeagues()

    private val _selectedLeague = MutableStateFlow<LeagueInfo?>(null)
    val selectedLeague: StateFlow<LeagueInfo?> = _selectedLeague.asStateFlow()

    private val _currentLeagueSchedule = MutableStateFlow<LeagueScheduleResult?>(null)
    val currentLeagueSchedule: StateFlow<LeagueScheduleResult?> = _currentLeagueSchedule.asStateFlow()

    private val _isLoadingSchedule = MutableStateFlow(false)
    val isLoadingSchedule: StateFlow<Boolean> = _isLoadingSchedule.asStateFlow()

    // Dialog & overlay states
    var showSettingsDialog = MutableStateFlow(false)
    var showVoiceSearch = MutableStateFlow(false)
    var showAllAppsDialog = MutableStateFlow(false)
    var showControlCenter = MutableStateFlow(false)
    var currentCategory = MutableStateFlow("Tonton Sekarang")
    var selectedAppForOptions = MutableStateFlow<AppInfo?>(null)
    var selectedMatchForDetails = MutableStateFlow<FootballMatch?>(null)

    init {
        loadApps()
        refreshWeather()
        refreshFootballSchedule()

        // Background ticker for real-time football and weather updates
        viewModelScope.launch {
            while (true) {
                delay(60_000) // update every minute
                refreshFootballSchedule()
                val current = _selectedLeague.value
                if (current != null) {
                    refreshLeagueSchedule(current.code)
                }
            }
        }
    }

    fun selectLeague(league: LeagueInfo) {
        _selectedLeague.value = league
        refreshLeagueSchedule(league.code)
    }

    fun closeLeagueSchedule() {
        _selectedLeague.value = null
        _currentLeagueSchedule.value = null
    }

    fun refreshLeagueSchedule(leagueCode: String) {
        viewModelScope.launch {
            _isLoadingSchedule.value = true
            try {
                val result = footballRepo.getScheduleForLeague(leagueCode)
                _currentLeagueSchedule.value = result
            } finally {
                _isLoadingSchedule.value = false
            }
        }
    }

    fun loadApps() {
        viewModelScope.launch {
            val favs = favorites.first()
            _apps.value = appManager.getInstalledApps(favs)
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            _isRefreshingWeather.value = true
            try {
                val currentSettings = settings.value
                val data = weatherRepo.fetchWeather(
                    cityName = currentSettings.selectedCity,
                    useAutoLocation = currentSettings.useAutoLocation
                )
                _weather.value = data
            } finally {
                _isRefreshingWeather.value = false
            }
        }
    }

    fun refreshFootballSchedule() {
        viewModelScope.launch {
            _isRefreshingMatches.value = true
            try {
                val schedule = footballRepo.getLiveSchedules()
                _footballMatches.value = schedule
            } finally {
                _isRefreshingMatches.value = false
            }
        }
    }

    fun updateSettings(newSettings: LauncherSettings) {
        prefsRepo.updateSettings(newSettings)
        refreshWeather()
    }

    fun toggleFavorite(app: AppInfo) {
        prefsRepo.toggleFavorite(app.packageName)
        loadApps()
    }

    fun launchApp(app: AppInfo) {
        appManager.launchApp(app)
    }

    fun openAppInfo(app: AppInfo) {
        appManager.openAppInfo(app.packageName)
    }

    fun uninstallApp(app: AppInfo) {
        appManager.uninstallApp(app.packageName)
    }

    fun openTvSettings() {
        appManager.openTvSettings()
    }

    fun openWifiSettings() {
        appManager.openWifiSettings()
    }

    fun openBluetoothSettings() {
        appManager.openBluetoothSettings()
    }

    fun openDisplaySettings() {
        appManager.openDisplaySettings()
    }

    fun openDefaultHomeSettings() {
        appManager.openDefaultHomeSettings()
    }
}
