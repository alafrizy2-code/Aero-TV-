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
 * AtvCardSettingsDialog:
 * Pengaturan Kartu khusus ATV Launcher Theme yang nyaman dioperasikan dengan Remote TV (D-Pad):
 * - Jumlah Kolom (4 - 8)
 * - Ukuran Kartu (Kecil / Sedang / Besar / Ekstra Besar)
 * - Jarak Antarkartu (4 - 20 dp)
 * - Bentuk Sudut Kartu (0 / 4 / 10 / 16 dp)
 * - Warna Latar Kartu & Transparansi (35% - 100%)
 * - Efek Fokus (Outline Putih / Oranye ATV / Cyan / Lift)
 * - Ukuran Ikon (34 / 46 / 54 / 62 dp)
 */
class AtvCardSettingsDialog(
    private val context: Context,
    private val onSettingsChanged: (ThemeManager.AtvCardSettings) -> Unit
) : Dialog(context) {

    private val density = context.resources.displayMetrics.density
    private var currentSettings: ThemeManager.AtvCardSettings = ThemeManager.getAtvCardSettings(context)
    private val rowsContainer = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }

    private val columnOptions = listOf(4, 5, 6, 7, 8)
    private val sizeOptions = listOf(
        0.86f to "Kecil (Compact)",
        1.00f to "Sedang (Default)",
        1.12f to "Besar",
        1.24f to "Ekstra Besar"
    )
    private val spacingOptions = listOf(4, 8, 10, 14, 20)
    private val radiusOptions = listOf(
        0 to "Tajam (0 dp)",
        4 to "Minimalis ATV (4 dp)",
        10 to "Bulat Halus (10 dp)",
        16 to "Rounded (16 dp)"
    )
    private val colorModes = ThemeManager.AtvBgColorMode.values().toList()
    private val alphaOptions = listOf(35, 55, 75, 92, 100)
    private val focusEffects = ThemeManager.AtvFocusEffect.values().toList()
    private val iconSizeOptions = listOf(
        34 to "Kecil (34 dp)",
        46 to "Normal (46 dp)",
        54 to "Besar (54 dp)",
        62 to "Ekstra Besar (62 dp)"
    )

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

        // Header
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
            text = "Pengaturan Kartu • ATV Launcher"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        })
        titleCol.addView(TextView(context).apply {
            text = "Gunakan tombol ◀ / ▶ atau OK pada remote TV untuk mengubah langsung tanpa restart"
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

        // Footer bar: Reset Default
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
        footer.addView(createActionBtn("↺ Reset Pengaturan Kartu ATV", Color.parseColor("#475569")) {
            currentSettings = ThemeManager.resetAtvCardSettings(context)
            onSettingsChanged(currentSettings)
            rebuildRows(focusIndex = 0)
            Toast.makeText(context, "Pengaturan kartu ATV dikembalikan ke bawaan!", Toast.LENGTH_SHORT).show()
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

    private fun applyUpdatedSettings(updated: ThemeManager.AtvCardSettings, rowIdx: Int) {
        currentSettings = updated
        ThemeManager.saveAtvCardSettings(context, updated)
        onSettingsChanged(updated)
        rebuildRows(focusIndex = rowIdx)
    }

    private fun rebuildRows(focusIndex: Int = -1) {
        rowsContainer.removeAllViews()

        val sizeLabel = sizeOptions.minByOrNull { kotlin.math.abs(it.first - currentSettings.cardSizeScale) }?.second
            ?: "Sedang (Default)"
        val radiusLabel = radiusOptions.minByOrNull { kotlin.math.abs(it.first - currentSettings.cornerRadiusDp) }?.second
            ?: "${currentSettings.cornerRadiusDp} dp"
        val iconLabel = iconSizeOptions.minByOrNull { kotlin.math.abs(it.first - currentSettings.iconSizeDp) }?.second
            ?: "${currentSettings.iconSizeDp} dp"

        addOptionRow(
            index = 0,
            title = "Jumlah Kolom Grid",
            subtitle = "Menentukan jumlah kartu per baris di layar 1920×1080",
            valueText = "${currentSettings.columns} Kolom"
        ) { dir ->
            val idx = columnOptions.indexOf(currentSettings.columns).let { if (it < 0) 2 else it }
            val next = columnOptions[(idx + dir + columnOptions.size) % columnOptions.size]
            applyUpdatedSettings(currentSettings.copy(columns = next), 0)
        }

        addOptionRow(
            index = 1,
            title = "Ukuran Kartu",
            subtitle = "Skala tinggi dan proporsi kartu aplikasi landscape",
            valueText = sizeLabel
        ) { dir ->
            val idx = sizeOptions.indexOfFirst { kotlin.math.abs(it.first - currentSettings.cardSizeScale) < 0.05f }
                .let { if (it < 0) 1 else it }
            val next = sizeOptions[(idx + dir + sizeOptions.size) % sizeOptions.size].first
            applyUpdatedSettings(currentSettings.copy(cardSizeScale = next), 1)
        }

        addOptionRow(
            index = 2,
            title = "Jarak Antarkartu",
            subtitle = "Spasi horizontal dan vertikal antarkartu di grid",
            valueText = "${currentSettings.spacingDp} dp"
        ) { dir ->
            val idx = spacingOptions.indexOfFirst { kotlin.math.abs(it - currentSettings.spacingDp) <= 2 }
                .let { if (it < 0) 2 else it }
            val next = spacingOptions[(idx + dir + spacingOptions.size) % spacingOptions.size]
            applyUpdatedSettings(currentSettings.copy(spacingDp = next), 2)
        }

        addOptionRow(
            index = 3,
            title = "Bentuk Sudut Kartu",
            subtitle = "Sudut tajam klasik ATV hingga membulat modern",
            valueText = radiusLabel
        ) { dir ->
            val idx = radiusOptions.indexOfFirst { it.first == currentSettings.cornerRadiusDp }
                .let { if (it < 0) 1 else it }
            val next = radiusOptions[(idx + dir + radiusOptions.size) % radiusOptions.size].first
            applyUpdatedSettings(currentSettings.copy(cornerRadiusDp = next), 3)
        }

        addOptionRow(
            index = 4,
            title = "Warna Latar Kartu",
            subtitle = "Pilih warna otomatis dari ikon aplikasi, palet ATV, atau monokrom",
            valueText = currentSettings.bgColorMode.label
        ) { dir ->
            val idx = colorModes.indexOf(currentSettings.bgColorMode).coerceAtLeast(0)
            val next = colorModes[(idx + dir + colorModes.size) % colorModes.size]
            applyUpdatedSettings(currentSettings.copy(bgColorMode = next), 4)
        }

        addOptionRow(
            index = 5,
            title = "Transparansi Latar Kartu",
            subtitle = "Atur tingkat kepekatan latar kartu agar wallpaper tetap terlihat",
            valueText = "${currentSettings.bgAlphaPercent}%"
        ) { dir ->
            val idx = alphaOptions.indexOfFirst { kotlin.math.abs(it - currentSettings.bgAlphaPercent) <= 8 }
                .let { if (it < 0) 3 else it }
            val next = alphaOptions[(idx + dir + alphaOptions.size) % alphaOptions.size]
            applyUpdatedSettings(currentSettings.copy(bgAlphaPercent = next), 5)
        }

        addOptionRow(
            index = 6,
            title = "Efek Fokus Remote TV",
            subtitle = "Gaya sorotan kartu saat dipilih dengan tombol arah D-Pad",
            valueText = currentSettings.focusEffect.label
        ) { dir ->
            val idx = focusEffects.indexOf(currentSettings.focusEffect).coerceAtLeast(0)
            val next = focusEffects[(idx + dir + focusEffects.size) % focusEffects.size]
            applyUpdatedSettings(currentSettings.copy(focusEffect = next), 6)
        }

        addOptionRow(
            index = 7,
            title = "Ukuran Ikon Aplikasi",
            subtitle = "Ukuran ikon di sebelah kiri dalam setiap kartu landscape",
            valueText = iconLabel
        ) { dir ->
            val idx = iconSizeOptions.indexOfFirst { kotlin.math.abs(it.first - currentSettings.iconSizeDp) <= 4 }
                .let { if (it < 0) 1 else it }
            val next = iconSizeOptions[(idx + dir + iconSizeOptions.size) % iconSizeOptions.size].first
            applyUpdatedSettings(currentSettings.copy(iconSizeDp = next), 7)
        }

        if (focusIndex >= 0) {
            rowsContainer.post {
                rowsContainer.getChildAt(focusIndex)?.requestFocus()
            }
        }
    }

    private fun addOptionRow(
        index: Int,
        title: String,
        subtitle: String,
        valueText: String,
        onStep: (delta: Int) -> Unit
    ) {
        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            isFocusable = true
            isClickable = true
            setPadding(dp(16), dp(11), dp(16), dp(11))
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
                setColor(Color.parseColor("#1D4ED8"))
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
