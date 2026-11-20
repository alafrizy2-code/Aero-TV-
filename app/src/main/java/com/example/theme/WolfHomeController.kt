package com.example.theme

import android.app.ActivityManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.AppModel
import com.example.R
import com.example.WeatherAnimationView
import com.example.WeatherHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * WolfHomeController:
 * Mengimplementasikan Wolf Launcher Home Screen di atas wallpaper pribadi Aero TV:
 * - Kiri Atas: Widget Cuaca (menggunakan WeatherHelper & WeatherAnimationView asli Aero TV)
 *   dan Informasi Penyimpanan Perangkat aktual (StatFs + ActivityManager.MemoryInfo) di bawahnya.
 * - Kanan Atas: Status sistem aktual (Kecepatan jaringan, Wi-Fi, Bluetooth, Pengaturan) + Jam Digital besar + Tanggal.
 * - Bagian Bawah: Dock transparan horizontal membulat dengan ikon aplikasi terpasang + tombol All Apps di sisi kanan
 *   yang membuka App Drawer asli Aero TV (`openThemedAppDrawer`).
 */
class WolfHomeController(
    private val context: Context,
    private val rootView: View,
    private val onAppClick: (AppModel) -> Unit,
    private val onOpenAppDrawer: () -> Unit,
    private val onOpenWifi: () -> Unit,
    private val onOpenBluetooth: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onRefreshWeather: () -> Unit,
    private val onDisplaySettingsUpdated: () -> Unit = {}
) {
    private val density = context.resources.displayMetrics.density
    private val decelerate = DecelerateInterpolator()
    private val localeId = Locale("id", "ID")

    private var settings: ThemeManager.WolfDisplaySettings = ThemeManager.getWolfDisplaySettings(context)
    private var allInstalledApps: List<AppModel> = emptyList()

    // Top-Left Weather & Storage Widgets
    private val layoutTopLeftColumn: LinearLayout = rootView.findViewById(R.id.layoutWolfTopLeftColumn)
    private val cardWeather: LinearLayout = rootView.findViewById(R.id.cardWolfWeather)
    private val tvWeatherLocation: TextView = rootView.findViewById(R.id.tvWolfWeatherLocation)
    private val tvWeatherTemp: TextView = rootView.findViewById(R.id.tvWolfWeatherTemp)
    private val tvWeatherCondition: TextView = rootView.findViewById(R.id.tvWolfWeatherCondition)
    private val tvWeatherSubtitle: TextView = rootView.findViewById(R.id.tvWolfWeatherSubtitle)
    private val weatherAnimView: WeatherAnimationView = rootView.findViewById(R.id.wolfWeatherAnimView)

    private val cardStorage: LinearLayout = rootView.findViewById(R.id.cardWolfStorage)
    private val tvStorageAvailable: TextView = rootView.findViewById(R.id.tvWolfStorageAvailable)
    private val tvStorageInternal: TextView = rootView.findViewById(R.id.tvWolfStorageInternal)
    private val tvRamAvailable: TextView = rootView.findViewById(R.id.tvWolfRamAvailable)

    // Top-Right Clock & System Status
    private val layoutTopRight: LinearLayout = rootView.findViewById(R.id.layoutWolfTopRight)
    private val tvNetSpeed: TextView = rootView.findViewById(R.id.tvWolfNetSpeed)
    private val btnWifi: ImageButton = rootView.findViewById(R.id.btnWolfWifi)
    private val btnBluetooth: ImageButton = rootView.findViewById(R.id.btnWolfBluetooth)
    private val btnDisplaySettings: ImageButton = rootView.findViewById(R.id.btnWolfDisplaySettings)
    private val btnSettings: ImageButton = rootView.findViewById(R.id.btnWolfSettings)
    private val layoutClockGroup: LinearLayout = rootView.findViewById(R.id.layoutWolfClockGroup)
    private val tvClock: TextView = rootView.findViewById(R.id.tvWolfClock)
    private val tvDate: TextView = rootView.findViewById(R.id.tvWolfDate)

    // Bottom Rounded Dock + All Apps Button
    private val layoutDockBox: LinearLayout = rootView.findViewById(R.id.layoutWolfDockBox)
    private val rvDockApps: RecyclerView = rootView.findViewById(R.id.rvWolfDockApps)
    private val viewDockDivider: View = rootView.findViewById(R.id.viewWolfDockDivider)
    private val btnAllApps: LinearLayout = rootView.findViewById(R.id.btnWolfAllApps)
    private val allAppsIconBox: FrameLayout = rootView.findViewById(R.id.wolfAllAppsIconBox)
    private val tvAllAppsDockLabel: TextView = rootView.findViewById(R.id.tvWolfAllAppsDockLabel)
    private val tvAllAppsCaption: TextView = rootView.findViewById(R.id.tvWolfAllAppsCaption)

    private val dockAdapter = WolfDockAdapter()
    private var dockSpacingDecoration: RecyclerView.ItemDecoration? = null

    private var isWifiOnline: Boolean = false
    private var currentWifiSsid: String? = null
    private var isBluetoothOn: Boolean = true
    private var currentWeatherText: String = ""
    private var lastStorageRefreshMs: Long = 0L

    data class DeviceStorageStats(
        val internalAvailableBytes: Long,
        val internalTotalBytes: Long,
        val internalUsedBytes: Long,
        val ramAvailableBytes: Long,
        val ramTotalBytes: Long
    )

    init {
        setupTopWidgets()
        setupTopRightControls()
        setupDockRecyclerAndAllApps()
        applyDisplaySettings()
        refreshStorageInfo(force = true)
        updateClockAndDate()
        syncFromWeatherHelperCache()
    }

    private fun setupTopWidgets() {
        val styleWidgetFocus: (View, Int, Int, () -> Unit) -> Unit = { v, startCol, endCol, onClick ->
            val normal = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(startCol, endCol)
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * density
                setStroke(dp(1), Color.parseColor("#4DA3D9FF"))
            }
            val focused = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(startCol, endCol)
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 18f * density
                setStroke(dp(2), Color.WHITE)
            }
            v.background = normal
            v.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                view.background = if (hasFocus) focused else normal
                view.animate()
                    .scaleX(if (hasFocus) 1.03f else 1.0f)
                    .scaleY(if (hasFocus) 1.03f else 1.0f)
                    .setDuration(90L)
                    .setInterpolator(decelerate)
                    .start()
            }
            v.setOnClickListener { onClick() }
        }

        styleWidgetFocus(
            cardWeather,
            Color.parseColor("#B01668B8"),
            Color.parseColor("#8C0E467D")
        ) {
            onRefreshWeather()
            Toast.makeText(context, "Memperbarui data cuaca lokasi perangkat...", Toast.LENGTH_SHORT).show()
        }

        styleWidgetFocus(
            cardStorage,
            Color.parseColor("#A6124A7A"),
            Color.parseColor("#8A0B2B4B")
        ) {
            refreshStorageInfo(force = true)
            Toast.makeText(context, "Info penyimpanan & memori diperbarui", Toast.LENGTH_SHORT).show()
        }

        cardWeather.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                if (cardStorage.visibility == View.VISIBLE) {
                    cardStorage.requestFocus()
                } else {
                    focusDockFirst()
                }
                return@setOnKeyListener true
            }
            false
        }

        cardStorage.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (cardWeather.visibility == View.VISIBLE) {
                            cardWeather.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        focusDockFirst()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun setupTopRightControls() {
        val styleCircleBtn: (View, () -> Unit) -> Unit = { view, onClick ->
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#590F172A"))
                setStroke(dp(1), Color.parseColor("#33FFFFFF"))
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC1E3A5F"))
                setStroke(dp(2), Color.WHITE)
            }
            view.background = normal
            view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focused else normal
                v.animate().scaleX(if (hasFocus) 1.12f else 1.0f).scaleY(if (hasFocus) 1.12f else 1.0f).setDuration(85L).start()
            }
            view.setOnClickListener { onClick() }
        }

        styleCircleBtn(btnWifi) { onOpenWifi() }
        styleCircleBtn(btnBluetooth) { onOpenBluetooth() }
        styleCircleBtn(btnDisplaySettings) { openDisplaySettingsDialog() }
        styleCircleBtn(btnSettings) { onOpenSettings() }

        val topDownListener = View.OnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                focusDockFirst()
                return@OnKeyListener true
            }
            false
        }
        btnWifi.setOnKeyListener(topDownListener)
        btnBluetooth.setOnKeyListener(topDownListener)
        btnDisplaySettings.setOnKeyListener(topDownListener)
        btnSettings.setOnKeyListener(topDownListener)
    }

    private fun setupDockRecyclerAndAllApps() {
        rvDockApps.layoutManager = object : LinearLayoutManager(context, RecyclerView.HORIZONTAL, false) {
            override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                val container = rvDockApps.findContainingItemView(focused)
                val pos = container?.let { getPosition(it) } ?: RecyclerView.NO_POSITION
                if (pos != RecyclerView.NO_POSITION) {
                    if (direction == View.FOCUS_RIGHT) {
                        if (pos < itemCount - 1) {
                            findViewByPosition(pos + 1)?.let { return it }
                        } else {
                            return btnAllApps
                        }
                    } else if (direction == View.FOCUS_LEFT) {
                        if (pos > 0) {
                            findViewByPosition(pos - 1)?.let { return it }
                        }
                        return focused
                    }
                }
                if (direction == View.FOCUS_UP) {
                    return resolveUpFocusTarget()
                }
                return super.onInterceptFocusSearch(focused, direction)
            }
        }
        rvDockApps.itemAnimator = null
        rvDockApps.setHasFixedSize(false)
        rvDockApps.setItemViewCacheSize(24)
        rvDockApps.adapter = dockAdapter

        btnAllApps.setOnClickListener { onOpenAppDrawer() }
        btnAllApps.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        onOpenAppDrawer()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        val lastPos = (dockAdapter.itemCount - 1).coerceAtLeast(0)
                        val lastView = rvDockApps.findViewHolderForAdapterPosition(lastPos)?.itemView
                        if (lastView != null) {
                            lastView.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        resolveUpFocusTarget()?.requestFocus()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_MENU -> {
                        openDisplaySettingsDialog()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun resolveUpFocusTarget(): View? {
        return when {
            cardStorage.visibility == View.VISIBLE -> cardStorage
            cardWeather.visibility == View.VISIBLE -> cardWeather
            else -> btnDisplaySettings
        }
    }

    fun applyDisplaySettings() {
        settings = ThemeManager.getWolfDisplaySettings(context)

        // 1. Visibility widget cuaca, penyimpanan, jam/tanggal
        cardWeather.visibility = if (settings.showWeatherWidget) View.VISIBLE else View.GONE
        cardStorage.visibility = if (settings.showStorageInfo) View.VISIBLE else View.GONE
        layoutTopLeftColumn.visibility =
            if (settings.showWeatherWidget || settings.showStorageInfo) View.VISIBLE else View.GONE
        layoutClockGroup.visibility = if (settings.showClockAndDate) View.VISIBLE else View.GONE

        // 2. Skala ukuran widget kiri atas & jam
        val wScale = settings.widgetSizeScale.coerceIn(0.80f, 1.35f)
        val widgetW = dp((268 * wScale).roundToInt())
        cardWeather.layoutParams = (cardWeather.layoutParams as LinearLayout.LayoutParams).apply {
            width = widgetW
        }
        cardStorage.layoutParams = (cardStorage.layoutParams as LinearLayout.LayoutParams).apply {
            width = widgetW
        }
        tvWeatherTemp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f * wScale)
        tvWeatherCondition.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f * wScale)
        tvStorageAvailable.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f * wScale)
        val animPx = dp((54 * wScale).roundToInt())
        weatherAnimView.layoutParams = (weatherAnimView.layoutParams as LinearLayout.LayoutParams).apply {
            width = animPx
            height = animPx
        }
        tvClock.setTextSize(TypedValue.COMPLEX_UNIT_SP, 42f * wScale)
        tvDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f * wScale)

        // 3. Transparansi dock bawah membulat
        val alphaVal = ((settings.dockAlphaPercent.coerceIn(15, 95) / 100f) * 255f).roundToInt().coerceIn(28, 245)
        layoutDockBox.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 26f * density
            setColor(Color.argb(alphaVal, 15, 23, 42))
            setStroke(dp(1), Color.parseColor("#40FFFFFF"))
        }

        // 4. Ukuran & jarak ikon di dock + tombol All Apps
        val iconPx = dp(settings.iconSizeDp)
        allAppsIconBox.layoutParams = (allAppsIconBox.layoutParams as LinearLayout.LayoutParams).apply {
            width = iconPx
            height = iconPx
        }
        val radiusPx = 14f * density
        val allAppsNormalBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.parseColor("#40FFFFFF"))
            setStroke(dp(1), Color.parseColor("#38FFFFFF"))
        }
        val allAppsFocusedBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.parseColor("#731E3A8A"))
            setStroke(dp(3), Color.WHITE)
        }
        allAppsIconBox.background = if (btnAllApps.isFocused) allAppsFocusedBg else allAppsNormalBg
        tvAllAppsDockLabel.visibility = when (settings.dockLabelMode) {
            ThemeManager.WolfDockLabelMode.ALWAYS -> View.VISIBLE
            ThemeManager.WolfDockLabelMode.ON_FOCUS -> if (btnAllApps.isFocused) View.VISIBLE else View.INVISIBLE
            ThemeManager.WolfDockLabelMode.HIDDEN -> View.GONE
        }
        tvAllAppsCaption.visibility = View.VISIBLE
        btnAllApps.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            allAppsIconBox.background = if (hasFocus) allAppsFocusedBg else allAppsNormalBg
            if (settings.dockLabelMode == ThemeManager.WolfDockLabelMode.ON_FOCUS) {
                tvAllAppsDockLabel.visibility = if (hasFocus) View.VISIBLE else View.INVISIBLE
            }
            v.animate()
                .scaleX(if (hasFocus) 1.13f else 1.0f)
                .scaleY(if (hasFocus) 1.13f else 1.0f)
                .setDuration(90L)
                .setInterpolator(decelerate)
                .start()
        }

        // Jarak antar ikon dock
        dockSpacingDecoration?.let { rvDockApps.removeItemDecoration(it) }
        val halfGap = dp(settings.cardSpacingDp) / 2
        val newDeco = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.set(halfGap, 0, halfGap, 0)
            }
        }
        dockSpacingDecoration = newDeco
        rvDockApps.addItemDecoration(newDeco)

        // Batasi lebar maks dock agar tetap rapi di layar 1920x1080
        val screenW = context.resources.displayMetrics.widthPixels
        val maxDockListW = (screenW - dp(210)).coerceAtLeast(dp(320))
        rvDockApps.layoutParams = (rvDockApps.layoutParams as LinearLayout.LayoutParams).apply {
            width = ViewGroup.LayoutParams.WRAP_CONTENT
        }
        rvDockApps.post {
            if (rvDockApps.width > maxDockListW) {
                rvDockApps.layoutParams = (rvDockApps.layoutParams as LinearLayout.LayoutParams).apply {
                    width = maxDockListW
                }
            }
        }

        refreshDockApps()
    }

    fun openDisplaySettingsDialog() {
        WolfDisplaySettingsDialog(context) {
            applyDisplaySettings()
            onDisplaySettingsUpdated()
        }.show()
    }

    fun focusPrimaryItem() {
        focusDockFirst()
    }

    private fun focusDockFirst() {
        rvDockApps.post {
            rvDockApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                ?: btnAllApps.requestFocus()
        }
    }

    fun updateInstalledApps(apps: List<AppModel>) {
        allInstalledApps = apps
        refreshDockApps()
    }

    private fun refreshDockApps() {
        val visibleApps = ThemeManager.orderAppsForWolfDock(
            context = context,
            apps = allInstalledApps,
            visibleCount = settings.visibleDockAppsCount
        )
        dockAdapter.update(visibleApps)
        viewDockDivider.visibility = if (visibleApps.isNotEmpty()) View.VISIBLE else View.GONE
    }

    fun updateNetworkSpeed(speedText: String) {
        if (speedText.isNotBlank()) {
            tvNetSpeed.text = speedText
        }
    }

    fun updateSystemStatus(
        wifiOnline: Boolean,
        wifiSsid: String?,
        bluetoothOn: Boolean,
        weatherText: String,
        weatherCode: Int? = null,
        isDay: Int = 1
    ) {
        isWifiOnline = wifiOnline
        currentWifiSsid = wifiSsid
        isBluetoothOn = bluetoothOn
        currentWeatherText = weatherText

        btnWifi.setColorFilter(if (wifiOnline) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
        btnWifi.alpha = if (wifiOnline) 1.0f else 0.72f
        btnWifi.contentDescription = if (wifiOnline) "Wi-Fi: ${wifiSsid ?: "Terhubung"}" else "Wi-Fi: Offline"

        btnBluetooth.setColorFilter(if (bluetoothOn) Color.parseColor("#38BDF8") else Color.parseColor("#94A3B8"))
        btnBluetooth.alpha = if (bluetoothOn) 1.0f else 0.72f
        btnBluetooth.contentDescription = if (bluetoothOn) "Bluetooth: Aktif" else "Bluetooth: Nonaktif"

        updateClockAndDate()
        applyWeatherState(weatherText, weatherCode, isDay)
        refreshStorageInfo(force = false)
    }

    fun setWeatherAnimationType(type: WeatherAnimationView.WeatherType) {
        weatherAnimView.setWeatherType(type)
    }

    fun updateClockAndDate() {
        val now = Date()
        tvClock.text = try {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        } catch (_: Exception) {
            "00:00"
        }
        tvDate.text = try {
            val formatted = SimpleDateFormat("EEEE, d MMM yyyy", localeId).format(now)
            formatted.replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeId) else it.toString() }
        } catch (_: Exception) {
            SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(now)
        }
    }

    private fun syncFromWeatherHelperCache() {
        val snapshot = WeatherHelper.getCachedSnapshot(context)
        if (snapshot != null) {
            applyWeatherState(
                weatherText = snapshot.formattedText,
                weatherCode = snapshot.weatherCode,
                isDay = snapshot.isDay
            )
        }
    }

    private fun applyWeatherState(weatherText: String, weatherCode: Int?, isDay: Int) {
        val snapshot = WeatherHelper.getCachedSnapshot(context)
        val raw = weatherText.trim().ifBlank { snapshot?.formattedText ?: "" }

        if (weatherCode != null) {
            weatherAnimView.setWeatherByCode(weatherCode, isDay)
        } else if (snapshot != null) {
            weatherAnimView.setWeatherByCode(snapshot.weatherCode, snapshot.isDay)
        }

        if (raw.isBlank() || raw.contains("Mendeteksi", ignoreCase = true) || raw.contains("Memuat", ignoreCase = true)) {
            val cachedDistrict = WeatherHelper.getCachedDistrictName(context)
            tvWeatherLocation.text = (cachedDistrict ?: "Mendeteksi Lokasi").uppercase(localeId)
            tvWeatherTemp.text = snapshot?.tempC?.let { "$it°" } ?: "--°"
            tvWeatherCondition.text = snapshot?.condition ?: raw.ifBlank { "Memuat cuaca..." }
            tvWeatherSubtitle.text = "Ketuk OK untuk perbarui cuaca"
            return
        }

        // Format standar WeatherHelper: "[Kecamatan] [Suhu]°C [Kondisi]" atau "[Kecamatan] • [Kondisi]"
        val tempMatch = Regex("(-?\\d+)\\s*°C?\\s*(.*)$").find(raw)
        if (tempMatch != null) {
            val prefixDistrict = raw.substring(0, tempMatch.range.first).trim()
                .ifBlank { snapshot?.districtName ?: WeatherHelper.getCachedDistrictName(context) ?: "Lokasi Perangkat" }
            val tempNum = tempMatch.groupValues[1]
            val cond = tempMatch.groupValues[2].trim()
                .ifBlank { snapshot?.condition ?: "Cerah Berawan" }
            tvWeatherLocation.text = prefixDistrict.uppercase(localeId)
            tvWeatherTemp.text = "$tempNum°"
            tvWeatherCondition.text = cond
            tvWeatherSubtitle.text = "Cuaca real-time perangkat"
        } else if (raw.contains("•")) {
            val parts = raw.split("•", limit = 2)
            tvWeatherLocation.text = parts[0].trim().ifBlank { "LOKASI PERANGKAT" }.uppercase(localeId)
            tvWeatherTemp.text = snapshot?.tempC?.let { "$it°" } ?: "--°"
            tvWeatherCondition.text = parts.getOrElse(1) { "" }.trim().ifBlank { "Tidak tersedia" }
            tvWeatherSubtitle.text = "Ketuk OK untuk segarkan"
        } else {
            tvWeatherLocation.text = (snapshot?.districtName ?: WeatherHelper.getCachedDistrictName(context) ?: "LOKASI PERANGKAT").uppercase(localeId)
            tvWeatherTemp.text = snapshot?.tempC?.let { "$it°" } ?: "--°"
            tvWeatherCondition.text = raw
            tvWeatherSubtitle.text = "Cuaca real-time perangkat"
        }
    }

    fun refreshStorageInfo(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastStorageRefreshMs) < 15_000L) return
        lastStorageRefreshMs = now

        val stats = readRealDeviceStorageStats(context)
        val availStr = formatBytesCompact(stats.internalAvailableBytes)
        val usedStr = formatBytesCompact(stats.internalUsedBytes)
        val totalStr = formatBytesCompact(stats.internalTotalBytes)
        val ramAvailStr = formatBytesCompact(stats.ramAvailableBytes)
        val ramTotalStr = formatBytesCompact(stats.ramTotalBytes)

        tvStorageAvailable.text = "$availStr tersedia"
        tvStorageInternal.text = "Terpakai: $usedStr / $totalStr"
        tvRamAvailable.text = if (stats.ramTotalBytes > 0L) {
            "Memori tersedia: $ramAvailStr / $ramTotalStr"
        } else {
            "Memori tersedia: $ramAvailStr"
        }
    }

    private fun showDockAppOptionsDialog(app: AppModel) {
        val isFav = ThemeManager.isWolfDockFavorite(context, app.packageName)
        val options = mutableListOf(
            "▶ Buka ${app.appName}",
            if (isFav) "☆ Lepas Sematan dari Depan Dock" else "★ Sematkan ke Depan Dock Wolf"
        )
        if (isFav) {
            options.add("⬅ Geser ke Kiri di Dock")
            options.add("➡ Geser ke Kanan di Dock")
        }
        options.add("📱 Buka Semua Aplikasi (App Drawer)")
        options.add("⚙ Pengaturan Tampilan Wolf")
        options.add("ℹ️ Info Aplikasi")
        options.add("🗑️ Copot Pemasangan (Uninstall)")

        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(app.appName)
            .setItems(options.toTypedArray()) { _, which ->
                val chosen = options[which]
                when {
                    chosen.startsWith("▶ Buka") -> onAppClick(app)
                    chosen.startsWith("★ Sematkan") || chosen.startsWith("☆ Lepas") -> {
                        val added = ThemeManager.toggleWolfDockFavorite(context, app.packageName)
                        refreshDockApps()
                        Toast.makeText(
                            context,
                            if (added) "${app.appName} disematkan di depan Dock" else "${app.appName} dilepas dari sematan Dock",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    chosen.startsWith("⬅ Geser") -> {
                        if (ThemeManager.moveWolfDockFavorite(context, app.packageName, -1)) {
                            refreshDockApps()
                        }
                    }
                    chosen.startsWith("➡ Geser") -> {
                        if (ThemeManager.moveWolfDockFavorite(context, app.packageName, +1)) {
                            refreshDockApps()
                        }
                    }
                    chosen.startsWith("📱 Buka Semua") -> onOpenAppDrawer()
                    chosen.startsWith("⚙ Pengaturan") -> openDisplaySettingsDialog()
                    chosen.startsWith("ℹ️ Info") -> {
                        try {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.parse("package:${app.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) { }
                    }
                    chosen.startsWith("🗑️ Copot") -> {
                        try {
                            context.startActivity(Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.parse("package:${app.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            })
                        } catch (_: Exception) { }
                    }
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    // ==============================================
    // ADAPTER DOCK WOLF LAUNCHER
    // ==============================================
    private inner class WolfDockAdapter : RecyclerView.Adapter<WolfDockAdapter.WolfDockVH>() {
        private var items: List<AppModel> = emptyList()

        fun update(newItems: List<AppModel>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WolfDockVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_wolf_dock_app, parent, false)
            return WolfDockVH(view)
        }

        override fun onBindViewHolder(holder: WolfDockVH, position: Int) {
            holder.bind(items[position])
        }

        inner class WolfDockVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardBox: FrameLayout = itemView.findViewById(R.id.wolfDockCardContainer)
            private val ivIcon: ImageView = itemView.findViewById(R.id.ivWolfDockIcon)
            private val tvFavDot: TextView = itemView.findViewById(R.id.tvWolfDockFavDot)
            private val viewFocusBorder: View = itemView.findViewById(R.id.viewWolfDockFocusBorder)
            private val viewBottomBar: View = itemView.findViewById(R.id.viewWolfDockBottomBar)
            private val tvAppName: TextView = itemView.findViewById(R.id.tvWolfDockAppName)

            fun bind(app: AppModel) {
                val iconPx = dp(settings.iconSizeDp)
                val radiusPx = 14f * density

                cardBox.layoutParams = (cardBox.layoutParams as LinearLayout.LayoutParams).apply {
                    width = iconPx
                    height = iconPx
                }
                ivIcon.clipToOutline = true
                ivIcon.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                    }
                }
                ivIcon.setImageDrawable(app.icon)

                tvAppName.text = app.appName
                tvAppName.layoutParams = (tvAppName.layoutParams as LinearLayout.LayoutParams).apply {
                    width = (iconPx + dp(18)).coerceAtLeast(dp(64))
                }
                tvFavDot.visibility = if (ThemeManager.isWolfDockFavorite(context, app.packageName)) View.VISIBLE else View.GONE

                viewFocusBorder.background = when (settings.focusEffect) {
                    ThemeManager.WolfFocusEffect.WHITE_CYAN_GLOW -> {
                        val outer = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = radiusPx
                            setColor(Color.TRANSPARENT)
                            setStroke(dp(3), Color.WHITE)
                        }
                        val inner = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = (radiusPx - 2f).coerceAtLeast(4f)
                            setColor(Color.TRANSPARENT)
                            setStroke(dp(1), Color.parseColor("#22D3EE"))
                        }
                        LayerDrawable(arrayOf(outer, inner)).apply {
                            val inset = dp(2)
                            setLayerInset(1, inset, inset, inset, inset)
                        }
                    }
                    ThemeManager.WolfFocusEffect.WHITE_RING_ZOOM -> GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = radiusPx
                        setColor(Color.TRANSPARENT)
                        setStroke(dp(3), Color.WHITE)
                    }
                    ThemeManager.WolfFocusEffect.BOTTOM_ACCENT_BAR,
                    ThemeManager.WolfFocusEffect.SOFT_LIFT_ONLY -> null
                }

                val applyFocusState: (Boolean, Boolean) -> Unit = { hasFocus, animate ->
                    viewFocusBorder.visibility = if (
                        hasFocus && (
                            settings.focusEffect == ThemeManager.WolfFocusEffect.WHITE_CYAN_GLOW ||
                                settings.focusEffect == ThemeManager.WolfFocusEffect.WHITE_RING_ZOOM
                            )
                    ) View.VISIBLE else View.GONE

                    viewBottomBar.visibility = if (
                        hasFocus && settings.focusEffect == ThemeManager.WolfFocusEffect.BOTTOM_ACCENT_BAR
                    ) View.VISIBLE else View.GONE

                    tvAppName.visibility = when (settings.dockLabelMode) {
                        ThemeManager.WolfDockLabelMode.ALWAYS -> View.VISIBLE
                        ThemeManager.WolfDockLabelMode.ON_FOCUS -> if (hasFocus) View.VISIBLE else View.INVISIBLE
                        ThemeManager.WolfDockLabelMode.HIDDEN -> View.GONE
                    }
                    itemView.elevation = if (hasFocus) 16f else 2f
                    itemView.alpha = if (hasFocus) 1.0f else 0.94f

                    val targetScale = if (hasFocus) 1.14f else 1.0f
                    if (animate) {
                        itemView.animate()
                            .scaleX(targetScale)
                            .scaleY(targetScale)
                            .setDuration(90L)
                            .setInterpolator(decelerate)
                            .start()
                    } else {
                        itemView.scaleX = targetScale
                        itemView.scaleY = targetScale
                    }
                }
                applyFocusState(itemView.isFocused, false)

                itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    applyFocusState(hasFocus, true)
                    if (hasFocus) {
                        v.requestRectangleOnScreen(Rect(0, 0, v.width, v.height), false)
                    }
                }

                itemView.setOnClickListener { onAppClick(app) }
                itemView.setOnLongClickListener {
                    showDockAppOptionsDialog(app)
                    true
                }
                itemView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                onAppClick(app)
                                return@setOnKeyListener true
                            }
                            KeyEvent.KEYCODE_MENU -> {
                                showDockAppOptionsDialog(app)
                                return@setOnKeyListener true
                            }
                        }
                    }
                    false
                }
            }
        }
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()

    companion object {
        fun readRealDeviceStorageStats(context: Context): DeviceStorageStats {
            var totalBytes = 0L
            var availBytes = 0L
            try {
                val path = Environment.getDataDirectory()
                val stat = StatFs(path.absolutePath)
                totalBytes = stat.totalBytes
                availBytes = stat.availableBytes
            } catch (_: Exception) {
                try {
                    val f = context.filesDir
                    totalBytes = f.totalSpace
                    availBytes = f.usableSpace
                } catch (_: Exception) { }
            }
            val usedBytes = (totalBytes - availBytes).coerceAtLeast(0L)

            var availRam = 0L
            var totalRam = 0L
            try {
                val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                if (am != null) {
                    val mi = ActivityManager.MemoryInfo()
                    am.getMemoryInfo(mi)
                    availRam = mi.availMem
                    totalRam = mi.totalMem
                }
            } catch (_: Exception) { }

            return DeviceStorageStats(
                internalAvailableBytes = availBytes,
                internalTotalBytes = totalBytes,
                internalUsedBytes = usedBytes,
                ramAvailableBytes = availRam,
                ramTotalBytes = totalRam
            )
        }

        fun formatBytesCompact(bytes: Long): String {
            val locale = Locale("id", "ID")
            val gb = bytes / (1024.0 * 1024.0 * 1024.0)
            if (gb >= 1.0) {
                return String.format(locale, "%.1f GB", gb)
            }
            val mb = bytes / (1024.0 * 1024.0)
            return String.format(locale, "%.0f MB", mb.coerceAtLeast(0.0))
        }
    }
}
