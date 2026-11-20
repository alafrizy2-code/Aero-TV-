package com.example.theme

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.AppModel
import kotlin.math.roundToInt

/**
 * ThemeAndIconSettingsDialog:
 * Halaman Pengaturan Terpadu: Pengaturan Launcher -> Tampilan Layar -> Tema & Ikon.
 * Menyediakan daftar 5 tema + Default Theme, pratinjau langsung, toggle Use Home Screen Default (ON/OFF),
 * tombol Download Theme (katalog ikon online + cache lokal offline), Apply Theme tanpa restart, dan Restore Default.
 */
class ThemeAndIconSettingsDialog(
    private val context: Context,
    private val installedApps: List<AppModel>,
    private val onThemeApplied: (LauncherThemeDefinition) -> Unit,
    private val onOpenAppDrawer: () -> Unit,
    private val onClosed: () -> Unit
) : Dialog(context) {

    private data class ThemeCardUiHolder(
        val theme: LauncherThemeDefinition,
        val cardView: LinearLayout,
        val badgeView: TextView,
        val applyBackground: (Boolean) -> Unit
    )

    private val density = context.resources.displayMetrics.density
    private val themes = ThemeManager.getAllThemes()
    private var selectedTheme: LauncherThemeDefinition = ThemeManager.getActiveTheme(context)
    private var isDownloading = false
    private val themeCardHolders = mutableListOf<ThemeCardUiHolder>()

    private val listContainer = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        clipChildren = false
        clipToPadding = false
        setPadding(dp(2), dp(2), dp(2), dp(2))
    }
    private lateinit var previewView: ThemePreviewView
    private lateinit var tvSelectedTitle: TextView
    private lateinit var tvSelectedSubtitle: TextView
    private lateinit var tvSelectedDesc: TextView
    private lateinit var tvSpecs: TextView
    private lateinit var tvOfflineStatus: TextView
    private lateinit var pbDownload: ProgressBar
    private lateinit var tvDownloadProgress: TextView
    private lateinit var btnDownloadTheme: Button
    private lateinit var btnApplyTheme: Button
    private lateinit var btnRestoreDefault: Button
    private lateinit var btnPreviewDrawer: Button
    private lateinit var cardGoogleTvHomeSetting: LinearLayout
    private lateinit var tvGoogleTvHomeDesc: TextView
    private lateinit var btnToggleGoogleTvHomeDefault: Button
    private lateinit var btnAtvCardSettings: Button
    private lateinit var btnWolfDisplaySettings: Button

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F20B1120"))
            setPadding(dp(32), dp(18), dp(32), dp(18))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Top Bar
        val topBar = RelativeLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
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
        titleCol.addView(TextView(context).apply {
            text = "Tampilan Layar • Tema & Ikon"
            setTextColor(Color.WHITE)
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
        })
        titleCol.addView(TextView(context).apply {
            text = "Klik tema untuk langsung menerapkan, lalu atur Use Home Screen Default (ON = tata letak tema, OFF = Aero TV)"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
        })
        topBar.addView(titleCol)

        val btnClose = createActionButton("✕ Tutup", Color.parseColor("#334155")) {
            dismiss()
        }.apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                dp(36)
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        }
        topBar.addView(btnClose)
        root.addView(topBar)

        // Body 2 Kolom: Kiri = Daftar Tema, Kanan = Preview & Aksi (ScrollView + Footer Tombol Tersemat)
        val body = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val leftScroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.41f).apply {
                marginEnd = dp(16)
            }
            isFocusable = false
            isVerticalScrollBarEnabled = false
            addView(listContainer)
        }
        body.addView(leftScroll)

        // Panel Kanan: Preview & Detail (Scrollable agar tidak pernah terpotong di TV 1080p/720p) + Footer Tombol Aksi Permanen
        val rightPanel = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * density
                setColor(Color.parseColor("#CC111827"))
                setStroke(dp(1), Color.parseColor("#33FFFFFF"))
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.59f)
        }

        val detailsScroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFocusable = false
            isVerticalScrollBarEnabled = false
        }
        val detailsContent = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        previewView = ThemePreviewView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(124)
            ).apply {
                bottomMargin = dp(8)
            }
        }
        detailsContent.addView(previewView)

        tvSelectedTitle = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
        }
        tvSelectedSubtitle = TextView(context).apply {
            setTextColor(Color.parseColor("#38BDF8"))
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(2), 0, dp(4))
        }
        tvSelectedDesc = TextView(context).apply {
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 11f
            setPadding(0, 0, 0, dp(6))
        }
        tvSpecs = TextView(context).apply {
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 10.5f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(Color.parseColor("#660F172A"))
            }
        }
        tvOfflineStatus = TextView(context).apply {
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(6), 0, dp(2))
        }
        pbDownload = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(6)
            ).apply {
                bottomMargin = dp(4)
            }
        }
        tvDownloadProgress = TextView(context).apply {
            setTextColor(Color.parseColor("#7DD3FC"))
            textSize = 11f
            visibility = View.GONE
        }

        // Pengaturan Khusus Home Screen Default per Tema (ON / OFF)
        cardGoogleTvHomeSetting = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(Color.parseColor("#CC1E293B"))
                setStroke(dp(1), Color.parseColor("#4438BDF8"))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
            }
            visibility = View.GONE
        }

        val gtvSettingTextCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(10)
            }
        }
        gtvSettingTextCol.addView(TextView(context).apply {
            text = "Use Home Screen Default"
            setTextColor(Color.WHITE)
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
        })
        tvGoogleTvHomeDesc = TextView(context).apply {
            setTextColor(Color.parseColor("#CBD5E1"))
            textSize = 10.5f
        }
        gtvSettingTextCol.addView(tvGoogleTvHomeDesc)
        cardGoogleTvHomeSetting.addView(gtvSettingTextCol)

        btnToggleGoogleTvHomeDefault = createActionButton("ON", Color.parseColor("#16A34A")) {
            toggleHomeScreenDefaultForSelectedTheme()
        }.apply {
            id = View.generateViewId()
            tag = "btn_toggle_home_default"
            layoutParams = LinearLayout.LayoutParams(dp(84), dp(36))
        }
        cardGoogleTvHomeSetting.addView(btnToggleGoogleTvHomeDefault)

        btnAtvCardSettings = createActionButton(
            "⚙ Pengaturan Kartu ATV (Kolom, Ukuran, Jarak, Warna, Fokus)",
            Color.parseColor("#1D4ED8")
        ) {
            AtvCardSettingsDialog(context) {
                val applied = ThemeManager.setActiveTheme(context, ThemeId.ATV_LAUNCHER)
                selectedTheme = applied
                onThemeApplied(applied)
                refreshThemeListBadgesAndBorders()
                updateDetailsPanel()
            }.show()
        }.apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
            ).apply {
                topMargin = dp(6)
            }
            visibility = View.GONE
        }

        btnWolfDisplaySettings = createActionButton(
            "⚙ Pengaturan Tampilan Wolf (Widget, Dock, Ikon, Fokus)",
            Color.parseColor("#0284C7")
        ) {
            WolfDisplaySettingsDialog(context) {
                val applied = ThemeManager.setActiveTheme(context, ThemeId.WOLF_LAUNCHER)
                selectedTheme = applied
                onThemeApplied(applied)
                refreshThemeListBadgesAndBorders()
                updateDetailsPanel()
            }.show()
        }.apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(36)
            ).apply {
                topMargin = dp(6)
            }
            visibility = View.GONE
        }

        detailsContent.addView(tvSelectedTitle)
        detailsContent.addView(tvSelectedSubtitle)
        detailsContent.addView(tvSelectedDesc)
        detailsContent.addView(tvSpecs)
        detailsContent.addView(cardGoogleTvHomeSetting)
        detailsContent.addView(btnAtvCardSettings)
        detailsContent.addView(btnWolfDisplaySettings)
        detailsContent.addView(tvOfflineStatus)
        detailsContent.addView(pbDownload)
        detailsContent.addView(tvDownloadProgress)
        detailsScroll.addView(detailsContent)
        rightPanel.addView(detailsScroll)

        // Baris Tombol Aksi (Selalu terlihat di bawah panel kanan tanpa pernah terpotong)
        val actionFooter = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }

        val actionRow1 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(6)
            }
        }
        btnDownloadTheme = createActionButton("⬇ Download Theme", Color.parseColor("#0284C7")) {
            startThemeDownload()
        }.apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f).apply { marginEnd = dp(8) }
        }
        btnApplyTheme = createActionButton("✓ Apply Theme", Color.parseColor("#16A34A")) {
            applySelectedThemeAndClose()
        }.apply {
            id = View.generateViewId()
            tag = "btn_apply_theme"
            layoutParams = LinearLayout.LayoutParams(0, dp(38), 1f)
        }
        actionRow1.addView(btnDownloadTheme)
        actionRow1.addView(btnApplyTheme)
        actionFooter.addView(actionRow1)

        val actionRow2 = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        btnRestoreDefault = createActionButton("↺ Restore Default", Color.parseColor("#475569")) {
            val defaultTheme = ThemeManager.restoreDefaultTheme(context)
            selectedTheme = defaultTheme
            onThemeApplied(defaultTheme)
            refreshThemeListBadgesAndBorders()
            updateDetailsPanel()
            Toast.makeText(context, "Default Theme (Aero TV) dipulihkan!", Toast.LENGTH_SHORT).show()
        }.apply {
            id = View.generateViewId()
            tag = "btn_restore_default"
            layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f).apply { marginEnd = dp(8) }
        }
        btnPreviewDrawer = createActionButton("📱 Buka App Drawer", Color.parseColor("#6D28D9")) {
            val applied = ThemeManager.setActiveTheme(context, selectedTheme.id)
            onThemeApplied(applied)
            dismiss()
            onOpenAppDrawer()
        }.apply {
            id = View.generateViewId()
            layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f)
        }
        actionRow2.addView(btnRestoreDefault)
        actionRow2.addView(btnPreviewDrawer)
        actionFooter.addView(actionRow2)
        rightPanel.addView(actionFooter)

        body.addView(rightPanel)
        root.addView(body)
        setContentView(root)

        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.CENTER)
        }

        buildThemeListOnce()
        wireFocusNavigation()
        updateDetailsPanel()

        setOnShowListener {
            val activeIdx = themes.indexOfFirst { it.id == selectedTheme.id }.coerceAtLeast(0)
            listContainer.post {
                listContainer.getChildAt(activeIdx)?.requestFocus()
            }
        }

        setOnDismissListener { onClosed() }

        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismiss()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun buildThemeListOnce() {
        listContainer.removeAllViews()
        themeCardHolders.clear()

        for (theme in themes) {
            lateinit var applyBgFn: (Boolean) -> Unit

            val itemCard = LinearLayout(context).apply {
                id = View.generateViewId()
                tag = "theme_card_${theme.id.name}"
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(10), dp(12), dp(10))
                isFocusable = true
                isClickable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(7)
                }
            }

            val topRow = RelativeLayout(context)
            val tvName = TextView(context).apply {
                text = theme.name
                setTextColor(Color.WHITE)
                textSize = 13.5f
                typeface = Typeface.DEFAULT_BOLD
            }
            val tvBadge = TextView(context).apply {
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                layoutParams = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                    addRule(RelativeLayout.CENTER_VERTICAL)
                }
            }
            topRow.addView(tvName)
            topRow.addView(tvBadge)

            val tvSub = TextView(context).apply {
                text = theme.subtitle
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 10.5f
                setPadding(0, dp(2), 0, 0)
            }

            itemCard.addView(topRow)
            itemCard.addView(tvSub)

            applyBgFn = { focused ->
                val isActive = (ThemeManager.getActiveThemeId(context) == theme.id)
                val isSelected = (selectedTheme.id == theme.id)
                itemCard.background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10f * density
                    setColor(
                        when {
                            focused -> Color.parseColor("#CC1E3A5F")
                            isSelected -> Color.parseColor("#AA1E293B")
                            else -> Color.parseColor("#880F172A")
                        }
                    )
                    val strokeColor = when {
                        focused -> Color.parseColor("#38BDF8")
                        isActive -> Color.parseColor("#22C55E")
                        isSelected -> Color.parseColor("#64748B")
                        else -> Color.parseColor("#29FFFFFF")
                    }
                    setStroke(dp(if (focused) 2 else 1), strokeColor)
                }
            }

            itemCard.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                applyBgFn(hasFocus)
                v.animate()
                    .scaleX(if (hasFocus) 1.02f else 1.0f)
                    .scaleY(if (hasFocus) 1.02f else 1.0f)
                    .setDuration(90L)
                    .start()
                if (hasFocus && selectedTheme.id != theme.id) {
                    selectedTheme = theme
                    refreshThemeListBadgesAndBorders()
                    updateDetailsPanel()
                    wireLeftCardRightTargets(itemCard, theme)
                }
            }

            itemCard.setOnClickListener {
                // Memilih kartu tema langsung mengaktifkan & menerapkan tema + layout Home Screen-nya tanpa restart
                val applied = ThemeManager.setActiveTheme(context, theme.id)
                selectedTheme = applied
                onThemeApplied(applied)
                refreshThemeListBadgesAndBorders()
                updateDetailsPanel()
                wireLeftCardRightTargets(itemCard, applied)
                Toast.makeText(context, "${applied.name} diterapkan!", Toast.LENGTH_SHORT).show()
            }

            listContainer.addView(itemCard)
            themeCardHolders.add(ThemeCardUiHolder(theme, itemCard, tvBadge, applyBgFn))
        }

        refreshThemeListBadgesAndBorders()
    }

    private fun refreshThemeListBadgesAndBorders() {
        val activeId = ThemeManager.getActiveThemeId(context)
        for (holder in themeCardHolders) {
            val isActive = (holder.theme.id == activeId)
            val isDownloaded = IconPackManager.isThemeDownloaded(context, holder.theme.id)
            val iconCount = IconPackManager.getDownloadedIconCount(context, holder.theme.id)
            holder.badgeView.text = when {
                isActive -> "AKTIF"
                holder.theme.id == ThemeId.DEFAULT -> "BAWAAN"
                isDownloaded && iconCount > 0 -> "OFFLINE ($iconCount)"
                else -> "ONLINE"
            }
            holder.badgeView.setTextColor(
                when {
                    isActive -> Color.parseColor("#22C55E")
                    isDownloaded -> Color.parseColor("#38BDF8")
                    else -> Color.parseColor("#94A3B8")
                }
            )
            holder.applyBackground(holder.cardView.isFocused)
        }
    }

    private fun wireLeftCardRightTargets(card: View, theme: LauncherThemeDefinition) {
        val rightTargetId = when {
            theme.id != ThemeId.DEFAULT -> btnToggleGoogleTvHomeDefault.id
            else -> btnApplyTheme.id
        }
        card.nextFocusRightId = rightTargetId
        val selectedCardId = card.id
        btnToggleGoogleTvHomeDefault.nextFocusLeftId = selectedCardId
        btnAtvCardSettings.nextFocusLeftId = selectedCardId
        btnWolfDisplaySettings.nextFocusLeftId = selectedCardId
        btnDownloadTheme.nextFocusLeftId = selectedCardId
        btnRestoreDefault.nextFocusLeftId = selectedCardId
    }

    private fun wireFocusNavigation() {
        btnToggleGoogleTvHomeDefault.nextFocusDownId = btnApplyTheme.id
        btnAtvCardSettings.nextFocusUpId = btnToggleGoogleTvHomeDefault.id
        btnAtvCardSettings.nextFocusDownId = btnApplyTheme.id
        btnWolfDisplaySettings.nextFocusUpId = btnToggleGoogleTvHomeDefault.id
        btnWolfDisplaySettings.nextFocusDownId = btnApplyTheme.id

        btnDownloadTheme.nextFocusRightId = btnApplyTheme.id
        btnDownloadTheme.nextFocusDownId = btnRestoreDefault.id
        btnApplyTheme.nextFocusLeftId = btnDownloadTheme.id
        btnApplyTheme.nextFocusDownId = btnPreviewDrawer.id
        btnRestoreDefault.nextFocusRightId = btnPreviewDrawer.id
        btnRestoreDefault.nextFocusUpId = btnDownloadTheme.id
        btnPreviewDrawer.nextFocusLeftId = btnRestoreDefault.id
        btnPreviewDrawer.nextFocusUpId = btnApplyTheme.id
    }

    private fun toggleHomeScreenDefaultForSelectedTheme() {
        val current = when (selectedTheme.id) {
            ThemeId.NEXFY -> ThemeManager.isNexfyUseHomeScreenDefault(context)
            ThemeId.PROJECTIVY -> ThemeManager.isProjectivyUseHomeScreenDefault(context)
            ThemeId.ATV_LAUNCHER -> ThemeManager.isAtvUseHomeScreenDefault(context)
            ThemeId.WOLF_LAUNCHER -> ThemeManager.isWolfUseHomeScreenDefault(context)
            else -> ThemeManager.isGoogleTvUseHomeScreenDefault(context)
        }
        val next = !current
        when (selectedTheme.id) {
            ThemeId.NEXFY -> ThemeManager.setNexfyUseHomeScreenDefault(context, next)
            ThemeId.PROJECTIVY -> ThemeManager.setProjectivyUseHomeScreenDefault(context, next)
            ThemeId.ATV_LAUNCHER -> ThemeManager.setAtvUseHomeScreenDefault(context, next)
            ThemeId.WOLF_LAUNCHER -> ThemeManager.setWolfUseHomeScreenDefault(context, next)
            else -> ThemeManager.setGoogleTvUseHomeScreenDefault(context, next)
        }
        // Pastikan tema terpilih langsung menjadi tema aktif agar ON/OFF langsung mengubah Home Screen!
        val applied = ThemeManager.setActiveTheme(context, selectedTheme.id)
        selectedTheme = applied
        refreshThemeListBadgesAndBorders()
        updateDetailsPanel()
        onThemeApplied(applied)

        val statusStr = if (next) {
            "ON (Tata Letak ${applied.name})"
        } else {
            "OFF (Kembali ke Tata Letak Default Aero TV)"
        }
        Toast.makeText(context, "${applied.name} • Use Home Screen Default: $statusStr", Toast.LENGTH_SHORT).show()
    }

    private fun updateDetailsPanel() {
        val t = selectedTheme
        val isActive = (ThemeManager.getActiveThemeId(context) == t.id)
        val isDownloaded = IconPackManager.isThemeDownloaded(context, t.id)
        val iconCount = IconPackManager.getDownloadedIconCount(context, t.id)

        previewView.setTheme(t)
        tvSelectedTitle.text = t.name
        tvSelectedSubtitle.text = t.subtitle
        tvSelectedDesc.text = t.description

        val shapeLabel = when (t.cardStyle.shapeType) {
            CardShapeType.FULL_CIRCLE_TILE -> "Bulat Penuh"
            CardShapeType.BANNER_16_9_FULL_BLEED -> "Banner 16:9 Penuh"
            CardShapeType.ATV_BANNER_GRID_TILE -> "Banner Grid Klasik"
            CardShapeType.WOLF_BANNER_TILE -> "Banner Bevel Penuh"
            CardShapeType.SQUIRCLE_GLASS_TILE -> "Squircle Kaca"
            CardShapeType.GLASS_PILL_RECT -> "Kartu Kaca Horizontal"
        }
        val layoutDesc = if (t.layout.spanCount > 1) {
            "Grid ${t.layout.spanCount} Baris Horizontal"
        } else {
            "1 Baris Horizontal"
        }
        val drawerColsDisplay = if (t.id == ThemeId.ATV_LAUNCHER) {
            ThemeManager.getAtvCardSettings(context).columns
        } else {
            t.layout.drawerColumns
        }
        tvSpecs.text = "Bentuk: $shapeLabel (${t.layout.cardWidthPx1080.toInt()}×${t.layout.cardHeightPx1080.toInt()} px)  •  Layout: $layoutDesc\n" +
            "App Drawer: Grid $drawerColsDisplay Kolom  •  Zoom Fokus: ${String.format("%.2fx", t.focusConfig.scaleFocused)}"

        tvOfflineStatus.text = when {
            t.id == ThemeId.DEFAULT -> "● Tema Bawaan Tersimpan Lokal (Siap Offline)"
            isDownloaded -> "✓ Paket Ikon Terunduh ($iconCount ikon tersimpan di cache lokal untuk offline)"
            else -> "○ Paket Ikon Belum Diunduh (Gunakan Download Theme untuk cache ikon online)"
        }
        tvOfflineStatus.setTextColor(
            if (t.id == ThemeId.DEFAULT || isDownloaded) Color.parseColor("#4ADE80") else Color.parseColor("#FBBF24")
        )

        btnDownloadTheme.isEnabled = (t.id != ThemeId.DEFAULT && !isDownloading)
        btnDownloadTheme.alpha = if (t.id == ThemeId.DEFAULT) 0.45f else 1.0f
        btnApplyTheme.text = if (isActive) "✓ Terapkan & Tutup" else "✓ Apply Theme"

        if (t.id == ThemeId.GOOGLE_TV ||
            t.id == ThemeId.NEXFY ||
            t.id == ThemeId.PROJECTIVY ||
            t.id == ThemeId.ATV_LAUNCHER ||
            t.id == ThemeId.WOLF_LAUNCHER
        ) {
            cardGoogleTvHomeSetting.visibility = View.VISIBLE
            updateHomeDefaultSettingUi()
        } else {
            cardGoogleTvHomeSetting.visibility = View.GONE
        }
        btnAtvCardSettings.visibility = if (t.id == ThemeId.ATV_LAUNCHER) View.VISIBLE else View.GONE
        btnWolfDisplaySettings.visibility = if (t.id == ThemeId.WOLF_LAUNCHER) View.VISIBLE else View.GONE

        // Update D-Pad vertical chain sesuai tombol yang aktif
        btnToggleGoogleTvHomeDefault.nextFocusDownId = when {
            t.id == ThemeId.ATV_LAUNCHER -> btnAtvCardSettings.id
            t.id == ThemeId.WOLF_LAUNCHER -> btnWolfDisplaySettings.id
            else -> btnApplyTheme.id
        }
        btnApplyTheme.nextFocusUpId = when {
            t.id == ThemeId.ATV_LAUNCHER -> btnAtvCardSettings.id
            t.id == ThemeId.WOLF_LAUNCHER -> btnWolfDisplaySettings.id
            t.id != ThemeId.DEFAULT -> btnToggleGoogleTvHomeDefault.id
            else -> View.NO_ID
        }
        btnDownloadTheme.nextFocusUpId = btnApplyTheme.nextFocusUpId
        themeCardHolders.firstOrNull { it.theme.id == t.id }?.cardView?.let { card ->
            wireLeftCardRightTargets(card, t)
        }
    }

    private fun updateHomeDefaultSettingUi() {
        val isOn = when (selectedTheme.id) {
            ThemeId.NEXFY -> ThemeManager.isNexfyUseHomeScreenDefault(context)
            ThemeId.PROJECTIVY -> ThemeManager.isProjectivyUseHomeScreenDefault(context)
            ThemeId.ATV_LAUNCHER -> ThemeManager.isAtvUseHomeScreenDefault(context)
            ThemeId.WOLF_LAUNCHER -> ThemeManager.isWolfUseHomeScreenDefault(context)
            else -> ThemeManager.isGoogleTvUseHomeScreenDefault(context)
        }
        tvGoogleTvHomeDesc.text = when (selectedTheme.id) {
            ThemeId.NEXFY -> if (isOn) {
                "ON: Home Screen berubah ke tata letak Nexfy (status atas, 8 kategori, wallpaper luas, kartu landscape)."
            } else {
                "OFF: Kembali ke tata letak dasar Aero TV dengan gaya kartu, warna, ikon, dan fokus Nexfy."
            }
            ThemeId.PROJECTIVY -> if (isOn) {
                "ON: Home Screen memakai tata letak Projectivy (Apps, sumber konten, Watch Next nyata, & jam kanan atas)."
            } else {
                "OFF: Kembali ke tata letak dasar Aero TV dengan gaya visual kartu, jarak, dan fokus Projectivy."
            }
            ThemeId.ATV_LAUNCHER -> if (isOn) {
                "ON: Home Screen memakai tata letak ATV (wallpaper penuh atas, grid 6 kolom kartu landscape berwarna)."
            } else {
                "OFF: Kembali ke tata letak dasar Aero TV dengan gaya visual kartu landscape berwarna, ikon, & fokus ATV."
            }
            ThemeId.WOLF_LAUNCHER -> if (isOn) {
                "ON: Home Screen memakai tata letak Wolf (cuaca & penyimpanan kiri atas, jam kanan atas, dock transparan + All Apps)."
            } else {
                "OFF: Kembali ke tata letak dasar Aero TV dengan gaya visual kartu, ikon, jarak, & fokus Wolf Launcher."
            }
            else -> if (isOn) {
                "ON: Home Screen berubah ke tata letak penuh Google TV (navigasi atas, banner, rekomendasi, ikon bulat)."
            } else {
                "OFF: Kembali ke tata letak dasar Aero TV dengan fokus wallpaper + kartu bulat Google TV."
            }
        }
        btnToggleGoogleTvHomeDefault.text = if (isOn) "ON" else "OFF"
        val bgCol = if (isOn) Color.parseColor("#16A34A") else Color.parseColor("#475569")
        val normalBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f * density
            setColor(bgCol)
        }
        val focusedBg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 10f * density
            setColor(bgCol)
            setStroke(dp(3), Color.WHITE)
        }
        btnToggleGoogleTvHomeDefault.background = if (btnToggleGoogleTvHomeDefault.isFocused) focusedBg else normalBg
        btnToggleGoogleTvHomeDefault.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            v.background = if (hasFocus) focusedBg else normalBg
            v.animate().scaleX(if (hasFocus) 1.05f else 1.0f).scaleY(if (hasFocus) 1.05f else 1.0f).setDuration(90L).start()
        }
    }

    private fun startThemeDownload() {
        if (selectedTheme.id == ThemeId.DEFAULT || isDownloading) return
        isDownloading = true
        btnDownloadTheme.isEnabled = false
        pbDownload.visibility = View.VISIBLE
        pbDownload.progress = 5
        tvDownloadProgress.visibility = View.VISIBLE
        tvDownloadProgress.text = "Menghubungkan ke katalog ikon online..."

        val targetTheme = selectedTheme
        IconPackManager.downloadThemePack(
            context = context,
            theme = targetTheme,
            installedApps = installedApps,
            onProgress = { pct, status ->
                pbDownload.progress = pct
                tvDownloadProgress.text = status
            },
            onComplete = { downloaded, total, success ->
                isDownloading = false
                btnDownloadTheme.isEnabled = true
                pbDownload.visibility = View.GONE
                tvDownloadProgress.visibility = View.GONE
                refreshThemeListBadgesAndBorders()
                updateDetailsPanel()
                val msg = if (success) {
                    "Unduhan selesai: $downloaded/$total ikon disimpan untuk penggunaan offline!"
                } else {
                    "Gagal mengunduh dari katalog online (periksa internet). Tema tetap memakai ikon seragam lokal."
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun applySelectedThemeAndClose() {
        val applied = ThemeManager.setActiveTheme(context, selectedTheme.id)
        onThemeApplied(applied)
        refreshThemeListBadgesAndBorders()
        updateDetailsPanel()
        Toast.makeText(context, "${applied.name} diterapkan tanpa restart!", Toast.LENGTH_SHORT).show()
        dismiss()
    }

    private fun createActionButton(label: String, bgColor: Int, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 11.5f
            isAllCaps = false
            isFocusable = true
            isClickable = true
            typeface = Typeface.DEFAULT_BOLD
            val normalBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(bgColor)
            }
            val focusedBg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(bgColor)
                setStroke(dp(3), Color.WHITE)
            }
            background = normalBg
            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                background = if (hasFocus) focusedBg else normalBg
                v.animate().scaleX(if (hasFocus) 1.04f else 1.0f).scaleY(if (hasFocus) 1.04f else 1.0f).setDuration(90L).start()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun dp(value: Int): Int = (value * density).roundToInt()
}
