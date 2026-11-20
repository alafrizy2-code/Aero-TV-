package com.example.theme

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
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
import androidx.recyclerview.widget.RecyclerView
import com.example.AppModel
import com.example.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class AtvViewMode {
    HOME,
    APPLICATIONS
}

/**
 * AtvHomeController:
 * Mengimplementasikan ATV Launcher Theme sesuai screenshot referensi terbaru:
 * 1. HOME SCREEN (`AtvViewMode.HOME`):
 *    - Area wallpaper utama terbuka penuh di bagian atas.
 *    - Bar atas: emblem ATV di kiri + status Wi-Fi/Bluetooth/cuaca/pengaturan/jam/tanggal di kanan.
 *    - Bagian bawah: judul "Applications" + grid 6 kolom (dapat diatur) berisi kartu landscape modern
 *      berwarna (ikon di kiri, nama aplikasi di kanan) dengan susunan favorit yang dapat diatur.
 * 2. HALAMAN APPLICATIONS (`AtvViewMode.APPLICATIONS`):
 *    - Judul "Applications" di kiri atas + jam/status di kanan atas.
 *    - Grid 6 kolom (dapat diatur 4–8 kolom) di atas wallpaper penuh dengan scrolling otomatis D-Pad.
 */
class AtvHomeController(
    private val context: Context,
    private val rootView: View,
    private val isDrawerStandaloneMode: Boolean = false,
    private val onAppClick: (AppModel) -> Unit,
    private val onOpenWifi: () -> Unit,
    private val onOpenBluetooth: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onOpenVoiceSearch: () -> Unit,
    private val onRefreshWeather: () -> Unit,
    private val onCardSettingsUpdated: () -> Unit = {},
    private val onDismissStandaloneDrawer: () -> Unit = {}
) {
    private val density = context.resources.displayMetrics.density
    private val scale = (context.resources.displayMetrics.widthPixels / 1920f).coerceAtLeast(0.5f)
    private val decelerate = DecelerateInterpolator()

    private var activeMode: AtvViewMode =
        if (isDrawerStandaloneMode) AtvViewMode.APPLICATIONS else AtvViewMode.HOME

    private var cardSettings: ThemeManager.AtvCardSettings = ThemeManager.getAtvCardSettings(context)
    private var allInstalledApps: List<AppModel> = emptyList()
    private var searchQuery: String = ""

    private var isWifiOnline: Boolean = false
    private var currentWifiSsid: String? = null
    private var isBluetoothOn: Boolean = true
    private var currentWeatherText: String = ""

    // Scrims
    private val viewTopScrim: View = rootView.findViewById(R.id.viewAtvTopScrim)
    private val viewBottomScrim: View = rootView.findViewById(R.id.viewAtvBottomScrim)
    private val viewAppsScrim: View = rootView.findViewById(R.id.viewAtvAppsScrim)

    // Top Bar
    private val btnEmblem: ImageButton = rootView.findViewById(R.id.btnAtvEmblem)
    private val layoutAppsHeaderGroup: LinearLayout = rootView.findViewById(R.id.layoutAtvAppsHeaderGroup)
    private val tvTopAppsCount: TextView = rootView.findViewById(R.id.tvAtvTopAppsCount)
    private val btnBluetooth: ImageButton = rootView.findViewById(R.id.btnAtvBluetooth)
    private val btnWifi: ImageButton = rootView.findViewById(R.id.btnAtvWifi)
    private val btnWeather: LinearLayout = rootView.findViewById(R.id.btnAtvWeather)
    private val tvWeatherBadge: TextView = rootView.findViewById(R.id.tvAtvWeatherBadge)
    private val btnSearch: ImageButton = rootView.findViewById(R.id.btnAtvSearch)
    private val btnCardSettings: ImageButton = rootView.findViewById(R.id.btnAtvCardSettings)
    private val btnSettings: ImageButton = rootView.findViewById(R.id.btnAtvSettings)
    private val tvClock: TextView = rootView.findViewById(R.id.tvAtvClock)
    private val tvDayUpper: TextView = rootView.findViewById(R.id.tvAtvDayUpper)
    private val tvDateUpper: TextView = rootView.findViewById(R.id.tvAtvDateUpper)

    // Home Mode
    private val layoutHomeBody: LinearLayout = rootView.findViewById(R.id.layoutAtvHomeBody)
    private val tvHomeSectionTitle: TextView = rootView.findViewById(R.id.tvAtvHomeSectionTitle)
    private val btnHomeFilterMode: TextView = rootView.findViewById(R.id.btnAtvHomeFilterMode)
    private val btnOpenAllApps: TextView = rootView.findViewById(R.id.btnAtvOpenAllApps)
    private val rvHomeGrid: RecyclerView = rootView.findViewById(R.id.rvAtvHomeGrid)

    // Full Applications Mode
    private val layoutAppsBody: LinearLayout = rootView.findViewById(R.id.layoutAtvAppsBody)
    private val layoutSearchBar: LinearLayout = rootView.findViewById(R.id.layoutAtvSearchBar)
    private val etSearch: EditText = rootView.findViewById(R.id.etAtvSearch)
    private val btnClearSearch: ImageButton = rootView.findViewById(R.id.btnAtvClearSearch)
    private val btnVoiceSearch: ImageButton = rootView.findViewById(R.id.btnAtvVoiceSearch)
    private val btnCloseSearch: ImageButton = rootView.findViewById(R.id.btnAtvCloseSearch)
    private val tvAppsEmpty: TextView = rootView.findViewById(R.id.tvAtvAppsEmpty)
    private val rvAppsGrid: RecyclerView = rootView.findViewById(R.id.rvAtvAppsGrid)

    private val homeAdapter = AtvCardAdapter(isHomeSection = true)
    private val appsAdapter = AtvCardAdapter(isHomeSection = false)

    private var computedColumns: Int = 6
    private var computedCardWidthPx: Int = 264
    private var computedCardHeightPx: Int = 152
    private var homeDecoration: RecyclerView.ItemDecoration? = null
    private var appsDecoration: RecyclerView.ItemDecoration? = null

    init {
        setupTopBar()
        setupHomeControls()
        setupSearchBar()
        applyCardSettingsAndLayoutManagers()
        switchMode(activeMode, focusContent = false)
        updateSystemStatus(isWifiOnline, currentWifiSsid, isBluetoothOn, currentWeatherText)
    }

    private fun setupTopBar() {
        val styleCircleIcon: (View, () -> Unit) -> Unit = { view, onClick ->
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33090E1A"))
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC1E293B"))
                setStroke(dp(2), Color.WHITE)
            }
            view.background = normal
            view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focused else normal
                v.animate().scaleX(if (hasFocus) 1.12f else 1.0f).scaleY(if (hasFocus) 1.12f else 1.0f).setDuration(85L).start()
            }
            view.setOnClickListener { onClick() }
        }

        styleCircleIcon(btnEmblem) {
            if (activeMode == AtvViewMode.HOME) {
                switchMode(AtvViewMode.APPLICATIONS, focusContent = true)
            } else if (isDrawerStandaloneMode) {
                onDismissStandaloneDrawer()
            } else {
                switchMode(AtvViewMode.HOME, focusContent = true)
            }
        }
        styleCircleIcon(btnBluetooth) { onOpenBluetooth() }
        styleCircleIcon(btnWifi) { onOpenWifi() }
        styleCircleIcon(btnSearch) { openOrToggleSearch() }
        styleCircleIcon(btnCardSettings) { openCardSettingsDialog() }
        styleCircleIcon(btnSettings) { onOpenSettings() }

        val weatherNormal = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor("#4D090E1A"))
            setStroke(dp(1), Color.parseColor("#44FFFFFF"))
        }
        val weatherFocused = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor("#CC1E293B"))
            setStroke(dp(2), Color.WHITE)
        }
        btnWeather.background = weatherNormal
        btnWeather.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            v.background = if (hasFocus) weatherFocused else weatherNormal
            v.animate().scaleX(if (hasFocus) 1.08f else 1.0f).scaleY(if (hasFocus) 1.08f else 1.0f).setDuration(85L).start()
        }
        btnWeather.setOnClickListener {
            onRefreshWeather()
            Toast.makeText(context, "Memperbarui info cuaca lokasi perangkat...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupHomeControls() {
        val stylePill: (TextView) -> Unit = { pill ->
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 999f
                setColor(Color.parseColor("#590F172A"))
                setStroke(dp(1), Color.parseColor("#38FFFFFF"))
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 999f
                setColor(Color.parseColor("#CC1E88E5"))
                setStroke(dp(2), Color.WHITE)
            }
            pill.background = normal
            pill.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focused else normal
                v.animate().scaleX(if (hasFocus) 1.06f else 1.0f).scaleY(if (hasFocus) 1.06f else 1.0f).setDuration(85L).start()
            }
        }

        stylePill(btnHomeFilterMode)
        stylePill(btnOpenAllApps)
        refreshFilterModeButtonText()

        btnHomeFilterMode.setOnClickListener {
            val nextFavOnly = !cardSettings.showFavoritesOnlyOnHome
            cardSettings = cardSettings.copy(showFavoritesOnlyOnHome = nextFavOnly)
            ThemeManager.saveAtvCardSettings(context, cardSettings)
            refreshFilterModeButtonText()
            refreshHomeGrid()
            Toast.makeText(
                context,
                if (nextFavOnly) "Menampilkan Aplikasi Favorit di Home" else "Menampilkan Favorit & Semua Aplikasi",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnOpenAllApps.setOnClickListener {
            switchMode(AtvViewMode.APPLICATIONS, focusContent = true)
        }
    }

    private fun refreshFilterModeButtonText() {
        val favCount = ThemeManager.getAtvFavoritePackages(context).size
        if (cardSettings.showFavoritesOnlyOnHome) {
            tvHomeSectionTitle.text = "Favorites"
            btnHomeFilterMode.text = if (favCount > 0) "★ Hanya Favorit ($favCount)" else "★ Hanya Favorit"
        } else {
            tvHomeSectionTitle.text = "Applications"
            btnHomeFilterMode.text = if (favCount > 0) "★ Favorit ($favCount) & Semua" else "★ Semua Aplikasi"
        }
    }

    private fun setupSearchBar() {
        layoutSearchBar.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6f * density
            setColor(Color.parseColor("#CC0F172A"))
            setStroke(dp(1), Color.parseColor("#55FFFFFF"))
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
                refreshApplicationsGrid()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                rvAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                return@setOnKeyListener true
            }
            false
        }
    }

    fun applyCardSettingsAndLayoutManagers() {
        cardSettings = ThemeManager.getAtvCardSettings(context)
        val screenW = context.resources.displayMetrics.widthPixels
        val sidePadPx = dp(44) * 2
        val gapPx = dp(cardSettings.spacingDp)
        val availW = (screenW - sidePadPx).coerceAtLeast(dp(480))
        computedColumns = if (screenW >= 900) cardSettings.columns.coerceIn(4, 8) else 4

        val cellW = ((availW - (computedColumns - 1) * gapPx) / computedColumns).coerceAtLeast(dp(100))
        // Proporsi landscape ATV modern dari screenshot (~1.72 : 1) + skala ukuran kartu pilihan pengguna
        computedCardWidthPx = cellW
        computedCardHeightPx = (cellW * 0.56f * cardSettings.cardSizeScale)
            .roundToInt()
            .coerceIn(dp(64), dp(220))

        configureGridRecyclerView(rvHomeGrid, homeAdapter, gapPx, isHome = true)
        configureGridRecyclerView(rvAppsGrid, appsAdapter, gapPx, isHome = false)

        refreshFilterModeButtonText()
        refreshHomeGrid()
        refreshApplicationsGrid()
    }

    private fun configureGridRecyclerView(
        rv: RecyclerView,
        adapterRef: AtvCardAdapter,
        gapPx: Int,
        isHome: Boolean
    ) {
        val cols = computedColumns
        rv.layoutManager = object : GridLayoutManager(context, cols) {
            override fun onInterceptFocusSearch(focused: View, direction: Int): View? {
                if (direction == View.FOCUS_LEFT || direction == View.FOCUS_RIGHT) {
                    val container = rv.findContainingItemView(focused)
                    val pos = container?.let { getPosition(it) } ?: RecyclerView.NO_POSITION
                    if (pos != RecyclerView.NO_POSITION) {
                        val col = pos % cols
                        if (direction == View.FOCUS_RIGHT) {
                            if (col < cols - 1 && pos + 1 < itemCount) {
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
        rv.itemAnimator = null
        rv.setHasFixedSize(true)
        rv.setItemViewCacheSize(36)

        val existingDeco = if (isHome) homeDecoration else appsDecoration
        existingDeco?.let { rv.removeItemDecoration(it) }
        val newDeco = object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val pos = parent.getChildAdapterPosition(view)
                val col = pos % cols
                val right = if (col < cols - 1) gapPx else 0
                outRect.set(0, 0, right, gapPx)
            }
        }
        if (isHome) homeDecoration = newDeco else appsDecoration = newDeco
        rv.addItemDecoration(newDeco)
        rv.adapter = adapterRef
    }

    fun openCardSettingsDialog() {
        AtvCardSettingsDialog(context) {
            applyCardSettingsAndLayoutManagers()
            onCardSettingsUpdated()
        }.show()
    }

    fun switchMode(mode: AtvViewMode, focusContent: Boolean = false) {
        activeMode = mode
        if (mode == AtvViewMode.HOME) {
            layoutAppsHeaderGroup.visibility = View.GONE
            viewTopScrim.visibility = View.VISIBLE
            viewBottomScrim.visibility = View.VISIBLE
            viewAppsScrim.visibility = View.GONE
            layoutHomeBody.visibility = View.VISIBLE
            layoutAppsBody.visibility = View.GONE
            refreshHomeGrid()
            if (focusContent) {
                focusPrimaryItem()
            }
        } else {
            layoutAppsHeaderGroup.visibility = View.VISIBLE
            viewTopScrim.visibility = View.GONE
            viewBottomScrim.visibility = View.GONE
            viewAppsScrim.visibility = View.VISIBLE
            layoutHomeBody.visibility = View.GONE
            layoutAppsBody.visibility = View.VISIBLE
            refreshApplicationsGrid()
            if (focusContent) {
                rvAppsGrid.post {
                    rvAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                        ?: btnSearch.requestFocus()
                }
            }
        }
    }

    fun openOrToggleSearch() {
        if (activeMode != AtvViewMode.APPLICATIONS) {
            switchMode(AtvViewMode.APPLICATIONS, focusContent = false)
        }
        layoutSearchBar.visibility = View.VISIBLE
        etSearch.post { etSearch.requestFocus() }
    }

    private fun closeSearchBar(focusGrid: Boolean) {
        searchQuery = ""
        etSearch.setText("")
        layoutSearchBar.visibility = View.GONE
        refreshApplicationsGrid()
        if (focusGrid) {
            rvAppsGrid.post {
                rvAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
            }
        }
    }

    fun focusPrimaryItem() {
        if (activeMode == AtvViewMode.HOME) {
            rvHomeGrid.post {
                rvHomeGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: btnOpenAllApps.requestFocus()
            }
        } else {
            rvAppsGrid.post {
                rvAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: btnSearch.requestFocus()
            }
        }
    }

    fun handleBackPressed(): Boolean {
        if (layoutSearchBar.visibility == View.VISIBLE || searchQuery.isNotEmpty()) {
            closeSearchBar(focusGrid = true)
            return true
        }
        if (activeMode == AtvViewMode.APPLICATIONS) {
            if (isDrawerStandaloneMode) {
                onDismissStandaloneDrawer()
                return true
            }
            switchMode(AtvViewMode.HOME, focusContent = true)
            return true
        }
        return false
    }

    fun updateInstalledApps(apps: List<AppModel>) {
        allInstalledApps = apps
        refreshHomeGrid()
        refreshApplicationsGrid()
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
        btnWifi.alpha = if (wifiOnline) 1.0f else 0.65f
        btnWifi.contentDescription = if (wifiOnline) "Wi-Fi: ${wifiSsid ?: "Terhubung"}" else "Wi-Fi: Offline"

        btnBluetooth.setColorFilter(if (bluetoothOn) Color.WHITE else Color.parseColor("#64748B"))
        btnBluetooth.alpha = if (bluetoothOn) 1.0f else 0.65f

        tvWeatherBadge.text = formatCompactWeatherBadge(weatherText)

        val now = Date()
        tvClock.text = try {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
        } catch (_: Exception) {
            "00:00"
        }
        val localeId = Locale("id", "ID")
        tvDayUpper.text = try {
            SimpleDateFormat("EEEE", localeId).format(now).uppercase(localeId)
        } catch (_: Exception) {
            ""
        }
        tvDateUpper.text = try {
            SimpleDateFormat("d MMM yyyy", localeId).format(now).uppercase(localeId)
        } catch (_: Exception) {
            ""
        }
    }

    private fun formatCompactWeatherBadge(raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank() || clean.contains("Memuat", ignoreCase = true) || clean.contains("Mendeteksi", ignoreCase = true)) {
            return "--°"
        }
        return clean
    }

    private fun refreshHomeGrid() {
        val ordered = ThemeManager.orderAppsForAtv(
            context = context,
            apps = allInstalledApps,
            favoritesOnly = cardSettings.showFavoritesOnlyOnHome
        )
        homeAdapter.update(ordered)
    }

    private fun refreshApplicationsGrid() {
        val ordered = ThemeManager.orderAppsForAtv(context, allInstalledApps, favoritesOnly = false)
        val q = searchQuery.lowercase(Locale.getDefault())
        val filtered = if (q.isEmpty()) {
            ordered
        } else {
            ordered.filter {
                it.appName.lowercase(Locale.getDefault()).contains(q) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }
        tvTopAppsCount.text = "• ${filtered.size} aplikasi"
        appsAdapter.update(filtered)
        tvAppsEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvAppsGrid.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAppOptionsDialog(app: AppModel) {
        val isFav = ThemeManager.isAtvFavorite(context, app.packageName)
        val optionsList = mutableListOf(
            "▶ Buka ${app.appName}",
            if (isFav) "☆ Hapus dari Favorit" else "★ Tambah ke Favorit (Urutan Depan)"
        )
        if (isFav) {
            optionsList.add("⬅ Geser Favorit ke Kiri / Atas")
            optionsList.add("➡ Geser Favorit ke Kanan / Bawah")
        }
        optionsList.add("⚙ Pengaturan Kartu ATV")
        optionsList.add("ℹ️ Info Aplikasi")
        optionsList.add("🗑️ Copot Pemasangan (Uninstall)")

        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(app.appName)
            .setItems(optionsList.toTypedArray()) { _, which ->
                val chosen = optionsList[which]
                when {
                    chosen.startsWith("▶ Buka") -> onAppClick(app)
                    chosen.startsWith("★ Tambah") || chosen.startsWith("☆ Hapus") -> {
                        val nowFav = ThemeManager.toggleAtvFavorite(context, app.packageName)
                        refreshFilterModeButtonText()
                        refreshHomeGrid()
                        refreshApplicationsGrid()
                        Toast.makeText(
                            context,
                            if (nowFav) "${app.appName} ditambahkan ke Favorit" else "${app.appName} dihapus dari Favorit",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    chosen.startsWith("⬅ Geser") -> {
                        if (ThemeManager.moveAtvFavorite(context, app.packageName, -1)) {
                            refreshHomeGrid()
                            refreshApplicationsGrid()
                        }
                    }
                    chosen.startsWith("➡ Geser") -> {
                        if (ThemeManager.moveAtvFavorite(context, app.packageName, +1)) {
                            refreshHomeGrid()
                            refreshApplicationsGrid()
                        }
                    }
                    chosen.startsWith("⚙ Pengaturan") -> openCardSettingsDialog()
                    chosen.startsWith("ℹ️ Info") -> openSystemAppInfo(app.packageName)
                    chosen.startsWith("🗑️ Copot") -> requestUninstallApp(app.packageName)
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
    // ADAPTER KARTU LANDSCAPE ATV LAUNCHER
    // ==============================================
    private inner class AtvCardAdapter(
        private val isHomeSection: Boolean
    ) : RecyclerView.Adapter<AtvCardAdapter.AtvCardVH>() {

        private var items: List<AppModel> = emptyList()

        fun update(newItems: List<AppModel>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AtvCardVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_atv_app_card, parent, false)
            view.layoutParams = RecyclerView.LayoutParams(computedCardWidthPx, computedCardHeightPx)
            return AtvCardVH(view)
        }

        override fun onBindViewHolder(holder: AtvCardVH, position: Int) {
            holder.bind(items[position], position)
        }

        inner class AtvCardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val cardContainer: FrameLayout = itemView.findViewById(R.id.atvCardContainer)
            private val viewSheen: View = itemView.findViewById(R.id.viewAtvCardSheen)
            private val ivIcon: ImageView = itemView.findViewById(R.id.ivAtvCardIcon)
            private val tvName: TextView = itemView.findViewById(R.id.tvAtvCardName)
            private val tvFavBadge: TextView = itemView.findViewById(R.id.tvAtvCardFavBadge)
            private val viewFocusBorder: View = itemView.findViewById(R.id.viewAtvCardFocusBorder)

            fun bind(app: AppModel, position: Int) {
                val radiusPx = (cardSettings.cornerRadiusDp * density).coerceAtLeast(0f)
                val iconPx = dp(cardSettings.iconSizeDp)

                // Ukuran ikon di sisi kiri kartu sesuai pengaturan
                ivIcon.layoutParams = (ivIcon.layoutParams as LinearLayout.LayoutParams).apply {
                    width = iconPx
                    height = iconPx
                }

                // Bentuk sudut kartu sesuai pengaturan
                cardContainer.clipToOutline = true
                cardContainer.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                    }
                }

                // Latar modern berwarna + transparansi sesuai pengaturan
                val (startCol, endCol) = IconPackManager.getAtvTileColors(app, cardSettings, position)
                cardContainer.background = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(startCol, endCol)
                ).apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radiusPx
                    setStroke(dp(1), Color.parseColor("#29FFFFFF"))
                }

                viewSheen.background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.parseColor("#24FFFFFF"), Color.TRANSPARENT)
                ).apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx, 0f, 0f, 0f, 0f)
                }

                // Efek fokus sesuai pilihan pengguna
                val borderColor = when (cardSettings.focusEffect) {
                    ThemeManager.AtvFocusEffect.WHITE_OUTLINE_ZOOM -> Color.WHITE
                    ThemeManager.AtvFocusEffect.ORANGE_BORDER_ZOOM -> Color.parseColor("#FF9800")
                    ThemeManager.AtvFocusEffect.CYAN_GLOW_ZOOM -> Color.parseColor("#22D3EE")
                    ThemeManager.AtvFocusEffect.BRIGHT_LIFT_ONLY -> Color.TRANSPARENT
                }
                viewFocusBorder.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radiusPx
                    setColor(Color.TRANSPARENT)
                    if (borderColor != Color.TRANSPARENT) {
                        setStroke(dp(3), borderColor)
                    }
                }

                tvName.text = app.appName
                tvName.textSize = (13f * cardSettings.cardSizeScale.coerceIn(0.9f, 1.15f))
                tvFavBadge.visibility = if (ThemeManager.isAtvFavorite(context, app.packageName)) View.VISIBLE else View.GONE

                IconPackManager.bindAtvLeftIcon(context, ivIcon, app)

                val applyFocus: (Boolean, Boolean) -> Unit = { focused, animate ->
                    val showBorder = focused && borderColor != Color.TRANSPARENT
                    viewFocusBorder.visibility = if (showBorder) View.VISIBLE else View.GONE
                    itemView.elevation = if (focused) 18f else 3f
                    val scaleVal = if (focused) 1.08f else 1.0f
                    if (animate) {
                        itemView.animate()
                            .scaleX(scaleVal)
                            .scaleY(scaleVal)
                            .setDuration(90L)
                            .setInterpolator(decelerate)
                            .start()
                    } else {
                        itemView.scaleX = scaleVal
                        itemView.scaleY = scaleVal
                    }
                }
                applyFocus(itemView.isFocused, false)

                itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    applyFocus(hasFocus, true)
                    if (hasFocus) {
                        v.requestRectangleOnScreen(Rect(0, 0, v.width, v.height), false)
                    }
                }

                itemView.setOnClickListener { onAppClick(app) }
                itemView.setOnLongClickListener {
                    showAppOptionsDialog(app)
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
                                showAppOptionsDialog(app)
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
}
