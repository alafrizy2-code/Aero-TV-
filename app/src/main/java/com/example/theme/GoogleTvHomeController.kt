package com.example.theme

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
import android.widget.ScrollView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.AppModel
import com.example.R
import com.example.model.FootballMatch
import com.example.model.MatchStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * GoogleTvHomeController:
 * Mengatur keseluruhan UI Google TV Home Screen (Referensi 1) & Google TV Apps (Referensi 2)
 * di atas layer wallpaper asli pengguna tanpa merusak komponen Aero TV / Pramana lainnya.
 */
class GoogleTvHomeController(
    private val context: Context,
    private val rootView: View,
    private val scope: CoroutineScope,
    private val onAppClick: (AppModel) -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onOpenWallpaperPicker: () -> Unit,
    private val onOpenVoiceSearch: () -> Unit,
    private val onOpenFootballCenter: () -> Unit
) {
    private val density = context.resources.displayMetrics.density
    private val decelerate = DecelerateInterpolator()

    private var activeTab: GoogleTvTabId = GoogleTvTabId.HOME
    private var currentApps: List<AppModel> = emptyList()
    private var currentMatches: List<FootballMatch> = emptyList()
    private var currentWeatherText: String = ""

    private var heroItems: List<GoogleTvHeroItem> = emptyList()
    private var heroIndex: Int = 0

    // Top Navigation Bar Views
    private val btnProfile: FrameLayout = rootView.findViewById(R.id.btnGtvProfile)
    private val btnSearch: ImageButton = rootView.findViewById(R.id.btnGtvSearch)
    private val layoutTabsPill: LinearLayout = rootView.findViewById(R.id.layoutGtvTabsPill)
    private val tabHome: TextView = rootView.findViewById(R.id.tabGtvHome)
    private val tabApps: TextView = rootView.findViewById(R.id.tabGtvApps)
    private val btnWallpaper: ImageButton = rootView.findViewById(R.id.btnGtvWallpaper)
    private val btnSettings: ImageButton = rootView.findViewById(R.id.btnGtvSettings)
    private val tvWeatherSummary: TextView = rootView.findViewById(R.id.tvGtvWeatherSummary)
    private val tvClockAndBrand: TextView = rootView.findViewById(R.id.tvGtvClockAndBrand)

    // Home Page Views
    private val scrollHome: ScrollView = rootView.findViewById(R.id.scrollGtvHomePage)
    private val cardHeroBanner: FrameLayout = rootView.findViewById(R.id.cardGtvHeroBanner)
    private val ivHeroBackdrop: ImageView = rootView.findViewById(R.id.ivGtvHeroBackdrop)
    private val ivHeroAppIcon: ImageView = rootView.findViewById(R.id.ivGtvHeroAppIcon)
    private val tvHeroBadge: TextView = rootView.findViewById(R.id.tvGtvHeroBadge)
    private val tvHeroTitle: TextView = rootView.findViewById(R.id.tvGtvHeroTitle)
    private val tvHeroSubtitle: TextView = rootView.findViewById(R.id.tvGtvHeroSubtitle)
    private val tvHeroActionPill: TextView = rootView.findViewById(R.id.tvGtvHeroActionPill)
    private val tvHeroCounter: TextView = rootView.findViewById(R.id.tvGtvHeroCounter)
    private val viewHeroFocusBorder: View = rootView.findViewById(R.id.viewGtvHeroFocusBorder)
    private val rvTopPicks: RecyclerView = rootView.findViewById(R.id.rvGtvTopPicks)
    private val rvYourApps: RecyclerView = rootView.findViewById(R.id.rvGtvYourApps)

    // Apps Page Views
    private val scrollApps: ScrollView = rootView.findViewById(R.id.scrollGtvAppsPage)
    private val etAppsSearch: EditText = rootView.findViewById(R.id.etGtvAppsSearch)
    private val btnClearSearch: ImageButton = rootView.findViewById(R.id.btnGtvAppsClearSearch)
    private val btnVoiceSearch: ImageButton = rootView.findViewById(R.id.btnGtvAppsVoiceSearch)
    private val tvAppsCountSubtitle: TextView = rootView.findViewById(R.id.tvGtvAppsCountSubtitle)
    private val tvFeaturedHeader: TextView = rootView.findViewById(R.id.tvGtvFeaturedAppsHeader)
    private val rvFeaturedLandscape: RecyclerView = rootView.findViewById(R.id.rvGtvFeaturedLandscape)
    private val tvCircleHeader: TextView = rootView.findViewById(R.id.tvGtvAppsCircleRowHeader)
    private val rvCircleQuick: RecyclerView = rootView.findViewById(R.id.rvGtvAppsCircleQuick)
    private val layoutCategoryChips: LinearLayout = rootView.findViewById(R.id.layoutGtvCategoryChips)
    private val tvInstalledHeader: TextView = rootView.findViewById(R.id.tvGtvInstalledHeader)
    private val tvEmptySearch: TextView = rootView.findViewById(R.id.tvGtvAppsEmptySearch)
    private val rvInstalledGrid: RecyclerView = rootView.findViewById(R.id.rvGtvInstalledGrid)

    private val topPicksAdapter = TopPicksAdapter(emptyList())
    private val homeCircleAppsAdapter = HomeCircleAppsAdapter(emptyList())
    private val appsPageController: GoogleTvAppsPageController

    init {
        setupTopBar()
        setupHeroSection()
        setupHomeRows()

        appsPageController = GoogleTvAppsPageController(
            context = context,
            rootScroll = scrollApps,
            etSearch = etAppsSearch,
            btnClearSearch = btnClearSearch,
            btnVoiceSearch = btnVoiceSearch,
            tvAppsCountSubtitle = tvAppsCountSubtitle,
            tvFeaturedHeader = tvFeaturedHeader,
            rvFeaturedLandscape = rvFeaturedLandscape,
            tvCircleHeader = tvCircleHeader,
            rvCircleQuick = rvCircleQuick,
            layoutCategoryChips = layoutCategoryChips,
            tvInstalledHeader = tvInstalledHeader,
            tvEmptySearch = tvEmptySearch,
            rvInstalledGrid = rvInstalledGrid,
            onAppClick = onAppClick,
            onRequestVoiceSearch = onOpenVoiceSearch
        )

        switchTab(GoogleTvTabId.HOME, focusContent = false)
        refreshClockAndWeather(currentWeatherText)
    }

    private fun setupTopBar() {
        layoutTabsPill.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor("#660F172A"))
            setStroke(dp(1), Color.parseColor("#28FFFFFF"))
        }

        val setupCircleTopBtn: (View, () -> Unit) -> Unit = { btn, click ->
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#661E293B"))
                setStroke(dp(1), Color.parseColor("#33FFFFFF"))
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC334155"))
                setStroke(dp(3), Color.WHITE)
            }
            btn.background = normal
            btn.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focused else normal
                v.animate().scaleX(if (hasFocus) 1.12f else 1.0f).scaleY(if (hasFocus) 1.12f else 1.0f).setDuration(100).start()
            }
            btn.setOnClickListener { click() }
        }

        setupCircleTopBtn(btnProfile) {
            AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("Profil Google TV • Aero TV")
                .setMessage("Tema aktif: Google TV Theme\nTotal aplikasi terpasang: ${currentApps.size}\nWallpaper & fitur Aero TV tetap aktif di latar belakang.")
                .setPositiveButton("Buka Pengaturan") { _, _ -> onOpenSettings() }
                .setNegativeButton("Tutup", null)
                .show()
        }

        setupCircleTopBtn(btnSearch) {
            switchTab(GoogleTvTabId.APPS, focusContent = false)
            appsPageController.focusSearchBox()
        }

        setupCircleTopBtn(btnWallpaper) {
            onOpenWallpaperPicker()
        }

        setupCircleTopBtn(btnSettings) {
            onOpenSettings()
        }

        tabHome.setOnClickListener {
            switchTab(GoogleTvTabId.HOME, focusContent = false)
        }
        tabApps.setOnClickListener {
            switchTab(GoogleTvTabId.APPS, focusContent = false)
        }

        tabHome.onFocusChangeListener = View.OnFocusChangeListener { _, _ -> updateTabStyles() }
        tabApps.onFocusChangeListener = View.OnFocusChangeListener { _, _ -> updateTabStyles() }

        updateTabStyles()
    }

    private fun updateTabStyles() {
        val styleTab: (TextView, Boolean) -> Unit = { tab, isActive ->
            val isFocused = tab.isFocused
            val bgColor = when {
                isActive && isFocused -> Color.WHITE
                isActive -> Color.parseColor("#F1F5F9")
                isFocused -> Color.parseColor("#44FFFFFF")
                else -> Color.TRANSPARENT
            }
            tab.setTextColor(if (isActive) Color.parseColor("#0F172A") else Color.WHITE)
            tab.background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 999f
                setColor(bgColor)
                if (isFocused) {
                    setStroke(dp(2), if (isActive) Color.parseColor("#38BDF8") else Color.WHITE)
                }
            }
            tab.animate().scaleX(if (isFocused) 1.05f else 1.0f).scaleY(if (isFocused) 1.05f else 1.0f).setDuration(90).start()
        }

        styleTab(tabHome, activeTab == GoogleTvTabId.HOME)
        styleTab(tabApps, activeTab == GoogleTvTabId.APPS)
    }

    private fun setupHeroSection() {
        val radiusPx = 14f * density
        cardHeroBanner.clipToOutline = true
        cardHeroBanner.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
        cardHeroBanner.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.parseColor("#CC0F172A"))
        }
        viewHeroFocusBorder.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.TRANSPARENT)
            setStroke(dp(3), Color.WHITE)
        }
        tvHeroActionPill.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 999f
            setColor(Color.parseColor("#F1F5F9"))
        }

        cardHeroBanner.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            viewHeroFocusBorder.visibility = if (hasFocus) View.VISIBLE else View.GONE
            v.elevation = if (hasFocus) 16f else 4f
            val scale = if (hasFocus) 1.02f else 1.0f
            v.animate().scaleX(scale).scaleY(scale).setDuration(120).setInterpolator(decelerate).start()
            if (hasFocus) {
                scrollHome.post { scrollHome.smoothScrollTo(0, 0) }
            }
        }

        cardHeroBanner.setOnClickListener {
            triggerActiveHeroAction()
        }

        cardHeroBanner.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (heroItems.size > 1) {
                            heroIndex = (heroIndex - 1 + heroItems.size) % heroItems.size
                            renderCurrentHero()
                        }
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> {
                        if (heroItems.size > 1) {
                            heroIndex = (heroIndex + 1) % heroItems.size
                            renderCurrentHero()
                        }
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                        triggerActiveHeroAction()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }

    private fun setupHomeRows() {
        rvTopPicks.apply {
            layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
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
            setItemViewCacheSize(14)
            adapter = topPicksAdapter
        }

        rvYourApps.apply {
            layoutManager = object : LinearLayoutManager(context, HORIZONTAL, false) {
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
            adapter = homeCircleAppsAdapter
        }
    }

    fun switchTab(tab: GoogleTvTabId, focusContent: Boolean = false) {
        activeTab = tab
        updateTabStyles()
        if (tab == GoogleTvTabId.HOME) {
            scrollHome.visibility = View.VISIBLE
            scrollApps.visibility = View.GONE
            if (focusContent) {
                rootView.post {
                    rvYourApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                        ?: cardHeroBanner.requestFocus()
                }
            }
        } else {
            scrollHome.visibility = View.GONE
            scrollApps.visibility = View.VISIBLE
            if (focusContent) {
                appsPageController.focusFirstGridItem()
            }
        }
    }

    fun focusPrimaryItem() {
        if (activeTab == GoogleTvTabId.HOME) {
            rvYourApps.post {
                rvYourApps.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: cardHeroBanner.requestFocus()
            }
        } else {
            appsPageController.focusFirstGridItem()
        }
    }

    fun handleBackPressed(): Boolean {
        if (activeTab == GoogleTvTabId.APPS) {
            if (appsPageController.clearSearchIfActive()) {
                return true
            }
            switchTab(GoogleTvTabId.HOME, focusContent = false)
            tabHome.post { tabHome.requestFocus() }
            return true
        }
        return false
    }

    fun updateInstalledApps(apps: List<AppModel>) {
        currentApps = apps
        heroItems = GoogleTvContentProvider.buildHeroItems(apps, currentMatches)
        if (heroIndex >= heroItems.size) heroIndex = 0
        renderCurrentHero()
        homeCircleAppsAdapter.updateApps(apps)
        appsPageController.updateApps(apps)
        rebuildTopPicksAsync()
    }

    fun updateFootballMatches(matches: List<FootballMatch>) {
        currentMatches = matches
        heroItems = GoogleTvContentProvider.buildHeroItems(currentApps, matches)
        if (heroIndex >= heroItems.size) heroIndex = 0
        renderCurrentHero()
        rebuildTopPicksAsync()
    }

    fun refreshClockAndWeather(weatherText: String) {
        currentWeatherText = weatherText
        val timeStr = try {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        } catch (_: Exception) {
            "00:00"
        }
        tvClockAndBrand.text = "$timeStr  |  Google TV"
        tvWeatherSummary.text = weatherText.takeIf {
            it.isNotBlank() && !it.contains("Memuat", ignoreCase = true)
        } ?: ""
    }

    private fun rebuildTopPicksAsync() {
        scope.launch {
            val picks = GoogleTvContentProvider.buildTopPickItems(
                installedApps = currentApps,
                footballMatches = currentMatches,
                weatherSummary = currentWeatherText
            )
            topPicksAdapter.update(picks)
        }
    }

    private fun renderCurrentHero() {
        if (heroItems.isEmpty()) return
        val item = heroItems[heroIndex.coerceIn(0, heroItems.size - 1)]
        val counterText = if (heroItems.size > 1) "${heroIndex + 1} / ${heroItems.size}  •  ◀ Geser ▶" else ""
        tvHeroCounter.text = counterText

        when (item) {
            is GoogleTvHeroItem.InstalledAppHero -> {
                val app = item.app
                tvHeroBadge.text = item.badgeLabel
                tvHeroTitle.text = app.appName
                tvHeroSubtitle.text = item.subtitle
                tvHeroActionPill.text = "Buka ${app.appName}"
                ivHeroAppIcon.visibility = View.VISIBLE
                ivHeroAppIcon.setImageDrawable(app.icon)
                IconPackManager.bindGoogleTvLandscapeBanner(context, ivHeroBackdrop, app)
            }
            is GoogleTvHeroItem.LiveMatchHero -> {
                val m = item.match
                val livePrefix = if (m.status == MatchStatus.LIVE) {
                    "● LIVE ${m.liveMinute ?: ""} • ${m.leagueName}"
                } else {
                    "Jadwal Liga • ${m.leagueName}"
                }
                tvHeroBadge.text = livePrefix
                tvHeroTitle.text = "${m.homeTeam} vs ${m.awayTeam}"
                val scoreOrTime = if (m.status == MatchStatus.LIVE && m.homeScore != null && m.awayScore != null) {
                    "Skor Sementara: ${m.homeScore} - ${m.awayScore}"
                } else {
                    "${m.matchDateWib} • ${m.matchTimeWib}"
                }
                tvHeroSubtitle.text = "$scoreOrTime  •  Data Resmi ESPN"
                tvHeroActionPill.text = "Lihat Jadwal Liga"
                ivHeroAppIcon.visibility = View.VISIBLE
                ivHeroAppIcon.setImageResource(R.drawable.ic_football)
                ivHeroBackdrop.setImageResource(R.drawable.bg_football_frosted_glass)
            }
            is GoogleTvHeroItem.EmptySourceNotice -> {
                tvHeroBadge.text = "Sumber Konten"
                tvHeroTitle.text = item.title
                tvHeroSubtitle.text = item.subtitle
                tvHeroActionPill.text = "Buka Aplikasi"
                ivHeroAppIcon.visibility = View.GONE
                ivHeroBackdrop.setImageDrawable(null)
            }
        }
    }

    private fun triggerActiveHeroAction() {
        if (heroItems.isEmpty()) {
            switchTab(GoogleTvTabId.APPS, focusContent = true)
            return
        }
        when (val item = heroItems[heroIndex.coerceIn(0, heroItems.size - 1)]) {
            is GoogleTvHeroItem.InstalledAppHero -> onAppClick(item.app)
            is GoogleTvHeroItem.LiveMatchHero -> onOpenFootballCenter()
            is GoogleTvHeroItem.EmptySourceNotice -> switchTab(GoogleTvTabId.APPS, focusContent = true)
        }
    }

    private fun handleTopPickClick(item: GoogleTvTopPickItem) {
        when (item) {
            is GoogleTvTopPickItem.FootballCard -> onOpenFootballCenter()
            is GoogleTvTopPickItem.QuickAppCard -> onAppClick(item.app)
            is GoogleTvTopPickItem.LauncherActionCard -> {
                when (item.actionId) {
                    GoogleTvContentProvider.ACTION_ALL_APPS -> switchTab(GoogleTvTabId.APPS, focusContent = true)
                    GoogleTvContentProvider.ACTION_VOICE_SEARCH -> onOpenVoiceSearch()
                    GoogleTvContentProvider.ACTION_WALLPAPER_SETTINGS -> onOpenWallpaperPicker()
                    GoogleTvContentProvider.ACTION_FOOTBALL_CENTER -> onOpenFootballCenter()
                }
            }
            is GoogleTvTopPickItem.RecommendationPlaceholder -> {
                switchTab(GoogleTvTabId.APPS, focusContent = true)
            }
        }
    }

    // ==============================================
    // ADAPTERS: TOP PICKS & YOUR APPS (CIRCULAR)
    // ==============================================
    private inner class TopPicksAdapter(
        private var items: List<GoogleTvTopPickItem>
    ) : RecyclerView.Adapter<TopPicksAdapter.TopPickViewHolder>() {

        fun update(newItems: List<GoogleTvTopPickItem>) {
            this.items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopPickViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_google_tv_top_pick, parent, false)
            return TopPickViewHolder(view)
        }

        override fun onBindViewHolder(holder: TopPickViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class TopPickViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivBackdrop: ImageView = itemView.findViewById(R.id.ivTopPickBackdrop)
            private val viewScrim: View = itemView.findViewById(R.id.viewTopPickScrim)
            private val tvBadge: TextView = itemView.findViewById(R.id.tvTopPickBadge)
            private val tvTitle: TextView = itemView.findViewById(R.id.tvTopPickTitle)
            private val tvSub: TextView = itemView.findViewById(R.id.tvTopPickSubtitle)
            private val focusBorder: View = itemView.findViewById(R.id.viewTopPickFocusBorder)

            init {
                val radiusPx = 10f * density
                itemView.clipToOutline = true
                itemView.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
                    }
                }
                focusBorder.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = radiusPx
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(3), Color.WHITE)
                }
            }

            fun bind(item: GoogleTvTopPickItem) {
                when (item) {
                    is GoogleTvTopPickItem.FootballCard -> {
                        val m = item.match
                        tvBadge.text = if (m.status == MatchStatus.LIVE) "● LIVE" else m.leagueName
                        tvBadge.setTextColor(if (m.status == MatchStatus.LIVE) Color.parseColor("#F87171") else Color.parseColor("#38BDF8"))
                        tvTitle.text = "${m.shortHomeTeam} vs ${m.shortAwayTeam}"
                        tvSub.text = "${m.matchTimeWib} • ${m.matchDateWib}"
                        ivBackdrop.setImageResource(R.drawable.bg_football_frosted_glass)
                        viewScrim.setBackgroundColor(Color.parseColor("#4D0F172A"))
                    }
                    is GoogleTvTopPickItem.QuickAppCard -> {
                        tvBadge.text = "Aplikasi"
                        tvBadge.setTextColor(Color.parseColor("#A7F3D0"))
                        tvTitle.text = item.app.appName
                        tvSub.text = item.subtitle
                        IconPackManager.bindGoogleTvLandscapeBanner(context, ivBackdrop, item.app)
                        viewScrim.setBackgroundColor(Color.parseColor("#66090F1E"))
                    }
                    is GoogleTvTopPickItem.LauncherActionCard -> {
                        tvBadge.text = item.badge
                        tvBadge.setTextColor(Color.parseColor("#38BDF8"))
                        tvTitle.text = item.title
                        tvSub.text = item.subtitle
                        val bgRes = when (item.actionId) {
                            GoogleTvContentProvider.ACTION_ALL_APPS -> R.drawable.ic_apps_grid
                            GoogleTvContentProvider.ACTION_VOICE_SEARCH -> R.drawable.ic_mic
                            else -> R.drawable.ic_wallpaper_photo
                        }
                        ivBackdrop.setImageResource(bgRes)
                        ivBackdrop.scaleType = ImageView.ScaleType.CENTER
                        ivBackdrop.alpha = 0.22f
                        viewScrim.setBackgroundColor(Color.parseColor("#990F172A"))
                    }
                    is GoogleTvTopPickItem.RecommendationPlaceholder -> {
                        tvBadge.text = "Info"
                        tvTitle.text = item.title
                        tvSub.text = item.subtitle
                        ivBackdrop.setImageDrawable(null)
                    }
                }
                if (item !is GoogleTvTopPickItem.LauncherActionCard) {
                    ivBackdrop.scaleType = ImageView.ScaleType.CENTER_CROP
                    ivBackdrop.alpha = 1.0f
                }

                val applyFocus: (Boolean, Boolean) -> Unit = { hasFocus, animate ->
                    focusBorder.visibility = if (hasFocus) View.VISIBLE else View.GONE
                    itemView.elevation = if (hasFocus) 14f else 3f
                    val scale = if (hasFocus) 1.08f else 1.0f
                    if (animate) {
                        itemView.animate().scaleX(scale).scaleY(scale).setDuration(110L).setInterpolator(decelerate).start()
                    } else {
                        itemView.scaleX = scale
                        itemView.scaleY = scale
                    }
                }
                applyFocus(itemView.isFocused, false)

                itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    applyFocus(hasFocus, true)
                    if (hasFocus) {
                        scrollChildIntoView(v)
                    }
                }
                itemView.setOnClickListener { handleTopPickClick(item) }
                itemView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        handleTopPickClick(item)
                        return@setOnKeyListener true
                    }
                    false
                }
            }
        }
    }

    private inner class HomeCircleAppsAdapter(
        private var apps: List<AppModel>
    ) : RecyclerView.Adapter<HomeCircleAppsAdapter.CircleItemVH>() {

        fun updateApps(newApps: List<AppModel>) {
            this.apps = newApps
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int = apps.size + 1

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircleItemVH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_google_tv_circle_app, parent, false)
            return CircleItemVH(view)
        }

        override fun onBindViewHolder(holder: CircleItemVH, position: Int) {
            if (position < apps.size) {
                holder.bindApp(apps[position])
            } else {
                holder.bindSeeAll()
            }
        }

        inner class CircleItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val frameCircle: FrameLayout = itemView.findViewById(R.id.frameGoogleTvCircle)
            private val ivIcon: ImageView = itemView.findViewById(R.id.ivGoogleTvCircleIcon)
            private val focusRing: View = itemView.findViewById(R.id.viewGoogleTvCircleFocusRing)
            private val tvLabel: TextView = itemView.findViewById(R.id.tvGoogleTvCircleLabel)

            init {
                frameCircle.clipToOutline = true
                frameCircle.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
                focusRing.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(3), Color.WHITE)
                }
            }

            fun bindApp(app: AppModel) {
                tvLabel.text = app.appName
                ivIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                ivIcon.setPadding(0, 0, 0, 0)
                ivIcon.background = null
                IconPackManager.bindGoogleTvCircleIcon(context, ivIcon, app)
                bindInteraction { onAppClick(app) }
            }

            fun bindSeeAll() {
                tvLabel.text = "Semua"
                ivIcon.tag = "see_all"
                ivIcon.setImageResource(R.drawable.ic_apps_grid)
                ivIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                val pad = dp(18)
                ivIcon.setPadding(pad, pad, pad, pad)
                ivIcon.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.parseColor("#CC1E293B"))
                }
                bindInteraction { switchTab(GoogleTvTabId.APPS, focusContent = true) }
            }

            private fun bindInteraction(onTrigger: () -> Unit) {
                val applyFocus: (Boolean, Boolean) -> Unit = { focused, animate ->
                    focusRing.visibility = if (focused) View.VISIBLE else View.GONE
                    tvLabel.setTextColor(if (focused) Color.WHITE else Color.parseColor("#CBD5E1"))
                    tvLabel.typeface = if (focused) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    itemView.elevation = if (focused) 14f else 2f
                    val scale = if (focused) 1.13f else 1.0f
                    if (animate) {
                        itemView.animate().scaleX(scale).scaleY(scale).setDuration(110L).setInterpolator(decelerate).start()
                    } else {
                        itemView.scaleX = scale
                        itemView.scaleY = scale
                    }
                }
                applyFocus(itemView.isFocused, false)

                itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    applyFocus(hasFocus, true)
                    if (hasFocus) {
                        scrollChildIntoView(v)
                    }
                }
                itemView.setOnClickListener { onTrigger() }
                itemView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        when (keyCode) {
                            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                                onTrigger()
                                return@setOnKeyListener true
                            }
                            KeyEvent.KEYCODE_DPAD_DOWN -> {
                                switchTab(GoogleTvTabId.APPS, focusContent = true)
                                return@setOnKeyListener true
                            }
                        }
                    }
                    false
                }
            }
        }
    }

    private fun scrollChildIntoView(child: View) {
        val rect = Rect(0, 0, child.width, child.height)
        child.requestRectangleOnScreen(rect, false)
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()
}
