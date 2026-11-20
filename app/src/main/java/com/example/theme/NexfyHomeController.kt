package com.example.theme

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.AppModel
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class NexfyViewMode {
    HOME,
    ALL_APPS
}

/**
 * NexfyHomeController:
 * Mengatur tampilan Nexfy Home Screen (Referensi 1) dan Nexfy All Apps (Referensi 2)
 * di atas layer wallpaper asli pengguna tanpa animasi berat/blur real-time (optimal untuk B866F).
 */
class NexfyHomeController(
    private val context: Context,
    private val rootView: View,
    private val isDrawerStandaloneMode: Boolean = false,
    private val onAppClick: (AppModel) -> Unit,
    private val onOpenWifi: () -> Unit,
    private val onOpenBluetooth: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onOpenVoiceSearch: () -> Unit,
    private val onRefreshWeather: () -> Unit,
    private val onDismissStandaloneDrawer: () -> Unit = {}
) {
    private val density = context.resources.displayMetrics.density
    private val scale = (context.resources.displayMetrics.widthPixels / 1920f).coerceAtLeast(0.5f)
    private val decelerate = DecelerateInterpolator()

    private var activeMode: NexfyViewMode = if (isDrawerStandaloneMode) NexfyViewMode.ALL_APPS else NexfyViewMode.HOME
    private var selectedCategory: NexfyCategory = NexfyCategory.SEMUA
    private var allInstalledApps: List<AppModel> = emptyList()
    private var searchQuery: String = ""

    private var isWifiOnline: Boolean = false
    private var currentWifiSsid: String? = null
    private var isBluetoothOn: Boolean = false
    private var currentWeatherText: String = ""

    // Scrims & Sections
    private val viewTopScrim: View = rootView.findViewById(R.id.viewNexfyTopScrim)
    private val viewBottomScrim: View = rootView.findViewById(R.id.viewNexfyBottomScrim)
    private val viewAllAppsScrim: View = rootView.findViewById(R.id.viewNexfyAllAppsScrim)

    // Top Bar
    private val btnProfile: FrameLayout = rootView.findViewById(R.id.btnNexfyProfile)
    private val layoutAllAppsTitleArea: LinearLayout = rootView.findViewById(R.id.layoutNexfyAllAppsTitleArea)
    private val tvAllAppsCountBadge: TextView = rootView.findViewById(R.id.tvNexfyAllAppsCountBadge)
    private val btnBluetooth: ImageButton = rootView.findViewById(R.id.btnNexfyBluetooth)
    private val btnWifi: ImageButton = rootView.findViewById(R.id.btnNexfyWifi)
    private val btnNotifications: ImageButton = rootView.findViewById(R.id.btnNexfyNotifications)
    private val btnSearch: ImageButton = rootView.findViewById(R.id.btnNexfySearch)
    private val btnSettings: ImageButton = rootView.findViewById(R.id.btnNexfySettings)
    private val layoutWeatherClockCapsule: LinearLayout = rootView.findViewById(R.id.layoutNexfyWeatherClockCapsule)
    private val btnWeather: LinearLayout = rootView.findViewById(R.id.btnNexfyWeather)
    private val tvWeather: TextView = rootView.findViewById(R.id.tvNexfyWeather)
    private val tvClock: TextView = rootView.findViewById(R.id.tvNexfyClock)

    // Home Body (Referensi 1)
    private val layoutHomeBody: LinearLayout = rootView.findViewById(R.id.layoutNexfyHomeBody)
    private val layoutCategoryBar: LinearLayout = rootView.findViewById(R.id.layoutNexfyCategoryBar)
    private val btnQuickPin: ImageButton = rootView.findViewById(R.id.btnNexfyQuickPin)
    private val tvShelfTitle: TextView = rootView.findViewById(R.id.tvNexfyShelfTitle)
    private val tvShelfHint: TextView = rootView.findViewById(R.id.tvNexfyShelfHint)
    private val rvHomeApps: RecyclerView = rootView.findViewById(R.id.rvNexfyHomeApps)

    // All Apps Body (Referensi 2)
    private val layoutAllAppsBody: LinearLayout = rootView.findViewById(R.id.layoutNexfyAllAppsBody)
    private val layoutSearchBar: LinearLayout = rootView.findViewById(R.id.layoutNexfySearchBar)
    private val etSearch: EditText = rootView.findViewById(R.id.etNexfySearch)
    private val btnClearSearch: ImageButton = rootView.findViewById(R.id.btnNexfyClearSearch)
    private val btnVoiceSearch: ImageButton = rootView.findViewById(R.id.btnNexfyVoiceSearch)
    private val btnCloseSearch: ImageButton = rootView.findViewById(R.id.btnNexfyCloseSearch)
    private val tvAllAppsEmpty: TextView = rootView.findViewById(R.id.tvNexfyAllAppsEmpty)
    private val rvAllAppsGrid: RecyclerView = rootView.findViewById(R.id.rvNexfyAllAppsGrid)

    private val homeAdapter = NexfyCardAdapter(isHomeDock = true)
    private val allAppsAdapter = NexfyCardAdapter(isHomeDock = false)
    private var gridColumns: Int = 6
    private var gridCardWidthPx: Int = 272
    private var gridCardHeightPx: Int = 154

    init {
        computeAllAppsGridMetrics()
        setupTopStatusBar()
        setupCategoryBar()
        setupQuickPinButton()
        setupHomeAppsRow()
        setupAllAppsGridAndSearch()

        switchMode(activeMode, focusContent = false)
        updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, currentWeatherText)
    }

    private fun computeAllAppsGridMetrics() {
        val screenW = context.resources.displayMetrics.widthPixels
        val sidePaddingPx = dp(36) * 2
        val gapPx = dp(10)
        val availW = (screenW - sidePaddingPx).coerceAtLeast(dp(480))
        gridColumns = if (screenW >= 1200) 6 else 4
        gridCardWidthPx = ((availW - (gridColumns - 1) * gapPx) / gridColumns).coerceAtLeast(dp(112))
        gridCardHeightPx = (gridCardWidthPx * 0.565f).roundToInt().coerceAtLeast(dp(64))
    }

    private fun setupTopStatusBar() {
        layoutWeatherClockCapsule.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor("#8C0F172A"))
            setStroke(dp(1), Color.parseColor("#2BFFFFFF"))
        }

        val setupCircleTopBtn: (View, () -> Unit) -> Unit = { view, onClick ->
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#7A0F172A"))
                setStroke(dp(1), Color.parseColor("#2EFFFFFF"))
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC1E3A5F"))
                setStroke(dp(2), Color.WHITE)
            }
            view.background = normal
            view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focused else normal
                v.animate().scaleX(if (hasFocus) 1.12f else 1.0f).scaleY(if (hasFocus) 1.12f else 1.0f).setDuration(95L).start()
            }
            view.setOnClickListener { onClick() }
        }

        setupCircleTopBtn(btnProfile) {
            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Nexfy Launcher Theme • Aero TV")
                .setMessage(
                    "Tema aktif: Nexfy Launcher Theme\n" +
                        "Aplikasi terpasang: ${allInstalledApps.size}\n" +
                        "Kategori aktif: ${selectedCategory.label}\n" +
                        "Wallpaper utama pengguna tetap aktif di latar belakang."
                )
                .setPositiveButton("All Apps") { _, _ -> switchMode(NexfyViewMode.ALL_APPS, focusContent = true) }
                .setNeutralButton("Pengaturan") { _, _ -> onOpenSettings() }
                .setNegativeButton("Tutup", null)
                .show()
        }

        setupCircleTopBtn(btnBluetooth) { onOpenBluetooth() }
        setupCircleTopBtn(btnWifi) { onOpenWifi() }
        setupCircleTopBtn(btnNotifications) { showSystemStatusNotificationsDialog() }
        setupCircleTopBtn(btnSearch) { openOrToggleSearch() }
        setupCircleTopBtn(btnSettings) { onOpenSettings() }

        val weatherNormal = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.TRANSPARENT)
        }
        val weatherFocused = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor("#3338BDF8"))
            setStroke(dp(1), Color.WHITE)
        }
        btnWeather.background = weatherNormal
        btnWeather.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            v.background = if (hasFocus) weatherFocused else weatherNormal
        }
        btnWeather.setOnClickListener {
            onRefreshWeather()
            Toast.makeText(context, "Memperbarui informasi cuaca lokasi saat ini...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSystemStatusNotificationsDialog() {
        val netStatus = if (isWifiOnline) "Terhubung (${currentWifiSsid ?: "Online"})" else "Tidak terhubung"
        val btStatus = if (isBluetoothOn) "Aktif" else "Nonaktif"
        val weatherStr = currentWeatherText.ifBlank { "Menunggu pembaruan cuaca" }
        val pinnedCount = NexfyContentClassifier.getPinnedPackages(context).size

        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Status & Notifikasi Sistem")
            .setMessage(
                "• Jaringan Wi-Fi: $netStatus\n" +
                    "• Bluetooth: $btStatus\n" +
                    "• Cuaca Saat Ini: $weatherStr\n" +
                    "• Aplikasi Terpasang: ${allInstalledApps.size} aplikasi\n" +
                    "• Aplikasi Disematkan: $pinnedCount aplikasi"
            )
            .setPositiveButton("Buka Pengaturan") { _, _ -> onOpenSettings() }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun setupCategoryBar() {
        layoutCategoryBar.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6f * density
            setColor(Color.parseColor("#880C1220"))
            setStroke(dp(1), Color.parseColor("#24FFFFFF"))
        }
        rebuildCategoryTabs()
    }

    private fun rebuildCategoryTabs() {
        layoutCategoryBar.removeAllViews()
        val categories = NexfyCategory.values()

        for ((index, category) in categories.withIndex()) {
            val isActive = (category == selectedCategory)
            val tab = TextView(context).apply {
                text = category.label
                textSize = 11.5f
                gravity = Gravity.CENTER
                maxLines = 1
                isFocusable = true
                isClickable = true
                typeface = if (isActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                ).apply {
                    if (index > 0) marginStart = dp(2)
                }

                val applyTabVisual: (Boolean) -> Unit = { hasFocus ->
                    val bgColor = when {
                        isActive && hasFocus -> Color.WHITE
                        isActive -> Color.parseColor("#F1F5F9")
                        hasFocus -> Color.parseColor("#44FFFFFF")
                        else -> Color.TRANSPARENT
                    }
                    setTextColor(if (isActive) Color.parseColor("#0F172A") else Color.parseColor("#E2E8F0"))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 5f * density
                        setColor(bgColor)
                        if (hasFocus) {
                            setStroke(dp(2), if (isActive) Color.parseColor("#38BDF8") else Color.WHITE)
                        }
                    }
                }
                applyTabVisual(isFocused)

                onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    applyTabVisual(hasFocus)
                    v.animate().scaleX(if (hasFocus) 1.04f else 1.0f).scaleY(if (hasFocus) 1.04f else 1.0f).setDuration(90L).start()
                }
                setOnClickListener {
                    if (selectedCategory != category) {
                        selectedCategory = category
                        rebuildCategoryTabs()
                        refreshHomeShelf()
                    }
                }
                setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                if (selectedCategory != category) {
                                    selectedCategory = category
                                    rebuildCategoryTabs()
                                    refreshHomeShelf()
                                }
                                return@setOnKeyListener true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                rvHomeApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                                    ?: btnQuickPin.requestFocus()
                                return@setOnKeyListener true
                            }
                        }
                    }
                    false
                }
            }
            layoutCategoryBar.addView(tab)
        }
    }

    private fun setupQuickPinButton() {
        val normal = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6f * density
            setColor(Color.parseColor("#661E293B"))
            setStroke(dp(1), Color.parseColor("#38FFFFFF"))
        }
        val focused = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6f * density
            setColor(Color.parseColor("#CC1E3A5F"))
            setStroke(dp(2), Color.WHITE)
        }
        btnQuickPin.background = normal
        btnQuickPin.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            v.background = if (hasFocus) focused else normal
            v.animate().scaleX(if (hasFocus) 1.1f else 1.0f).scaleY(if (hasFocus) 1.1f else 1.0f).setDuration(95L).start()
        }
        btnQuickPin.setOnClickListener { showQuickPinDialog() }
    }

    private fun showQuickPinDialog() {
        if (allInstalledApps.isEmpty()) return
        val pinnedSet = NexfyContentClassifier.getPinnedPackages(context).toHashSet()
        val labels = allInstalledApps.map { app ->
            if (pinnedSet.contains(app.packageName)) "★ ${app.appName} (Disematkan)" else app.appName
        }.toTypedArray()

        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Sematkan Aplikasi ke Depan Baris Nexfy")
            .setItems(labels) { _, which ->
                val chosen = allInstalledApps[which]
                val nowPinned = NexfyContentClassifier.togglePinnedPackage(context, chosen.packageName)
                refreshHomeShelf()
                if (activeMode == NexfyViewMode.ALL_APPS) {
                    refreshAllAppsGrid()
                }
                Toast.makeText(
                    context,
                    if (nowPinned) "${chosen.appName} disematkan di depan" else "${chosen.appName} dilepas dari sematan",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun setupHomeAppsRow() {
        val extraPrefetchPx = (480f * scale).roundToInt().coerceAtLeast(240)
        rvHomeApps.apply {
            layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
                @Deprecated("Deprecated in Java")
                override fun getExtraLayoutSpace(state: RecyclerView.State): Int = extraPrefetchPx

                override fun calculateExtraLayoutSpace(state: RecyclerView.State, extraLayoutSpace: IntArray) {
                    extraLayoutSpace[0] = extraPrefetchPx
                    extraLayoutSpace[1] = extraPrefetchPx
                }

                override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                    if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {
                        val container = findContainingItemView(focused)
                        val pos = container?.let { getPosition(it) } ?: RecyclerView.NO_POSITION
                        if (pos != RecyclerView.NO_POSITION) {
                            if (direction == View.FOCUS_RIGHT && pos < itemCount - 1) {
                                findViewByPosition(pos + 1)?.let { return it }
                            } else if (direction == View.FOCUS_LEFT && pos > 0) {
                                findViewByPosition(pos - 1)?.let { return it }
                            }
                        }
                        return focused
                    }
                    return super.onInterceptFocusSearch(focused, direction)
                }
            }
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(24)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val pos = parent.getChildAdapterPosition(view)
                    val count = parent.adapter?.itemCount ?: 0
                    outRect.set(0, 0, if (pos < count - 1) dp(10) else 0, 0)
                }
            })
            adapter = homeAdapter
        }
    }

    private fun setupAllAppsGridAndSearch() {
        rvAllAppsGrid.apply {
            layoutManager = object : GridLayoutManager(context, gridColumns) {
                override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                    if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {
                        val container = findContainingItemView(focused)
                        val pos = container?.let { getPosition(it) } ?: RecyclerView.NO_POSITION
                        if (pos != RecyclerView.NO_POSITION) {
                            val col = pos % gridColumns
                            if (direction == View.FOCUS_RIGHT) {
                                if (col < gridColumns - 1 && pos + 1 < itemCount) {
                                    findViewByPosition(pos + 1)?.let { return it }
                                }
                                return focused
                            } else if (direction == View.FOCUS_LEFT) {
                                if (col > 0) {
                                    findViewByPosition(pos - 1)?.let { return it }
                                }
                                return focused
                            }
                        }
                    }
                    return super.onInterceptFocusSearch(focused, direction)
                }
            }
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(36)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val pos = parent.getChildAdapterPosition(view)
                    val col = pos % gridColumns
                    val right = if (col < gridColumns - 1) dp(10) else 0
                    outRect.set(0, 0, right, dp(10))
                }
            })
            adapter = allAppsAdapter
        }

        layoutSearchBar.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f * density
            setColor(Color.parseColor("#CC0F172A"))
            setStroke(dp(1), Color.parseColor("#4438BDF8"))
        }

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            etSearch.requestFocus()
        }
        btnVoiceSearch.setOnClickListener {
            onOpenVoiceSearch()
        }
        btnCloseSearch.setOnClickListener {
            closeSearchBar(focusGrid = true)
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                refreshAllAppsGrid()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                rvAllAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                return@setOnKeyListener true
            }
            false
        }
    }

    fun switchMode(mode: NexfyViewMode, focusContent: Boolean = false) {
        activeMode = mode
        if (mode == NexfyViewMode.HOME) {
            btnProfile.visibility = View.VISIBLE
            layoutAllAppsTitleArea.visibility = View.GONE
            viewTopScrim.visibility = View.VISIBLE
            viewBottomScrim.visibility = View.VISIBLE
            viewAllAppsScrim.visibility = View.GONE
            layoutHomeBody.visibility = View.VISIBLE
            layoutAllAppsBody.visibility = View.GONE
            if (focusContent) {
                focusPrimaryItem()
            }
        } else {
            btnProfile.visibility = View.GONE
            layoutAllAppsTitleArea.visibility = View.VISIBLE
            viewTopScrim.visibility = View.GONE
            viewBottomScrim.visibility = View.GONE
            viewAllAppsScrim.visibility = View.VISIBLE
            layoutHomeBody.visibility = View.GONE
            layoutAllAppsBody.visibility = View.VISIBLE
            refreshAllAppsGrid()
            if (focusContent) {
                rvAllAppsGrid.post {
                    rvAllAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                        ?: btnSearch.requestFocus()
                }
            }
        }
    }

    fun openOrToggleSearch() {
        if (activeMode != NexfyViewMode.ALL_APPS) {
            switchMode(NexfyViewMode.ALL_APPS, focusContent = false)
        }
        layoutSearchBar.visibility = View.VISIBLE
        etSearch.post {
            etSearch.requestFocus()
        }
    }

    private fun closeSearchBar(focusGrid: Boolean) {
        searchQuery = ""
        etSearch.setText("")
        layoutSearchBar.visibility = View.GONE
        refreshAllAppsGrid()
        if (focusGrid) {
            rvAllAppsGrid.post {
                rvAllAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
            }
        }
    }

    fun focusPrimaryItem() {
        if (activeMode == NexfyViewMode.HOME) {
            rvHomeApps.post {
                rvHomeApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: layoutCategoryBar.getChildAt(0)?.requestFocus()
            }
        } else {
            rvAllAppsGrid.post {
                rvAllAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: btnSearch.requestFocus()
            }
        }
    }

    fun handleBackPressed(): Boolean {
        if (layoutSearchBar.visibility == View.VISIBLE || searchQuery.isNotEmpty()) {
            closeSearchBar(focusGrid = true)
            return true
        }
        if (activeMode == NexfyViewMode.ALL_APPS) {
            if (isDrawerStandaloneMode) {
                onDismissStandaloneDrawer()
                return true
            }
            switchMode(NexfyViewMode.HOME, focusContent = true)
            return true
        }
        if (selectedCategory != NexfyCategory.SEMUA) {
            selectedCategory = NexfyCategory.SEMUA
            rebuildCategoryTabs()
            refreshHomeShelf()
            return true
        }
        return false
    }

    fun updateInstalledApps(apps: List<AppModel>) {
        allInstalledApps = apps
        refreshHomeShelf()
        refreshAllAppsGrid()
    }

    fun updateSystemStatus(
        wifiOnline: Boolean,
        wifiSsid: String?,
        bluetoothOn: Boolean,
        weatherText: String
    ) {
        isWifiOnline = wifiOnline
        currentWifiSsid = wifiSsid
        isBluetoothOn = bluetoothOn
        currentWeatherText = weatherText

        btnWifi.setColorFilter(if (wifiOnline) Color.WHITE else Color.parseColor("#64748B"))
        btnWifi.alpha = if (wifiOnline) 1.0f else 0.72f
        btnWifi.contentDescription = if (wifiOnline) "Wi-Fi: ${wifiSsid ?: "Terhubung"}" else "Wi-Fi: Offline"

        btnBluetooth.setColorFilter(if (bluetoothOn) Color.parseColor("#7DD3FC") else Color.parseColor("#64748B"))
        btnBluetooth.alpha = if (bluetoothOn) 1.0f else 0.72f
        btnBluetooth.contentDescription = if (bluetoothOn) "Bluetooth: Aktif" else "Bluetooth: Nonaktif"

        tvWeather.text = weatherText.takeIf {
            it.isNotBlank() && !it.contains("Memuat", ignoreCase = true)
        } ?: "Cuaca"

        val timeStr = try {
            SimpleDateFormat("HH.mm", Locale.getDefault()).format(Date())
        } catch (_: Exception) {
            "00.00"
        }
        tvClock.text = timeStr
    }

    private fun refreshHomeShelf() {
        val filtered = NexfyContentClassifier.orderAndFilterApps(context, allInstalledApps, selectedCategory)
        val items = mutableListOf<NexfyCardItem>()
        if (selectedCategory == NexfyCategory.SEMUA) {
            items.add(NexfyCardItem.AllAppsTile)
        }
        for (app in filtered) {
            items.add(NexfyCardItem.InstalledAppTile(app))
        }
        homeAdapter.update(items)

        tvShelfTitle.text = if (selectedCategory == NexfyCategory.SEMUA) {
            "Semua Aplikasi"
        } else {
            selectedCategory.label
        }
        tvShelfHint.text = "•  ${filtered.size} aplikasi"
    }

    private fun refreshAllAppsGrid() {
        val ordered = NexfyContentClassifier.orderAndFilterApps(context, allInstalledApps, NexfyCategory.SEMUA)
        val q = searchQuery.lowercase(Locale.getDefault())
        val filtered = if (q.isEmpty()) {
            ordered
        } else {
            ordered.filter {
                it.appName.lowercase(Locale.getDefault()).contains(q) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }
        tvAllAppsCountBadge.text = "${filtered.size} aplikasi"
        allAppsAdapter.update(filtered.map { NexfyCardItem.InstalledAppTile(it) })
        tvAllAppsEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvAllAppsGrid.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAppOptionsDialog(app: AppModel) {
        val pinned = NexfyContentClassifier.isPinned(context, app.packageName)
        val options = arrayOf(
            "▶ Buka ${app.appName}",
            if (pinned) "☆ Lepas Sematan dari Baris Depan" else "★ Sematkan ke Depan Baris Nexfy",
            "ℹ️ Info Aplikasi",
            "🗑️ Copot Pemasangan (Uninstall)"
        )
        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(app.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onAppClick(app)
                    1 -> {
                        val nowPinned = NexfyContentClassifier.togglePinnedPackage(context, app.packageName)
                        refreshHomeShelf()
                        refreshAllAppsGrid()
                        Toast.makeText(
                            context,
                            if (nowPinned) "${app.appName} disematkan" else "${app.appName} dilepas",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    2 -> openSystemAppInfo(app.packageName)
                    3 -> requestUninstallApp(app.packageName)
                }
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun openSystemAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Tidak dapat membuka info aplikasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestUninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Tidak dapat mencopot aplikasi ini", Toast.LENGTH_SHORT).show()
        }
    }

    // ==============================================
    // SHARED LIGHTWEIGHT CARD ADAPTER (HOME & ALL APPS)
    // ==============================================
    private sealed class NexfyCardItem {
        object AllAppsTile : NexfyCardItem()
        data class InstalledAppTile(val app: AppModel) : NexfyCardItem()
    }

    private inner class NexfyCardAdapter(
        private val isHomeDock: Boolean
    ) : RecyclerView.Adapter<NexfyCardAdapter.CardVH>() {

        private var items: List<NexfyCardItem> = emptyList()

        fun update(newItems: List<NexfyCardItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_nexfy_landscape_app, parent, false)
            val cardW = if (isHomeDock) (276f * scale).roundToInt().coerceAtLeast(dp(136)) else gridCardWidthPx
            val cardH = if (isHomeDock) (156f * scale).roundToInt().coerceAtLeast(dp(78)) else gridCardHeightPx
            view.layoutParams = RecyclerView.LayoutParams(cardW, cardH)
            return CardVH(view)
        }

        override fun onBindViewHolder(holder: CardVH, position: Int) {
            when (val item = items[position]) {
                is NexfyCardItem.AllAppsTile -> holder.bindAllAppsTile()
                is NexfyCardItem.InstalledAppTile -> holder.bindApp(item.app)
            }
        }

        inner class CardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivArtwork: ImageView = itemView.findViewById(R.id.ivNexfyLandscapeArtwork)
            private val focusBorder: View = itemView.findViewById(R.id.viewNexfyCardFocusBorder)

            init {
                val radiusPx = (14f * scale).coerceAtLeast(6f * density)
                itemView.clipToOutline = true
                itemView.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                    }
                }

                val outer = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radiusPx
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(3), Color.WHITE)
                }
                val inner = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = (radiusPx - 2f).coerceAtLeast(2f)
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(1).coerceAtLeast(1), Color.parseColor("#38BDF8"))
                }
                val layers = LayerDrawable(arrayOf(outer, inner))
                val inset = dp(2)
                layers.setLayerInset(1, inset, inset, inset, inset)
                focusBorder.background = layers
            }

            fun bindAllAppsTile() {
                ivArtwork.tag = "nexfy_all_apps_tile"
                ivArtwork.setImageResource(R.drawable.ic_nexfy_all_apps_tile)
                ivArtwork.scaleType = ImageView.ScaleType.CENTER_INSIDE
                val padV = dp(18)
                ivArtwork.setPadding(padV, padV, padV, padV)
                itemView.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#D9141C2E"))
                }
                bindInteraction(
                    onTrigger = { switchMode(NexfyViewMode.ALL_APPS, focusContent = true) },
                    onMenu = null
                )
            }

            fun bindApp(app: AppModel) {
                ivArtwork.scaleType = ImageView.ScaleType.CENTER_CROP
                ivArtwork.setPadding(0, 0, 0, 0)
                itemView.background = null
                IconPackManager.bindNexfyLandscapeCard(context, ivArtwork, app)
                bindInteraction(
                    onTrigger = { onAppClick(app) },
                    onMenu = { showAppOptionsDialog(app) }
                )
            }

            private fun bindInteraction(
                onTrigger: () -> Unit,
                onMenu: (() -> Unit)?
            ) {
                val applyFocus: (Boolean, Boolean) -> Unit = { focused, animate ->
                    focusBorder.visibility = if (focused) View.VISIBLE else View.GONE
                    itemView.elevation = if (focused) 14f else 3f
                    val scaleVal = if (focused) 1.07f else 1.0f
                    if (animate) {
                        itemView.animate().scaleX(scaleVal).scaleY(scaleVal).setDuration(95L).setInterpolator(decelerate).start()
                    } else {
                        itemView.scaleX = scaleVal
                        itemView.scaleY = scaleVal
                    }
                }
                applyFocus(itemView.isFocused, false)

                itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    applyFocus(hasFocus, true)
                    if (hasFocus) {
                        val rect = Rect(0, 0, v.width, v.height)
                        v.requestRectangleOnScreen(rect, false)
                    }
                }
                itemView.setOnClickListener { onTrigger() }
                if (onMenu != null) {
                    itemView.setOnLongClickListener {
                        onMenu()
                        true
                    }
                } else {
                    itemView.setOnLongClickListener(null)
                }
                itemView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                onTrigger()
                                return@setOnKeyListener true
                            }
                            KeyEvent.KEYCODE_MENU -> {
                                if (onMenu != null) {
                                    onMenu()
                                    return@setOnKeyListener true
                                }
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                if (isHomeDock) {
                                    switchMode(NexfyViewMode.ALL_APPS, focusContent = true)
                                    return@setOnKeyListener true
                                }
                            }
                        }
                    }
                    false
                }
            }
        }
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()
}
