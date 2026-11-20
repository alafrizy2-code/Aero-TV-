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
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.roundToInt

/**
 * WolfDisplaySettingsDialog:
 * Pengaturan Tampilan khusus Wolf Launcher Theme yang nyaman dioperasikan dengan Remote TV (D-Pad):
 * - Tampilkan / Sembunyikan Widget Cuaca
 * - Tampilkan / Sembunyikan Informasi Penyimpanan
 * - Tampilkan / Sembunyikan Jam dan Tanggal
 * - Ukuran Widget (Kecil / Sedang / Besar / Ekstra Besar)
 * - Transparansi Dock (20% - 95%)
 * - Ukuran Ikon Aplikasi (44 - 76 dp)
 * - Jarak Antarkartu / Ikon (6 - 26 dp)
 * - Efek Fokus Remote TV
 * - Jumlah Aplikasi Terlihat di Dock (5 / 7 / 9 / 11 / Semua)
 * - Tampilan Nama Aplikasi di Dock (Saat Fokus / Selalu / Sembunyikan)
 */
class WolfDisplaySettingsDialog(
    private val context: Context,
    private val onSettingsChanged: (ThemeManager.WolfDisplaySettings) -> Unit
) : Dialog(context) {

    private val density = context.resources.displayMetrics.density
    private var currentSettings: ThemeManager.WolfDisplaySettings = ThemeManager.getWolfDisplaySettings(context)
    private val rowsContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val widgetScaleOptions = listOf(
        0.85f to "Kecil (0.85x)",
        1.00f to "Sedang (Default)",
        1.15f to "Besar (1.15x)",
        1.30f to "Ekstra Besar (1.30x)"
    )
    private val dockAlphaOptions = listOf(20, 40, 62, 80, 95)
    private val iconSizeOptions = listOf(
        44 to "Kecil (44 dp)",
        52 to "Ringkas (52 dp)",
        58 to "Normal (58 dp)",
        68 to "Besar (68 dp)",
        76 to "Ekstra Besar (76 dp)"
    )
    private val spacingOptions = listOf(6, 10, 14, 20, 26)
    private val focusEffects = ThemeManager.WolfFocusEffect.values().toList()
    private val dockCountOptions = listOf(5, 7, 9, 11, 0)
    private val labelModes = ThemeManager.WolfDockLabelMode.values().toList()

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(true)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F00A101D"))
            setPadding(dp(36), dp(22), dp(36), dp(22))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val topBar = RelativeLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
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
            text = "Pengaturan Tampilan • Wolf Launcher"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        })
        titleCol.addView(TextView(context).apply {
            text = "Gunakan tombol ◀ / ▶ atau OK pada remote TV untuk mengubah tampilan langsung tanpa restart"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 12f
        })
        topBar.addView(titleCol)

        val btnClose = createActionBtn("✓ Selesai", Color.parseColor("#16A34A")) {
            dismiss()
        }.apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                dp(38)
            ).apply {
                addRule(RelativeLayout.ALIGN_PARENT_END)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
        }
        topBar.addView(btnClose)
        root.addView(topBar)

        val scroll = ScrollView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            isFocusable = false
            addView(rowsContainer)
        }
        root.addView(scroll)

        val footer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        }
        footer.addView(createActionBtn("↺ Reset Pengaturan Tampilan Wolf", Color.parseColor("#475569")) {
            currentSettings = ThemeManager.resetWolfDisplaySettings(context)
            onSettingsChanged(currentSettings)
            rebuildRows(focusIndex = 0)
            Toast.makeText(context, "Pengaturan tampilan Wolf dikembalikan ke bawaan!", Toast.LENGTH_SHORT).show()
        }.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                dp(40)
            )
        })
        root.addView(footer)

        setContentView(root)
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.CENTER)
        }

        rebuildRows(focusIndex = 0)

        setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dismiss()
                return@setOnKeyListener true
            }
            false
        }
    }

    private fun applyUpdated(updated: ThemeManager.WolfDisplaySettings, rowIdx: Int) {
        currentSettings = updated
        ThemeManager.saveWolfDisplaySettings(context, updated)
        onSettingsChanged(updated)
        rebuildRows(focusIndex = rowIdx)
    }

    private fun rebuildRows(focusIndex: Int = -1) {
        rowsContainer.removeAllViews()

        val scaleLabel = widgetScaleOptions.minByOrNull { kotlin.math.abs(it.first - currentSettings.widgetSizeScale) }?.second
            ?: "Sedang (Default)"
        val iconLabel = iconSizeOptions.minByOrNull { kotlin.math.abs(it.first - currentSettings.iconSizeDp) }?.second
            ?: "${currentSettings.iconSizeDp} dp"
        val dockCountLabel = if (currentSettings.visibleDockAppsCount <= 0) {
            "Semua Aplikasi"
        } else {
            "${currentSettings.visibleDockAppsCount} Aplikasi"
        }

        addOptionRow(
            title = "Widget Cuaca (Kiri Atas)",
            subtitle = "Tampilkan atau sembunyikan widget cuaca & animasi cuaca aktual Aero TV",
            valueText = if (currentSettings.showWeatherWidget) "Tampil (ON)" else "Tersembunyi (OFF)",
            isOn = currentSettings.showWeatherWidget
        ) { _ ->
            applyUpdated(currentSettings.copy(showWeatherWidget = !currentSettings.showWeatherWidget), 0)
        }

        addOptionRow(
            title = "Informasi Penyimpanan (Di Bawah Cuaca)",
            subtitle = "Tampilkan kapasitas penyimpanan internal & memori RAM perangkat secara aktual",
            valueText = if (currentSettings.showStorageInfo) "Tampil (ON)" else "Tersembunyi (OFF)",
            isOn = currentSettings.showStorageInfo
        ) { _ ->
            applyUpdated(currentSettings.copy(showStorageInfo = !currentSettings.showStorageInfo), 1)
        }

        addOptionRow(
            title = "Jam Digital & Tanggal (Kanan Atas)",
            subtitle = "Tampilkan atau sembunyikan jam digital besar beserta hari & tanggal",
            valueText = if (currentSettings.showClockAndDate) "Tampil (ON)" else "Tersembunyi (OFF)",
            isOn = currentSettings.showClockAndDate
        ) { _ ->
            applyUpdated(currentSettings.copy(showClockAndDate = !currentSettings.showClockAndDate), 2)
        }

        addOptionRow(
            title = "Ukuran Widget Cuaca & Penyimpanan",
            subtitle = "Atur skala besar kecilnya widget informasi di atas layar",
            valueText = scaleLabel
        ) { dir ->
            val idx = widgetScaleOptions.indexOfFirst { kotlin.math.abs(it.first - currentSettings.widgetSizeScale) < 0.06f }
                .let { if (it < 0) 1 else it }
            val next = widgetScaleOptions[(idx + dir + widgetScaleOptions.size) % widgetScaleOptions.size].first
            applyUpdated(currentSettings.copy(widgetSizeScale = next), 3)
        }

        addOptionRow(
            title = "Transparansi Dock Bawah",
            subtitle = "Atur tingkat kepekatan latar dock agar wallpaper pribadi tetap terlihat jelas",
            valueText = "${currentSettings.dockAlphaPercent}%"
        ) { dir ->
            val idx = dockAlphaOptions.indexOfFirst { kotlin.math.abs(it - currentSettings.dockAlphaPercent) <= 10 }
                .let { if (it < 0) 2 else it }
            val next = dockAlphaOptions[(idx + dir + dockAlphaOptions.size) % dockAlphaOptions.size]
            applyUpdated(currentSettings.copy(dockAlphaPercent = next), 4)
        }

        addOptionRow(
            title = "Ukuran Ikon Aplikasi",
            subtitle = "Berlaku di Dock Wolf Launcher maupun tata letak kartu saat mode OFF",
            valueText = iconLabel
        ) { dir ->
            val idx = iconSizeOptions.indexOfFirst { kotlin.math.abs(it.first - currentSettings.iconSizeDp) <= 4 }
                .let { if (it < 0) 2 else it }
            val next = iconSizeOptions[(idx + dir + iconSizeOptions.size) % iconSizeOptions.size].first
            applyUpdated(currentSettings.copy(iconSizeDp = next), 5)
        }

        addOptionRow(
            title = "Jarak Antarkartu / Ikon",
            subtitle = "Spasi horizontal antar ikon di dock dan antar kartu tema Wolf",
            valueText = "${currentSettings.cardSpacingDp} dp"
        ) { dir ->
            val idx = spacingOptions.indexOfFirst { kotlin.math.abs(it - currentSettings.cardSpacingDp) <= 3 }
                .let { if (it < 0) 2 else it }
            val next = spacingOptions[(idx + dir + spacingOptions.size) % spacingOptions.size]
            applyUpdated(currentSettings.copy(cardSpacingDp = next), 6)
        }

        addOptionRow(
            title = "Efek Fokus Remote TV",
            subtitle = "Gaya sorotan ikon atau kartu saat dipilih dengan tombol D-Pad",
            valueText = currentSettings.focusEffect.label
        ) { dir ->
            val idx = focusEffects.indexOf(currentSettings.focusEffect).coerceAtLeast(0)
            val next = focusEffects[(idx + dir + focusEffects.size) % focusEffects.size]
            applyUpdated(currentSettings.copy(focusEffect = next), 7)
        }

        addOptionRow(
            title = "Jumlah Aplikasi Terlihat di Dock",
            subtitle = "Tentukan berapa aplikasi tampil di dock sebelum tombol Semua Aplikasi",
            valueText = dockCountLabel
        ) { dir ->
            val idx = dockCountOptions.indexOf(currentSettings.visibleDockAppsCount).let { if (it < 0) 2 else it }
            val next = dockCountOptions[(idx + dir + dockCountOptions.size) % dockCountOptions.size]
            applyUpdated(currentSettings.copy(visibleDockAppsCount = next), 8)
        }

        addOptionRow(
            title = "Nama Aplikasi di Dock",
            subtitle = "Tampilkan nama di bawah ikon saat fokus, selalu tampil, atau sembunyikan",
            valueText = currentSettings.dockLabelMode.label
        ) { dir ->
            val idx = labelModes.indexOf(currentSettings.dockLabelMode).coerceAtLeast(0)
            val next = labelModes[(idx + dir + labelModes.size) % labelModes.size]
            applyUpdated(currentSettings.copy(dockLabelMode = next), 9)
        }

        if (focusIndex >= 0) {
            rowsContainer.post {
                rowsContainer.getChildAt(focusIndex)?.requestFocus()
            }
        }
    }

    private fun addOptionRow(
        title: String,
        subtitle: String,
        valueText: String,
        isOn: Boolean? = null,
        onStep: (delta: Int) -> Unit
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isClickable = true
            setPadding(dp(16), dp(10), dp(16), dp(10))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }

            val applyBg: (Boolean) -> Unit = { focused ->
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 10f * density
                    setColor(if (focused) Color.parseColor("#E61E3A5F") else Color.parseColor("#990F172A"))
                    setStroke(
                        dp(if (focused) 2 else 1),
                        if (focused) Color.WHITE else Color.parseColor("#29FFFFFF")
                    )
                }
            }
            applyBg(false)
            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                applyBg(hasFocus)
                v.animate().scaleX(if (hasFocus) 1.015f else 1.0f).scaleY(if (hasFocus) 1.015f else 1.0f).setDuration(85L).start()
            }
            setOnClickListener { onStep(+1) }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                            onStep(+1)
                            return@setOnKeyListener true
                        }
                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            onStep(-1)
                            return@setOnKeyListener true
                        }
                    }
                }
                false
            }
        }

        val textCol = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(14)
            }
        }
        textCol.addView(TextView(context).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
        })
        textCol.addView(TextView(context).apply {
            text = subtitle
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 11f
        })
        row.addView(textCol)

        val pillBgColor = when (isOn) {
            true -> Color.parseColor("#16A34A")
            false -> Color.parseColor("#475569")
            null -> Color.parseColor("#0284C7")
        }
        val valuePill = TextView(context).apply {
            text = "◀  $valueText  ▶"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(7), dp(14), dp(7))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(pillBgColor)
            }
        }
        row.addView(valuePill)
        rowsContainer.addView(row)
    }

    private fun createActionBtn(label: String, bgColor: Int, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 12f
            isAllCaps = false
            isFocusable = true
            isClickable = true
            typeface = Typeface.DEFAULT_BOLD
            val normal = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(bgColor)
            }
            val focused = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10f * density
                setColor(bgColor)
                setStroke(dp(3), Color.WHITE)
            }
            background = normal
            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                v.background = if (hasFocus) focused else normal
                v.animate().scaleX(if (hasFocus) 1.04f else 1.0f).scaleY(if (hasFocus) 1.04f else 1.0f).setDuration(85L).start()
            }
            setOnClickListener { onClick() }
        }
    }

    private fun dp(v: Int): Int = (v * density).roundToInt()
}
