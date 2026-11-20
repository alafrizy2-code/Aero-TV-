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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.AppCategory
import com.example.AppModel
import com.example.R
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Pengendali halaman Google TV Apps (Referensi 2).
 * Digunakan bersama baik saat tab "Aplikasi" dibuka di Google TV Home Screen
 * maupun saat App Drawer dibuka ketika "Use Home Screen Default" dalam keadaan OFF.
 */
class GoogleTvAppsPageController(
    private val context: Context,
    private val rootScroll: ScrollView,
    private val etSearch: EditText,
    private val btnClearSearch: ImageButton,
    private val btnVoiceSearch: ImageButton,
    private val tvAppsCountSubtitle: TextView,
    private val tvFeaturedHeader: TextView,
    private val rvFeaturedLandscape: RecyclerView,
    private val tvCircleHeader: TextView,
    private val rvCircleQuick: RecyclerView,
    private val layoutCategoryChips: LinearLayout,
    private val tvInstalledHeader: TextView,
    private val tvEmptySearch: TextView,
    private val rvInstalledGrid: RecyclerView,
    private val onAppClick: (AppModel) -> Unit,
    private val onRequestVoiceSearch: () -> Unit
) {
    private val density = context.resources.displayMetrics.density
    private val decelerate = DecelerateInterpolator()

    private var allApps: List<AppModel> = emptyList()
    private var selectedCategory: AppCategory = AppCategory.ALL
    private var searchQuery: String = ""

    private val featuredAdapter = LandscapeAppsAdapter(emptyList())
    private val circleQuickAdapter = CircleAppsAdapter(emptyList())
    private val gridAppsAdapter = LandscapeAppsAdapter(emptyList())

    private var gridColumns: Int = 5

    init {
        setupRecyclerViews()
        setupSearchControls()
    }

    private fun setupRecyclerViews() {
        val screenW = context.resources.displayMetrics.widthPixels
        val sidePadPx = dp(72)
        val availW = (screenW - sidePadPx).coerceAtLeast(dp(500))
        val tileUnitPx = dp(176) // 164dp card + 12dp gap
        gridColumns = (availW / tileUnitPx).coerceIn(4, 6)

        rvFeaturedLandscape.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(10)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    val pos = parent.getChildAdapterPosition(view)
                    val count = parent.adapter?.itemCount ?: 0
                    outRect.set(0, 0, if (pos < count - 1) dp(12) else 0, 0)
                }
            })
            adapter = featuredAdapter
        }

        rvCircleQuick.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            itemAnimator = null
            setHasFixedSize(true)
            setItemViewCacheSize(16)
            adapter = circleQuickAdapter
        }

        rvInstalledGrid.apply {
            layoutManager = GridLayoutManager(context, gridColumns)
            itemAnimator = null
            setHasFixedSize(false)
            isNestedScrollingEnabled = false
            setItemViewCacheSize(30)
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                    outRect.set(dp(4), dp(4), dp(8), dp(10))
                }
            })
            adapter = gridAppsAdapter
        }
    }

    private fun setupSearchControls() {
        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            etSearch.requestFocus()
        }
        btnVoiceSearch.setOnClickListener {
            onRequestVoiceSearch()
        }

        val applyBtnFocusStyle: (View) -> Unit = { btn ->
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.TRANSPARENT)
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33FFFFFF"))
                setStroke(dp(2), Color.WHITE)
            }
            btn.background = normal
            btn.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focused else normal
                v.animate().scaleX(if (hasFocus) 1.12f else 1.0f).scaleY(if (hasFocus) 1.12f else 1.0f).setDuration(100).start()
            }
        }
        applyBtnFocusStyle(btnClearSearch)
        applyBtnFocusStyle(btnVoiceSearch)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        etSearch.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                val moved = if (rvFeaturedLandscape.visibility == View.VISIBLE) {
                    rvFeaturedLandscape.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() == true
                } else {
                    false
                }
                if (!moved) {
                    val chipFocused = (layoutCategoryChips.childCount > 0 && layoutCategoryChips.getChildAt(0).requestFocus())
                    if (!chipFocused) {
                        rvInstalledGrid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    }
                }
                return@setOnKeyListener true
            }
            false
        }
    }

    fun updateApps(apps: List<AppModel>) {
        allApps = apps
        tvAppsCountSubtitle.text = "${apps.size} aplikasi terpasang di perangkat • Tekan OK untuk membuka • Menu untuk opsi"

        // Unggulan: aplikasi media / TV nyata pertama
        val featured = apps.filter {
            it.category == AppCategory.MEDIA_VIDEO || it.isTvApp || it.banner != null
        }.ifEmpty { apps }.take(8)
        featuredAdapter.update(featured)

        // Deretan ikon bulat cepat
        circleQuickAdapter.update(apps.take(16))

        rebuildCategoryChips()
        applyFilters()
    }

    fun focusSearchBox() {
        rootScroll.post {
            rootScroll.smoothScrollTo(0, 0)
            etSearch.requestFocus()
        }
    }

    fun focusFirstGridItem() {
        rootScroll.post {
            val vh = rvInstalledGrid.findViewHolderForAdapterPosition(0)
            if (vh != null) {
                vh.itemView.requestFocus()
            } else {
                rvFeaturedLandscape.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    ?: etSearch.requestFocus()
            }
        }
    }

    fun clearSearchIfActive(): Boolean {
        if (searchQuery.isNotEmpty() || selectedCategory != AppCategory.ALL) {
            searchQuery = ""
            selectedCategory = AppCategory.ALL
            etSearch.setText("")
            rebuildCategoryChips()
            applyFilters()
            return true
        }
        return false
    }

    private fun rebuildCategoryChips() {
        layoutCategoryChips.removeAllViews()
        val categories = AppCategory.values().filter { cat ->
            cat == AppCategory.ALL || allApps.any { it.category == cat }
        }

        for (cat in categories) {
            val count = if (cat == AppCategory.ALL) allApps.size else allApps.count { it.category == cat }
            val isSelected = (cat == selectedCategory)

            val chip = TextView(context).apply {
                text = "${cat.label} ($count)"
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                isFocusable = true
                isClickable = true
                setPadding(dp(14), dp(7), dp(14), dp(7))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dp(8)
                }

                val updateBg: (Boolean) -> Unit = { hasFocus ->
                    val bgColor = when {
                        hasFocus -> Color.WHITE
                        isSelected -> Color.parseColor("#E2E8F0")
                        else -> Color.parseColor("#661E293B")
                    }
                    val txtColor = if (hasFocus || isSelected) Color.parseColor("#0F172A") else Color.parseColor("#E2E8F0")
                    setTextColor(txtColor)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 999f
                        setColor(bgColor)
                        if (hasFocus) {
                            setStroke(dp(2), Color.parseColor("#38BDF8"))
                        } else if (!isSelected) {
                            setStroke(dp(1), Color.parseColor("#33FFFFFF"))
                        }
                    }
                }
                updateBg(false)

                onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    updateBg(hasFocus)
                    v.animate().scaleX(if (hasFocus) 1.06f else 1.0f).scaleY(if (hasFocus) 1.06f else 1.0f).setDuration(100).start()
                }
                setOnClickListener {
                    selectedCategory = cat
                    rebuildCategoryChips()
                    applyFilters()
                }
            }
            layoutCategoryChips.addView(chip)
        }
    }

    private fun applyFilters() {
        val q = searchQuery.lowercase(Locale.getDefault())
        val isFiltering = q.isNotEmpty() || selectedCategory != AppCategory.ALL

        val filtered = allApps.filter { app ->
            val matchCat = (selectedCategory == AppCategory.ALL || app.category == selectedCategory)
            val matchQuery = q.isEmpty() ||
                app.appName.lowercase(Locale.getDefault()).contains(q) ||
                app.packageName.lowercase(Locale.getDefault()).contains(q)
            matchCat && matchQuery
        }

        // Saat mencari, fokuskan layar pada hasil grid
        val showShowcase = q.isEmpty()
        tvFeaturedHeader.visibility = if (showShowcase) View.VISIBLE else View.GONE
        rvFeaturedLandscape.visibility = if (showShowcase) View.VISIBLE else View.GONE
        tvCircleHeader.visibility = if (showShowcase) View.VISIBLE else View.GONE
        rvCircleQuick.visibility = if (showShowcase) View.VISIBLE else View.GONE

        tvInstalledHeader.text = if (isFiltering) {
            "Hasil: ${selectedCategory.label} (${filtered.size})"
        } else {
            "Aplikasi Terpasang (${filtered.size})"
        }

        gridAppsAdapter.update(filtered)
        tvEmptySearch.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        rvInstalledGrid.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun showAppOptionsDialog(app: AppModel) {
        val options = arrayOf(
            "▶ Buka ${app.appName}",
            "ℹ️ Info Aplikasi (${app.packageName})",
            "🗑️ Copot Pemasangan (Uninstall)"
        )
        AlertDialog.Builder(context, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(app.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> onAppClick(app)
                    1 -> openSystemAppInfo(app.packageName)
                    2 -> requestUninstallApp(app.packageName)
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

    private inner class LandscapeAppsAdapter(
        private var items: List<AppModel>
    ) : RecyclerView.Adapter<LandscapeAppsAdapter.LandscapeViewHolder>() {

        fun update(newItems: List<AppModel>) {
            this.items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LandscapeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_google_tv_landscape_app, parent, false)
            return LandscapeViewHolder(view)
        }

        override fun onBindViewHolder(holder: LandscapeViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class LandscapeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val frameCard: FrameLayout = itemView.findViewById(R.id.frameGoogleTvLandscapeCard)
            private val ivBanner: ImageView = itemView.findViewById(R.id.ivGoogleTvLandscapeBanner)
            private val focusBorder: View = itemView.findViewById(R.id.viewGoogleTvLandscapeFocusBorder)
            private val tvLabel: TextView = itemView.findViewById(R.id.tvGoogleTvLandscapeLabel)

            init {
                val radiusPx = 10f * density
                frameCard.clipToOutline = true
                frameCard.outlineProvider = object : ViewOutlineProvider() {
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

            fun bind(app: AppModel) {
                tvLabel.text = app.appName
                IconPackManager.bindGoogleTvLandscapeBanner(context, ivBanner, app)

                val updateFocusVisual: (Boolean, Boolean) -> Unit = { focused, animate ->
                    focusBorder.visibility = if (focused) View.VISIBLE else View.GONE
                    tvLabel.setTextColor(if (focused) Color.WHITE else Color.parseColor("#CBD5E1"))
                    tvLabel.typeface = if (focused) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    itemView.elevation = if (focused) 14f else 2f
                    val scale = if (focused) 1.08f else 1.0f
                    if (animate) {
                        itemView.animate().scaleX(scale).scaleY(scale).setDuration(110L).setInterpolator(decelerate).start()
                    } else {
                        itemView.scaleX = scale
                        itemView.scaleY = scale
                    }
                }
                updateFocusVisual(itemView.isFocused, false)

                itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    updateFocusVisual(hasFocus, true)
                    if (hasFocus) {
                        scrollIntoView(v)
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

    private inner class CircleAppsAdapter(
        private var items: List<AppModel>
    ) : RecyclerView.Adapter<CircleAppsAdapter.CircleViewHolder>() {

        fun update(newItems: List<AppModel>) {
            this.items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircleViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_google_tv_circle_app, parent, false)
            return CircleViewHolder(view)
        }

        override fun onBindViewHolder(holder: CircleViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class CircleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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

            fun bind(app: AppModel) {
                tvLabel.text = app.appName
                IconPackManager.bindGoogleTvCircleIcon(context, ivIcon, app)

                val updateFocus: (Boolean, Boolean) -> Unit = { focused, animate ->
                    focusRing.visibility = if (focused) View.VISIBLE else View.GONE
                    tvLabel.setTextColor(if (focused) Color.WHITE else Color.parseColor("#CBD5E1"))
                    tvLabel.typeface = if (focused) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
                    itemView.elevation = if (focused) 14f else 2f
                    val scale = if (focused) 1.12f else 1.0f
                    if (animate) {
                        itemView.animate().scaleX(scale).scaleY(scale).setDuration(110L).setInterpolator(decelerate).start()
                    } else {
                        itemView.scaleX = scale
                        itemView.scaleY = scale
                    }
                }
                updateFocus(itemView.isFocused, false)

                itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                    updateFocus(hasFocus, true)
                    if (hasFocus) {
                        scrollIntoView(v)
                    }
                }
                itemView.setOnClickListener { onAppClick(app) }
                itemView.setOnKeyListener { _, keyCode, event ->
                    if (event.action == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    ) {
                        onAppClick(app)
                        return@setOnKeyListener true
                    }
                    false
                }
            }
        }
    }

    private fun scrollIntoView(child: View) {
        val rect = Rect(0, 0, child.width, child.height)
        child.requestRectangleOnScreen(rect, false)
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()
}
