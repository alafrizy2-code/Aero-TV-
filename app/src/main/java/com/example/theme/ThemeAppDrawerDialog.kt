package com.example.theme

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.AppModel
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * ThemeAppDrawerDialog:
 * - Untuk GOOGLE_TV: Merender halaman Google TV Apps penuh (Referensi 2).
 * - Untuk NEXFY: Merender halaman Nexfy All Apps 6 kolom.
 * - Untuk PROJECTIVY: Merender halaman Projectivy TV Apps 6 kolom kartu landscape dengan ikon di tengah.
 * - Untuk tema lain (ATV, Wolf, Default): Merender grid App Drawer sesuai ThemeRenderer.
 */
class ThemeAppDrawerDialog(
    context: Context,
    private val apps: List<AppModel>,
    private val theme: LauncherThemeDefinition,
    private val onAppClick: (AppModel) -> Unit,
    private val onClosed: () -> Unit,
    private val onOpenSettings: () -> Unit = {},
    private val onOpenVoiceSearch: () -> Unit = {},
    private val onOpenWifi: () -> Unit = {},
    private val onOpenBluetooth: () -> Unit = {},
    private val onRefreshWeather: () -> Unit = {},
    private val isWifiOnline: Boolean = false,
    private val wifiSsid: String? = null,
    private val isBluetoothOn: Boolean = false,
    private val weatherSummary: String = ""
) : Dialog(context) {

    private var googleTvAppsController: GoogleTvAppsPageController? = null
    private var nexfyController: NexfyHomeController? = null
    private var projectivyController: ProjectivyHomeController? = null
    private var projectivyScope: CoroutineScope? = null
    private var atvController: AtvHomeController? = null
    private var genericAdapter: ThemedAppAdapter? = null
    private var allStandardApps: List<AppModel> = apps
    private var standardSearchQuery: String = ""
    private var standardSearchEditText: EditText? = null
    private var standardSubtitleView: TextView? = null
    private var standardEmptyView: TextView? = null
    private var standardGridView: RecyclerView? = null

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)

        when (theme.id) {
            ThemeId.GOOGLE_TV -> setupGoogleTvAppsDrawer(context)
            ThemeId.NEXFY -> setupNexfyAllAppsDrawer(context)
            ThemeId.PROJECTIVY -> setupProjectivyTvAppsDrawer(context)
            ThemeId.ATV_LAUNCHER -> setupAtvApplicationsDrawer(context)
            else -> setupStandardThemedDrawer(context)
        }

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.CENTER)
        }

        setOnDismissListener {
            projectivyScope?.cancel()
            projectivyScope = null
            onClosed()
        }

        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                val handledGtv = googleTvAppsController?.clearSearchIfActive() == true
                val handledNexfy = nexfyController?.handleBackPressed() == true
                val handledProjectivy = projectivyController?.handleBackPressed() == true
                val handledAtv = atvController?.handleBackPressed() == true
                val handledStandardSearch = if (standardSearchQuery.isNotEmpty()) {
                    standardSearchQuery = ""
                    standardSearchEditText?.setText("")
                    applyStandardDrawerFilter()
                    standardGridView?.post {
                        standardGridView?.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    }
                    true
                } else {
                    false
                }
                if (!handledGtv && !handledNexfy && !handledProjectivy && !handledAtv && !handledStandardSearch) {
                    dismiss()
                }
                return@setOnKeyListener true
            }
            false
        }
    }

    fun updateApps(newApps: List<AppModel>) {
        allStandardApps = newApps
        googleTvAppsController?.updateApps(newApps)
        nexfyController?.updateInstalledApps(newApps)
        projectivyController?.updateInstalledApps(newApps)
        atvController?.updateInstalledApps(newApps)
        if (genericAdapter != null) {
            applyStandardDrawerFilter()
        }
    }

    private fun setupAtvApplicationsDrawer(context: Context) {
        val root = LayoutInflater.from(context).inflate(R.layout.layout_atv_host, null, false)
        val ctrl = AtvHomeController(
            context = context,
            rootView = root,
            isDrawerStandaloneMode = true,
            onAppClick = { app ->
                dismiss()
                onAppClick(app)
            },
            onOpenWifi = {
                dismiss()
                onOpenWifi()
            },
            onOpenBluetooth = {
                dismiss()
                onOpenBluetooth()
            },
            onOpenSettings = {
                dismiss()
                onOpenSettings()
            },
            onOpenVoiceSearch = {
                dismiss()
                onOpenVoiceSearch()
            },
            onRefreshWeather = onRefreshWeather,
            onDismissStandaloneDrawer = { dismiss() }
        )
        ctrl.updateInstalledApps(apps)
        ctrl.updateSystemStatus(isWifiOnline, wifiSsid, isBluetoothOn, weatherSummary)
        atvController = ctrl
        setContentView(root)
        setOnShowListener {
            ctrl.focusPrimaryItem()
        }
    }

    private fun setupProjectivyTvAppsDrawer(context: Context) {
        val root = LayoutInflater.from(context).inflate(R.layout.layout_projectivy_host, null, false)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        projectivyScope = scope
        val ctrl = ProjectivyHomeController(
            context = context,
            rootView = root,
            scope = scope,
            isDrawerStandaloneMode = true,
            onAppClick = { app ->
                dismiss()
                onAppClick(app)
            },
            onOpenWifi = {
                dismiss()
                onOpenWifi()
            },
            onOpenSettings = {
                dismiss()
                onOpenSettings()
            },
            onOpenVoiceSearch = {
                dismiss()
                onOpenVoiceSearch()
            },
            onRefreshWeather = onRefreshWeather,
            onDismissStandaloneDrawer = { dismiss() }
        )
        ctrl.updateInstalledApps(apps)
        ctrl.updateSystemStatus(isWifiOnline, wifiSsid, weatherSummary)
        projectivyController = ctrl
        setContentView(root)
        setOnShowListener {
            ctrl.focusPrimaryItem()
        }
    }

    private fun setupNexfyAllAppsDrawer(context: Context) {
        val root = LayoutInflater.from(context).inflate(R.layout.layout_nexfy_host, null, false)
        val ctrl = NexfyHomeController(
            context = context,
            rootView = root,
            isDrawerStandaloneMode = true,
            onAppClick = { app ->
                dismiss()
                onAppClick(app)
            },
            onOpenWifi = {
                dismiss()
                onOpenWifi()
            },
            onOpenBluetooth = {
                dismiss()
                onOpenBluetooth()
            },
            onOpenSettings = {
                dismiss()
                onOpenSettings()
            },
            onOpenVoiceSearch = {
                dismiss()
                onOpenVoiceSearch()
            },
            onRefreshWeather = onRefreshWeather,
            onDismissStandaloneDrawer = { dismiss() }
        )
        ctrl.updateInstalledApps(apps)
        ctrl.updateSystemStatus(isWifiOnline, wifiSsid, isBluetoothOn, weatherSummary)
        nexfyController = ctrl
        setContentView(root)
        setOnShowListener {
            ctrl.focusPrimaryItem()
        }
    }

    private fun setupGoogleTvAppsDrawer(context: Context) {
        val density = context.resources.displayMetrics.density
        val root = LayoutInflater.from(context).inflate(R.layout.layout_google_tv_host, null, false)
        root.setBackgroundColor(Color.parseColor("#EE080D1A"))

        val scrollHome: ScrollView = root.findViewById(R.id.scrollGtvHomePage)
        val scrollApps: ScrollView = root.findViewById(R.id.scrollGtvAppsPage)
        scrollHome.visibility = View.GONE
        scrollApps.visibility = View.VISIBLE

        val btnProfile: View = root.findViewById(R.id.btnGtvProfile)
        val btnSearch: View = root.findViewById(R.id.btnGtvSearch)
        val layoutTabsPill: LinearLayout = root.findViewById(R.id.layoutGtvTabsPill)
        val tabHome: TextView = root.findViewById(R.id.tabGtvHome)
        val tabApps: TextView = root.findViewById(R.id.tabGtvApps)
        val btnWallpaper: View = root.findViewById(R.id.btnGtvWallpaper)
        val btnSettings: View = root.findViewById(R.id.btnGtvSettings)
        val tvClockBrand: TextView = root.findViewById(R.id.tvGtvClockAndBrand)
        val tvWeatherSummary: TextView = root.findViewById(R.id.tvGtvWeatherSummary)

        val timeStr = try {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        } catch (_: Exception) { "00:00" }
        tvClockBrand.text = "$timeStr  |  Google TV"
        tvWeatherSummary.text = ""
        btnWallpaper.visibility = View.GONE

        layoutTabsPill.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor("#660F172A"))
            setStroke((1 * density).roundToInt(), Color.parseColor("#28FFFFFF"))
        }

        val styleTab: (TextView, Boolean) -> Unit = { tab, isActive ->
            val update: (Boolean) -> Unit = { hasFocus ->
                val bg = when {
                    isActive && hasFocus -> Color.WHITE
                    isActive -> Color.parseColor("#F1F5F9")
                    hasFocus -> Color.parseColor("#44FFFFFF")
                    else -> Color.TRANSPARENT
                }
                tab.setTextColor(if (isActive) Color.parseColor("#0F172A") else Color.WHITE)
                tab.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 999f
                    setColor(bg)
                    if (hasFocus) setStroke((2 * density).roundToInt(), Color.WHITE)
                }
            }
            update(false)
            tab.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus -> update(hasFocus) }
        }
        styleTab(tabHome, false)
        styleTab(tabApps, true)

        val styleRoundBtn: (View) -> Unit = { v ->
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#661E293B"))
                setStroke((1 * density).roundToInt(), Color.parseColor("#33FFFFFF"))
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC334155"))
                setStroke((2 * density).roundToInt(), Color.WHITE)
            }
            v.background = normal
            v.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
                view.background = if (hasFocus) focused else normal
            }
        }
        styleRoundBtn(btnProfile)
        styleRoundBtn(btnSearch)
        styleRoundBtn(btnSettings)

        val controller = GoogleTvAppsPageController(
            context = context,
            rootScroll = scrollApps,
            etSearch = root.findViewById(R.id.etGtvAppsSearch),
            btnClearSearch = root.findViewById<ImageButton>(R.id.btnGtvAppsClearSearch),
            btnVoiceSearch = root.findViewById<ImageButton>(R.id.btnGtvAppsVoiceSearch),
            tvAppsCountSubtitle = root.findViewById(R.id.tvGtvAppsCountSubtitle),
            tvFeaturedHeader = root.findViewById(R.id.tvGtvFeaturedAppsHeader),
            rvFeaturedLandscape = root.findViewById(R.id.rvGtvFeaturedLandscape),
            tvCircleHeader = root.findViewById(R.id.tvGtvAppsCircleRowHeader),
            rvCircleQuick = root.findViewById(R.id.rvGtvAppsCircleQuick),
            layoutCategoryChips = root.findViewById(R.id.layoutGtvCategoryChips),
            tvInstalledHeader = root.findViewById(R.id.tvGtvInstalledHeader),
            tvEmptySearch = root.findViewById(R.id.tvGtvAppsEmptySearch),
            rvInstalledGrid = root.findViewById(R.id.rvGtvInstalledGrid),
            onAppClick = { app ->
                dismiss()
                onAppClick(app)
            },
            onRequestVoiceSearch = {
                dismiss()
                onOpenVoiceSearch()
            }
        )
        controller.updateApps(apps)
        googleTvAppsController = controller

        tabHome.setOnClickListener { dismiss() }
        btnProfile.setOnClickListener { dismiss() }
        btnSearch.setOnClickListener { controller.focusSearchBox() }
        btnSettings.setOnClickListener {
            dismiss()
            onOpenSettings()
        }

        setContentView(root)
        setOnShowListener {
            controller.focusFirstGridItem()
        }
    }

    private fun setupStandardThemedDrawer(context: Context) {
        val density = context.resources.displayMetrics.density
        val scale = (context.resources.displayMetrics.widthPixels / 1920f).coerceAtLeast(0.5f)
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#EB090E1A"))
            setPadding((44 * density).roundToInt(), (28 * density).roundToInt(), (44 * density).roundToInt(), (24 * density).roundToInt())
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val header = RelativeLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = (14 * density).roundToInt()
            }
        }

        val titleCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_START)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        }
        val tvTitle = TextView(context).apply {
            text = "Semua Aplikasi • ${theme.name}"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        val tvSub = TextView(context).apply {
            text = "${apps.size} aplikasi terpasang • Tekan BACK untuk kembali ke Beranda"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
        }
        standardSubtitleView = tvSub
        titleCol.addView(tvTitle)
        titleCol.addView(tvSub)
        header.addView(titleCol)

        val btnClose = Button(context).apply {
            text = "✕ Tutup (Back)"
            setTextColor(Color.WHITE)
            textSize = 12f
            isAllCaps = false
            isFocusable = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(Color.parseColor("#334155"))
            }
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                (38 * density).roundToInt()
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            setOnClickListener { dismiss() }
        }
        header.addView(btnClose)
        root.addView(header)

        // Bar Pencarian Aplikasi Terpasang (untuk navigasi TV & remote)
        val searchRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((14 * density).roundToInt(), (6 * density).roundToInt(), (10 * density).roundToInt(), (6 * density).roundToInt())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(Color.parseColor("#CC111827"))
                setStroke((1 * density).roundToInt(), Color.parseColor("#44FFFFFF"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (44 * density).roundToInt()
            ).apply {
                bottomMargin = (12 * density).roundToInt()
            }
        }
        val etSearch = EditText(context).apply {
            tag = "standard_drawer_search"
            hint = "Cari aplikasi terpasang..."
            setHintTextColor(Color.parseColor("#94A3B8"))
            setTextColor(Color.WHITE)
            textSize = 13f
            background = null
            isSingleLine = true
            isFocusable = true
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        standardSearchEditText = etSearch
        val btnClear = Button(context).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 12f
            isAllCaps = false
            visibility = View.GONE
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#334155"))
            }
            layoutParams = LinearLayout.LayoutParams((32 * density).roundToInt(), (32 * density).roundToInt()).apply {
                marginEnd = (6 * density).roundToInt()
            }
            setOnClickListener {
                etSearch.setText("")
                etSearch.requestFocus()
            }
        }
        val btnVoice = Button(context).apply {
            text = "🎤 Suara"
            setTextColor(Color.WHITE)
            textSize = 11f
            isAllCaps = false
            isFocusable = true
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(Color.parseColor("#1E3A5F"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (32 * density).roundToInt()
            )
            setOnClickListener {
                dismiss()
                onOpenVoiceSearch()
            }
        }
        searchRow.addView(etSearch)
        searchRow.addView(btnClear)
        searchRow.addView(btnVoice)
        root.addView(searchRow)

        val tvEmpty = TextView(context).apply {
            text = "Tidak ada aplikasi yang cocok dengan pencarian."
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        standardEmptyView = tvEmpty

        val adapter = ThemedAppAdapter(context, allStandardApps, theme) { app ->
            dismiss()
            onAppClick(app)
        }
        genericAdapter = adapter
        val renderer = adapter.renderer
        val screenW = context.resources.displayMetrics.widthPixels
        val rootPadH = (44 * density).roundToInt() * 2 + (12 * scale).roundToInt() * 2
        val availW = (screenW - rootPadH).coerceAtLeast(renderer.cardWidthPx)
        val unitW = (renderer.cardWidthPx + renderer.gapHorizontalPx).coerceAtLeast(1)
        val maxFitCols = (availW / unitW).coerceIn(3, 9)
        val spanCount = minOf(theme.layout.drawerColumns.coerceIn(3, 9), maxFitCols)
        val halfH = (renderer.gapHorizontalPx / 2).coerceAtLeast(4)
        val halfV = (renderer.gapVerticalPx / 2).coerceAtLeast(4)

        val rvGrid = RecyclerView(context).apply {
            tag = "standard_drawer_grid"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            clipChildren = false
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_NEVER
            itemAnimator = null
            setItemViewCacheSize(32)
            layoutManager = GridLayoutManager(context, spanCount)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(halfH, halfV, halfH, halfV)
                }
            })
            val padEdge = (12 * scale).roundToInt()
            setPadding(padEdge, padEdge, padEdge, (24 * scale).roundToInt())
            this.adapter = adapter
        }
        standardGridView = rvGrid

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                standardSearchQuery = s?.toString()?.trim() ?: ""
                btnClear.visibility = if (standardSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyStandardDrawerFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                rvGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                return@setOnKeyListener true
            }
            false
        }

        root.addView(tvEmpty)
        root.addView(rvGrid)
        setContentView(root)

        setOnShowListener {
            rvGrid.post {
                rvGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: etSearch.requestFocus()
            }
        }
    }

    private fun applyStandardDrawerFilter() {
        val adapter = genericAdapter ?: return
        val q = standardSearchQuery.lowercase(Locale.getDefault())
        val filtered = if (q.isEmpty()) {
            allStandardApps
        } else {
            allStandardApps.filter {
                it.appName.lowercase(Locale.getDefault()).contains(q) ||
                    it.packageName.lowercase(Locale.getDefault()).contains(q)
            }
        }
        adapter.updateApps(filtered)
        standardSubtitleView?.text = if (q.isEmpty()) {
            "${filtered.size} aplikasi terpasang • Tekan BACK untuk kembali ke Beranda"
        } else {
            "${filtered.size} hasil pencarian \"$standardSearchQuery\" • Tekan BACK untuk reset"
        }
        standardEmptyView?.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        standardGridView?.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }
}
