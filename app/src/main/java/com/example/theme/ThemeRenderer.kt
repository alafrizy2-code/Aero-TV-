package com.example.theme

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.AppModel
import com.example.R
import kotlin.math.roundToInt

/**
 * ThemeRenderer:
 * Merender desain kartu aplikasi yang sesungguhnya untuk setiap gaya launcher:
 * - DEFAULT (Aero TV): Kartu kaca horizontal 224x96, ikon kiri + teks kanan.
 * - GOOGLE_TV: Kartu bulat penuh (OVAL) tanpa kotak latar belakang + label di bawah lingkaran.
 * - NEXFY: Kartu squircle kaca frosted + ikon tengah bersih + label bawah dalam squircle.
 * - PROJECTIVY: Banner persegi panjang 16:9 penuh tanpa kotak latar ikon default + judul saat fokus.
 * - ATV_LAUNCHER: Ubin banner persegi panjang 2 baris penuh warna brand.
 * - WOLF_LAUNCHER: Banner 2 baris + kilau atas + bevel fokus putih-cyan.
 */
class ThemeRenderer(
    context: Context,
    private val theme: LauncherThemeDefinition
) {
    val scale: Float = (context.resources.displayMetrics.widthPixels / 1920f).coerceAtLeast(0.5f)
    private val density: Float = context.resources.displayMetrics.density
    private val atvSettings: ThemeManager.AtvCardSettings? =
        if (theme.id == ThemeId.ATV_LAUNCHER) ThemeManager.getAtvCardSettings(context) else null
    private val wolfSettings: ThemeManager.WolfDisplaySettings? =
        if (theme.id == ThemeId.WOLF_LAUNCHER) ThemeManager.getWolfDisplaySettings(context) else null

    val cardWidthPx: Int = when {
        atvSettings != null -> (theme.layout.cardWidthPx1080 * scale * atvSettings.cardSizeScale).roundToInt().coerceAtLeast(80)
        wolfSettings != null -> {
            val factor = (wolfSettings.iconSizeDp / 58f).coerceIn(0.82f, 1.25f)
            (theme.layout.cardWidthPx1080 * scale * factor).roundToInt().coerceAtLeast(120)
        }
        else -> (theme.layout.cardWidthPx1080 * scale).roundToInt().coerceAtLeast(80)
    }
    val cardHeightPx: Int = when {
        atvSettings != null -> (theme.layout.cardHeightPx1080 * scale * atvSettings.cardSizeScale).roundToInt().coerceAtLeast(48)
        wolfSettings != null -> {
            val factor = (wolfSettings.iconSizeDp / 58f).coerceIn(0.82f, 1.25f)
            (theme.layout.cardHeightPx1080 * scale * factor).roundToInt().coerceAtLeast(64)
        }
        else -> (theme.layout.cardHeightPx1080 * scale).roundToInt().coerceAtLeast(48)
    }
    val gapHorizontalPx: Int = when {
        atvSettings != null -> (atvSettings.spacingDp * density).roundToInt().coerceAtLeast(4)
        wolfSettings != null -> (wolfSettings.cardSpacingDp * density).roundToInt().coerceAtLeast(4)
        else -> (theme.layout.gapHorizontalPx1080 * scale).roundToInt().coerceAtLeast(6)
    }
    val gapVerticalPx: Int = when {
        atvSettings != null -> (atvSettings.spacingDp * density).roundToInt().coerceAtLeast(4)
        wolfSettings != null -> (wolfSettings.cardSpacingDp * density).roundToInt().coerceAtLeast(4)
        else -> (theme.layout.gapVerticalPx1080 * scale).roundToInt().coerceAtLeast(6)
    }
    val edgePaddingHorizontalPx: Int = (theme.layout.edgePaddingHorizontalPx1080 * scale).roundToInt().coerceAtLeast(24)
    val edgePaddingVerticalPx: Int = (theme.layout.edgePaddingVerticalPx1080 * scale).roundToInt().coerceAtLeast(8)

    private val iconSizePx: Int = when {
        atvSettings != null -> (atvSettings.iconSizeDp * density).roundToInt().coerceAtLeast(24)
        wolfSettings != null -> (wolfSettings.iconSizeDp * density).roundToInt().coerceAtLeast(28)
        else -> (theme.iconConfig.iconSizePx1080 * scale).roundToInt().coerceAtLeast(24)
    }
    private val cornerRadiusPx: Float = if (atvSettings != null) {
        (atvSettings.cornerRadiusDp * density).coerceAtLeast(0f)
    } else {
        (theme.cardStyle.cornerRadiusPx1080 * scale).coerceAtLeast(2f)
    }
    private val labelSizePx: Float = (theme.cardStyle.labelSizePx1080 * scale).coerceAtLeast(11f)
    private val belowLabelExtraHeightPx: Int = if (
        theme.cardStyle.labelPlacement == CardLabelPlacement.BELOW_CARD_ALWAYS ||
        theme.cardStyle.labelPlacement == CardLabelPlacement.BELOW_CARD_FOCUS_ONLY
    ) {
        (32f * scale).roundToInt()
    } else {
        0
    }

    fun createViewHolder(parent: ViewGroup): ThemedAppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_themed_app_card, parent, false)
        view.layoutParams = RecyclerView.LayoutParams(
            cardWidthPx,
            cardHeightPx + belowLabelExtraHeightPx
        )
        return ThemedAppViewHolder(view)
    }

    fun bindViewHolder(
        holder: ThemedAppViewHolder,
        app: AppModel,
        onAppClick: (AppModel) -> Unit
    ) {
        holder.bind(app, onAppClick)
    }

    inner class ThemedAppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardBox: FrameLayout = itemView.findViewById(R.id.cardBoxContainer)
        private val ivFullCardBanner: ImageView = itemView.findViewById(R.id.ivFullCardBanner)
        private val layoutInner: LinearLayout = itemView.findViewById(R.id.layoutBottomCardContent)
        private val layoutIconFrame: FrameLayout = itemView.findViewById(R.id.layoutIconFrame)
        private val ivAppIcon: ImageView = itemView.findViewById(R.id.ivBottomAppIcon)
        private val tvInsideLabel: TextView = itemView.findViewById(R.id.tvBottomAppName)
        private val viewTopSheen: View = itemView.findViewById(R.id.viewTopSheen)
        private val viewFocusBorder: View = itemView.findViewById(R.id.viewBottomFocusBorder)
        private val tvBelowLabel: TextView = itemView.findViewById(R.id.tvBelowCardName)

        private var normalBg: GradientDrawable? = null
        private var focusedBg: GradientDrawable? = null
        private val isFullBleedVisual: Boolean = (
            theme.cardStyle.shapeType == CardShapeType.FULL_CIRCLE_TILE ||
            theme.cardStyle.shapeType == CardShapeType.SQUIRCLE_GLASS_TILE ||
            theme.cardStyle.shapeType == CardShapeType.BANNER_16_9_FULL_BLEED ||
            theme.cardStyle.shapeType == CardShapeType.WOLF_BANNER_TILE
        )

        init {
            val cardStyle = theme.cardStyle

            // 1. Ukuran & Outline Clipping kartu sesuai bentuk nyata masing-masing launcher
            cardBox.layoutParams = (cardBox.layoutParams as LinearLayout.LayoutParams).apply {
                width = cardWidthPx
                height = cardHeightPx
            }
            cardBox.clipToOutline = true
            cardBox.outlineProvider = if (cardStyle.shapeType == CardShapeType.FULL_CIRCLE_TILE) {
                object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setOval(0, 0, view.width, view.height)
                    }
                }
            } else {
                object : ViewOutlineProvider() {
                    override fun getOutline(view: View, outline: Outline) {
                        outline.setRoundRect(0, 0, view.width, view.height, cornerRadiusPx)
                    }
                }
            }

            // 2. Pilih Mode Konten Kartu: Full-Bleed Banner/Circle vs Kartu Konten Klasik (Default & Nexfy)
            if (isFullBleedVisual) {
                ivFullCardBanner.visibility = View.VISIBLE
                layoutInner.visibility = View.GONE
                cardBox.background = null
                normalBg = null
                focusedBg = null
            } else {
                ivFullCardBanner.visibility = View.GONE
                layoutInner.visibility = View.VISIBLE
                layoutIconFrame.background = null

                ivAppIcon.layoutParams = (ivAppIcon.layoutParams as FrameLayout.LayoutParams).apply {
                    width = iconSizePx
                    height = iconSizePx
                    gravity = Gravity.CENTER
                }
                layoutIconFrame.layoutParams = (layoutIconFrame.layoutParams as LinearLayout.LayoutParams).apply {
                    width = iconSizePx
                    height = iconSizePx
                }

                val normalStrokePx = (cardStyle.normalBorderWidthPx1080 * scale).roundToInt().coerceAtLeast(1)
                normalBg = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(cardStyle.normalBgStartColor, cardStyle.normalBgEndColor)
                ).apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = cornerRadiusPx
                    setStroke(normalStrokePx, cardStyle.normalBorderColor)
                }

                focusedBg = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(cardStyle.focusedBgStartColor, cardStyle.focusedBgEndColor)
                ).apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = cornerRadiusPx
                }
                cardBox.background = normalBg
            }

            configureCardLayoutAndLabels(cardStyle)

            // 3. Border / Ring Fokus sesuai bentuk kartu tema
            val focusStrokePx = (cardStyle.focusedBorderWidthPx1080 * scale).roundToInt().coerceAtLeast(2)
            viewFocusBorder.background = buildFocusBorderDrawable(cardStyle, focusStrokePx)

            // 4. Kilau atas opsional (Nexfy & Wolf Launcher)
            if (cardStyle.hasTopSheen && cardStyle.shapeType != CardShapeType.FULL_CIRCLE_TILE) {
                viewTopSheen.visibility = View.VISIBLE
                viewTopSheen.background = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.parseColor("#2AFFFFFF"), Color.TRANSPARENT)
                ).apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadii = floatArrayOf(
                        cornerRadiusPx, cornerRadiusPx,
                        cornerRadiusPx, cornerRadiusPx,
                        0f, 0f, 0f, 0f
                    )
                }
            } else {
                viewTopSheen.visibility = View.GONE
            }
        }

        private fun configureCardLayoutAndLabels(cardStyle: ThemeCardStyleConfig) {
            val padH = (16f * scale).roundToInt().coerceAtLeast(8)
            val padEnd = (14f * scale).roundToInt().coerceAtLeast(8)
            val padV = (10f * scale).roundToInt().coerceAtLeast(4)

            when (cardStyle.labelPlacement) {
                CardLabelPlacement.INSIDE_RIGHT -> {
                    layoutInner.orientation = LinearLayout.HORIZONTAL
                    layoutInner.gravity = Gravity.CENTER_VERTICAL
                    layoutInner.setPadding(padH, padV, padEnd, padV)

                    tvInsideLabel.visibility = View.VISIBLE
                    tvInsideLabel.gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    tvInsideLabel.maxLines = 2
                    tvInsideLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelSizePx)
                    (tvInsideLabel.layoutParams as LinearLayout.LayoutParams).apply {
                        width = 0
                        height = LinearLayout.LayoutParams.WRAP_CONTENT
                        weight = 1f
                        marginStart = (12f * scale).roundToInt().coerceAtLeast(6)
                        topMargin = 0
                    }

                    tvBelowLabel.visibility = View.GONE
                }

                CardLabelPlacement.INSIDE_BOTTOM_STACK -> {
                    layoutInner.orientation = LinearLayout.VERTICAL
                    layoutInner.gravity = Gravity.CENTER
                    layoutInner.setPadding(padV, padV, padV, padV)

                    tvInsideLabel.visibility = View.VISIBLE
                    tvInsideLabel.gravity = Gravity.CENTER
                    tvInsideLabel.maxLines = 1
                    tvInsideLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelSizePx)
                    (tvInsideLabel.layoutParams as LinearLayout.LayoutParams).apply {
                        width = LinearLayout.LayoutParams.MATCH_PARENT
                        height = LinearLayout.LayoutParams.WRAP_CONTENT
                        weight = 0f
                        marginStart = 0
                        topMargin = (10f * scale).roundToInt().coerceAtLeast(4)
                    }

                    tvBelowLabel.visibility = View.GONE
                }

                CardLabelPlacement.BANNER_INTEGRATED -> {
                    tvBelowLabel.visibility = View.GONE
                }

                CardLabelPlacement.BELOW_CARD_ALWAYS,
                CardLabelPlacement.BELOW_CARD_FOCUS_ONLY -> {
                    tvBelowLabel.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelSizePx)
                    tvBelowLabel.visibility = if (cardStyle.labelPlacement == CardLabelPlacement.BELOW_CARD_ALWAYS) {
                        View.VISIBLE
                    } else {
                        View.INVISIBLE
                    }
                }
            }
        }

        private fun buildFocusBorderDrawable(cardStyle: ThemeCardStyleConfig, strokePx: Int): Drawable {
            if (cardStyle.shapeType == CardShapeType.FULL_CIRCLE_TILE) {
                return GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    setStroke(strokePx, cardStyle.focusedBorderColor)
                }
            }

            if (cardStyle.shapeType == CardShapeType.ATV_BANNER_GRID_TILE && atvSettings != null) {
                val borderColor = when (atvSettings.focusEffect) {
                    ThemeManager.AtvFocusEffect.WHITE_OUTLINE_ZOOM -> Color.WHITE
                    ThemeManager.AtvFocusEffect.ORANGE_BORDER_ZOOM -> Color.parseColor("#FF9800")
                    ThemeManager.AtvFocusEffect.CYAN_GLOW_ZOOM -> Color.parseColor("#22D3EE")
                    ThemeManager.AtvFocusEffect.BRIGHT_LIFT_ONLY -> Color.TRANSPARENT
                }
                return GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = cornerRadiusPx
                    setColor(Color.TRANSPARENT)
                    if (borderColor != Color.TRANSPARENT) {
                        setStroke(strokePx, borderColor)
                    }
                }
            }

            if (cardStyle.shapeType == CardShapeType.WOLF_BANNER_TILE && wolfSettings != null) {
                return when (wolfSettings.focusEffect) {
                    ThemeManager.WolfFocusEffect.WHITE_CYAN_GLOW -> {
                        val outer = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = cornerRadiusPx
                            setColor(Color.TRANSPARENT)
                            setStroke(strokePx, cardStyle.focusedBorderColor)
                        }
                        val inner = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = (cornerRadiusPx - 2f).coerceAtLeast(2f)
                            setColor(Color.TRANSPARENT)
                            setStroke((strokePx / 2).coerceAtLeast(1), cardStyle.focusedBorderAccentColor)
                        }
                        LayerDrawable(arrayOf(outer, inner)).apply {
                            val inset = (strokePx * 0.7f).roundToInt().coerceAtLeast(2)
                            setLayerInset(1, inset, inset, inset, inset)
                        }
                    }
                    ThemeManager.WolfFocusEffect.WHITE_RING_ZOOM,
                    ThemeManager.WolfFocusEffect.BOTTOM_ACCENT_BAR -> GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = cornerRadiusPx
                        setColor(Color.TRANSPARENT)
                        setStroke(strokePx, Color.WHITE)
                    }
                    ThemeManager.WolfFocusEffect.SOFT_LIFT_ONLY -> GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = cornerRadiusPx
                        setColor(Color.TRANSPARENT)
                    }
                }
            }

            return when (theme.focusConfig.effectStyle) {
                FocusEffectStyle.NEXFY_NEON_DUAL_BORDER,
                FocusEffectStyle.WOLF_WHITE_CYAN_BEVEL -> {
                    val outer = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = cornerRadiusPx
                        setColor(Color.TRANSPARENT)
                        setStroke(strokePx, cardStyle.focusedBorderColor)
                    }
                    val inner = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = (cornerRadiusPx - 2f).coerceAtLeast(2f)
                        setColor(Color.TRANSPARENT)
                        setStroke((strokePx / 2).coerceAtLeast(1), cardStyle.focusedBorderAccentColor)
                    }
                    val layers = LayerDrawable(arrayOf(outer, inner))
                    val inset = (strokePx * 0.7f).roundToInt().coerceAtLeast(2)
                    layers.setLayerInset(1, inset, inset, inset, inset)
                    layers
                }
                else -> {
                    GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = cornerRadiusPx
                        setColor(Color.TRANSPARENT)
                        setStroke(strokePx, cardStyle.focusedBorderColor)
                    }
                }
            }
        }

        fun bind(app: AppModel, onClick: (AppModel) -> Unit) {
            tvInsideLabel.text = app.appName
            tvBelowLabel.text = app.appName

            if (theme.id == ThemeId.ATV_LAUNCHER && atvSettings != null) {
                val pair = IconPackManager.getAtvTileColors(app, atvSettings, bindingAdapterPosition.coerceAtLeast(0))
                val normalStrokePx = (theme.cardStyle.normalBorderWidthPx1080 * scale).roundToInt().coerceAtLeast(1)
                normalBg = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(pair.first, pair.second)
                ).apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = cornerRadiusPx
                    setStroke(normalStrokePx, Color.parseColor("#29FFFFFF"))
                }
                focusedBg = GradientDrawable(
                    GradientDrawable.Orientation.TL_BR,
                    intArrayOf(pair.first, pair.second)
                ).apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = cornerRadiusPx
                }
                IconPackManager.bindAtvLeftIcon(itemView.context, ivAppIcon, app)
            } else {
                val targetImageView = if (isFullBleedVisual) ivFullCardBanner else ivAppIcon
                IconPackManager.bindCardVisual(itemView.context, targetImageView, app, theme)
            }

            ThemeFocusManager.applyFocusState(
                rootView = itemView,
                cardBox = cardBox,
                focusBorderView = viewFocusBorder,
                tvInsideLabel = tvInsideLabel,
                tvBelowLabel = tvBelowLabel,
                normalBg = normalBg,
                focusedBg = focusedBg,
                theme = theme,
                hasFocus = itemView.isFocused,
                animate = false
            )

            itemView.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                ThemeFocusManager.applyFocusState(
                    rootView = v,
                    cardBox = cardBox,
                    focusBorderView = viewFocusBorder,
                    tvInsideLabel = tvInsideLabel,
                    tvBelowLabel = tvBelowLabel,
                    normalBg = normalBg,
                    focusedBg = focusedBg,
                    theme = theme,
                    hasFocus = hasFocus,
                    animate = true
                )
            }

            itemView.setOnClickListener { onClick(app) }

            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN &&
                    (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    onClick(app)
                    return@setOnKeyListener true
                }
                false
            }
        }
    }
}
