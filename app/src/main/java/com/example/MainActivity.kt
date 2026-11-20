package com.example

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.KeyEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewStub
import android.view.ViewTreeObserver
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.data.FootballScheduleRepository
import com.example.theme.AtvCardSettingsDialog
import com.example.theme.AtvHomeController
import com.example.theme.AtvViewMode
import com.example.theme.GoogleTvHomeController
import com.example.theme.GoogleTvTabId
import com.example.theme.LauncherThemeDefinition
import com.example.theme.NexfyHomeController
import com.example.theme.NexfyViewMode
import com.example.theme.ProjectivyHomeController
import com.example.theme.ProjectivyViewMode
import com.example.theme.ThemeAndIconSettingsDialog
import com.example.theme.ThemeAppDrawerDialog
import com.example.theme.ThemeId
import com.example.theme.ThemeLayoutManager
import com.example.theme.ThemeManager
import com.example.theme.ThemedAppAdapter
import com.example.theme.WolfDisplaySettingsDialog
import com.example.theme.WolfHomeController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Cache Memori Statis untuk Aplikasi Terpasang:
 * Menghindari pemindaian PackageManager yang berat saat tombol HOME ditekan,
 * sehingga kembali dari YouTube/Netflix berjalan instan (0ms).
 */
object AppCache {
    @Volatile
    var cachedApps: List<AppModel>? = null
}

class MainActivity : ComponentActivity() {

    enum class WallpaperMode { STATIC, SLIDESHOW, CANVAS_LIVE, STORAGE_LIVE }

    private val PREFS_NAME = "b866f_launcher_prefs"
    private val KEY_SHOW_KICKOFF = "key_show_kickoff_widget"
    private val KEY_WALLPAPER_MODE = "key_active_wallpaper_mode"
    private val KEY_SHOW_APP_NAMES = "key_show_app_names"
    private val KEY_BLUETOOTH_ENABLED = "key_bluetooth_enabled_state"
    private val KEY_OVERLAY_SPEED_ENABLED = "key_overlay_speed_enabled"
    private val KEY_GRID_ICONS_PER_ROW = "key_grid_icons_per_row"
    private val KEY_GRID_AUTO_FIT = "key_grid_auto_fit"

    // Wallpaper Layers
    private lateinit var ivWallpaper: ImageView
    private lateinit var ivWallpaperNext: ImageView
    private lateinit var wallpaperSlideshowManager: WallpaperSlideshowManager

    // Storage Live Wallpaper (Video/GIF)
    private lateinit var layoutLiveWallpaperMediaContainer: android.widget.FrameLayout
    private lateinit var surfaceLiveWallpaper: SurfaceView
    private lateinit var ivGifLiveWallpaper: ImageView
    private lateinit var viewLiveWallpaperBrightnessDim: View
    private lateinit var storageLiveWallpaperManager: StorageLiveWallpaperManager

    // Canvas Live Wallpaper
    private lateinit var liveWallpaperCanvasView: LiveWallpaperCanvasView

    // Slideshow Controls & Indicators
    private lateinit var layoutSlideshowControls: View
    private lateinit var btnSlideshowPrev: View
    private lateinit var btnSlideshowNext: View
    private lateinit var layoutSlideshowDots: LinearLayout

    // Top-Left Clock, Weather & Date Widget
    private lateinit var layoutTopLeft: View
    private lateinit var tvDigitalClockTopLeft: TextView
    private lateinit var tvWeatherDate: TextView
    private lateinit var tvWeatherInfo: TextView
    private lateinit var weatherAnimationView: WeatherAnimationView
    private lateinit var analogClock: AnalogClockView

    // Top-Right Actions & Network Speed
    private lateinit var layoutTopRightContainer: View
    private lateinit var tvNetworkSpeed: TextView
    private lateinit var tvTopIconTooltip: TextView
    private lateinit var btnWifi: ImageButton
    private lateinit var btnMic: ImageButton
    private lateinit var btnBluetooth: ImageButton
    private lateinit var viewBluetoothDot: View
    private lateinit var btnSettings: ImageButton

    private var isWifiOnline = false
    private var currentWifiSsid: String? = null
    private var isBluetoothOn = true
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var settingsGearAnimator: ObjectAnimator? = null
    private var btDiscoveryReceiver: BroadcastReceiver? = null
    private val discoveredBtDevices = mutableListOf<String>()

    // Sliding Right Sidebar
    private lateinit var layoutSidebarScrim: View
    private lateinit var layoutRightSidebar: View
    private lateinit var tvSidebarTitle: TextView
    private lateinit var btnCloseSidebar: ImageButton
    private lateinit var layoutSidebarContent: LinearLayout
    private var isSidebarOpen = false
    private var currentSidebarType: SidebarType? = null
    private var lastFocusedTopButton: View? = null

    // Voice Search Modal Overlay
    private lateinit var layoutVoiceSearchOverlay: View
    private lateinit var tvVoiceTitle: TextView
    private lateinit var tvVoicePrompt: TextView
    private lateinit var tvVoiceResultText: TextView
    private lateinit var voiceWaveView: VoiceWaveView
    private lateinit var btnStartListening: Button
    private lateinit var btnUseRemoteVoice: Button
    private lateinit var btnCloseVoiceSearch: Button
    private var speechRecognizer: SpeechRecognizer? = null
    private var isVoiceListening = false
    private var hasUsedSystemVoiceFallback = false
    private var lastPartialVoiceText: String = ""

    // Football Center Widget
    private lateinit var cardFootballSchedule: View
    private lateinit var btnRefreshFootballSchedule: View
    private lateinit var layoutFootballCenterSidebar: View
    private lateinit var itemLeagueEpl: View
    private lateinit var itemLeagueSerieA: View
    private lateinit var itemLeagueLaLiga: View
    private lateinit var itemLeagueUcl: View
    private lateinit var ivLogoEpl: ImageView
    private lateinit var ivLogoSerieA: ImageView
    private lateinit var ivLogoLaLiga: ImageView
    private lateinit var ivLogoUcl: ImageView

    private lateinit var layoutLeagueSchedulePanel: View
    private lateinit var btnBackFromSchedule: View
    private lateinit var ivScheduleLeagueLogo: ImageView
    private lateinit var tvScheduleLeagueName: TextView
    private lateinit var tvScheduleDateTitle: TextView
    private lateinit var tvScheduleMatchCount: TextView
    private lateinit var pbScheduleLoading: ProgressBar
    private lateinit var tvScheduleEmpty: TextView

    private lateinit var rvFootballMatches: RecyclerView
    private lateinit var footballAdapter: FootballMatchAdapter
    private val footballRepo = FootballScheduleRepository()
    private var isKickoffWidgetVisible = true
    private var selectedLeagueCard: View? = null

    // Bottom Horizontal Applications Row (Unified Theme System)
    private lateinit var tvThemeShelfHeader: TextView
    private lateinit var rvBottomApps: TvHorizontalRecyclerView
    private lateinit var bottomAppRowAdapter: ThemedAppAdapter
    private var activeThemeDialog: ThemeAndIconSettingsDialog? = null
    private var activeDrawerDialog: ThemeAppDrawerDialog? = null
    private var allInstalledApps = listOf<AppModel>()
    private var lastAppliedThemeId: ThemeId? = null
    private var lastAppliedCustomHome: Boolean? = null

    // Google TV Theme Host (Lazy ViewStub + Controller)
    private lateinit var stubGoogleTvHost: ViewStub
    private var googleTvHostRoot: View? = null
    private var googleTvController: GoogleTvHomeController? = null

    // Nexfy Launcher Theme Host (Lazy ViewStub + Controller)
    private lateinit var stubNexfyHost: ViewStub
    private var nexfyHostRoot: View? = null
    private var nexfyController: NexfyHomeController? = null

    // Projectivy Launcher Theme Host (Lazy ViewStub + Controller)
    private lateinit var stubProjectivyHost: ViewStub
    private var projectivyHostRoot: View? = null
    private var projectivyController: ProjectivyHomeController? = null

    // ATV Launcher Theme Host (Lazy ViewStub + Controller)
    private lateinit var stubAtvHost: ViewStub
    private var atvHostRoot: View? = null
    private var atvController: AtvHomeController? = null

    // Wolf Launcher Theme Host (Lazy ViewStub + Controller)
    private lateinit var stubWolfHost: ViewStub
    private var wolfHostRoot: View? = null
    private var wolfController: WolfHomeController? = null

    private var currentWallpaperMode = WallpaperMode.STATIC
    private var networkMonitorJob: Job? = null
    private var weatherRefreshJob: Job? = null
    private var timeTickReceiver: BroadcastReceiver? = null
    private var packageChangeReceiver: BroadcastReceiver? = null
    private val startupHandler = Handler(Looper.getMainLooper())
    private var hasScheduledInitialPermissions = false
    private val deferredOverlayStartRunnable = Runnable {
        if (!isFinishing && !isDestroyed) {
            NetworkSpeedOverlayService.startIfPermitted(this)
            DigitalClockOverlayService.startIfPermitted(this)
        }
    }
    private val deferredPermissionsRunnable = Runnable {
        if (!isFinishing && !isDestroyed) {
            checkStorageAndLocationPermissions()
        }
    }

    // Activity Result Launchers
    private val wallpaperPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                applyWallpaperMode(WallpaperMode.STATIC)
                val success = WallpaperHelper.saveCustomWallpaperUri(this, uri)
                WallpaperHelper.loadWallpaper(this, ivWallpaper)
                Toast.makeText(this, if (success) "Wallpaper berhasil disimpan!" else "Wallpaper diterapkan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val folderPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val treeUri: Uri? = result.data?.data
            if (treeUri != null) {
                lifecycleScope.launch {
                    val count = wallpaperSlideshowManager.onFolderSelected(treeUri)
                    if (count > 0) {
                        applyWallpaperMode(WallpaperMode.SLIDESHOW)
                        Toast.makeText(this@MainActivity, "Slideshow aktif! Ditemukan $count foto.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Tidak ditemukan file foto di folder terpilih.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private val liveWallpaperPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                val item = storageLiveWallpaperManager.addItem(uri)
                if (item != null) {
                    applyWallpaperMode(WallpaperMode.STORAGE_LIVE)
                    storageLiveWallpaperManager.applyCurrentActive()
                    Toast.makeText(this@MainActivity, "Live Wallpaper '${item.fileName}' berhasil diaktifkan!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private val clockWallpaperPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                val success = WallpaperHelper.saveClockWallpaperUri(this, uri)
                WallpaperHelper.loadClockWallpaper(this, analogClock)
                Toast.makeText(this, if (success) "Wallpaper jam disimpan!" else "Wallpaper jam diterapkan.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val locGranted = results[android.Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locGranted) {
            WeatherHelper.invalidateCache()
            fetchRealtimeWeather(forceRefresh = true)
        }
    }

    private val recordAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginSpeechRecognizer()
        } else {
            isVoiceListening = false
            voiceWaveView.stopWave()
            tvVoicePrompt.text = "Izin mikrofon ditolak. Mencoba Mic Remote TV..."
            btnStartListening.text = "Mulai Bicara"
            launchSystemVoiceIntentFallback(showUnavailableMsg = false)
        }
    }

    private val voiceFallbackLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isVoiceListening = false
        voiceWaveView.stopWave()
        btnStartListening.text = "Mulai Bicara"
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val spoken = matches[0].trim()
                layoutVoiceSearchOverlay.visibility = View.VISIBLE
                tvVoicePrompt.text = "Kata terdeteksi dari remote:"
                tvVoiceResultText.text = "\"$spoken\""
                tvVoiceResultText.setTextColor(Color.parseColor("#38BDF8"))
                searchAndLaunchSpokenApp(spoken)
                return@registerForActivityResult
            }
        }
        if (layoutVoiceSearchOverlay.visibility == View.VISIBLE) {
            tvVoicePrompt.text = "Tidak ada suara yang tertangkap. Tekan 'Mulai Bicara' atau 'Mic Remote TV'."
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)) {
            NetworkSpeedOverlayService.isOverlayEnabledPref = true
            DigitalClockOverlayService.isOverlayEnabledPref = true
            prefs.edit().putBoolean(KEY_OVERLAY_SPEED_ENABLED, true).apply()
            NetworkSpeedOverlayService.startIfPermitted(this)
            DigitalClockOverlayService.startIfPermitted(this)
            Toast.makeText(this, "Overlay jam & kecepatan jaringan aktif di semua aplikasi!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Izin tampilkan di atas aplikasi lain belum diaktifkan", Toast.LENGTH_SHORT).show()
        }
        if (isSidebarOpen && currentSidebarType == SidebarType.SETTINGS) {
            populateSidebarContent(SidebarType.SETTINGS)
        }
    }

    private fun checkStorageAndLocationPermissions() {
        val permissions = mutableListOf<String>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (permissions.isNotEmpty()) {
            try {
                storagePermissionLauncher.launch(permissions.toTypedArray())
            } catch (_: Exception) { }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        bindViews()

        // Inisialisasi Wallpaper Managers
        wallpaperSlideshowManager = WallpaperSlideshowManager(this, ivWallpaper, ivWallpaperNext)
        wallpaperSlideshowManager.setOnSlideChangedListener { currentIdx: Int, totalCount: Int ->
            updateSlideshowDots(currentIdx, totalCount)
        }

        storageLiveWallpaperManager = StorageLiveWallpaperManager(
            this,
            layoutLiveWallpaperMediaContainer,
            surfaceLiveWallpaper,
            ivGifLiveWallpaper,
            viewLiveWallpaperBrightnessDim
        )

        // Muat Mode Wallpaper Terpilih (Hanya 1 yang aktif, decode async non-blocking)
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedModeStr = prefs.getString(KEY_WALLPAPER_MODE, WallpaperMode.STATIC.name)
        val initialMode = try {
            WallpaperMode.valueOf(savedModeStr ?: WallpaperMode.STATIC.name)
        } catch (e: Exception) {
            WallpaperMode.STATIC
        }
        applyWallpaperMode(initialMode)

        // Muat Wallpaper Jam Analog & Status Visibility
        WallpaperHelper.loadClockWallpaper(this, analogClock)
        val showAnalog = prefs.getBoolean("key_show_analog_clock", true)
        analogClock.visibility = if (showAnalog) View.VISIBLE else View.GONE

        // Update Hari & Tanggal Dinamis (Top Left)
        updateWeatherDate()

        // Muat Cuaca Dinamis Real-time Berdasarkan Lokasi Perangkat (Kecamatan)
        fetchRealtimeWeather(forceRefresh = false)
        startPeriodicWeatherRefresh()

        // Konfigurasi Slideshow Manual Controls
        setupSlideshowControls()

        // Monitor Kecepatan Jaringan & Jam Digital Real-Time (Launcher + Overlay Global di YouTube/Netflix)
        startNetworkSpeedMonitor()
        NetworkSpeedOverlayService.isOverlayEnabledPref = prefs.getBoolean(KEY_OVERLAY_SPEED_ENABLED, true)
        NetworkSpeedOverlayService.isLauncherForeground = true

        DigitalClockOverlayService.isOverlayEnabledPref = prefs.getBoolean(KEY_OVERLAY_SPEED_ENABLED, true)
        DigitalClockOverlayService.isLauncherForeground = true

        // Tunda penambahan jendela overlay & dialog izin hingga frame pertama MainActivity selesai dirender
        // agar tidak memicu timeout SurfaceSyncGroup(wmsSync-VRI[MainActivity]).
        startupHandler.postDelayed(deferredOverlayStartRunnable, 900L)
        if (!hasScheduledInitialPermissions) {
            hasScheduledInitialPermissions = true
            startupHandler.postDelayed(deferredPermissionsRunnable, 1100L)
        }

        // Status Asli WiFi & Bluetooth
        initWifiStatusMonitor()
        initBluetoothStatus(prefs)

        // Konfigurasi 4 Tombol Top-Right (Mic, WiFi, Bluetooth, Settings)
        setupTopRightIcons()

        // Konfigurasi Sidebar & Voice Search
        setupSidebarActions()
        setupVoiceSearchOverlay()

        // Konfigurasi Kick-Off Sepakbola
        isKickoffWidgetVisible = prefs.getBoolean(KEY_SHOW_KICKOFF, true)
        cardFootballSchedule.visibility = if (isKickoffWidgetVisible) View.VISIBLE else View.GONE
        setupFootballSchedule()

        // Konfigurasi Baris Aplikasi Horizontal di Bawah Layar (sesuai foto referensi)
        setupBottomAppRow()
    }

    private fun bindViews() {
        // Wallpaper Views
        ivWallpaper = findViewById(R.id.ivWallpaper)
        ivWallpaperNext = findViewById(R.id.ivWallpaperNext)
        layoutLiveWallpaperMediaContainer = findViewById(R.id.layoutLiveWallpaperMediaContainer)
        surfaceLiveWallpaper = findViewById(R.id.surfaceLiveWallpaper)
        ivGifLiveWallpaper = findViewById(R.id.ivGifLiveWallpaper)
        viewLiveWallpaperBrightnessDim = findViewById(R.id.viewLiveWallpaperBrightnessDim)
        liveWallpaperCanvasView = findViewById(R.id.liveWallpaperCanvasView)

        // Slideshow controls
        layoutSlideshowControls = findViewById(R.id.layoutSlideshowControls)
        btnSlideshowPrev = findViewById(R.id.btnSlideshowPrev)
        btnSlideshowNext = findViewById(R.id.btnSlideshowNext)
        layoutSlideshowDots = findViewById(R.id.layoutSlideshowDots)

        // Top Left Clock, Weather & Date
        layoutTopLeft = findViewById(R.id.layoutTopLeft)
        tvDigitalClockTopLeft = findViewById(R.id.tvDigitalClockTopLeft)
        tvWeatherDate = findViewById(R.id.tvWeatherDate)
        tvWeatherInfo = findViewById(R.id.tvWeatherInfo)
        weatherAnimationView = findViewById(R.id.weatherAnimationView)
        analogClock = findViewById(R.id.analogClock)

        // Top Right
        layoutTopRightContainer = findViewById(R.id.layoutTopRightContainer)
        tvNetworkSpeed = findViewById(R.id.tvNetworkSpeed)
        tvTopIconTooltip = findViewById(R.id.tvTopIconTooltip)
        btnWifi = findViewById(R.id.btnWifi)
        btnMic = findViewById(R.id.btnMic)
        btnBluetooth = findViewById(R.id.btnBluetooth)
        viewBluetoothDot = findViewById(R.id.viewBluetoothDot)
        btnSettings = findViewById(R.id.btnSettings)

        // Bottom Horizontal Applications Row & Theme Hosts
        tvThemeShelfHeader = findViewById(R.id.tvThemeShelfHeader)
        rvBottomApps = findViewById(R.id.rvBottomApps)
        stubGoogleTvHost = findViewById(R.id.stubGoogleTvHost)
        stubNexfyHost = findViewById(R.id.stubNexfyHost)
        stubProjectivyHost = findViewById(R.id.stubProjectivyHost)
        stubAtvHost = findViewById(R.id.stubAtvHost)
        stubWolfHost = findViewById(R.id.stubWolfHost)

        // Right Sidebar
        layoutSidebarScrim = findViewById(R.id.layoutSidebarScrim)
        layoutRightSidebar = findViewById(R.id.layoutRightSidebar)
        tvSidebarTitle = findViewById(R.id.tvSidebarTitle)
        btnCloseSidebar = findViewById(R.id.btnCloseSidebar)
        layoutSidebarContent = findViewById(R.id.layoutSidebarContent)

        // Voice Search Overlay
        layoutVoiceSearchOverlay = findViewById(R.id.layoutVoiceSearchOverlay)
        tvVoiceTitle = findViewById(R.id.tvVoiceTitle)
        tvVoicePrompt = findViewById(R.id.tvVoicePrompt)
        tvVoiceResultText = findViewById(R.id.tvVoiceResultText)
        voiceWaveView = findViewById(R.id.voiceWaveView)
        btnStartListening = findViewById(R.id.btnStartListening)
        btnUseRemoteVoice = findViewById(R.id.btnUseRemoteVoice)
        btnCloseVoiceSearch = findViewById(R.id.btnCloseVoiceSearch)

        // Football Widget
        cardFootballSchedule = findViewById(R.id.cardFootballSchedule)
        btnRefreshFootballSchedule = findViewById(R.id.btnRefreshFootballSchedule)
        layoutFootballCenterSidebar = findViewById(R.id.layoutFootballCenterSidebar)
        itemLeagueEpl = findViewById(R.id.itemLeagueEpl)
        itemLeagueSerieA = findViewById(R.id.itemLeagueSerieA)
        itemLeagueLaLiga = findViewById(R.id.itemLeagueLaLiga)
        itemLeagueUcl = findViewById(R.id.itemLeagueUcl)
        ivLogoEpl = findViewById(R.id.ivLogoEpl)
        ivLogoSerieA = findViewById(R.id.ivLogoSerieA)
        ivLogoLaLiga = findViewById(R.id.ivLogoLaLiga)
        ivLogoUcl = findViewById(R.id.ivLogoUcl)

        layoutLeagueSchedulePanel = findViewById(R.id.layoutLeagueSchedulePanel)
        btnBackFromSchedule = findViewById(R.id.btnBackFromSchedule)
        ivScheduleLeagueLogo = findViewById(R.id.ivScheduleLeagueLogo)
        tvScheduleLeagueName = findViewById(R.id.tvScheduleLeagueName)
        tvScheduleDateTitle = findViewById(R.id.tvScheduleDateTitle)
        tvScheduleMatchCount = findViewById(R.id.tvScheduleMatchCount)
        pbScheduleLoading = findViewById(R.id.pbScheduleLoading)
        tvScheduleEmpty = findViewById(R.id.tvScheduleEmpty)
        rvFootballMatches = findViewById(R.id.rvFootballMatches)
    }

    private fun setupImmersiveTvMode() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    // ==========================================
    // UNIFIED WALLPAPER CONTROLLER (Only 1 Active)
    // ==========================================
    private fun applyWallpaperMode(mode: WallpaperMode) {
        currentWallpaperMode = mode
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WALLPAPER_MODE, mode.name)
            .apply()

        when (mode) {
            WallpaperMode.STATIC -> {
                storageLiveWallpaperManager.pause()
                layoutLiveWallpaperMediaContainer.visibility = View.GONE
                liveWallpaperCanvasView.stopAnimation()
                liveWallpaperCanvasView.visibility = View.GONE
                wallpaperSlideshowManager.setSlideshowEnabled(false)
                wallpaperSlideshowManager.stopSlideshow()
                layoutSlideshowControls.visibility = View.GONE

                ivWallpaper.visibility = View.VISIBLE
                ivWallpaper.alpha = 1.0f
                WallpaperHelper.loadWallpaper(this, ivWallpaper)
            }
            WallpaperMode.SLIDESHOW -> {
                storageLiveWallpaperManager.pause()
                layoutLiveWallpaperMediaContainer.visibility = View.GONE
                liveWallpaperCanvasView.stopAnimation()
                liveWallpaperCanvasView.visibility = View.GONE

                ivWallpaper.visibility = View.VISIBLE
                ivWallpaperNext.visibility = View.VISIBLE
                layoutSlideshowControls.visibility = View.GONE
                wallpaperSlideshowManager.setSlideshowEnabled(true)
                wallpaperSlideshowManager.startSlideshow(lifecycleScope)
            }
            WallpaperMode.CANVAS_LIVE -> {
                storageLiveWallpaperManager.pause()
                layoutLiveWallpaperMediaContainer.visibility = View.GONE
                wallpaperSlideshowManager.setSlideshowEnabled(false)
                wallpaperSlideshowManager.stopSlideshow()
                layoutSlideshowControls.visibility = View.GONE

                ivWallpaper.visibility = View.GONE
                ivWallpaperNext.visibility = View.GONE
                liveWallpaperCanvasView.visibility = View.VISIBLE
                liveWallpaperCanvasView.startAnimation()
            }
            WallpaperMode.STORAGE_LIVE -> {
                wallpaperSlideshowManager.setSlideshowEnabled(false)
                wallpaperSlideshowManager.stopSlideshow()
                layoutSlideshowControls.visibility = View.GONE
                liveWallpaperCanvasView.stopAnimation()
                liveWallpaperCanvasView.visibility = View.GONE

                ivWallpaper.visibility = View.GONE
                ivWallpaperNext.visibility = View.GONE
                layoutLiveWallpaperMediaContainer.visibility = View.VISIBLE
                storageLiveWallpaperManager.applyCurrentActive()
            }
        }
    }

    private fun setupSlideshowControls() {
        btnSlideshowPrev.setOnClickListener {
            wallpaperSlideshowManager.advanceToPreviousImage(animated = true)
        }
        btnSlideshowNext.setOnClickListener {
            wallpaperSlideshowManager.advanceToNextImage(animated = true)
        }
    }

    private fun updateSlideshowDots(current: Int, total: Int) {
        layoutSlideshowDots.removeAllViews()
        val displayTotal = total.coerceIn(1, 12)
        val density = resources.displayMetrics.density
        val dotSize = (6 * density).toInt()
        val dotMargin = (3 * density).toInt()

        for (i in 0 until displayTotal) {
            val dot = View(this)
            val params = LinearLayout.LayoutParams(dotSize, dotSize).apply {
                marginStart = dotMargin
                marginEnd = dotMargin
            }
            dot.layoutParams = params
            val active = (i == (current % displayTotal))
            val bg = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.OVAL
                setColor(if (active) Color.WHITE else Color.parseColor("#66FFFFFF"))
            }
            dot.background = bg
            layoutSlideshowDots.addView(dot)
        }
    }

    // ==========================================
    // TOP LEFT WEATHER & DATE
    // ==========================================
    private fun updateWeatherDate() {
        val now = Date()
        try {
            val timeSdf = SimpleDateFormat("HH : mm", Locale.getDefault())
            tvDigitalClockTopLeft.text = timeSdf.format(now)
        } catch (_: Exception) { }
        try {
            val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            val formatted = sdf.format(now)
            tvWeatherDate.text = formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("id", "ID")) else it.toString() }
        } catch (e: Exception) {
            tvWeatherDate.text = SimpleDateFormat("EEEE, d MMM yyyy", Locale.getDefault()).format(now)
        }
        val weatherStr = tvWeatherInfo.text?.toString() ?: ""
        googleTvController?.refreshClockAndWeather(weatherStr)
        nexfyController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherStr)
        projectivyController?.updateSystemStatus(isWifiOnline, currentWifiSsid, weatherStr)
        atvController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherStr)
        wolfController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherStr)
    }

    private fun fetchRealtimeWeather(forceRefresh: Boolean = false) {
        WeatherHelper.fetchWeather(
            context = this,
            textView = tvWeatherInfo,
            forceRefresh = forceRefresh,
            onWeatherCodeFetched = { weatherCode: Int, isDay: Int ->
                weatherAnimationView.setWeatherByCode(weatherCode, isDay)
                val weatherStr = tvWeatherInfo.text?.toString() ?: ""
                googleTvController?.refreshClockAndWeather(weatherStr)
                nexfyController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherStr)
                projectivyController?.updateSystemStatus(isWifiOnline, currentWifiSsid, weatherStr)
                atvController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherStr)
                wolfController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherStr, weatherCode, isDay)
            }
        )
    }

    private fun startPeriodicWeatherRefresh() {
        weatherRefreshJob?.cancel()
        weatherRefreshJob = lifecycleScope.launch {
            while (isActive) {
                delay(15 * 60 * 1000L)
                fetchRealtimeWeather(forceRefresh = true)
            }
        }
    }

    // ==========================================
    // NETWORK SPEED MONITOR (Real-time in Top Right)
    // ==========================================
    private fun startNetworkSpeedMonitor() {
        networkMonitorJob?.cancel()
        networkMonitorJob = lifecycleScope.launch(Dispatchers.IO) {
            var lastBytes = (TrafficStats.getTotalRxBytes().coerceAtLeast(0L) +
                TrafficStats.getTotalTxBytes().coerceAtLeast(0L))
            while (isActive) {
                delay(1500)
                val currentBytes = (TrafficStats.getTotalRxBytes().coerceAtLeast(0L) +
                    TrafficStats.getTotalTxBytes().coerceAtLeast(0L))
                val deltaBytes = (currentBytes - lastBytes).coerceAtLeast(0L)
                lastBytes = currentBytes
                val bytesPerSec = (deltaBytes / 1.5).toLong()

                val speedText = if (bytesPerSec < 1024 * 1024) {
                    String.format(Locale.US, "↓ %.1f KB/s", bytesPerSec / 1024.0)
                } else {
                    String.format(Locale.US, "↓ %.2f MB/s", bytesPerSec / (1024.0 * 1024.0))
                }

                withContext(Dispatchers.Main) {
                    tvNetworkSpeed.text = speedText
                    wolfController?.updateNetworkSpeed(speedText)
                }
            }
        }
    }

    // ==========================================
    // WIFI & BLUETOOTH LIVE STATUS MONITORS
    // ==========================================
    private fun initWifiStatusMonitor() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        refreshCurrentWifiStatus()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    refreshCurrentWifiStatus()
                    fetchRealtimeWeather(forceRefresh = false)
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread { refreshCurrentWifiStatus() }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                runOnUiThread { refreshCurrentWifiStatus() }
            }
        }
        networkCallback = callback

        try {
            val request = android.net.NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connectivityManager?.registerDefaultNetworkCallback(callback)
                }
            } catch (_: Exception) { }
        }
    }

    @Suppress("DEPRECATION")
    private fun refreshCurrentWifiStatus() {
        val cm = connectivityManager
        var online = false
        var ssidName: String? = null

        try {
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            if (caps != null && (
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    )
            ) {
                online = true
            }
            val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wm != null && wm.isWifiEnabled) {
                val info = wm.connectionInfo
                val rawSsid = info?.ssid?.replace("\"", "")?.trim()
                if (!rawSsid.isNullOrEmpty() && rawSsid != "<unknown ssid>") {
                    ssidName = rawSsid
                    online = true
                }
            }
            if (online && ssidName == null && caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) {
                ssidName = "Ethernet LAN"
            }
        } catch (_: Exception) { }

        isWifiOnline = online
        currentWifiSsid = ssidName
        updateWifiIconVisual()
    }

    private fun updateWifiIconVisual() {
        if (isWifiOnline) {
            btnWifi.setColorFilter(Color.parseColor("#38BDF8")) // Biru muda cerah saat terhubung
            btnWifi.alpha = 1.0f
        } else {
            btnWifi.setColorFilter(Color.parseColor("#64748B")) // Abu-abu redup saat offline
            btnWifi.alpha = 0.78f
        }
        if (btnWifi.isFocused) {
            tvTopIconTooltip.text = getWifiTooltipText()
        }
        val weatherNow = tvWeatherInfo.text?.toString() ?: ""
        nexfyController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherNow)
        projectivyController?.updateSystemStatus(isWifiOnline, currentWifiSsid, weatherNow)
        atvController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherNow)
        wolfController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherNow)
    }

    private fun getWifiTooltipText(): String {
        return if (isWifiOnline) {
            "WiFi: ${currentWifiSsid ?: "Terhubung"}"
        } else {
            "WiFi: Offline"
        }
    }

    private fun initBluetoothStatus(prefs: android.content.SharedPreferences) {
        isBluetoothOn = prefs.getBoolean(KEY_BLUETOOTH_ENABLED, true)
        try {
            val bm = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bm?.adapter
            if (adapter != null) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
                ) {
                    isBluetoothOn = adapter.isEnabled
                    prefs.edit().putBoolean(KEY_BLUETOOTH_ENABLED, isBluetoothOn).apply()
                }
            }
        } catch (_: Exception) { }
        updateBluetoothIconVisual()
    }

    private fun updateBluetoothIconVisual() {
        if (isBluetoothOn) {
            btnBluetooth.setColorFilter(Color.parseColor("#38BDF8")) // Biru muda terang saat aktif
            btnBluetooth.alpha = 1.0f
            viewBluetoothDot.visibility = View.VISIBLE
        } else {
            btnBluetooth.setColorFilter(Color.parseColor("#64748B")) // Abu-abu redup saat nonaktif
            btnBluetooth.alpha = 0.78f
            viewBluetoothDot.visibility = View.GONE
        }
        if (btnBluetooth.isFocused) {
            tvTopIconTooltip.text = if (isBluetoothOn) "Bluetooth: Aktif" else "Bluetooth: Nonaktif"
        }
        val weatherNow = tvWeatherInfo.text?.toString() ?: ""
        nexfyController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherNow)
        atvController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherNow)
        wolfController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherNow)
    }

    // ==========================================
    // TOP RIGHT BUTTONS & FOCUS / HOVER
    // ==========================================
    private fun setupTopRightIcons() {
        // Warna default ikon Mic & Gear putih bersih
        btnMic.setColorFilter(Color.WHITE)
        btnSettings.setColorFilter(Color.WHITE)

        val buttons = listOf(btnMic, btnWifi, btnBluetooth, btnSettings)

        val showTooltipFor: (View) -> Unit = { v ->
            val tooltip = when (v) {
                btnMic -> if (isVoiceListening) "Hentikan Pencarian Suara" else "Pencarian Suara"
                btnWifi -> getWifiTooltipText()
                btnBluetooth -> if (isBluetoothOn) "Bluetooth: Aktif" else "Bluetooth: Nonaktif"
                btnSettings -> "Pengaturan Launcher"
                else -> ""
            }
            if (tooltip.isNotEmpty()) {
                tvTopIconTooltip.text = tooltip
                tvTopIconTooltip.visibility = View.VISIBLE
            }
        }

        for (button in buttons) {
            button.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.14f).scaleY(1.14f).setDuration(160).start()
                    v.elevation = 12f
                    showTooltipFor(v)

                    if (v == btnSettings) {
                        settingsGearAnimator?.cancel()
                        settingsGearAnimator = ObjectAnimator.ofFloat(
                            v,
                            "rotation",
                            v.rotation,
                            v.rotation + 360f
                        ).apply {
                            duration = 4500L
                            repeatCount = ObjectAnimator.INFINITE
                            interpolator = LinearInterpolator()
                            start()
                        }
                    }
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(160).start()
                    v.elevation = 3f * resources.displayMetrics.density
                    if (!buttons.any { it.isFocused }) {
                        tvTopIconTooltip.visibility = View.GONE
                    }
                    if (v == btnSettings) {
                        settingsGearAnimator?.cancel()
                        settingsGearAnimator = null
                    }
                }
            }

            button.setOnHoverListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_HOVER_ENTER -> {
                        showTooltipFor(v)
                        if (v == btnSettings && settingsGearAnimator == null) {
                            settingsGearAnimator = ObjectAnimator.ofFloat(
                                v,
                                "rotation",
                                v.rotation,
                                v.rotation + 360f
                            ).apply {
                                duration = 4500L
                                repeatCount = ObjectAnimator.INFINITE
                                interpolator = LinearInterpolator()
                                start()
                            }
                        }
                    }
                    android.view.MotionEvent.ACTION_HOVER_EXIT -> {
                        if (!v.isFocused) {
                            tvTopIconTooltip.visibility = View.GONE
                            if (v == btnSettings) {
                                settingsGearAnimator?.cancel()
                                settingsGearAnimator = null
                            }
                        }
                    }
                }
                false
            }

            button.setOnClickListener {
                lastFocusedTopButton = button
                when (button) {
                    btnMic -> {
                        if (isSidebarOpen) closeSidebar()
                        if (isVoiceListening) {
                            stopListeningOnly()
                        } else {
                            openVoiceSearchOverlay()
                        }
                    }
                    btnWifi -> openSidebar(SidebarType.WIFI)
                    btnBluetooth -> openSidebar(SidebarType.BLUETOOTH)
                    btnSettings -> openSidebar(SidebarType.SETTINGS)
                }
            }
        }
    }

    // ==========================================
    // SLIDING RIGHT SIDEBAR (0.3s Slide-in / Slide-out)
    // ==========================================
    enum class SidebarType { SETTINGS, WIFI, BLUETOOTH }

    private fun setupSidebarActions() {
        btnCloseSidebar.setOnClickListener {
            closeSidebar()
        }
        layoutSidebarScrim.setOnClickListener {
            closeSidebar()
        }
    }

    private fun openSidebar(type: SidebarType) {
        if (layoutVoiceSearchOverlay.visibility == View.VISIBLE) {
            closeVoiceSearchOverlay()
        }

        // Hanya 1 sidebar terbuka; jika klik ikon lain saat sidebar terbuka, isi langsung berganti tanpa menutup dulu
        if (isSidebarOpen) {
            if (currentSidebarType == type) {
                populateSidebarContent(type)
                return
            }
            currentSidebarType = type
            populateSidebarContent(type)
            return
        }

        isSidebarOpen = true
        currentSidebarType = type
        populateSidebarContent(type)

        layoutSidebarScrim.visibility = View.VISIBLE
        layoutSidebarScrim.alpha = 0f
        layoutSidebarScrim.animate().alpha(1f).setDuration(300).start()

        val slideWidth = 420f * resources.displayMetrics.density
        layoutRightSidebar.translationX = slideWidth
        layoutRightSidebar.animate()
            .translationX(0f)
            .setDuration(300)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                btnCloseSidebar.requestFocus()
            }
            .start()
    }

    private fun populateSidebarContent(type: SidebarType, focusChildIndex: Int = -1) {
        currentSidebarType = type
        layoutSidebarContent.removeAllViews()
        when (type) {
            SidebarType.SETTINGS -> {
                tvSidebarTitle.text = "Pengaturan Launcher"
                buildSettingsSidebarContent()
            }
            SidebarType.WIFI -> {
                tvSidebarTitle.text = "Jaringan WiFi"
                buildWifiSidebarContent()
            }
            SidebarType.BLUETOOTH -> {
                tvSidebarTitle.text = "Perangkat Bluetooth"
                buildBluetoothSidebarContent()
            }
        }
        if (focusChildIndex >= 0) {
            layoutSidebarContent.post {
                val target = layoutSidebarContent.getChildAt(focusChildIndex.coerceIn(0, (layoutSidebarContent.childCount - 1).coerceAtLeast(0)))
                target?.requestFocus()
            }
        }
    }

    private fun closeSidebar(restoreFocus: Boolean = true) {
        if (!isSidebarOpen) return
        val slideWidth = 420f * resources.displayMetrics.density
        layoutSidebarScrim.animate().alpha(0f).setDuration(300).start()
        layoutRightSidebar.animate()
            .translationX(slideWidth)
            .setDuration(300)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                layoutSidebarScrim.visibility = View.GONE
                layoutSidebarScrim.alpha = 1f
                isSidebarOpen = false
                currentSidebarType = null
                if (restoreFocus) {
                    if (isCustomHomeLayoutActive(this@MainActivity)) {
                        focusPrimaryItemForActiveTheme()
                    } else {
                        lastFocusedTopButton?.requestFocus()
                    }
                }
            }
            .start()
    }

    private fun createSidebarButton(text: String, onClick: () -> Unit): Button {
        val density = resources.displayMetrics.density
        return Button(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 12f
            isAllCaps = false
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding((14 * density).toInt(), 0, (14 * density).toInt(), 0)
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_top_icon_focus)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (44 * density).toInt()
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
            isFocusable = true
            isClickable = true
            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.02f else 1.0f).scaleY(if (hasFocus) 1.02f else 1.0f).setDuration(120).start()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun createSidebarRowCard(
        title: String,
        subtitle: String,
        actionLabel: String,
        isConnectedAction: Boolean = false,
        onActionClick: () -> Unit,
        secondaryDeleteClick: (() -> Unit)? = null
    ): LinearLayout {
        val density = resources.displayMetrics.density
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((12 * density).toInt(), (10 * density).toInt(), (10 * density).toInt(), (10 * density).toInt())
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_top_icon_focus)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (8 * density).toInt()
            }
        }

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val tvTitle = TextView(this).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val tvSub = TextView(this).apply {
            text = subtitle
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 10f
            maxLines = 1
        }
        textCol.addView(tvTitle)
        textCol.addView(tvSub)
        card.addView(textCol)

        val btnAction = Button(this).apply {
            text = actionLabel
            setTextColor(Color.WHITE)
            textSize = 10f
            isAllCaps = false
            setPadding((10 * density).toInt(), 0, (10 * density).toInt(), 0)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(if (isConnectedAction) Color.parseColor("#B91C1C") else Color.parseColor("#2563EB"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (34 * density).toInt()
            ).apply {
                marginStart = (8 * density).toInt()
            }
            isFocusable = true
            isClickable = true
            setOnClickListener { onActionClick() }
        }
        card.addView(btnAction)

        if (secondaryDeleteClick != null) {
            val btnDel = Button(this).apply {
                text = "✕"
                setTextColor(Color.parseColor("#FCA5A5"))
                textSize = 11f
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 10f * density
                    setColor(Color.parseColor("#44EF4444"))
                }
                layoutParams = LinearLayout.LayoutParams(
                    (34 * density).toInt(),
                    (34 * density).toInt()
                ).apply {
                    marginStart = (6 * density).toInt()
                }
                isFocusable = true
                isClickable = true
                setOnClickListener { secondaryDeleteClick() }
            }
            card.addView(btnDel)
        }

        return card
    }

    private fun buildSettingsSidebarContent() {
        val ctx = this
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val density = resources.displayMetrics.density

        // Tampilan Layar -> Tema & Ikon + Opsi Khusus Google TV + App Drawer + Widget Header
        layoutSidebarContent.addView(createSectionHeader("TAMPILAN LAYAR"))
        val currentTheme = ThemeManager.getActiveTheme(ctx)
        layoutSidebarContent.addView(createSidebarButton("🎨 Tema & Ikon (${currentTheme.name})") {
            openThemeAndIconSettings()
        })
        if (currentTheme.id == ThemeId.GOOGLE_TV) {
            val isGtvHomeOn = ThemeManager.isGoogleTvUseHomeScreenDefault(ctx)
            layoutSidebarContent.addView(createSidebarButton(
                if (isGtvHomeOn) "✓ Use Home Screen Default: ON" else "○ Use Home Screen Default: OFF"
            ) {
                val next = !isGtvHomeOn
                ThemeManager.setGoogleTvUseHomeScreenDefault(ctx, next)
                applyActiveLauncherTheme(theme = currentTheme, focusFirst = false)
                populateSidebarContent(SidebarType.SETTINGS)
                Toast.makeText(
                    ctx,
                    if (next) "Google TV Home Screen: ON" else "Kembali ke Tata Letak Dasar Aero TV: OFF",
                    Toast.LENGTH_SHORT
                ).show()
            })
        } else if (currentTheme.id == ThemeId.NEXFY) {
            val isNexfyHomeOn = ThemeManager.isNexfyUseHomeScreenDefault(ctx)
            layoutSidebarContent.addView(createSidebarButton(
                if (isNexfyHomeOn) "✓ Use Home Screen Default: ON" else "○ Use Home Screen Default: OFF"
            ) {
                val next = !isNexfyHomeOn
                ThemeManager.setNexfyUseHomeScreenDefault(ctx, next)
                applyActiveLauncherTheme(theme = currentTheme, focusFirst = false)
                populateSidebarContent(SidebarType.SETTINGS)
                Toast.makeText(
                    ctx,
                    if (next) "Nexfy Home Screen: ON" else "Kembali ke Tata Letak Dasar Aero TV: OFF",
                    Toast.LENGTH_SHORT
                ).show()
            })
        } else if (currentTheme.id == ThemeId.PROJECTIVY) {
            val isProjectivyHomeOn = ThemeManager.isProjectivyUseHomeScreenDefault(ctx)
            layoutSidebarContent.addView(createSidebarButton(
                if (isProjectivyHomeOn) "✓ Use Home Screen Default: ON" else "○ Use Home Screen Default: OFF"
            ) {
                val next = !isProjectivyHomeOn
                ThemeManager.setProjectivyUseHomeScreenDefault(ctx, next)
                applyActiveLauncherTheme(theme = currentTheme, focusFirst = false)
                populateSidebarContent(SidebarType.SETTINGS)
                Toast.makeText(
                    ctx,
                    if (next) "Projectivy Home Screen: ON" else "Kembali ke Tata Letak Dasar Aero TV: OFF",
                    Toast.LENGTH_SHORT
                ).show()
            })
        } else if (currentTheme.id == ThemeId.ATV_LAUNCHER) {
            val isAtvHomeOn = ThemeManager.isAtvUseHomeScreenDefault(ctx)
            layoutSidebarContent.addView(createSidebarButton(
                if (isAtvHomeOn) "✓ Use Home Screen Default: ON" else "○ Use Home Screen Default: OFF"
            ) {
                val next = !isAtvHomeOn
                ThemeManager.setAtvUseHomeScreenDefault(ctx, next)
                applyActiveLauncherTheme(theme = currentTheme, focusFirst = false)
                populateSidebarContent(SidebarType.SETTINGS)
                Toast.makeText(
                    ctx,
                    if (next) "ATV Launcher Home Screen: ON" else "Kembali ke Tata Letak Dasar Aero TV: OFF",
                    Toast.LENGTH_SHORT
                ).show()
            })
            layoutSidebarContent.addView(createSidebarButton("⚙ Pengaturan Kartu ATV") {
                closeSidebar(restoreFocus = false)
                AtvCardSettingsDialog(ctx) {
                    applyActiveLauncherTheme(theme = currentTheme, focusFirst = false)
                }.show()
            })
        } else if (currentTheme.id == ThemeId.WOLF_LAUNCHER) {
            val isWolfHomeOn = ThemeManager.isWolfUseHomeScreenDefault(ctx)
            layoutSidebarContent.addView(createSidebarButton(
                if (isWolfHomeOn) "✓ Use Home Screen Default: ON" else "○ Use Home Screen Default: OFF"
            ) {
                val next = !isWolfHomeOn
                ThemeManager.setWolfUseHomeScreenDefault(ctx, next)
                applyActiveLauncherTheme(theme = currentTheme, focusFirst = false)
                populateSidebarContent(SidebarType.SETTINGS)
                Toast.makeText(
                    ctx,
                    if (next) "Wolf Launcher Home Screen: ON" else "Kembali ke Tata Letak Dasar Aero TV: OFF",
                    Toast.LENGTH_SHORT
                ).show()
            })
            layoutSidebarContent.addView(createSidebarButton("⚙ Pengaturan Tampilan Wolf") {
                closeSidebar(restoreFocus = false)
                WolfDisplaySettingsDialog(ctx) {
                    applyActiveLauncherTheme(theme = currentTheme, focusFirst = false)
                }.show()
            })
        }
        layoutSidebarContent.addView(createSidebarButton("📱 Buka App Drawer (Tema Aktif)") {
            closeSidebar(restoreFocus = false)
            openThemedAppDrawer()
        })

        // Widget Header (Jam Analog Toggle)
        layoutSidebarContent.addView(createSectionHeader("WIDGET HEADER"))
        val showAnalog = prefs.getBoolean("key_show_analog_clock", true)
        layoutSidebarContent.addView(createSidebarButton(
            if (showAnalog) "✓ Jam Analog Header: AKTIF" else "❌ Jam Analog Header: MATI"
        ) {
            val next = !showAnalog
            prefs.edit().putBoolean("key_show_analog_clock", next).apply()
            analogClock.visibility = if (next && !isCustomHomeLayoutActive(ctx)) View.VISIBLE else View.GONE
            populateSidebarContent(SidebarType.SETTINGS)
            Toast.makeText(ctx, "Jam Analog Header ${if (next) "Diaktifkan" else "Dimatikan"}", Toast.LENGTH_SHORT).show()
        })

        // 1. Wallpaper Foto Statis
        layoutSidebarContent.addView(createSectionHeader("WALLPAPER UTAMA"))
        layoutSidebarContent.addView(createSidebarButton(
            if (currentWallpaperMode == WallpaperMode.STATIC) "✓ Wallpaper Foto Statis (Aktif)" else "🖼️ Wallpaper Foto Statis"
        ) {
            applyWallpaperMode(WallpaperMode.STATIC)
            populateSidebarContent(SidebarType.SETTINGS)
            Toast.makeText(ctx, "Mode Wallpaper Statis Diaktifkan", Toast.LENGTH_SHORT).show()
        })
        layoutSidebarContent.addView(createSidebarButton("📁 Pilih Foto Statis dari Penyimpanan") {
            wallpaperPickerLauncher.launch(WallpaperHelper.createWallpaperPickerIntent())
        })
        layoutSidebarContent.addView(createSidebarButton("🔄 Reset Wallpaper Utama (Foto Bawaan)") {
            applyWallpaperMode(WallpaperMode.STATIC)
            WallpaperHelper.resetToDefaultWallpaper(ctx, ivWallpaper)
            Toast.makeText(ctx, "Wallpaper dikembalikan ke foto bawaan", Toast.LENGTH_SHORT).show()
        })

        // 2. Wallpaper Slideshow
        layoutSidebarContent.addView(createSectionHeader("WALLPAPER SLIDESHOW"))
        val slideshowStatusText = if (currentWallpaperMode == WallpaperMode.SLIDESHOW) {
            "✓ Slideshow Aktif (${wallpaperSlideshowManager.getSavedFolderName()})"
        } else {
            "🎞️ Aktifkan Slideshow Wallpaper"
        }
        layoutSidebarContent.addView(createSidebarButton(slideshowStatusText) {
            if (wallpaperSlideshowManager.getSavedFolderUri().isNullOrEmpty()) {
                showFolderSelectionOptionsDialog()
            } else {
                applyWallpaperMode(WallpaperMode.SLIDESHOW)
                populateSidebarContent(SidebarType.SETTINGS)
            }
        })
        layoutSidebarContent.addView(createSidebarButton("📂 Pilih Folder Foto Slideshow") {
            showFolderSelectionOptionsDialog()
        })
        val currentIntervalLabel = when (wallpaperSlideshowManager.getIntervalSeconds()) {
            10 -> "10 Detik"
            30 -> "30 Detik"
            180 -> "3 Menit"
            else -> "1 Menit"
        }
        layoutSidebarContent.addView(createSidebarButton("⏱️ Interval Pergantian: $currentIntervalLabel") {
            showIntervalPickerDialog()
        })
        val isRandomOrder = wallpaperSlideshowManager.getOrderMode() == WallpaperSlideshowManager.MODE_RANDOM
        layoutSidebarContent.addView(createSidebarButton(
            if (isRandomOrder) "🔀 Urutan Slideshow: Acak (Shuffle)" else "➡️ Urutan Slideshow: Berurutan"
        ) {
            val nextOrder = if (isRandomOrder) {
                WallpaperSlideshowManager.MODE_SEQUENTIAL
            } else {
                WallpaperSlideshowManager.MODE_RANDOM
            }
            wallpaperSlideshowManager.setOrderMode(nextOrder)
            populateSidebarContent(SidebarType.SETTINGS)
            Toast.makeText(ctx, "Urutan diubah ke ${if (nextOrder == WallpaperSlideshowManager.MODE_RANDOM) "Acak" else "Berurutan"}", Toast.LENGTH_SHORT).show()
        })
        layoutSidebarContent.addView(createSidebarButton("✨ Efek Transisi: Fade / Slide / Zoom") {
            showTransitionEffectPickerDialog()
        })

        // 3. Live Wallpaper (Canvas Bawaan & Penyimpanan MP4/GIF)
        layoutSidebarContent.addView(createSectionHeader("LIVE WALLPAPER BAWAAN & PENYIMPANAN"))
        layoutSidebarContent.addView(createSidebarButton("+ Tambah Live Wallpaper Video / GIF (MP4/GIF)") {
            liveWallpaperPickerLauncher.launch(storageLiveWallpaperManager.createMediaPickerIntent())
        })

        val canvasOptions = listOf(
            Pair(LiveWallpaperCanvasView.LiveType.CAMPFIRE, "🔥 Api Unggun (Senja Pantai)"),
            Pair(LiveWallpaperCanvasView.LiveType.RAIN, "🌧️ Hujan (Danau Toba)"),
            Pair(LiveWallpaperCanvasView.LiveType.GALAXY, "🌌 Galaksi / Bintang Berkelap-kelip"),
            Pair(LiveWallpaperCanvasView.LiveType.CLOUDS, "☁️ Awan Bergerak")
        )
        for ((type, label) in canvasOptions) {
            val isActive = (currentWallpaperMode == WallpaperMode.CANVAS_LIVE && liveWallpaperCanvasView.liveType == type)
            layoutSidebarContent.addView(createSidebarButton(if (isActive) "✓ $label (Aktif)" else label) {
                liveWallpaperCanvasView.liveType = type
                applyWallpaperMode(WallpaperMode.CANVAS_LIVE)
                populateSidebarContent(SidebarType.SETTINGS)
                Toast.makeText(ctx, "Live wallpaper $label diaktifkan!", Toast.LENGTH_SHORT).show()
            })
        }

        // Kontrol Kecepatan & Kerapatan Partikel Canvas Live Wallpaper
        layoutSidebarContent.addView(createSidebarButton(
            "⚡ Kecepatan Animasi Canvas: ${String.format(Locale.US, "%.1fx", liveWallpaperCanvasView.animSpeedMultiplier)}"
        ) {
            val speeds = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f)
            val nextIdx = (speeds.indexOfFirst { kotlin.math.abs(it - liveWallpaperCanvasView.animSpeedMultiplier) < 0.1f } + 1) % speeds.size
            liveWallpaperCanvasView.animSpeedMultiplier = speeds[nextIdx]
            populateSidebarContent(SidebarType.SETTINGS)
        })
        layoutSidebarContent.addView(createSidebarButton(
            "✨ Kerapatan Partikel Canvas: ${String.format(Locale.US, "%.1fx", liveWallpaperCanvasView.particleDensityMultiplier)}"
        ) {
            val densities = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f)
            val nextIdx = (densities.indexOfFirst { kotlin.math.abs(it - liveWallpaperCanvasView.particleDensityMultiplier) < 0.1f } + 1) % densities.size
            liveWallpaperCanvasView.particleDensityMultiplier = densities[nextIdx]
            populateSidebarContent(SidebarType.SETTINGS)
        })

        // Kontrol Storage Live Wallpaper (MP4 / GIF)
        val savedLiveItems = storageLiveWallpaperManager.getSavedItems()
        if (savedLiveItems.isNotEmpty()) {
            layoutSidebarContent.addView(createSectionHeader("DAFTAR LIVE WALLPAPER PENYIMPANAN"))
            val activeItemUri = storageLiveWallpaperManager.getActiveItem()?.uriString
            for (item in savedLiveItems) {
                val isItemActive = (currentWallpaperMode == WallpaperMode.STORAGE_LIVE && activeItemUri == item.uriString)
                layoutSidebarContent.addView(
                    createSidebarRowCard(
                        title = "${if (isItemActive) "✓ " else ""}${item.fileName}",
                        subtitle = if (item.isVideo) "Video MP4/WEBM" else "Animasi GIF",
                        actionLabel = if (isItemActive) "Aktif" else "Pakai",
                        isConnectedAction = false,
                        onActionClick = {
                            storageLiveWallpaperManager.setActiveItem(item)
                            applyWallpaperMode(WallpaperMode.STORAGE_LIVE)
                            populateSidebarContent(SidebarType.SETTINGS)
                        },
                        secondaryDeleteClick = {
                            storageLiveWallpaperManager.deleteItem(item)
                            if (storageLiveWallpaperManager.getSavedItems().isEmpty() && currentWallpaperMode == WallpaperMode.STORAGE_LIVE) {
                                applyWallpaperMode(WallpaperMode.STATIC)
                            }
                            populateSidebarContent(SidebarType.SETTINGS)
                            Toast.makeText(ctx, "Dihapus dari daftar", Toast.LENGTH_SHORT).show()
                        }
                    )
                )
            }

            // Kontrol Playback Video/GIF (Kecepatan, Mode Tampilan, Kecerahan, Mute)
            val currentSpeed = storageLiveWallpaperManager.getSpeed()
            layoutSidebarContent.addView(createSidebarButton("⏩ Kecepatan Video: ${currentSpeed}x") {
                val speeds = floatArrayOf(0.5f, 1.0f, 1.5f, 2.0f)
                val next = speeds[(speeds.indexOfFirst { kotlin.math.abs(it - currentSpeed) < 0.1f } + 1) % speeds.size]
                storageLiveWallpaperManager.setSpeed(next)
                populateSidebarContent(SidebarType.SETTINGS)
            })

            val displayMode = storageLiveWallpaperManager.getDisplayMode()
            layoutSidebarContent.addView(createSidebarButton("📐 Mode Tampilan Video/GIF: ${displayMode.uppercase()}") {
                val modes = listOf(
                    StorageLiveWallpaperManager.MODE_COVER,
                    StorageLiveWallpaperManager.MODE_CONTAIN,
                    StorageLiveWallpaperManager.MODE_STRETCH
                )
                val next = modes[(modes.indexOf(displayMode) + 1) % modes.size]
                storageLiveWallpaperManager.setDisplayMode(next)
                populateSidebarContent(SidebarType.SETTINGS)
            })

            val isMuted = storageLiveWallpaperManager.getIsMuted()
            layoutSidebarContent.addView(createSidebarButton(
                if (isMuted) "🔇 Suara Video Wallpaper: Mute" else "🔊 Suara Video Wallpaper: Aktif"
            ) {
                storageLiveWallpaperManager.setIsMuted(!isMuted)
                populateSidebarContent(SidebarType.SETTINGS)
            })

            val brightnessVal = storageLiveWallpaperManager.getBrightness()
            val tvBrightLabel = TextView(ctx).apply {
                text = "☀️ Kecerahan Video/GIF: $brightnessVal%"
                setTextColor(Color.parseColor("#E2E8F0"))
                textSize = 11f
                setPadding(0, (4 * density).toInt(), 0, (2 * density).toInt())
            }
            val seekBrightness = SeekBar(ctx).apply {
                max = 100
                progress = brightnessVal
                isFocusable = true
                setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            storageLiveWallpaperManager.setBrightness(progress)
                            tvBrightLabel.text = "☀️ Kecerahan Video/GIF: $progress%"
                        }
                    }
                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }
            layoutSidebarContent.addView(tvBrightLabel)
            layoutSidebarContent.addView(seekBrightness)
        }

        // 4. Cuaca Lokasi Saat Ini & Test Animasi Cuaca
        layoutSidebarContent.addView(createSectionHeader("CUACA & LOKASI PERANGKAT"))
        layoutSidebarContent.addView(createSidebarButton("🔄 Deteksi Lokasi & Cuaca Saat Ini") {
            val hasFine = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasCoarse = ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (!hasFine && !hasCoarse) {
                try {
                    storagePermissionLauncher.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                } catch (_: Exception) { }
            }
            WeatherHelper.invalidateCache()
            tvWeatherInfo.text = "Mendeteksi lokasi..."
            fetchRealtimeWeather(forceRefresh = true)
            Toast.makeText(ctx, "Memperbarui lokasi kecamatan & cuaca...", Toast.LENGTH_SHORT).show()
        })
        val weatherTests = listOf(
            Pair(WeatherAnimationView.WeatherType.RAIN, "🌧️ Hujan (Splash & Drops)"),
            Pair(WeatherAnimationView.WeatherType.SUNNY, "☀️ Cerah (Matahari & Glow)"),
            Pair(WeatherAnimationView.WeatherType.CLOUDY, "☁️ Berawan / Mendung"),
            Pair(WeatherAnimationView.WeatherType.THUNDERSTORM, "⚡ Badai Petir"),
            Pair(WeatherAnimationView.WeatherType.FOGGY, "🌫️ Berkabut")
        )
        for ((wType, wLabel) in weatherTests) {
            layoutSidebarContent.addView(createSidebarButton(wLabel) {
                weatherAnimationView.setWeatherType(wType)
                val district = WeatherHelper.getCachedDistrictName(ctx) ?: "Lokasi"
                val weatherSummary = "$district • ${wLabel.substring(3)}"
                tvWeatherInfo.text = weatherSummary
                googleTvController?.refreshClockAndWeather(weatherSummary)
                nexfyController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherSummary)
                projectivyController?.updateSystemStatus(isWifiOnline, currentWifiSsid, weatherSummary)
                atvController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherSummary)
                wolfController?.setWeatherAnimationType(wType)
                wolfController?.updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, weatherSummary)
                Toast.makeText(ctx, "Simulasi animasi: $wLabel", Toast.LENGTH_SHORT).show()
            })
        }

        // 5. Widget Kickoff Bola
        layoutSidebarContent.addView(createSectionHeader("JADWAL LIGA SEPAKBOLA"))
        val toggleKickoffText = if (isKickoffWidgetVisible) "❌ Sembunyikan Jadwal Liga" else "⚽ Tampilkan Jadwal Liga"
        layoutSidebarContent.addView(createSidebarButton(toggleKickoffText) {
            isKickoffWidgetVisible = !isKickoffWidgetVisible
            prefs.edit().putBoolean(KEY_SHOW_KICKOFF, isKickoffWidgetVisible).apply()
            cardFootballSchedule.visibility = if (isKickoffWidgetVisible && !isCustomHomeLayoutActive(ctx)) View.VISIBLE else View.GONE
            populateSidebarContent(SidebarType.SETTINGS)
        })
        layoutSidebarContent.addView(createSidebarButton("🔄 Segarkan Jadwal Bola Sekarang") {
            refreshFootballSchedule()
            Toast.makeText(ctx, "Memperbarui jadwal liga...", Toast.LENGTH_SHORT).show()
            closeSidebar()
        })

        // 6. Jam Analog Wallpaper
        layoutSidebarContent.addView(createSectionHeader("JAM ANALOG"))
        layoutSidebarContent.addView(createSidebarButton("🕐 Ganti Gambar Jam Analog") {
            clockWallpaperPickerLauncher.launch(WallpaperHelper.createWallpaperPickerIntent())
        })
        layoutSidebarContent.addView(createSidebarButton("🔄 Reset Jam Analog") {
            WallpaperHelper.resetClockWallpaper(ctx, analogClock)
            Toast.makeText(ctx, "Jam analog direset", Toast.LENGTH_SHORT).show()
        })

        // 7. Kecepatan Jaringan Overlay & Baris Aplikasi
        layoutSidebarContent.addView(createSectionHeader("TAMPILAN & OVERLAY JARINGAN"))
        val hasOverlayPerm = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ctx)
        val overlayEnabled = prefs.getBoolean(KEY_OVERLAY_SPEED_ENABLED, true)
        val overlayBtnText = when {
            !hasOverlayPerm -> "🌐 Aktifkan Izin Overlay Kecepatan (YouTube/Netflix)"
            overlayEnabled -> "✓ Kecepatan Jaringan di Semua App (Aktif)"
            else -> "🌐 Aktifkan Kecepatan Jaringan di Semua App"
        }
        layoutSidebarContent.addView(createSidebarButton(overlayBtnText) {
            toggleOverlaySpeedPermission()
        })

        // 8. Pengaturan Sistem TV & Launcher Default
        layoutSidebarContent.addView(createSectionHeader("SISTEM & LAUNCHER DEFAULT"))
        layoutSidebarContent.addView(createSidebarButton("🏠 Jadikan Launcher Default") {
            showSetDefaultLauncherGuideAndTrigger()
        })
        layoutSidebarContent.addView(createSidebarButton("📺 Matikan Tampilan Bawaan STB") {
            openDisableStockLauncherDialog()
        })
        layoutSidebarContent.addView(createSidebarButton("💾 Sistem & Cadangan") {
            showSystemAndBackupDialog()
        })
        layoutSidebarContent.addView(createSidebarButton("⚙️ Pengaturan Android STB") {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        })
    }

    @Suppress("DEPRECATION")
    private fun buildWifiSidebarContent() {
        refreshCurrentWifiStatus()
        val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val wifiEnabled = try { wm?.isWifiEnabled ?: isWifiOnline } catch (_: Exception) { isWifiOnline }

        layoutSidebarContent.addView(createSectionHeader("STATUS KONEKSI"))
        val statusText = TextView(this).apply {
            text = when {
                isWifiOnline && !currentWifiSsid.isNullOrEmpty() -> "● Terhubung ke: $currentWifiSsid"
                isWifiOnline -> "● Terhubung ke Internet"
                wifiEnabled -> "○ WiFi Aktif (Tidak Terhubung)"
                else -> "○ WiFi Nonaktif (Offline)"
            }
            setTextColor(if (isWifiOnline) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, (12 * resources.displayMetrics.density).toInt())
        }
        layoutSidebarContent.addView(statusText)

        // Tombol Toggle On/Off WiFi + Refresh
        layoutSidebarContent.addView(createSidebarButton(
            if (wifiEnabled) "📡 Matikan / Kelola WiFi" else "📡 Aktifkan WiFi"
        ) {
            var toggledLocally = false
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wm != null) {
                    toggledLocally = wm.setWifiEnabled(!wifiEnabled)
                }
            } catch (_: Exception) { }

            if (toggledLocally) {
                Handler(Looper.getMainLooper()).postDelayed({
                    refreshCurrentWifiStatus()
                    if (isSidebarOpen && currentSidebarType == SidebarType.WIFI) {
                        populateSidebarContent(SidebarType.WIFI)
                    }
                }, 600)
            } else {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        startActivity(Intent(Settings.Panel.ACTION_WIFI))
                    } else {
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                } catch (e: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        })

        layoutSidebarContent.addView(createSidebarButton("🔄 Segarkan Daftar WiFi") {
            try {
                wm?.startScan()
            } catch (_: Exception) { }
            refreshCurrentWifiStatus()
            populateSidebarContent(SidebarType.WIFI)
            Toast.makeText(this, "Daftar jaringan WiFi diperbarui", Toast.LENGTH_SHORT).show()
        })

        // Daftar WiFi Tersedia
        layoutSidebarContent.addView(createSectionHeader("JARINGAN WIFI TERSEDIA"))

        val scanEntries = mutableListOf<Triple<String, String, Boolean>>() // SSID, Signal/Security, IsConnected
        val activeSsid = currentWifiSsid
        if (isWifiOnline && !activeSsid.isNullOrEmpty()) {
            scanEntries.add(Triple(activeSsid, "Terhubung • Sinyal Kuat", true))
        }

        try {
            val canReadScan = ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            if (canReadScan && wm != null) {
                val results = wm.scanResults
                    .filter { !it.SSID.isNullOrBlank() }
                    .distinctBy { it.SSID }
                    .sortedByDescending { it.level }
                for (sr in results) {
                    if (sr.SSID != activeSsid) {
                        val levelDesc = when {
                            sr.level >= -55 -> "Sinyal Kuat"
                            sr.level >= -75 -> "Sinyal Sedang"
                            else -> "Sinyal Lemah"
                        }
                        val secured = sr.capabilities.contains("WPA") || sr.capabilities.contains("WEP")
                        val secText = if (secured) "Terkunci" else "Terbuka"
                        scanEntries.add(Triple(sr.SSID, "$levelDesc • $secText", false))
                    }
                }
            }
        } catch (_: Exception) { }

        if (scanEntries.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Tidak ada SSID terpindai langsung atau izin lokasi belum diberikan. Buka Pengaturan WiFi Sistem untuk menyambung."
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 11f
                setPadding(0, 4, 0, 12)
            }
            layoutSidebarContent.addView(tvEmpty)
        } else {
            for ((ssid, info, isConn) in scanEntries.take(8)) {
                layoutSidebarContent.addView(
                    createSidebarRowCard(
                        title = ssid,
                        subtitle = info,
                        actionLabel = if (isConn) "Putuskan" else "Sambungkan",
                        isConnectedAction = isConn,
                        onActionClick = {
                            if (isConn) {
                                var disconnected = false
                                try {
                                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && wm != null) {
                                        disconnected = wm.disconnect()
                                    }
                                } catch (_: Exception) { }
                                if (disconnected) {
                                    Toast.makeText(this, "Memutuskan koneksi $ssid...", Toast.LENGTH_SHORT).show()
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        refreshCurrentWifiStatus()
                                        if (isSidebarOpen && currentSidebarType == SidebarType.WIFI) {
                                            populateSidebarContent(SidebarType.WIFI)
                                        }
                                    }, 600)
                                } else {
                                    try {
                                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                    } catch (_: Exception) { }
                                }
                            } else {
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        startActivity(Intent(Settings.Panel.ACTION_WIFI))
                                    } else {
                                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                                    }
                                } catch (_: Exception) {
                                    startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                        }
                    )
                )
            }
        }

        layoutSidebarContent.addView(createSidebarButton("⚙️ Buka Pengaturan WiFi Lengkap STB") {
            try {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun buildBluetoothSidebarContent(isScanning: Boolean = false) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter

        layoutSidebarContent.addView(createSectionHeader("STATUS BLUETOOTH"))
        val tvBtStatus = TextView(this).apply {
            text = if (isBluetoothOn) "● Bluetooth Aktif" else "○ Bluetooth Nonaktif"
            setTextColor(if (isBluetoothOn) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, (12 * resources.displayMetrics.density).toInt())
        }
        layoutSidebarContent.addView(tvBtStatus)

        // Toggle On / Off Bluetooth (Disimpan di SharedPreferences)
        layoutSidebarContent.addView(createSidebarButton(
            if (isBluetoothOn) "🔵 Matikan Bluetooth" else "⚪ Aktifkan Bluetooth"
        ) {
            isBluetoothOn = !isBluetoothOn
            prefs.edit().putBoolean(KEY_BLUETOOTH_ENABLED, isBluetoothOn).apply()
            try {
                if (adapter != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
                ) {
                    @Suppress("DEPRECATION")
                    if (isBluetoothOn) adapter.enable() else adapter.disable()
                }
            } catch (_: Exception) { }
            updateBluetoothIconVisual()
            populateSidebarContent(SidebarType.BLUETOOTH)
            Toast.makeText(this, if (isBluetoothOn) "Bluetooth diaktifkan" else "Bluetooth dimatikan", Toast.LENGTH_SHORT).show()
        })

        // Tombol Cari Perangkat Baru + Indikator Loading
        val pbBtScan = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = if (isScanning) View.VISIBLE else View.GONE
            layoutParams = LinearLayout.LayoutParams(
                (28 * resources.displayMetrics.density).toInt(),
                (28 * resources.displayMetrics.density).toInt()
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                bottomMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }
        layoutSidebarContent.addView(createSidebarButton(
            if (isScanning) "⏳ Mencari Perangkat Bluetooth Baru..." else "🔍 Cari Perangkat Baru"
        ) {
            if (!isBluetoothOn) {
                Toast.makeText(this, "Aktifkan Bluetooth terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@createSidebarButton
            }
            startBluetoothDiscoveryInSidebar()
        })
        layoutSidebarContent.addView(pbBtScan)

        // Daftar Perangkat Terpasang / Terhubung
        layoutSidebarContent.addView(createSectionHeader("PERANGKAT TERPASANG & TERSEDIA"))

        val pairedDevices = mutableListOf<BluetoothDevice>()
        try {
            if (adapter != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
            ) {
                pairedDevices.addAll(adapter.bondedDevices)
            }
        } catch (_: Exception) { }

        if (pairedDevices.isEmpty() && discoveredBtDevices.isEmpty()) {
            val tvEmpty = TextView(this).apply {
                text = "Belum ada perangkat Bluetooth terpasang (Remote TV, Speaker, atau Gamepad). Tekan 'Cari Perangkat Baru' atau buka Pengaturan Bluetooth STB."
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 11f
                setPadding(0, 4, 0, 12)
            }
            layoutSidebarContent.addView(tvEmpty)
        } else {
            for (device in pairedDevices) {
                val devName = try { device.name ?: device.address } catch (_: Exception) { "Perangkat Bluetooth" }
                layoutSidebarContent.addView(
                    createSidebarRowCard(
                        title = devName,
                        subtitle = "Terpasang • ${device.address}",
                        actionLabel = "Putuskan",
                        isConnectedAction = true,
                        onActionClick = {
                            try {
                                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            } catch (_: Exception) {
                                startActivity(Intent(Settings.ACTION_SETTINGS))
                            }
                        }
                    )
                )
            }
            for (foundName in discoveredBtDevices.distinct()) {
                if (pairedDevices.none { runCatching { it.name }.getOrNull() == foundName }) {
                    layoutSidebarContent.addView(
                        createSidebarRowCard(
                            title = foundName,
                            subtitle = "Ditemukan di sekitar",
                            actionLabel = "Pasangkan",
                            isConnectedAction = false,
                            onActionClick = {
                                try {
                                    startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                                } catch (_: Exception) {
                                    startActivity(Intent(Settings.ACTION_SETTINGS))
                                }
                            }
                        )
                    )
                }
            }
        }

        layoutSidebarContent.addView(createSidebarButton("🎮 Buka Pengaturan Bluetooth STB") {
            try {
                startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothDiscoveryInSidebar() {
        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter = bm?.adapter

        discoveredBtDevices.clear()
        buildBluetoothSidebarContentScanningRefresh(true)

        btDiscoveryReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) { }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (BluetoothDevice.ACTION_FOUND == intent?.action) {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val devName = try { device?.name } catch (_: Exception) { null }
                    if (!devName.isNullOrBlank() && !discoveredBtDevices.contains(devName)) {
                        discoveredBtDevices.add(devName)
                        if (isSidebarOpen && currentSidebarType == SidebarType.BLUETOOTH) {
                            buildBluetoothSidebarContentScanningRefresh(true)
                        }
                    }
                }
            }
        }
        btDiscoveryReceiver = receiver
        try {
            registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            if (adapter != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
            ) {
                if (adapter.isDiscovering) adapter.cancelDiscovery()
                adapter.startDiscovery()
            }
        } catch (_: Exception) { }

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                if (adapter != null && (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
                ) {
                    if (adapter.isDiscovering) adapter.cancelDiscovery()
                }
            } catch (_: Exception) { }
            if (isSidebarOpen && currentSidebarType == SidebarType.BLUETOOTH) {
                buildBluetoothSidebarContentScanningRefresh(false)
            }
        }, 4500L)
    }

    private fun buildBluetoothSidebarContentScanningRefresh(scanning: Boolean) {
        layoutSidebarContent.removeAllViews()
        tvSidebarTitle.text = "Perangkat Bluetooth"
        buildBluetoothSidebarContent(isScanning = scanning)
    }

    private fun createSectionHeader(title: String): TextView {
        val density = resources.displayMetrics.density
        return TextView(this).apply {
            text = title
            setTextColor(Color.parseColor("#93C5FD"))
            textSize = 11f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, (14 * density).toInt(), 0, (6 * density).toInt())
        }
    }

    // ==========================================
    // VOICE SEARCH POPUP OVERLAY
    // ==========================================
    private fun setupVoiceSearchOverlay() {
        btnStartListening.setOnClickListener {
            if (isVoiceListening) {
                stopListeningOnly()
            } else {
                startListeningSpeech()
            }
        }
        btnUseRemoteVoice.setOnClickListener {
            launchSystemVoiceIntentFallback(showUnavailableMsg = true)
        }
        btnCloseVoiceSearch.setOnClickListener {
            closeVoiceSearchOverlay()
        }
        layoutVoiceSearchOverlay.setOnClickListener {
            closeVoiceSearchOverlay()
        }
    }

    private fun openVoiceSearchOverlay() {
        hasUsedSystemVoiceFallback = false
        lastPartialVoiceText = ""
        layoutVoiceSearchOverlay.visibility = View.VISIBLE
        tvVoicePrompt.text = "Mendengarkan... Silakan bicara"
        tvVoiceResultText.setTextColor(Color.WHITE)
        tvVoiceResultText.text = "Katakan nama aplikasi atau ucapan apa saja..."
        btnStartListening.text = "Hentikan"
        btnStartListening.requestFocus()
        startListeningSpeech()
    }

    private fun stopListeningOnly() {
        isVoiceListening = false
        voiceWaveView.stopWave()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) { }
        tvVoicePrompt.text = "Mendengarkan dihentikan"
        btnStartListening.text = "Mulai Bicara"
        if (btnMic.isFocused) {
            tvTopIconTooltip.text = "Pencarian Suara"
        }
    }

    private fun closeVoiceSearchOverlay() {
        isVoiceListening = false
        voiceWaveView.stopWave()
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) { }
        speechRecognizer = null
        layoutVoiceSearchOverlay.visibility = View.GONE
        lastFocusedTopButton?.requestFocus()
    }

    private fun startListeningSpeech() {
        hasUsedSystemVoiceFallback = false
        lastPartialVoiceText = ""
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            if (!launchSystemVoiceIntentFallback(showUnavailableMsg = true)) {
                isVoiceListening = false
                voiceWaveView.stopWave()
                tvVoicePrompt.text = "Pencarian suara tidak didukung di perangkat ini"
                tvVoiceResultText.text = "Pencarian suara tidak didukung di perangkat ini"
                btnStartListening.text = "Mulai Bicara"
            }
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            tvVoicePrompt.text = "Meminta izin mikrofon..."
            recordAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            return
        }

        beginSpeechRecognizer()
    }

    private fun beginSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            if (!launchSystemVoiceIntentFallback(showUnavailableMsg = true)) {
                isVoiceListening = false
                voiceWaveView.stopWave()
                tvVoicePrompt.text = "Pencarian suara tidak didukung di perangkat ini"
                tvVoiceResultText.text = "Pencarian suara tidak didukung di perangkat ini"
                btnStartListening.text = "Mulai Bicara"
            }
            return
        }

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "id-ID")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            }

            isVoiceListening = true
            lastPartialVoiceText = ""
            tvVoicePrompt.text = "Mendengarkan... Silakan bicara"
            tvVoiceResultText.setTextColor(Color.WHITE)
            tvVoiceResultText.text = "Mendengarkan suara Anda..."
            btnStartListening.text = "Hentikan"
            voiceWaveView.startWave()

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    runOnUiThread {
                        tvVoicePrompt.text = "Mendengarkan... Silakan bicara"
                        voiceWaveView.startWave()
                    }
                }
                override fun onBeginningOfSpeech() {
                    runOnUiThread {
                        tvVoicePrompt.text = "Suara terdeteksi, teruskan berbicara..."
                    }
                }
                override fun onRmsChanged(rmsdB: Float) {
                    voiceWaveView.updateRms(rmsdB)
                }
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    runOnUiThread {
                        tvVoicePrompt.text = "Memproses ucapan Anda..."
                    }
                }
                override fun onError(error: Int) {
                    runOnUiThread {
                        isVoiceListening = false
                        voiceWaveView.stopWave()
                        btnStartListening.text = "Mulai Bicara"

                        // Jika sudah sempat ada partial text sebelum timeout, tampilkan dan proses
                        if (lastPartialVoiceText.isNotEmpty()) {
                            tvVoicePrompt.text = "Kata terdeteksi:"
                            tvVoiceResultText.setTextColor(Color.parseColor("#38BDF8"))
                            tvVoiceResultText.text = "\"$lastPartialVoiceText\""
                            searchAndLaunchSpokenApp(lastPartialVoiceText)
                            return@runOnUiThread
                        }

                        // Banyak remote Bluetooth/IR Android TV hanya mengalirkan audio mic saat layanan sistem dipanggil
                        val shouldTrySystemRemoteMic = !hasUsedSystemVoiceFallback && (
                            error == SpeechRecognizer.ERROR_AUDIO ||
                                error == SpeechRecognizer.ERROR_CLIENT ||
                                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                                error == SpeechRecognizer.ERROR_SERVER ||
                                error == SpeechRecognizer.ERROR_NO_MATCH ||
                                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                            )
                        if (shouldTrySystemRemoteMic && launchSystemVoiceIntentFallback(showUnavailableMsg = false)) {
                            return@runOnUiThread
                        }

                        tvVoicePrompt.text = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Mikrofon tidak terdeteksi. Coba tekan 'Mic Remote TV'."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Koneksi internet diperlukan untuk pengenalan suara."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Izin mikrofon diperlukan."
                            else -> "Tidak terdengar suara. Tekan 'Mulai Bicara' atau 'Mic Remote TV'."
                        }
                    }
                }
                override fun onResults(results: Bundle?) {
                    runOnUiThread {
                        isVoiceListening = false
                        voiceWaveView.stopWave()
                        btnStartListening.text = "Mulai Bicara"
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val spoken = matches?.firstOrNull { it.isNotBlank() }?.trim()
                            ?: lastPartialVoiceText.takeIf { it.isNotBlank() }
                        if (!spoken.isNullOrEmpty()) {
                            tvVoicePrompt.text = "Kata terdeteksi:"
                            tvVoiceResultText.setTextColor(Color.parseColor("#38BDF8"))
                            tvVoiceResultText.text = "\"$spoken\""
                            searchAndLaunchSpokenApp(spoken)
                        } else {
                            tvVoicePrompt.text = "Tidak ada kata terdeteksi. Coba bicara lagi."
                        }
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val stable = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull { it.isNotBlank() }?.trim() ?: ""
                    val unstable = partialResults?.getStringArrayList("android.speech.extra.UNSTABLE_TEXT")
                        ?.firstOrNull { it.isNotBlank() }?.trim()
                        ?: partialResults?.getStringArrayList("com.google.android.voicesearch.UNSTABLE_TEXT")
                            ?.firstOrNull { it.isNotBlank() }?.trim()
                        ?: ""

                    val combined = listOf(stable, unstable)
                        .filter { it.isNotEmpty() }
                        .joinToString(" ")
                        .trim()

                    if (combined.isNotEmpty()) {
                        lastPartialVoiceText = combined
                        runOnUiThread {
                            tvVoiceResultText.setTextColor(Color.parseColor("#38BDF8"))
                            tvVoiceResultText.text = "\"$combined\""
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            isVoiceListening = false
            voiceWaveView.stopWave()
            btnStartListening.text = "Mulai Bicara"
            if (!launchSystemVoiceIntentFallback(showUnavailableMsg = true)) {
                tvVoicePrompt.text = "Pencarian suara tidak didukung di perangkat ini"
                tvVoiceResultText.text = "Pencarian suara tidak didukung di perangkat ini"
            }
        }
    }

    private fun launchSystemVoiceIntentFallback(showUnavailableMsg: Boolean): Boolean {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Bicara sekarang melalui mic remote...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }
        return try {
            if (intent.resolveActivity(packageManager) != null) {
                hasUsedSystemVoiceFallback = true
                voiceFallbackLauncher.launch(intent)
                true
            } else {
                if (showUnavailableMsg) {
                    tvVoicePrompt.text = "Pencarian suara tidak didukung di perangkat ini"
                    tvVoiceResultText.text = "Pencarian suara tidak didukung di perangkat ini"
                    Toast.makeText(this, "Pencarian suara tidak didukung di perangkat ini", Toast.LENGTH_LONG).show()
                }
                false
            }
        } catch (e: Exception) {
            if (showUnavailableMsg) {
                tvVoicePrompt.text = "Pencarian suara tidak didukung di perangkat ini"
                tvVoiceResultText.text = "Pencarian suara tidak didukung di perangkat ini"
            }
            false
        }
    }

    private fun searchAndLaunchSpokenApp(query: String) {
        val rawLower = query.lowercase().trim()
        val cleanedQuery = rawLower
            .removePrefix("buka aplikasi ")
            .removePrefix("buka ")
            .removePrefix("open ")
            .removePrefix("jalankan ")
            .removePrefix("putar ")
            .removePrefix("nyalakan ")
            .trim()

        val match = allInstalledApps.find {
            val name = it.appName.lowercase()
            name == cleanedQuery || name.contains(cleanedQuery) || cleanedQuery.contains(name)
        }
        if (match != null && cleanedQuery.isNotEmpty()) {
            tvVoicePrompt.text = "Membuka ${match.appName}..."
            Toast.makeText(this, "Membuka ${match.appName}...", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({
                closeVoiceSearchOverlay()
                launchApp(match)
            }, 1200)
        } else {
            tvVoicePrompt.text = "Kata terucap: \"$query\" (bukan nama aplikasi terpasang)"
        }
    }

    // ==========================================
    // FOOTBALL SCHEDULE WIDGET (4 Liga & Detail)
    // ==========================================
    private fun setupFootballSchedule() {
        cardFootballSchedule.post {
            if (!isFinishing && !isDestroyed) {
                ivLogoEpl.load("https://a.espncdn.com/i/leaguelogos/soccer/500/23.png")
                ivLogoSerieA.load("https://a.espncdn.com/i/leaguelogos/soccer/500/12.png")
                ivLogoLaLiga.load("https://a.espncdn.com/i/leaguelogos/soccer/500/15.png")
                ivLogoUcl.load("https://a.espncdn.com/i/leaguelogos/soccer/500/2.png")
            }
        }

        val leagues = listOf(
            Triple(itemLeagueEpl, "eng.1", "Premier League"),
            Triple(itemLeagueSerieA, "ita.1", "Serie A"),
            Triple(itemLeagueLaLiga, "esp.1", "La Liga"),
            Triple(itemLeagueUcl, "uefa.champions", "UEFA Champions League")
        )

        for ((card, code, name) in leagues) {
            card.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.animate().scaleX(if (hasFocus) 1.06f else 1.0f).scaleY(if (hasFocus) 1.06f else 1.0f).setDuration(160).start()
                v.elevation = if (hasFocus) 10f else 0f
            }
            card.setOnClickListener {
                openLeagueSchedule(code, name, getLeagueLogo(code), card)
            }
        }

        btnBackFromSchedule.setOnClickListener {
            closeLeagueSchedule()
        }
        btnRefreshFootballSchedule.setOnClickListener {
            refreshFootballSchedule()
        }

        footballAdapter = FootballMatchAdapter(this, emptyList()) { match ->
            Toast.makeText(this, "${match.homeTeam} vs ${match.awayTeam} (${match.matchTimeWib})", Toast.LENGTH_SHORT).show()
        }
        rvFootballMatches.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        rvFootballMatches.adapter = footballAdapter
    }

    private fun getLeagueLogo(code: String): String {
        return when {
            code.contains("eng.1") -> "https://a.espncdn.com/i/leaguelogos/soccer/500/23.png"
            code.contains("ita.1") -> "https://a.espncdn.com/i/leaguelogos/soccer/500/12.png"
            code.contains("esp.1") -> "https://a.espncdn.com/i/leaguelogos/soccer/500/15.png"
            else -> "https://a.espncdn.com/i/leaguelogos/soccer/500/2.png"
        }
    }

    private fun openLeagueSchedule(leagueCode: String, leagueName: String, leagueLogoUrl: String, cardView: View) {
        selectedLeagueCard = cardView
        layoutLeagueSchedulePanel.visibility = View.VISIBLE
        tvScheduleLeagueName.text = leagueName
        ivScheduleLeagueLogo.load(leagueLogoUrl)
        tvScheduleDateTitle.text = "MEMERIKSA JADWAL..."
        tvScheduleMatchCount.text = ""
        pbScheduleLoading.visibility = View.VISIBLE
        rvFootballMatches.visibility = View.GONE
        tvScheduleEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val result = footballRepo.getScheduleForLeague(leagueCode)
                pbScheduleLoading.visibility = View.GONE
                tvScheduleDateTitle.text = result.dateTitle
                tvScheduleMatchCount.text = "${result.matches.size} Pertandingan"

                if (result.matches.isEmpty()) {
                    tvScheduleEmpty.visibility = View.VISIBLE
                    rvFootballMatches.visibility = View.GONE
                } else {
                    tvScheduleEmpty.visibility = View.GONE
                    rvFootballMatches.visibility = View.VISIBLE
                    footballAdapter.updateMatches(result.matches)
                }
                btnBackFromSchedule.requestFocus()
            } catch (e: Exception) {
                pbScheduleLoading.visibility = View.GONE
                tvScheduleEmpty.text = "Gagal memuat jadwal"
                tvScheduleEmpty.visibility = View.VISIBLE
            }
        }
    }

    private fun closeLeagueSchedule() {
        layoutLeagueSchedulePanel.visibility = View.GONE
        layoutFootballCenterSidebar.visibility = View.VISIBLE
        if (ThemeManager.shouldUseGoogleTvHomeLayout(this)) {
            cardFootballSchedule.visibility = View.GONE
            googleTvController?.focusPrimaryItem()
        } else {
            selectedLeagueCard?.requestFocus()
        }
    }

    private fun refreshFootballSchedule() {
        if (::layoutLeagueSchedulePanel.isInitialized && layoutLeagueSchedulePanel.visibility == View.VISIBLE && selectedLeagueCard != null) {
            selectedLeagueCard?.performClick()
        } else {
            footballRepo.clearCache()
            Toast.makeText(this, "Jadwal liga diperbarui!", Toast.LENGTH_SHORT).show()
        }
    }

    // ==========================================
    // BOTTOM HORIZONTAL APPLICATION ROW & THEME ENGINE
    // ==========================================
    private fun setupBottomAppRow() {
        val initialApps = AppCache.cachedApps ?: emptyList()
        allInstalledApps = initialApps

        applyActiveLauncherTheme(focusFirst = initialApps.isNotEmpty())

        if (initialApps.isEmpty()) {
            reloadInstalledAppsAsync(focusFirst = true)
        }

        registerPackageReceiver()
    }

    private fun isCustomHomeLayoutActive(ctx: Context = this): Boolean {
        return ThemeManager.shouldUseGoogleTvHomeLayout(ctx) ||
            ThemeManager.shouldUseNexfyHomeLayout(ctx) ||
            ThemeManager.shouldUseProjectivyHomeLayout(ctx) ||
            ThemeManager.shouldUseAtvHomeLayout(ctx) ||
            ThemeManager.shouldUseWolfHomeLayout(ctx)
    }

    private fun focusPrimaryItemForActiveTheme() {
        when {
            ThemeManager.shouldUseGoogleTvHomeLayout(this) -> googleTvController?.focusPrimaryItem()
            ThemeManager.shouldUseNexfyHomeLayout(this) -> nexfyController?.focusPrimaryItem()
            ThemeManager.shouldUseProjectivyHomeLayout(this) -> projectivyController?.focusPrimaryItem()
            ThemeManager.shouldUseAtvHomeLayout(this) -> atvController?.focusPrimaryItem()
            ThemeManager.shouldUseWolfHomeLayout(this) -> wolfController?.focusPrimaryItem()
            ::rvBottomApps.isInitialized -> rvBottomApps.post {
                rvBottomApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
            }
        }
    }

    private fun syncActiveThemeIfNeeded(focusFirst: Boolean = false) {
        val expectedTheme = ThemeManager.getActiveTheme(this)
        val expectedCustom = isCustomHomeLayoutActive(this)
        val expectedViewVisible = when {
            ThemeManager.shouldUseGoogleTvHomeLayout(this, expectedTheme) -> googleTvHostRoot?.visibility == View.VISIBLE
            ThemeManager.shouldUseNexfyHomeLayout(this, expectedTheme) -> nexfyHostRoot?.visibility == View.VISIBLE
            ThemeManager.shouldUseProjectivyHomeLayout(this, expectedTheme) -> projectivyHostRoot?.visibility == View.VISIBLE
            ThemeManager.shouldUseAtvHomeLayout(this, expectedTheme) -> atvHostRoot?.visibility == View.VISIBLE
            ThemeManager.shouldUseWolfHomeLayout(this, expectedTheme) -> wolfHostRoot?.visibility == View.VISIBLE
            else -> ::rvBottomApps.isInitialized && rvBottomApps.visibility == View.VISIBLE &&
                googleTvHostRoot?.visibility != View.VISIBLE &&
                nexfyHostRoot?.visibility != View.VISIBLE &&
                projectivyHostRoot?.visibility != View.VISIBLE &&
                atvHostRoot?.visibility != View.VISIBLE &&
                wolfHostRoot?.visibility != View.VISIBLE
        }
        if (lastAppliedThemeId != expectedTheme.id || lastAppliedCustomHome != expectedCustom || !expectedViewVisible) {
            applyActiveLauncherTheme(theme = expectedTheme, focusFirst = focusFirst)
        }
    }

    private fun applyActiveLauncherTheme(
        theme: LauncherThemeDefinition = ThemeManager.getActiveTheme(this),
        focusFirst: Boolean = false
    ) {
        val useGoogleTvHome = ThemeManager.shouldUseGoogleTvHomeLayout(this, theme)
        val useNexfyHome = ThemeManager.shouldUseNexfyHomeLayout(this, theme)
        val useProjectivyHome = ThemeManager.shouldUseProjectivyHomeLayout(this, theme)
        val useAtvHome = ThemeManager.shouldUseAtvHomeLayout(this, theme)
        val useWolfHome = ThemeManager.shouldUseWolfHomeLayout(this, theme)
        val isCustomLayout = useGoogleTvHome || useNexfyHome || useProjectivyHome || useAtvHome || useWolfHome
        lastAppliedThemeId = theme.id
        lastAppliedCustomHome = isCustomLayout
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (useGoogleTvHome) {
            // Mode Google TV Home Screen Default = ON
            nexfyHostRoot?.visibility = View.GONE
            projectivyHostRoot?.visibility = View.GONE
            atvHostRoot?.visibility = View.GONE
            wolfHostRoot?.visibility = View.GONE
            layoutTopLeft.visibility = View.GONE
            analogClock.visibility = View.GONE
            layoutTopRightContainer.visibility = View.GONE
            cardFootballSchedule.visibility = View.GONE
            tvThemeShelfHeader.visibility = View.GONE
            rvBottomApps.visibility = View.GONE

            val host = ensureGoogleTvHostInitialized()
            host.visibility = View.VISIBLE
            googleTvController?.let { ctrl ->
                ctrl.updateInstalledApps(allInstalledApps)
                ctrl.refreshClockAndWeather(tvWeatherInfo.text?.toString() ?: "")
                if (focusFirst) {
                    ctrl.focusPrimaryItem()
                }
            }

            lifecycleScope.launch {
                try {
                    val liveMatches = footballRepo.getLiveSchedules()
                    googleTvController?.updateFootballMatches(liveMatches)
                } catch (_: Exception) { }
            }
        } else if (useNexfyHome) {
            // Mode Nexfy Home Screen Default = ON (Referensi 1 & 2)
            googleTvHostRoot?.visibility = View.GONE
            projectivyHostRoot?.visibility = View.GONE
            atvHostRoot?.visibility = View.GONE
            wolfHostRoot?.visibility = View.GONE
            layoutTopLeft.visibility = View.GONE
            analogClock.visibility = View.GONE
            layoutTopRightContainer.visibility = View.GONE
            cardFootballSchedule.visibility = View.GONE
            tvThemeShelfHeader.visibility = View.GONE
            rvBottomApps.visibility = View.GONE

            val host = ensureNexfyHostInitialized()
            host.visibility = View.VISIBLE
            nexfyController?.let { ctrl ->
                ctrl.updateInstalledApps(allInstalledApps)
                ctrl.updateSystemStatus(
                    wifiOnline = isWifiOnline,
                    wifiSsid = currentWifiSsid,
                    bluetoothOn = isBluetoothOn,
                    weatherText = tvWeatherInfo.text?.toString() ?: ""
                )
                if (focusFirst) {
                    ctrl.focusPrimaryItem()
                }
            }
        } else if (useProjectivyHome) {
            // Mode Projectivy Home Screen Default = ON (Referensi 1 & 2)
            googleTvHostRoot?.visibility = View.GONE
            nexfyHostRoot?.visibility = View.GONE
            atvHostRoot?.visibility = View.GONE
            wolfHostRoot?.visibility = View.GONE
            layoutTopLeft.visibility = View.GONE
            analogClock.visibility = View.GONE
            layoutTopRightContainer.visibility = View.GONE
            cardFootballSchedule.visibility = View.GONE
            tvThemeShelfHeader.visibility = View.GONE
            rvBottomApps.visibility = View.GONE

            val host = ensureProjectivyHostInitialized()
            host.visibility = View.VISIBLE
            projectivyController?.let { ctrl ->
                ctrl.updateInstalledApps(allInstalledApps)
                ctrl.updateSystemStatus(
                    wifiOnline = isWifiOnline,
                    wifiSsid = currentWifiSsid,
                    weatherText = tvWeatherInfo.text?.toString() ?: ""
                )
                ctrl.refreshWatchNext()
                if (focusFirst) {
                    ctrl.focusPrimaryItem()
                }
            }
        } else if (useAtvHome) {
            // Mode ATV Launcher Home Screen Default = ON (Sesuai Screenshot Referensi ATV)
            googleTvHostRoot?.visibility = View.GONE
            nexfyHostRoot?.visibility = View.GONE
            projectivyHostRoot?.visibility = View.GONE
            wolfHostRoot?.visibility = View.GONE
            layoutTopLeft.visibility = View.GONE
            analogClock.visibility = View.GONE
            layoutTopRightContainer.visibility = View.GONE
            cardFootballSchedule.visibility = View.GONE
            tvThemeShelfHeader.visibility = View.GONE
            rvBottomApps.visibility = View.GONE

            val host = ensureAtvHostInitialized()
            host.visibility = View.VISIBLE
            atvController?.let { ctrl ->
                ctrl.applyCardSettingsAndLayoutManagers()
                ctrl.updateInstalledApps(allInstalledApps)
                ctrl.updateSystemStatus(
                    wifiOnline = isWifiOnline,
                    wifiSsid = currentWifiSsid,
                    bluetoothOn = isBluetoothOn,
                    weatherText = tvWeatherInfo.text?.toString() ?: ""
                )
                if (focusFirst) {
                    ctrl.focusPrimaryItem()
                }
            }
        } else if (useWolfHome) {
            // Mode Wolf Launcher Home Screen Default = ON (Sesuai Screenshot Referensi Wolf)
            googleTvHostRoot?.visibility = View.GONE
            nexfyHostRoot?.visibility = View.GONE
            projectivyHostRoot?.visibility = View.GONE
            atvHostRoot?.visibility = View.GONE
            layoutTopLeft.visibility = View.GONE
            analogClock.visibility = View.GONE
            layoutTopRightContainer.visibility = View.GONE
            cardFootballSchedule.visibility = View.GONE
            tvThemeShelfHeader.visibility = View.GONE
            rvBottomApps.visibility = View.GONE

            val host = ensureWolfHostInitialized()
            host.visibility = View.VISIBLE
            wolfController?.let { ctrl ->
                ctrl.applyDisplaySettings()
                ctrl.updateInstalledApps(allInstalledApps)
                ctrl.updateSystemStatus(
                    wifiOnline = isWifiOnline,
                    wifiSsid = currentWifiSsid,
                    bluetoothOn = isBluetoothOn,
                    weatherText = tvWeatherInfo.text?.toString() ?: ""
                )
                ctrl.updateNetworkSpeed(tvNetworkSpeed.text?.toString() ?: "")
                ctrl.refreshStorageInfo(force = true)
                if (focusFirst) {
                    ctrl.focusPrimaryItem()
                }
            }
        } else {
            // Mode Tata Letak Dasar Aero TV / Pramana (Default Theme, tema lain, atau Use Home Screen Default = OFF)
            googleTvHostRoot?.visibility = View.GONE
            nexfyHostRoot?.visibility = View.GONE
            projectivyHostRoot?.visibility = View.GONE
            atvHostRoot?.visibility = View.GONE
            wolfHostRoot?.visibility = View.GONE

            layoutTopLeft.visibility = View.VISIBLE
            val showAnalog = prefs.getBoolean("key_show_analog_clock", true)
            analogClock.visibility = if (showAnalog) View.VISIBLE else View.GONE
            layoutTopRightContainer.visibility = View.VISIBLE
            cardFootballSchedule.visibility = if (isKickoffWidgetVisible) View.VISIBLE else View.GONE
            rvBottomApps.visibility = View.VISIBLE

            val density = resources.displayMetrics.density
            if (theme.id == ThemeId.WOLF_LAUNCHER) {
                val wolfSettings = ThemeManager.getWolfDisplaySettings(this)
                val alphaVal = ((wolfSettings.dockAlphaPercent.coerceIn(15, 95) / 100f) * 255f)
                    .roundToInt().coerceIn(28, 245)
                rvBottomApps.background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                    cornerRadius = 24f * density
                    setColor(Color.argb(alphaVal, 15, 23, 42))
                    setStroke((1 * density).roundToInt(), Color.parseColor("#3DFFFFFF"))
                }
                (rvBottomApps.layoutParams as? android.widget.RelativeLayout.LayoutParams)?.let { lp ->
                    val marginH = (28 * density).roundToInt()
                    lp.marginStart = marginH
                    lp.marginEnd = marginH
                    rvBottomApps.layoutParams = lp
                }
            } else {
                rvBottomApps.background = null
                (rvBottomApps.layoutParams as? android.widget.RelativeLayout.LayoutParams)?.let { lp ->
                    lp.marginStart = 0
                    lp.marginEnd = 0
                    rvBottomApps.layoutParams = lp
                }
            }

            bottomAppRowAdapter = ThemeLayoutManager.applyHomeLayout(
                context = this,
                recyclerView = rvBottomApps,
                headerTextView = tvThemeShelfHeader,
                apps = allInstalledApps,
                theme = theme,
                onAppClick = { app -> launchApp(app) },
                onRequestAppDrawer = { openThemedAppDrawer() }
            )

            if (focusFirst && allInstalledApps.isNotEmpty()) {
                rvBottomApps.post {
                    rvBottomApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                }
            }
        }
    }

    private fun ensureGoogleTvHostInitialized(): View {
        val existing = googleTvHostRoot
        if (existing != null) return existing

        val inflated = stubGoogleTvHost.inflate()
        googleTvHostRoot = inflated
        googleTvController = GoogleTvHomeController(
            context = this,
            rootView = inflated,
            scope = lifecycleScope,
            onAppClick = { app -> launchApp(app) },
            onOpenSettings = { openSidebar(SidebarType.SETTINGS) },
            onOpenWallpaperPicker = { showQuickWallpaperOptionsDialog() },
            onOpenVoiceSearch = { openVoiceSearchOverlay() },
            onOpenFootballCenter = { openGoogleTvFootballAction() }
        )
        return inflated
    }

    private fun ensureNexfyHostInitialized(): View {
        val existing = nexfyHostRoot
        if (existing != null) return existing

        val inflated = stubNexfyHost.inflate()
        nexfyHostRoot = inflated
        nexfyController = NexfyHomeController(
            context = this,
            rootView = inflated,
            isDrawerStandaloneMode = false,
            onAppClick = { app -> launchApp(app) },
            onOpenWifi = { openSidebar(SidebarType.WIFI) },
            onOpenBluetooth = { openSidebar(SidebarType.BLUETOOTH) },
            onOpenSettings = { openSidebar(SidebarType.SETTINGS) },
            onOpenVoiceSearch = { openVoiceSearchOverlay() },
            onRefreshWeather = {
                WeatherHelper.invalidateCache()
                fetchRealtimeWeather(forceRefresh = true)
            }
        )
        return inflated
    }

    private fun ensureProjectivyHostInitialized(): View {
        val existing = projectivyHostRoot
        if (existing != null) return existing

        val inflated = stubProjectivyHost.inflate()
        projectivyHostRoot = inflated
        projectivyController = ProjectivyHomeController(
            context = this,
            rootView = inflated,
            scope = lifecycleScope,
            isDrawerStandaloneMode = false,
            onAppClick = { app -> launchApp(app) },
            onOpenWifi = { openSidebar(SidebarType.WIFI) },
            onOpenSettings = { openSidebar(SidebarType.SETTINGS) },
            onOpenVoiceSearch = { openVoiceSearchOverlay() },
            onRefreshWeather = {
                WeatherHelper.invalidateCache()
                fetchRealtimeWeather(forceRefresh = true)
            }
        )
        return inflated
    }

    private fun ensureAtvHostInitialized(): View {
        val existing = atvHostRoot
        if (existing != null) return existing

        val inflated = stubAtvHost.inflate()
        atvHostRoot = inflated
        atvController = AtvHomeController(
            context = this,
            rootView = inflated,
            isDrawerStandaloneMode = false,
            onAppClick = { app -> launchApp(app) },
            onOpenWifi = { openSidebar(SidebarType.WIFI) },
            onOpenBluetooth = { openSidebar(SidebarType.BLUETOOTH) },
            onOpenSettings = { openSidebar(SidebarType.SETTINGS) },
            onOpenVoiceSearch = { openVoiceSearchOverlay() },
            onRefreshWeather = {
                WeatherHelper.invalidateCache()
                fetchRealtimeWeather(forceRefresh = true)
            }
        )
        return inflated
    }

    private fun ensureWolfHostInitialized(): View {
        val existing = wolfHostRoot
        if (existing != null) return existing

        val inflated = stubWolfHost.inflate()
        wolfHostRoot = inflated
        wolfController = WolfHomeController(
            context = this,
            rootView = inflated,
            onAppClick = { app -> launchApp(app) },
            onOpenAppDrawer = { openThemedAppDrawer() },
            onOpenWifi = { openSidebar(SidebarType.WIFI) },
            onOpenBluetooth = { openSidebar(SidebarType.BLUETOOTH) },
            onOpenSettings = { openSidebar(SidebarType.SETTINGS) },
            onRefreshWeather = {
                WeatherHelper.invalidateCache()
                fetchRealtimeWeather(forceRefresh = true)
            },
            onDisplaySettingsUpdated = {
                applyActiveLauncherTheme(focusFirst = false)
            }
        )
        return inflated
    }

    private fun showQuickWallpaperOptionsDialog() {
        val options = arrayOf(
            "🖼️ Pilih Foto Wallpaper dari Penyimpanan",
            "🎞️ Pilih Folder Slideshow Foto",
            "🔥 Atur Wallpaper & Pengaturan Tampilan Lengkap",
            "🔄 Reset ke Wallpaper Bawaan Aero TV"
        )
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Wallpaper Utama (Tetap Terjaga)")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> wallpaperPickerLauncher.launch(WallpaperHelper.createWallpaperPickerIntent())
                    1 -> showFolderSelectionOptionsDialog()
                    2 -> openSidebar(SidebarType.SETTINGS)
                    3 -> {
                        applyWallpaperMode(WallpaperMode.STATIC)
                        WallpaperHelper.resetToDefaultWallpaper(this, ivWallpaper)
                        Toast.makeText(this, "Wallpaper dikembalikan ke foto bawaan", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun openGoogleTvFootballAction() {
        // Buka panel jadwal liga langsung atau arahkan ke Pengaturan Jadwal Liga
        openLeagueSchedule("eng.1", "Premier League", getLeagueLogo("eng.1"), itemLeagueEpl)
        cardFootballSchedule.visibility = View.VISIBLE
        layoutFootballCenterSidebar.visibility = View.GONE
    }

    private fun openThemeAndIconSettings() {
        closeSidebar(restoreFocus = false)
        activeThemeDialog?.dismiss()
        val dlg = ThemeAndIconSettingsDialog(
            context = this,
            installedApps = allInstalledApps,
            onThemeApplied = { appliedTheme ->
                applyActiveLauncherTheme(theme = appliedTheme, focusFirst = false)
            },
            onOpenAppDrawer = {
                openThemedAppDrawer()
            },
            onClosed = {
                activeThemeDialog = null
                syncActiveThemeIfNeeded(focusFirst = false)
                focusPrimaryItemForActiveTheme()
            }
        )
        activeThemeDialog = dlg
        dlg.show()
    }

    private fun openThemedAppDrawer() {
        if (allInstalledApps.isEmpty()) return
        val theme = ThemeManager.getActiveTheme(this)
        if (ThemeManager.shouldUseGoogleTvHomeLayout(this, theme) && googleTvHostRoot?.visibility == View.VISIBLE) {
            googleTvController?.switchTab(GoogleTvTabId.APPS, focusContent = true)
            return
        }
        if (ThemeManager.shouldUseNexfyHomeLayout(this, theme) && nexfyHostRoot?.visibility == View.VISIBLE) {
            nexfyController?.switchMode(NexfyViewMode.ALL_APPS, focusContent = true)
            return
        }
        if (ThemeManager.shouldUseProjectivyHomeLayout(this, theme) && projectivyHostRoot?.visibility == View.VISIBLE) {
            projectivyController?.switchMode(ProjectivyViewMode.TV_APPS, focusContent = true)
            return
        }
        if (ThemeManager.shouldUseAtvHomeLayout(this, theme) && atvHostRoot?.visibility == View.VISIBLE) {
            atvController?.switchMode(AtvViewMode.APPLICATIONS, focusContent = true)
            return
        }
        activeDrawerDialog?.dismiss()
        val dlg = ThemeAppDrawerDialog(
            context = this,
            apps = allInstalledApps,
            theme = theme,
            onAppClick = { app -> launchApp(app) },
            onClosed = {
                activeDrawerDialog = null
                syncActiveThemeIfNeeded(focusFirst = false)
                focusPrimaryItemForActiveTheme()
            },
            onOpenSettings = { openSidebar(SidebarType.SETTINGS) },
            onOpenVoiceSearch = { openVoiceSearchOverlay() },
            onOpenWifi = { openSidebar(SidebarType.WIFI) },
            onOpenBluetooth = { openSidebar(SidebarType.BLUETOOTH) },
            onRefreshWeather = {
                WeatherHelper.invalidateCache()
                fetchRealtimeWeather(forceRefresh = true)
            },
            isWifiOnline = isWifiOnline,
            wifiSsid = currentWifiSsid,
            isBluetoothOn = isBluetoothOn,
            weatherSummary = tvWeatherInfo.text?.toString() ?: ""
        )
        activeDrawerDialog = dlg
        dlg.show()
    }

    private fun reloadInstalledAppsAsync(focusFirst: Boolean = false) {
        lifecycleScope.launch(Dispatchers.IO) {
            val loaded = loadInstalledApps()
            AppCache.cachedApps = loaded
            withContext(Dispatchers.Main) {
                allInstalledApps = loaded
                if (::bottomAppRowAdapter.isInitialized) {
                    bottomAppRowAdapter.updateApps(loaded)
                }
                googleTvController?.updateInstalledApps(loaded)
                nexfyController?.updateInstalledApps(loaded)
                projectivyController?.updateInstalledApps(loaded)
                atvController?.updateInstalledApps(loaded)
                wolfController?.updateInstalledApps(loaded)
                activeDrawerDialog?.updateApps(loaded)
                if (focusFirst && loaded.isNotEmpty()) {
                    when {
                        ThemeManager.shouldUseGoogleTvHomeLayout(this@MainActivity) -> googleTvController?.focusPrimaryItem()
                        ThemeManager.shouldUseNexfyHomeLayout(this@MainActivity) -> nexfyController?.focusPrimaryItem()
                        ThemeManager.shouldUseProjectivyHomeLayout(this@MainActivity) -> projectivyController?.focusPrimaryItem()
                        ThemeManager.shouldUseAtvHomeLayout(this@MainActivity) -> atvController?.focusPrimaryItem()
                        ThemeManager.shouldUseWolfHomeLayout(this@MainActivity) -> wolfController?.focusPrimaryItem()
                        else -> rvBottomApps.post {
                            rvBottomApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                        }
                    }
                }
            }
        }
    }

    private fun registerPackageReceiver() {
        if (packageChangeReceiver != null) return
        packageChangeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                AppCache.cachedApps = null
                reloadInstalledAppsAsync(focusFirst = false)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        registerReceiver(packageChangeReceiver, filter)
    }

    private fun launchApp(app: AppModel) {
        if (app.launchIntent != null) {
            try {
                startActivity(app.launchIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Gagal membuka ${app.appName}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "${app.appName} tidak dapat dijalankan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadInstalledApps(): List<AppModel> {
        val pm = packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val tvIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }

        val tvResolves = pm.queryIntentActivities(tvIntent, 0)
        val tvPackages = tvResolves.mapTo(HashSet()) { it.activityInfo.packageName }

        val allResolves = mutableListOf<ResolveInfo>()
        allResolves.addAll(tvResolves)
        allResolves.addAll(pm.queryIntentActivities(mainIntent, 0))

        val installedAppMap = mutableMapOf<String, AppModel>()
        for (resolve in allResolves) {
            val pkg = resolve.activityInfo.packageName
            if (pkg != packageName && !installedAppMap.containsKey(pkg)) {
                val label = resolve.loadLabel(pm).toString()
                val icon = resolve.loadIcon(pm)
                val appInfo = resolve.activityInfo.applicationInfo
                val banner = try {
                    resolve.activityInfo.loadBanner(pm) ?: appInfo.loadBanner(pm)
                } catch (_: Exception) {
                    null
                }
                val launchIntent = pm.getLeanbackLaunchIntentForPackage(pkg)
                    ?: pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    val isTv = tvPackages.contains(pkg)
                    val category = classifyAppCategory(pkg, label, appInfo, isTv)
                    installedAppMap[pkg] = AppModel(
                        packageName = pkg,
                        appName = label,
                        icon = icon,
                        launchIntent = launchIntent,
                        banner = banner,
                        category = category,
                        isTvApp = isTv
                    )
                }
            }
        }

        val targetKeywords = listOf("cloudstream", "gvision", "bit tv", "tizentube", "ppsspp", "youtube", "netflix", "vidio", "disney", "prime")
        val prioritizedApps = mutableListOf<AppModel>()

        for (keyword in targetKeywords) {
            val match = installedAppMap.values.find {
                it.appName.lowercase().contains(keyword) || it.packageName.lowercase().contains(keyword)
            }
            if (match != null && !prioritizedApps.contains(match)) {
                prioritizedApps.add(match)
            }
        }

        val remainingApps = installedAppMap.values
            .filter { !prioritizedApps.contains(it) }
            .sortedBy { it.appName.lowercase() }
        prioritizedApps.addAll(remainingApps)

        return prioritizedApps
    }

    private fun classifyAppCategory(
        pkg: String,
        label: String,
        appInfo: ApplicationInfo,
        isTvApp: Boolean
    ): AppCategory {
        val lower = "${pkg.lowercase(Locale.US)} ${label.lowercase(Locale.US)}"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_AUDIO -> return AppCategory.MUSIC_AUDIO
                ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.MEDIA_VIDEO
            }
        }
        return when {
            lower.contains("spotify") || lower.contains("music") || lower.contains("resso") ||
                lower.contains("joox") || lower.contains("radio") || lower.contains("soundcloud") -> AppCategory.MUSIC_AUDIO
            lower.contains("game") || lower.contains("ppsspp") || lower.contains("retroarch") ||
                lower.contains("aether") || lower.contains("dolphin") || lower.contains("play games") -> AppCategory.GAMES
            lower.contains("youtube") || lower.contains("netflix") || lower.contains("vidio") ||
                lower.contains("disney") || lower.contains("prime") || lower.contains("vlc") ||
                lower.contains("kodi") || lower.contains("plex") || lower.contains("stremio") ||
                lower.contains("cloudstream") || lower.contains("tizentube") || lower.contains("vision") ||
                lower.contains("tv") || lower.contains("iptv") || lower.contains("iqiyi") ||
                lower.contains("wetv") || lower.contains("bilibili") || lower.contains("crunchyroll") ||
                lower.contains("twitch") || lower.contains("mxtech") || isTvApp -> AppCategory.MEDIA_VIDEO
            else -> AppCategory.TOOLS_SYSTEM
        }
    }

    // ==========================================
    // WALLPAPER HELPER DIALOGS
    // ==========================================
    private fun showFolderSelectionOptionsDialog() {
        val options = arrayOf(
            "📂 Browser Folder Internal TV (Penyimpanan STB)",
            "💾 Jelajahi Flashdisk USB (/storage)",
            "📸 Folder Kamera (/sdcard/DCIM)",
            "🖼️ Folder Gambar (/sdcard/Pictures)",
            "📥 Folder Unduhan (/sdcard/Download)",
            "🎨 Folder Wallpapers (/sdcard/Wallpapers)",
            "📱 Buka File Manager Sistem (SAF)"
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Pilih Sumber Folder Foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showInternalFolderBrowserDialog(File("/storage/emulated/0"))
                    1 -> showInternalFolderBrowserDialog(File("/storage"))
                    2 -> selectQuickFolder("/storage/emulated/0/DCIM", "DCIM / Kamera")
                    3 -> selectQuickFolder("/storage/emulated/0/Pictures", "Pictures / Gambar")
                    4 -> selectQuickFolder("/storage/emulated/0/Download", "Download / Unduhan")
                    5 -> selectQuickFolder("/storage/emulated/0/Wallpapers", "Wallpapers")
                    6 -> {
                        try {
                            folderPickerLauncher.launch(wallpaperSlideshowManager.createFolderPickerIntent())
                        } catch (e: Exception) {
                            showInternalFolderBrowserDialog(File("/storage/emulated/0"))
                        }
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun selectQuickFolder(path: String, label: String) {
        val dir = File(path)
        if (!dir.exists()) dir.mkdirs()
        lifecycleScope.launch {
            val count = wallpaperSlideshowManager.onLocalFolderSelected(dir.absolutePath, label)
            if (count > 0) {
                applyWallpaperMode(WallpaperMode.SLIDESHOW)
                Toast.makeText(this@MainActivity, "Slideshow aktif! $count foto dari folder $label", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Folder '$label' kosong. Belum ada foto JPG/PNG/WEBP.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showInternalFolderBrowserDialog(currentDir: File) {
        val validDir = if (currentDir.exists() && currentDir.isDirectory) currentDir else File("/storage/emulated/0")
        val files = validDir.listFiles() ?: arrayOf()

        val subDirs = files.filter { it.isDirectory && !it.name.startsWith(".") }
            .sortedBy { it.name.lowercase() }

        val imageExtensions = listOf("jpg", "jpeg", "png", "webp")
        val imageCount = files.count { file ->
            file.isFile && imageExtensions.contains(file.extension.lowercase())
        }

        val menuItems = mutableListOf<String>()
        menuItems.add("✓ PILIH FOLDER INI: ${validDir.name} ($imageCount foto)")
        if (validDir.parentFile != null && validDir.absolutePath != "/storage" && validDir.absolutePath != "/") {
            menuItems.add(".. [Kembali ke Folder Atas]")
        }
        for (dir in subDirs) {
            val count = dir.listFiles()?.count { f -> f.isFile && imageExtensions.contains(f.extension.lowercase()) } ?: 0
            menuItems.add("📁 ${dir.name} ($count foto)")
        }

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(validDir.absolutePath)
            .setItems(menuItems.toTypedArray()) { _, which ->
                val choice = menuItems[which]
                when {
                    choice.startsWith("✓ PILIH FOLDER INI") -> {
                        lifecycleScope.launch {
                            val count = wallpaperSlideshowManager.onLocalFolderSelected(validDir.absolutePath, validDir.name)
                            if (count > 0) {
                                applyWallpaperMode(WallpaperMode.SLIDESHOW)
                                Toast.makeText(this@MainActivity, "Slideshow aktif! Ditemukan $count foto.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, "Folder ini kosong (0 foto).", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    choice.startsWith(".. [Kembali") -> {
                        validDir.parentFile?.let { showInternalFolderBrowserDialog(it) }
                    }
                    else -> {
                        val selectedDirName = choice.substringAfter("📁 ").substringBefore(" (")
                        val nextDir = File(validDir, selectedDirName)
                        showInternalFolderBrowserDialog(nextDir)
                    }
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showIntervalPickerDialog() {
        val intervals = arrayOf(
            Pair("10 Detik", 10),
            Pair("30 Detik", 30),
            Pair("1 Menit", 60),
            Pair("3 Menit", 180)
        )
        val names = intervals.map { it.first }.toTypedArray()
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Pilih Interval Pergantian Foto Slideshow")
            .setItems(names) { _, which ->
                val selected = intervals[which]
                wallpaperSlideshowManager.setIntervalSeconds(selected.second)
                Toast.makeText(this, "Interval diatur ke: ${selected.first}", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showTransitionEffectPickerDialog() {
        val effects = arrayOf("Fade", "Slide", "Zoom")
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Pilih Efek Transisi Slideshow")
            .setItems(effects) { _, which ->
                val chosen = effects[which]
                wallpaperSlideshowManager.setTransitionEffect(chosen)
                Toast.makeText(this, "Efek transisi diubah ke: $chosen", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showSetDefaultLauncherGuideAndTrigger() {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    private fun toggleOverlaySpeedPermission() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                overlayPermissionLauncher.launch(intent)
            } catch (_: Exception) {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            }
            return
        }

        val currentEnabled = prefs.getBoolean(KEY_OVERLAY_SPEED_ENABLED, true)
        val next = !currentEnabled
        prefs.edit().putBoolean(KEY_OVERLAY_SPEED_ENABLED, next).apply()
        NetworkSpeedOverlayService.isOverlayEnabledPref = next
        DigitalClockOverlayService.isOverlayEnabledPref = next
        if (next) {
            NetworkSpeedOverlayService.startIfPermitted(this)
            DigitalClockOverlayService.startIfPermitted(this)
            Toast.makeText(this, "Jam & kecepatan jaringan tetap tampil di semua aplikasi", Toast.LENGTH_SHORT).show()
        } else {
            NetworkSpeedOverlayService.stop(this)
            DigitalClockOverlayService.stop(this)
            Toast.makeText(this, "Overlay jam & kecepatan di aplikasi lain dinonaktifkan", Toast.LENGTH_SHORT).show()
        }
        if (isSidebarOpen && currentSidebarType == SidebarType.SETTINGS) {
            populateSidebarContent(SidebarType.SETTINGS)
        }
    }

    private fun openDisableStockLauncherDialog() {
        val message = """
            Agar tombol HOME langsung kembali ke Aero TV dengan cepat di STB B866F:
            1. Masuk ke Pengaturan Android STB > Aplikasi > Aplikasi Default > Aplikasi Layar Utama, pilih Aero TV.
            2. Atau aktifkan Layanan Aksesibilitas Aero TV di Pengaturan > Aksesibilitas untuk menahan launcher bawaan STB.
        """.trimIndent()

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Matikan Tampilan Bawaan STB")
            .setMessage(message)
            .setPositiveButton("Buka Pengaturan Default") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            .setNeutralButton("Buka Aksesibilitas") { _, _ ->
                try {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                } catch (_: Exception) {
                    startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun showSystemAndBackupDialog() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val versionStr = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "5.3.0"
        } catch (_: Exception) { "5.3.0" }

        val options = arrayOf(
            "🔄 Muat Ulang Daftar Aplikasi Terpasang (Clear Cache)",
            "🗑️ Hapus Pengaturan Slideshow Foto",
            "ℹ️ Versi Launcher: Aero TV v$versionStr"
        )

        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Sistem & Cadangan")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        AppCache.cachedApps = null
                        lifecycleScope.launch(Dispatchers.IO) {
                            val fresh = loadInstalledApps()
                            AppCache.cachedApps = fresh
                            withContext(Dispatchers.Main) {
                                allInstalledApps = fresh
                                if (::bottomAppRowAdapter.isInitialized) {
                                    bottomAppRowAdapter.updateApps(fresh)
                                }
                                googleTvController?.updateInstalledApps(fresh)
                                nexfyController?.updateInstalledApps(fresh)
                                projectivyController?.updateInstalledApps(fresh)
                                atvController?.updateInstalledApps(fresh)
                                wolfController?.updateInstalledApps(fresh)
                                activeDrawerDialog?.updateApps(fresh)
                                Toast.makeText(this@MainActivity, "Daftar ${fresh.size} aplikasi diperbarui", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    1 -> {
                        wallpaperSlideshowManager.clearSlideshowSettings()
                        applyWallpaperMode(WallpaperMode.STATIC)
                        Toast.makeText(this, "Konfigurasi slideshow dibersihkan", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        Toast.makeText(this, "Aero TV Launcher v$versionStr (Android TV B866F)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    // ==========================================
    // LIFECYCLE & KEY EVENTS
    // ==========================================
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            setupImmersiveTvMode()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        syncActiveThemeIfNeeded(focusFirst = false)
    }

    override fun onResume() {
        super.onResume()
        syncActiveThemeIfNeeded(focusFirst = false)
        updateWeatherDate()
        refreshCurrentWifiStatus()
        fetchRealtimeWeather(forceRefresh = false)
        val showAnalog = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("key_show_analog_clock", true)
        analogClock.visibility = if (showAnalog && !isCustomHomeLayoutActive(this)) View.VISIBLE else View.GONE

        NetworkSpeedOverlayService.isLauncherForeground = true
        DigitalClockOverlayService.isLauncherForeground = true
        startupHandler.removeCallbacks(deferredOverlayStartRunnable)
        startupHandler.postDelayed(deferredOverlayStartRunnable, 600L)

        when (currentWallpaperMode) {
            WallpaperMode.STATIC -> WallpaperHelper.loadWallpaper(this, ivWallpaper)
            WallpaperMode.SLIDESHOW -> wallpaperSlideshowManager.startSlideshow(lifecycleScope)
            WallpaperMode.CANVAS_LIVE -> liveWallpaperCanvasView.startAnimation()
            WallpaperMode.STORAGE_LIVE -> storageLiveWallpaperManager.resume()
        }

        if (projectivyHostRoot?.visibility == View.VISIBLE) {
            projectivyController?.refreshWatchNext()
        }
        if (wolfHostRoot?.visibility == View.VISIBLE) {
            wolfController?.refreshStorageInfo(force = false)
        }

        if (allInstalledApps.isEmpty() || AppCache.cachedApps == null) {
            reloadInstalledAppsAsync(focusFirst = allInstalledApps.isEmpty())
        }
    }

    override fun onPause() {
        super.onPause()
        NetworkSpeedOverlayService.isLauncherForeground = false
        DigitalClockOverlayService.isLauncherForeground = false
    }

    override fun onStart() {
        super.onStart()
        timeTickReceiver = DateTimeHelper.registerTimeTickReceiver(this) {
            updateWeatherDate()
        }
    }

    override fun onStop() {
        super.onStop()
        wallpaperSlideshowManager.stopSlideshow()
        liveWallpaperCanvasView.stopAnimation()
        storageLiveWallpaperManager.pause()
        timeTickReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) { }
            timeTickReceiver = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        startupHandler.removeCallbacksAndMessages(null)
        networkMonitorJob?.cancel()
        weatherRefreshJob?.cancel()
        activeThemeDialog?.dismiss()
        activeThemeDialog = null
        activeDrawerDialog?.dismiss()
        activeDrawerDialog = null
        googleTvController = null
        googleTvHostRoot = null
        nexfyController = null
        nexfyHostRoot = null
        projectivyController = null
        projectivyHostRoot = null
        atvController = null
        atvHostRoot = null
        wolfController = null
        wolfHostRoot = null
        settingsGearAnimator?.cancel()
        settingsGearAnimator = null
        storageLiveWallpaperManager.release()
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) { }
        networkCallback?.let {
            try { connectivityManager?.unregisterNetworkCallback(it) } catch (_: Exception) { }
            networkCallback = null
        }
        btDiscoveryReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) { }
            btDiscoveryReceiver = null
        }
        packageChangeReceiver?.let {
            try { unregisterReceiver(it) } catch (_: Exception) { }
            packageChangeReceiver = null
        }
    }

    override fun onBackPressed() {
        if (layoutVoiceSearchOverlay.visibility == View.VISIBLE) {
            closeVoiceSearchOverlay()
            return
        }
        if (isSidebarOpen) {
            closeSidebar()
            return
        }
        if (::layoutLeagueSchedulePanel.isInitialized && layoutLeagueSchedulePanel.visibility == View.VISIBLE) {
            closeLeagueSchedule()
            return
        }
        if (googleTvHostRoot?.visibility == View.VISIBLE && googleTvController?.handleBackPressed() == true) {
            return
        }
        if (nexfyHostRoot?.visibility == View.VISIBLE && nexfyController?.handleBackPressed() == true) {
            return
        }
        if (projectivyHostRoot?.visibility == View.VISIBLE && projectivyController?.handleBackPressed() == true) {
            return
        }
        if (atvHostRoot?.visibility == View.VISIBLE && atvController?.handleBackPressed() == true) {
            return
        }
        super.onBackPressed()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (layoutVoiceSearchOverlay.visibility == View.VISIBLE) {
                closeVoiceSearchOverlay()
                return true
            }
            if (isSidebarOpen) {
                closeSidebar()
                return true
            }
            if (::layoutLeagueSchedulePanel.isInitialized && layoutLeagueSchedulePanel.visibility == View.VISIBLE) {
                closeLeagueSchedule()
                return true
            }
            if (googleTvHostRoot?.visibility == View.VISIBLE && googleTvController?.handleBackPressed() == true) {
                return true
            }
            if (nexfyHostRoot?.visibility == View.VISIBLE && nexfyController?.handleBackPressed() == true) {
                return true
            }
            if (projectivyHostRoot?.visibility == View.VISIBLE && projectivyController?.handleBackPressed() == true) {
                return true
            }
            if (atvHostRoot?.visibility == View.VISIBLE && atvController?.handleBackPressed() == true) {
                return true
            }
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            openSidebar(SidebarType.SETTINGS)
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_VOICE_ASSIST) {
            openVoiceSearchOverlay()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}
