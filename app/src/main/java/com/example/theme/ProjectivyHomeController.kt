package com.example.theme

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import coil.load
import com.example.AppCategory
import com.example.AppModel
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class ProjectivyViewMode {
    HOME,
    TV_APPS
}

/**
 * ProjectivyHomeController:
 * Mengelola Projectivy Home Screen (Referensi 1: Apps, kategori/sumber konten, Watch Next nyata,
 * status & jam kanan atas di atas wallpaper luas) serta Projectivy App Drawer / TV Apps (Referensi 2:
 * grid 6 kolom kartu landscape dengan ikon di tengah kartu, wallpaper terlihat di belakang, dan
 * outline putih fokus yang jelas).
 */
class ProjectivyHomeController(
    private val context: Context,
    private val rootView: View,
    private val scope: CoroutineScope,
    private val isDrawerStandaloneMode: Boolean = false,
    private val onAppClick: (AppModel) -> Unit,
    private val onOpenWifi: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onOpenVoiceSearch: () -> Unit,
    private val onRefreshWeather: () -> Unit,
    private val onDismissStandaloneDrawer: () -> Unit = {}
) {
    private val density = context.resources.displayMetrics.density
    private val scale = (context.resources.displayMetrics.widthPixels / 1920f).coerceAtLeast(0.5f)
    private val decelerate = DecelerateInterpolator()

    private var activeMode: ProjectivyViewMode =
        if (isDrawerStandaloneMode) ProjectivyViewMode.TV_APPS else ProjectivyViewMode.HOME

    private var allInstalledApps: List<AppModel> = emptyList()
    private var watchNextItems: List<ProjectivyWatchNextItem> = emptyList()
    private var externalSources: List<ProjectivyContentSource> = emptyList()
    private var selectedSourceFilterId: String = SOURCE_ALL
    private var selectedCategoryFilter: AppCategory? = null
    private var searchQuery: String = ""

    private var isWifiOnline: Boolean = false
    private var currentWifiSsid: String? = null
    private var currentWeatherText: String = ""

    // Scrims
    private val viewTopScrim: View = rootView.findViewById(R.id.viewProjectivyTopScrim)
    private val viewBottomScrim: View = rootView.findViewById(R.id.viewProjectivyBottomScrim)
    private val viewDrawerScrim: View = rootView.findViewById(R.id.viewProjectivyDrawerScrim)

    // Top Bar
    private val layoutDrawerTitleArea: LinearLayout = rootView.findViewById(R.id.layoutProjectivyDrawerTitleArea)
    private val tvDrawerAppsCount: TextView = rootView.findViewById(R.id.tvProjectivyDrawerAppsCount)
    private val btnWifi: ImageButton = rootView.findViewById(R.id.btnProjectivyWifi)
    private val btnWeather: LinearLayout = rootView.findViewById(R.id.btnProjectivyWeather)
    private val tvWeatherBadge: TextView = rootView.findViewById(R.id.tvProjectivyWeatherBadge)
    private val btnSearch: ImageButton = rootView.findViewById(R.id.btnProjectivySearch)
    private val btnSettings: ImageButton = rootView.findViewById(R.id.btnProjectivySettings)
    private val tvClock: TextView = rootView.findViewById(R.id.tvProjectivyClock)

    // Home Body (Referensi 1)
    private val layoutHomeBody: LinearLayout = rootView.findViewById(R.id.layoutProjectivyHomeBody)
    private val tvHomeFocusedApp: TextView = rootView.findViewById(R.id.tvProjectivyHomeFocusedApp)
    private val rvHomeApps: RecyclerView = rootView.findViewById(R.id.rvProjectivyHomeApps)
    private val tvContentSourceTitle: TextView = rootView.findViewById(R.id.tvProjectivyContentSourceTitle)
    private val layoutSourceChips: LinearLayout = rootView.findViewById(R.id.layoutProjectivySourceChips)
    private val tvWatchNextStatus: TextView = rootView.findViewById(R.id.tvProjectivyWatchNextStatus)
    private val rvWatchNext: RecyclerView = rootView.findViewById(R.id.rvProjectivyWatchNext)
    private val layoutWatchNextEmpty: LinearLayout = rootView.findViewById(R.id.layoutProjectivyWatchNextEmpty)
    private val cardWatchNextEmpty: LinearLayout = rootView.findViewById(R.id.cardProjectivyWatchNextEmpty)
    private val viewEmptySlot1: View = rootView.findViewById(R.id.viewProjectivyEmptySlot1)
    private val viewEmptySlot2: View = rootView.findViewById(R.id.viewProjectivyEmptySlot2)

    // TV Apps Body (Referensi 2)
    private val layoutTvAppsBody: LinearLayout = rootView.findViewById(R.id.layoutProjectivyTvAppsBody)
    private val layoutSearchBar: LinearLayout = rootView.findViewById(R.id.layoutProjectivySearchBar)
    private val etSearch: EditText = rootView.findViewById(R.id.etProjectivySearch)
    private val btnClearSearch: ImageButton = rootView.findViewById(R.id.btnProjectivyClearSearch)
    private val btnVoiceSearch: ImageButton = rootView.findViewById(R.id.btnProjectivyVoiceSearch)
    private val btnCloseSearch: ImageButton = rootView.findViewById(R.id.btnProjectivyCloseSearch)
    private val tvTvAppsEmpty: TextView = rootView.findViewById(R.id.tvProjectivyTvAppsEmpty)
    private val rvTvAppsGrid: RecyclerView = rootView.findViewById(R.id.rvProjectivyTvAppsGrid)
    private val tvDrawerFocusedName: TextView = rootView.findViewById(R.id.tvProjectivyDrawerFocusedName)

    private val homeAppsAdapter = ProjectivyCardAdapter(isHomeRow = true)
    private val watchNextAdapter = WatchNextAdapter()
    private val tvAppsAdapter = ProjectivyCardAdapter(isHomeRow = false)

    private var gridColumns: Int = 6
    private var gridCardWidthPx: Int = 270
    private var gridCardHeightPx: Int = 168

    companion object {
        private const val SOURCE_ALL = "source_all"
    }

    init {
        computeTvAppsGridMetrics()
        setupTopBar()
        setupHomeAppsRow()
        setupWatchNextArea()
        setupTvAppsGridAndSearch()

        switchMode(activeMode, focusContent = false)
        updateSystemStatus(isWifiOnline, currentWifiSsid, currentWeatherText)
        refreshWatchNext()
    }

    private fun computeTvAppsGridMetrics() {
        val screenW = context.resources.displayMetrics.widthPixels
        val sidePaddingPx = dp(44) * 2
        val gapPx = dp(12)
        val availW = (screenW - sidePaddingPx).coerceAtLeast(dp(480))
        gridColumns = if (screenW >= 1200) 6 else 4
        gridCardWidthPx = ((availW - (gridColumns - 1) * gapPx) / gridColumns).coerceAtLeast(dp(116))
        // Proporsi landscape Projectivy (TV Apps Referensi 2): ~1.61 : 1 dengan ikon tepat di tengah
        gridCardHeightPx = (gridCardWidthPx * 0.62f).roundToInt().coerceAtLeast(dp(72))
    }

    private fun setupTopBar() {
        val styleCircleIcon: (View, () -> Unit) -> Unit = { view, onClick ->
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#4D090E1A"))
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC1E293B"))
                setStroke(dp(2), Color.WHITE)
            }
            view.background = normal
            view.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focused else normal
                v.animate().scaleX(if (hasFocus) 1.12f else 1.0f).scaleY(if (hasFocus) 1.12f else 1.0f).setDuration(90L).start()
            }
            view.setOnClickListener { onClick() }
        }

        styleCircleIcon(btnWifi) { onOpenWifi() }
        styleCircleIcon(btnSearch) { openOrToggleSearch() }
        styleCircleIcon(btnSettings) { onOpenSettings() }

        val weatherNormal = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor("#66090E1A"))
            setStroke(dp(1), Color.parseColor("#55FFFFFF"))
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
            v.animate().scaleX(if (hasFocus) 1.08f else 1.0f).scaleY(if (hasFocus) 1.08f else 1.0f).setDuration(90L).start()
        }
        btnWeather.setOnClickListener {
            onRefreshWeather()
            Toast.makeText(context, "Memperbarui info cuaca lokasi perangkat...", Toast.LENGTH_SHORT).show()
        }
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
            adapter = homeAppsAdapter
        }
    }

    private fun setupWatchNextArea() {
        val extraPrefetchPx = (400f * scale).roundToInt().coerceAtLeast(200)
        rvWatchNext.apply {
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
            setItemViewCacheSize(16)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val pos = parent.getChildAdapterPosition(view)
                    val count = parent.adapter?.itemCount ?: 0
                    outRect.set(0, 0, if (pos < count - 1) dp(12) else 0, 0)
                }
            })
            adapter = watchNextAdapter
        }

        // Area kosong Watch Next (saat belum ada sumber data nyata)
        val radiusPx = 8f * density
        val emptyNormalBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.parseColor("#590B1222"))
            setStroke(dp(1), Color.parseColor("#33FFFFFF"))
        }
        val emptyFocusedBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.parseColor("#8C142038"))
            setStroke(dp(3), Color.WHITE)
        }
        cardWatchNextEmpty.background = emptyNormalBg
        cardWatchNextEmpty.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            v.background = if (hasFocus) emptyFocusedBg else emptyNormalBg
            v.animate().scaleX(if (hasFocus) 1.04f else 1.0f).scaleY(if (hasFocus) 1.04f else 1.0f).setDuration(90L).start()
        }
        cardWatchNextEmpty.setOnClickListener {
            showEmptyWatchNextDialog()
        }

        val ghostBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.parseColor("#220B1222"))
            setStroke(dp(1), Color.parseColor("#1EFFFFFF"))
        }
        viewEmptySlot1.background = ghostBg
        viewEmptySlot2.background = ghostBg
    }

    private fun showEmptyWatchNextDialog() {
        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Watch Next • Sumber Konten")
            .setMessage(
                "Belum ada data Watch Next yang dikirim oleh sistem Android TV atau aplikasi video terpasang.\n\n" +
                    "Area ini otomatis terisi thumbnail konten nyata saat sumber Watch Next tersedia atau terhubung."
            )
            .setPositiveButton("Periksa Ulang") { _, _ ->
                refreshWatchNext()
                Toast.makeText(context, "Memeriksa data Watch Next sistem...", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Buka TV Apps") { _, _ ->
                switchMode(ProjectivyViewMode.TV_APPS, focusContent = true)
            }
            .setNegativeButton("Tutup", null)
            .show()
    }

    private fun setupTvAppsGridAndSearch() {
        rvTvAppsGrid.apply {
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
                    val right = if (col < gridColumns - 1) dp(12) else 0
                    outRect.set(0, 0, right, dp(12))
                }
            })
            adapter = tvAppsAdapter
        }

        layoutSearchBar.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 8f * density
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
                refreshTvAppsGrid()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                rvTvAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                return@setOnKeyListener true
            }
            false
        }
    }

    fun switchMode(mode: ProjectivyViewMode, focusContent: Boolean = false) {
        activeMode = mode
        if (mode == ProjectivyViewMode.HOME) {
            layoutDrawerTitleArea.visibility = View.GONE
            viewTopScrim.visibility = View.VISIBLE
            viewBottomScrim.visibility = View.VISIBLE
            viewDrawerScrim.visibility = View.GONE
            layoutHomeBody.visibility = View.VISIBLE
            layoutTvAppsBody.visibility = View.GONE
            if (focusContent) {
                focusPrimaryItem()
            }
        } else {
            layoutDrawerTitleArea.visibility = View.VISIBLE
            viewTopScrim.visibility = View.GONE
            viewBottomScrim.visibility = View.GONE
            viewDrawerScrim.visibility = View.VISIBLE
            layoutHomeBody.visibility = View.GONE
            layoutTvAppsBody.visibility = View.VISIBLE
            refreshTvAppsGrid()
            if (focusContent) {
                rvTvAppsGrid.post {
                    rvTvAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                        ?: btnSearch.requestFocus()
                }
            }
        }
    }

    fun openOrToggleSearch() {
        if (activeMode != ProjectivyViewMode.TV_APPS) {
            switchMode(ProjectivyViewMode.TV_APPS, focusContent = false)
        }
        layoutSearchBar.visibility = View.VISIBLE
        etSearch.post { etSearch.requestFocus() }
    }

    private fun closeSearchBar(focusGrid: Boolean) {
        searchQuery = ""
        etSearch.setText("")
        layoutSearchBar.visibility = View.GONE
        refreshTvAppsGrid()
        if (focusGrid) {
            rvTvAppsGrid.post {
                rvTvAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
            }
        }
    }

    fun focusPrimaryItem() {
        if (activeMode == ProjectivyViewMode.HOME) {
            rvHomeApps.post {
                rvHomeApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: cardWatchNextEmpty.requestFocus()
            }
        } else {
            rvTvAppsGrid.post {
                rvTvAppsGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: btnSearch.requestFocus()
            }
        }
    }

    fun handleBackPressed(): Boolean {
        if (layoutSearchBar.visibility == View.VISIBLE || searchQuery.isNotEmpty()) {
            closeSearchBar(focusGrid = true)
            return true
        }
        if (activeMode == ProjectivyViewMode.TV_APPS) {
            if (isDrawerStandaloneMode) {
                onDismissStandaloneDrawer()
                return true
            }
            switchMode(ProjectivyViewMode.HOME, focusContent = true)
            return true
        }
        if (selectedCategoryFilter != null || selectedSourceFilterId != SOURCE_ALL) {
            selectedCategoryFilter = null
            selectedSourceFilterId = SOURCE_ALL
            rebuildContentSourceChips()
            refreshHomeAppsRow()
            refreshWatchNextUi()
            return true
        }
        return false
    }

    fun updateInstalledApps(apps: List<AppModel>) {
        allInstalledApps = apps
        refreshHomeAppsRow()
        refreshTvAppsGrid()
        if (externalSources.isEmpty()) {
            rebuildContentSourceChips()
        }
    }

    fun refreshWatchNext() {
        scope.launch {
            val snapshot = withContext(Dispatchers.IO) {
                ProjectivyWatchNextProvider.loadWatchNextAndSources(context)
            }
            applyWatchNextSnapshot(snapshot)
        }
    }

    fun applyWatchNextSnapshot(snapshot: ProjectivyContentSnapshot) {
        watchNextItems = snapshot.watchNextItems
        externalSources = snapshot.externalSources
        rebuildContentSourceChips()
        refreshWatchNextUi()
    }

    fun updateSystemStatus(
        wifiOnline: Boolean,
        wifiSsid: String?,
        weatherText: String
    ) {
        isWifiOnline = wifiOnline
        currentWifiSsid = wifiSsid
        currentWeatherText = weatherText

        btnWifi.setColorFilter(if (wifiOnline) Color.WHITE else Color.parseColor("#64748B"))
        btnWifi.alpha = if (wifiOnline) 1.0f else 0.72f
        btnWifi.contentDescription = if (wifiOnline) "Wi-Fi: ${wifiSsid ?: "Terhubung"}" else "Wi-Fi: Offline"

        tvWeatherBadge.text = formatCompactWeatherBadge(weatherText)

        val timeStr = try {
            SimpleDateFormat("H:mm", Locale.getDefault()).format(Date())
        } catch (_: Exception) {
            "0:00"
        }
        tvClock.text = timeStr
    }

    private fun formatCompactWeatherBadge(raw: String): String {
        val clean = raw.trim()
        if (clean.isBlank() || clean.contains("Memuat", ignoreCase = true) || clean.contains("Mendeteksi", ignoreCase = true)) {
            return "--°"
        }
        return clean
    }

    private fun refreshHomeAppsRow() {
        val ordered = ProjectivyWatchNextProvider.orderAppsForProjectivy(
            context = context,
            apps = allInstalledApps,
            categoryFilter = selectedCategoryFilter
        )
        val items = mutableListOf<ProjectivyCardItem>()
        items.add(ProjectivyCardItem.TvAppsDrawerTile)
        for (app in ordered) {
            items.add(ProjectivyCardItem.InstalledAppTile(app))
        }
        homeAppsAdapter.update(items)
        if (tvHomeFocusedApp.text.isNullOrBlank()) {
            tvHomeFocusedApp.text = "• ${ordered.size} aplikasi terpasang"
        }
    }

    private fun refreshTvAppsGrid() {
        val ordered = ProjectivyWatchNextProvider.orderAppsForProjectivy(context, allInstalledApps, null)
        val q = searchQuery.lowercase(Locale.getDefault())
        val filtered = if (q.isEmpty()) {
            ordered
        } else {
            ordered.filter {
                it.appName.lowercase(Locale.getDefault()).contains(q) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }
        tvDrawerAppsCount.text = "• ${filtered.size} aplikasi"
        tvAppsAdapter.update(filtered.map { ProjectivyCardItem.InstalledAppTile(it) })
        tvTvAppsEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvTvAppsGrid.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
        if (filtered.isNotEmpty() && tvDrawerFocusedName.text.isNullOrBlank()) {
            tvDrawerFocusedName.text = filtered.first().appName
        } else if (filtered.isEmpty()) {
            tvDrawerFocusedName.text = ""
        }
    }

    private fun rebuildContentSourceChips() {
        layoutSourceChips.removeAllViews()

        if (externalSources.isNotEmpty()) {
            val activeSource = externalSources.firstOrNull { it.id == selectedSourceFilterId }
            tvContentSourceTitle.text = activeSource?.label ?: externalSources.first().label

            addSourceChip(
                label = "Semua Sumber",
                isActive = (selectedSourceFilterId == SOURCE_ALL),
                onClick = {
                    selectedSourceFilterId = SOURCE_ALL
                    rebuildContentSourceChips()
                    refreshWatchNextUi()
                }
            )
            for (src in externalSources) {
                addSourceChip(
                    label = src.label,
                    isActive = (selectedSourceFilterId == src.id),
                    onClick = {
                        selectedSourceFilterId = src.id
                        rebuildContentSourceChips()
                        refreshWatchNextUi()
                    }
                )
            }
        } else {
            tvContentSourceTitle.text = selectedCategoryFilter?.label ?: "Sumber Konten"
            addSourceChip(
                label = "Semua",
                isActive = (selectedCategoryFilter == null),
                onClick = {
                    selectedCategoryFilter = null
                    rebuildContentSourceChips()
                    refreshHomeAppsRow()
                }
            )
            val categoryPills = listOf(
                AppCategory.MEDIA_VIDEO to "Video & TV",
                AppCategory.MUSIC_AUDIO to "Musik",
                AppCategory.GAMES to "Game",
                AppCategory.TOOLS_SYSTEM to "Alat"
            )
            for ((cat, label) in categoryPills) {
                addSourceChip(
                    label = label,
                    isActive = (selectedCategoryFilter == cat),
                    onClick = {
                        selectedCategoryFilter = cat
                        rebuildContentSourceChips()
                        refreshHomeAppsRow()
                    }
                )
            }
        }
    }

    private fun addSourceChip(label: String, isActive: Boolean, onClick: () -> Unit) {
        val chip = TextView(context).apply {
            text = label
            textSize = 11f
            maxLines = 1
            gravity = Gravity.CENTER
            isFocusable = true
            isClickable = true
            typeface = if (isActive) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            setPadding(dp(12), dp(5), dp(12), dp(5))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }

            val applyBg: (Boolean) -> Unit = { focused ->
                val bg = when {
                    isActive -> Color.parseColor("#E6F8FAFC")
                    focused -> Color.parseColor("#66FFFFFF")
                    else -> Color.parseColor("#660F172A")
                }
                setTextColor(if (isActive) Color.parseColor("#0F172A") else Color.WHITE)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 999f
                    setColor(bg)
                    if (focused) {
                        setStroke(dp(2), Color.WHITE)
                    } else if (!isActive) {
                        setStroke(dp(1), Color.parseColor("#33FFFFFF"))
                    }
                }
            }
            applyBg(isFocused)
            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                applyBg(hasFocus)
                v.animate().scaleX(if (hasFocus) 1.05f else 1.0f).scaleY(if (hasFocus) 1.05f else 1.0f).setDuration(85L).start()
            }
            setOnClickListener { onClick() }
        }
        layoutSourceChips.addView(chip)
    }

    private fun refreshWatchNextUi() {
        val visibleItems = if (selectedSourceFilterId == SOURCE_ALL) {
            watchNextItems
        } else {
            val activeSrc = externalSources.firstOrNull { it.id == selectedSourceFilterId }
            if (activeSrc == null) {
                watchNextItems
            } else {
                watchNextItems.filter {
                    (!activeSrc.packageName.isNullOrBlank() && it.packageName == activeSrc.packageName) ||
                        it.sourceName.equals(activeSrc.label, ignoreCase = true)
                }
            }
        }

        if (visibleItems.isEmpty()) {
            rvWatchNext.visibility = View.GONE
            layoutWatchNextEmpty.visibility = View.VISIBLE
            tvWatchNextStatus.text = "• Belum ada sumber Watch Next terhubung"
            watchNextAdapter.update(emptyList())
        } else {
            layoutWatchNextEmpty.visibility = View.GONE
            rvWatchNext.visibility = View.VISIBLE
            tvWatchNextStatus.text = "• ${visibleItems.size} item"
            watchNextAdapter.update(visibleItems)
        }
    }

    private fun launchWatchNextItem(item: ProjectivyWatchNextItem) {
        if (!item.intentUri.isNullOrBlank()) {
            try {
                val intent = Intent.parseUri(item.intentUri, Intent.URI_INTENT_SCHEME).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (_: Exception) { }
        }
        if (item.packageName.isNotBlank()) {
            val matched = allInstalledApps.firstOrNull { it.packageName == item.packageName }
            if (matched != null) {
                onAppClick(matched)
                return
            }
            try {
                val pm = context.packageManager
                val fallbackIntent = pm.getLeanbackLaunchIntentForPackage(item.packageName)
                    ?: pm.getLaunchIntentForPackage(item.packageName)
                if (fallbackIntent != null) {
                    context.startActivity(fallbackIntent)
                    return
                }
            } catch (_: Exception) { }
        }
        Toast.makeText(context, "Tidak dapat membuka ${item.title}", Toast.LENGTH_SHORT).show()
    }

    private fun showAppOptionsDialog(app: AppModel) {
        val pinned = ProjectivyWatchNextProvider.isPinned(context, app.packageName)
        val options = arrayOf(
            "▶ Buka ${app.appName}",
            if (pinned) "☆ Lepas Sematan dari Depan" else "★ Sematkan ke Depan",
            "ℹ️ Info Aplikasi",
            "🗑️ Copot Pemasangan (Uninstall)"
        )
        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(app.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onAppClick(app)
                    1 -> {
                        val nowPinned = ProjectivyWatchNextProvider.togglePinnedPackage(context, app.packageName)
                        refreshHomeAppsRow()
                        refreshTvAppsGrid()
                        Toast.makeText(
                            context,
                            if (nowPinned) "${app.appName} disematkan di depan" else "${app.appName} dilepas dari sematan",
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
    // ADAPTER 1: APPS ROW & 6-COL TV APPS GRID
    // ==============================================
    private sealed class ProjectivyCardItem {
        object TvAppsDrawerTile : ProjectivyCardItem()
        data class InstalledAppTile(val app: AppModel) : ProjectivyCardItem()
    }

    private inner class ProjectivyCardAdapter(
        private val isHomeRow: Boolean
    ) : RecyclerView.Adapter<ProjectivyCardAdapter.CardVH>() {

        private var items: List<ProjectivyCardItem> = emptyList()

        fun update(newItems: List<ProjectivyCardItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_projectivy_app_card, parent, false)
            val cardW = if (isHomeRow) (220f * scale).roundToInt().coerceAtLeast(dp(106)) else gridCardWidthPx
            val cardH = if (isHomeRow) (136f * scale).roundToInt().coerceAtLeast(dp(66)) else gridCardHeightPx
            view.layoutParams = RecyclerView.LayoutParams(cardW, cardH)
            return CardVH(view)
        }

        override fun onBindViewHolder(holder: CardVH, position: Int) {
            when (val item = items[position]) {
                is ProjectivyCardItem.TvAppsDrawerTile -> holder.bindDrawerTile()
                is ProjectivyCardItem.InstalledAppTile -> holder.bindApp(item.app)
            }
        }

        inner class CardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivArtwork: ImageView = itemView.findViewById(R.id.ivProjectivyCardArtwork)
            private val focusBorder: View = itemView.findViewById(R.id.viewProjectivyCardFocusBorder)

            init {
                val radiusPx = (8f * scale).coerceAtLeast(4f * density)
                itemView.clipToOutline = true
                itemView.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                    }
                }
                // Outline putih tegas sesuai Referensi 1 & 2 Projectivy Launcher
                focusBorder.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radiusPx
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(3), Color.WHITE)
                }
            }

            fun bindDrawerTile() {
                ivArtwork.tag = "projectivy_tv_apps_tile"
                ivArtwork.setImageResource(R.drawable.ic_projectivy_apps_tile)
                ivArtwork.scaleType = ImageView.ScaleType.CENTER_INSIDE
                val pad = dp(16)
                ivArtwork.setPadding(pad, pad, pad, pad)
                itemView.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    setColor(Color.parseColor("#E6161B26"))
                }
                bindInteraction(
                    label = "TV Apps",
                    onTrigger = { switchMode(ProjectivyViewMode.TV_APPS, focusContent = true) },
                    onMenu = null
                )
            }

            fun bindApp(app: AppModel) {
                ivArtwork.scaleType = ImageView.ScaleType.CENTER_CROP
                ivArtwork.setPadding(0, 0, 0, 0)
                itemView.background = null
                IconPackManager.bindProjectivyCenteredCard(context, ivArtwork, app)
                bindInteraction(
                    label = app.appName,
                    onTrigger = { onAppClick(app) },
                    onMenu = { showAppOptionsDialog(app) }
                )
            }

            private fun bindInteraction(
                label: String,
                onTrigger: () -> Unit,
                onMenu: (() -> Unit)?
            ) {
                val applyFocus: (Boolean, Boolean) -> Unit = { focused, animate ->
                    focusBorder.visibility = if (focused) View.VISIBLE else View.GONE
                    itemView.elevation = if (focused) 16f else 2f
                    val scaleVal = if (focused) 1.07f else 1.0f
                    if (animate) {
                        itemView.animate().scaleX(scaleVal).scaleY(scaleVal).setDuration(90L).setInterpolator(decelerate).start()
                    } else {
                        itemView.scaleX = scaleVal
                        itemView.scaleY = scaleVal
                    }
                }
                applyFocus(itemView.isFocused, false)

                itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    applyFocus(hasFocus, true)
                    if (hasFocus) {
                        if (isHomeRow) {
                            tvHomeFocusedApp.text = "• $label"
                        } else {
                            tvDrawerFocusedName.text = label
                        }
                        v.requestRectangleOnScreen(Rect(0, 0, v.width, v.height), false)
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
                        }
                    }
                    false
                }
            }
        }
    }

    // ==============================================
    // ADAPTER 2: WATCH NEXT NYATA (16:9 THUMBNAILS)
    // ==============================================
    private inner class WatchNextAdapter : RecyclerView.Adapter<WatchNextAdapter.WatchNextVH>() {
        private var items: List<ProjectivyWatchNextItem> = emptyList()

        fun update(newItems: List<ProjectivyWatchNextItem>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WatchNextVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_projectivy_watch_next_card, parent, false)
            val cardW = (340f * scale).roundToInt().coerceAtLeast(dp(168))
            val cardH = (cardW * 9f / 16f).roundToInt().coerceAtLeast(dp(94))
            view.layoutParams = RecyclerView.LayoutParams(cardW, cardH)
            return WatchNextVH(view)
        }

        override fun onBindViewHolder(holder: WatchNextVH, position: Int) {
            holder.bind(items[position])
        }

        inner class WatchNextVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivThumb: ImageView = itemView.findViewById(R.id.ivProjectivyWatchNextThumb)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvProjectivyWatchNextTitle)
            private val tvSub: TextView = itemView.findViewById(R.id.tvProjectivyWatchNextSubtitle)
            private val progressTrack: FrameLayout = itemView.findViewById(R.id.layoutProjectivyWatchNextProgressTrack)
            private val viewProgress: View = itemView.findViewById(R.id.viewProjectivyWatchNextProgress)
            private val focusBorder: View = itemView.findViewById(R.id.viewProjectivyWatchNextFocusBorder)

            init {
                val radiusPx = (8f * scale).coerceAtLeast(4f * density)
                itemView.clipToOutline = true
                itemView.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                    }
                }
                itemView.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radiusPx
                    setColor(Color.parseColor("#CC0F172A"))
                }
                focusBorder.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radiusPx
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(3), Color.WHITE)
                }
            }

            fun bind(item: ProjectivyWatchNextItem) {
                tvTitle.text = item.title
                tvSub.text = item.subtitle.ifBlank { item.sourceName }

                if (!item.posterUri.isNullOrBlank()) {
                    ivThumb.load(item.posterUri) {
                        crossfade(false)
                    }
                } else {
                    ivThumb.setImageDrawable(null)
                }

                if (item.progressFraction > 0f) {
                    progressTrack.visibility = View.VISIBLE
                    progressTrack.post {
                        val w = (progressTrack.width * item.progressFraction).roundToInt().coerceAtLeast(dp(6))
                        val lp = viewProgress.layoutParams
                        lp.width = w
                        viewProgress.layoutParams = lp
                    }
                } else {
                    progressTrack.visibility = View.GONE
                }

                val applyFocus: (Boolean, Boolean) -> Unit = { focused, animate ->
                    focusBorder.visibility = if (focused) View.VISIBLE else View.GONE
                    itemView.elevation = if (focused) 14f else 3f
                    val scaleVal = if (focused) 1.06f else 1.0f
                    if (animate) {
                        itemView.animate().scaleX(scaleVal).scaleY(scaleVal).setDuration(90L).setInterpolator(decelerate).start()
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
                itemView.setOnClickListener { launchWatchNextItem(item) }
                itemView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        launchWatchNextItem(item)
                        return@setOnKeyListener true
                    }
                    false
                }
            }
        }
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()
}
