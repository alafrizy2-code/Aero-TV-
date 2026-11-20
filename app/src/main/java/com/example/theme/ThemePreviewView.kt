package com.example.theme

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View

/**
 * ThemePreviewView:
 * Menggambar pratinjau visual langsung bentuk kartu sesungguhnya dari setiap tema:
 * - DEFAULT: Kartu kaca horizontal Aero TV (ikon kiri, nama kanan).
 * - GOOGLE_TV: Kartu bulat penuh tanpa kotak latar belakang + judul di bawah lingkaran.
 * - NEXFY: Ubin squircle frosted glass + ikon tengah + judul di bawah dalam kartu.
 * - PROJECTIVY: Banner persegi panjang 16:9 full-bleed tanpa latar belakang ikon default.
 * - ATV_LAUNCHER: Grid 2 baris banner persegi panjang penuh warna.
 * - WOLF_LAUNCHER: Grid 2 baris banner penuh + bevel fokus putih-cyan.
 */
class ThemePreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var theme: LauncherThemeDefinition = ThemeRegistry.allThemes.first()

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val focusStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val focusAccentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val iconGlyphPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val textLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.LEFT
        typeface = Typeface.DEFAULT_BOLD
    }
    private val mockHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val headerRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val previewRect = RectF()
    private val cardRect = RectF()
    private val insetRect = RectF()

    fun setTheme(definition: LauncherThemeDefinition) {
        this.theme = definition
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val density = resources.displayMetrics.density

        // Latar mockup layar TV mini (wallpaper & jam tetap utuh)
        bgPaint.shader = LinearGradient(
            0f, 0f, w, h,
            intArrayOf(
                Color.parseColor("#0B1120"),
                theme.previewPrimaryColor,
                Color.parseColor("#0F172A")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        previewRect.set(0f, 0f, w, h)
        canvas.drawRoundRect(previewRect, 14f * density, 14f * density, bgPaint)
        bgPaint.shader = null

        mockHeaderPaint.color = Color.parseColor("#88FFFFFF")
        mockHeaderPaint.textSize = 10f * density
        canvas.drawText("19:45  •  Cuaca & Jam Tetap Utuh", 16f * density, 22f * density, mockHeaderPaint)

        headerRingPaint.strokeWidth = 1.5f * density
        headerRingPaint.color = Color.parseColor("#55FFFFFF")
        canvas.drawCircle(w * 0.5f, 18f * density, 9f * density, headerRingPaint)

        val shape = theme.cardStyle.shapeType
        val isTwoRow = theme.layout.layoutType == ThemeLayoutType.HORIZONTAL_TWO_ROW_GRID
        val rows = if (isTwoRow) 2 else 1
        val cols = when (shape) {
            CardShapeType.FULL_CIRCLE_TILE -> 5
            CardShapeType.SQUIRCLE_GLASS_TILE -> 4
            CardShapeType.BANNER_16_9_FULL_BLEED -> 3
            CardShapeType.ATV_BANNER_GRID_TILE -> 4
            CardShapeType.WOLF_BANNER_TILE -> 3
            CardShapeType.GLASS_PILL_RECT -> 3
        }

        val padH = 16f * density
        val topArea = if (isTwoRow) h * 0.28f else h * 0.38f
        val availW = w - (padH * 2f)
        val availH = h - topArea - (16f * density)

        val gapH = (theme.layout.gapHorizontalPx1080 / 16f) * 8f * density
        val gapV = 8f * density

        val labelExtra = if (
            theme.cardStyle.labelPlacement == CardLabelPlacement.BELOW_CARD_ALWAYS ||
            theme.cardStyle.labelPlacement == CardLabelPlacement.BELOW_CARD_FOCUS_ONLY
        ) 14f * density else 0f

        val rawCellW = (availW - gapH * (cols - 1)) / cols
        val rawCellH = ((availH - gapV * (rows - 1)) / rows) - labelExtra
        val aspect = theme.layout.cardHeightPx1080 / theme.layout.cardWidthPx1080

        val cardW: Float
        val cardH: Float
        if (shape == CardShapeType.FULL_CIRCLE_TILE) {
            val d = minOf(rawCellW, rawCellH).coerceAtLeast(26f * density)
            cardW = d
            cardH = d
        } else {
            val candidateH = rawCellW * aspect
            if (candidateH <= rawCellH) {
                cardW = rawCellW
                cardH = candidateH
            } else {
                cardH = rawCellH.coerceAtLeast(24f * density)
                cardW = (cardH / aspect).coerceAtMost(rawCellW)
            }
        }

        val totalRowW = cols * cardW + (cols - 1) * gapH
        val startX = (w - totalRowW) / 2f

        val demoApps = listOf("YouTube", "Netflix", "Prime", "Spotify", "Vidio", "VLC", "Kodi", "Plex")
        val brandStarts = listOf(
            Color.parseColor("#B91C1C"),
            Color.parseColor("#991B1B"),
            Color.parseColor("#0284C7"),
            Color.parseColor("#15803D"),
            Color.parseColor("#EA580C"),
            Color.parseColor("#D97706"),
            Color.parseColor("#1D4ED8"),
            Color.parseColor("#4338CA")
        )
        val brandEnds = listOf(
            Color.parseColor("#450A0A"),
            Color.parseColor("#18181B"),
            Color.parseColor("#0C4A6E"),
            Color.parseColor("#052E16"),
            Color.parseColor("#7C2D12"),
            Color.parseColor("#78350F"),
            Color.parseColor("#1E3A8A"),
            Color.parseColor("#1E1B4B")
        )

        var idx = 0
        for (r in 0 until rows) {
            for (c in 0 until cols) {
                val isFocused = (r == 0 && c == 0)
                val cx = startX + c * (cardW + gapH)
                val cy = topArea + r * (cardH + labelExtra + gapV)

                val scale = if (isFocused) 1.07f else 1.0f
                val dw = (cardW * (scale - 1f)) / 2f
                val dh = (cardH * (scale - 1f)) / 2f
                cardRect.set(cx - dw, cy - dh, cx + cardW + dw, cy + cardH + dh)

                val appTitle = demoApps[idx % demoApps.size]
                val bStart = brandStarts[idx % brandStarts.size]
                val bEnd = brandEnds[idx % brandEnds.size]

                when (shape) {
                    CardShapeType.FULL_CIRCLE_TILE -> {
                        val centerX = cardRect.centerX()
                        val centerY = cardRect.centerY()
                        val rad = cardRect.width() / 2f

                        // Google TV: Kartu bulat penuh tanpa kotak latar belakang
                        cardPaint.shader = LinearGradient(
                            cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                            bStart, bEnd, Shader.TileMode.CLAMP
                        )
                        canvas.drawCircle(centerX, centerY, rad, cardPaint)
                        cardPaint.shader = null

                        // Emblem ikon di tengah lingkaran
                        iconGlyphPaint.color = Color.WHITE
                        canvas.drawCircle(centerX, centerY, rad * 0.34f, iconGlyphPaint)

                        // Judul selalu tampil di bawah lingkaran
                        textPaint.color = if (isFocused) Color.WHITE else Color.parseColor("#CBD5E1")
                        textPaint.textSize = 8.5f * density
                        canvas.drawText(appTitle, centerX, cardRect.bottom + 11f * density, textPaint)

                        if (isFocused) {
                            focusStrokePaint.color = theme.cardStyle.focusedBorderColor
                            focusStrokePaint.strokeWidth = 2.6f * density
                            canvas.drawCircle(centerX, centerY, rad, focusStrokePaint)
                        }
                    }

                    CardShapeType.ATV_BANNER_GRID_TILE -> {
                        val radius = (theme.cardStyle.cornerRadiusPx1080 / 16f) * 6f * density
                        cardPaint.shader = LinearGradient(
                            cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                            bStart, bEnd, Shader.TileMode.CLAMP
                        )
                        canvas.drawRoundRect(cardRect, radius, radius, cardPaint)
                        cardPaint.shader = null

                        val emblemSize = cardRect.height() * 0.24f
                        val emblemX = cardRect.left + cardRect.height() * 0.36f
                        val emblemY = cardRect.centerY()
                        iconGlyphPaint.color = Color.parseColor("#F1FFFFFF")
                        canvas.drawRoundRect(
                            RectF(emblemX - emblemSize, emblemY - emblemSize, emblemX + emblemSize, emblemY + emblemSize),
                            3f * density, 3f * density, iconGlyphPaint
                        )

                        textLeftPaint.color = Color.WHITE
                        textLeftPaint.textSize = 7.8f * density
                        canvas.drawText(appTitle, emblemX + emblemSize + 5f * density, emblemY + 3f * density, textLeftPaint)

                        drawRectBorder(canvas, cardRect, radius, isFocused, density)
                    }

                    CardShapeType.BANNER_16_9_FULL_BLEED -> {
                        val radius = (theme.cardStyle.cornerRadiusPx1080 / 16f) * 8f * density
                        // Banner persegi panjang full-bleed penuh warna brand (tanpa kotak ikon default)
                        cardPaint.shader = LinearGradient(
                            cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                            bStart, bEnd, Shader.TileMode.CLAMP
                        )
                        canvas.drawRoundRect(cardRect, radius, radius, cardPaint)
                        cardPaint.shader = null

                        val emblemSize = cardRect.height() * 0.28f
                        val emblemX = cardRect.left + cardRect.height() * 0.42f
                        val emblemY = cardRect.centerY()
                        iconGlyphPaint.color = Color.parseColor("#E6FFFFFF")
                        canvas.drawCircle(emblemX, emblemY, emblemSize, iconGlyphPaint)

                        textLeftPaint.color = Color.WHITE
                        textLeftPaint.textSize = 9.5f * density
                        canvas.drawText(appTitle, emblemX + emblemSize + 6f * density, emblemY + 3f * density, textLeftPaint)

                        if (isFocused) {
                            textPaint.color = Color.WHITE
                            textPaint.textSize = 8.5f * density
                            canvas.drawText(appTitle, cardRect.centerX(), cardRect.bottom + 11f * density, textPaint)
                        }

                        drawRectBorder(canvas, cardRect, radius, isFocused, density)
                    }

                    CardShapeType.WOLF_BANNER_TILE -> {
                        // Gambar mockup khas Wolf Launcher satu kali saja (saat idx == 0)
                        if (idx == 0) {
                            // 1. Widget cuaca kiri atas
                            cardPaint.shader = LinearGradient(
                                14f * density, 30f * density, 128f * density, 74f * density,
                                Color.parseColor("#CC1565C0"), Color.parseColor("#990D47A1"),
                                Shader.TileMode.CLAMP
                            )
                            canvas.drawRoundRect(
                                RectF(14f * density, 30f * density, 128f * density, 72f * density),
                                8f * density, 8f * density, cardPaint
                            )
                            cardPaint.shader = null
                            textLeftPaint.color = Color.WHITE
                            textLeftPaint.textSize = 10f * density
                            canvas.drawText("26°  Cerah Berawan", 22f * density, 54f * density, textLeftPaint)

                            // 2. Kartu penyimpanan di bawah cuaca
                            cardPaint.color = Color.parseColor("#B0113B68")
                            canvas.drawRoundRect(
                                RectF(14f * density, 78f * density, 128f * density, 114f * density),
                                8f * density, 8f * density, cardPaint
                            )
                            textLeftPaint.color = Color.parseColor("#BAE6FD")
                            textLeftPaint.textSize = 8f * density
                            canvas.drawText("Penyimpanan tersedia", 22f * density, 98f * density, textLeftPaint)

                            // 3. Jam besar kanan atas
                            textLeftPaint.color = Color.WHITE
                            textLeftPaint.textSize = 16f * density
                            canvas.drawText("08:44", w - 58f * density, 50f * density, textLeftPaint)

                            // 4. Dock transparan membulat di bagian bawah + tombol All Apps
                            val dockTop = h - 54f * density
                            val dockBottom = h - 14f * density
                            val dockRect = RectF(16f * density, dockTop, w - 16f * density, dockBottom)
                            cardPaint.color = Color.parseColor("#9E0F172A")
                            canvas.drawRoundRect(dockRect, 14f * density, 14f * density, cardPaint)
                            strokePaint.color = Color.parseColor("#44FFFFFF")
                            strokePaint.strokeWidth = 1f * density
                            canvas.drawRoundRect(dockRect, 14f * density, 14f * density, strokePaint)

                            val slotCount = 7
                            val slotSize = 24f * density
                            val slotGap = ((dockRect.width() - 20f * density) - slotCount * slotSize) / (slotCount - 1)
                            val slotY = dockRect.centerY() - slotSize / 2f
                            for (s in 0 until slotCount) {
                                val sx = dockRect.left + 10f * density + s * (slotSize + slotGap)
                                val sr = RectF(sx, slotY, sx + slotSize, slotY + slotSize)
                                val isAllAppsSlot = (s == slotCount - 1)
                                cardPaint.color = if (isAllAppsSlot) Color.parseColor("#44FFFFFF") else brandStarts[s % brandStarts.size]
                                canvas.drawRoundRect(sr, 6f * density, 6f * density, cardPaint)
                                if (s == 0) {
                                    drawRectBorder(canvas, sr, 6f * density, true, density)
                                }
                            }
                        }
                    }

                    CardShapeType.SQUIRCLE_GLASS_TILE -> {
                        val radius = (theme.cardStyle.cornerRadiusPx1080 / 16f) * 8f * density
                        // Pratinjau mini bar kategori Nexfy di bagian atas saat merender kartu pertama
                        if (idx == 0) {
                            cardPaint.color = Color.parseColor("#660F172A")
                            canvas.drawRoundRect(
                                RectF(14f * density, 32f * density, w - 14f * density, 46f * density),
                                4f * density, 4f * density, cardPaint
                            )
                            iconGlyphPaint.color = Color.parseColor("#F1F5F9")
                            canvas.drawRoundRect(
                                RectF(16f * density, 34f * density, 54f * density, 44f * density),
                                3f * density, 3f * density, iconGlyphPaint
                            )
                        }

                        cardPaint.shader = LinearGradient(
                            cardRect.left, cardRect.top, cardRect.right, cardRect.bottom,
                            bStart, Color.parseColor("#0F172A"), Shader.TileMode.CLAMP
                        )
                        canvas.drawRoundRect(cardRect, radius, radius, cardPaint)
                        cardPaint.shader = null

                        val emblemSize = cardRect.height() * 0.25f
                        val emblemX = cardRect.left + cardRect.height() * 0.36f
                        val emblemY = cardRect.centerY()
                        iconGlyphPaint.color = Color.parseColor("#E6FFFFFF")
                        canvas.drawRoundRect(
                            RectF(emblemX - emblemSize, emblemY - emblemSize, emblemX + emblemSize, emblemY + emblemSize),
                            4f * density, 4f * density, iconGlyphPaint
                        )

                        textLeftPaint.color = Color.WHITE
                        textLeftPaint.textSize = 7.5f * density
                        canvas.drawText(appTitle, emblemX + emblemSize + 5f * density, emblemY + 3f * density, textLeftPaint)

                        drawRectBorder(canvas, cardRect, radius, isFocused, density)
                    }

                    CardShapeType.GLASS_PILL_RECT -> {
                        val radius = 3f * density
                        val bgStart = if (isFocused) theme.cardStyle.focusedBgStartColor else theme.cardStyle.normalBgStartColor
                        val bgEnd = if (isFocused) theme.cardStyle.focusedBgEndColor else theme.cardStyle.normalBgEndColor
                        cardPaint.shader = LinearGradient(
                            cardRect.left, cardRect.top, cardRect.left, cardRect.bottom,
                            bgStart, bgEnd, Shader.TileMode.CLAMP
                        )
                        canvas.drawRoundRect(cardRect, radius, radius, cardPaint)
                        cardPaint.shader = null

                        val emblemRad = cardRect.height() * 0.22f
                        val iconCx = cardRect.left + cardRect.height() * 0.45f
                        val iconCy = cardRect.centerY()
                        iconGlyphPaint.color = bStart
                        canvas.drawRoundRect(
                            RectF(iconCx - emblemRad, iconCy - emblemRad, iconCx + emblemRad, iconCy + emblemRad),
                            emblemRad * 0.3f, emblemRad * 0.3f, iconGlyphPaint
                        )

                        textLeftPaint.color = Color.WHITE
                        textLeftPaint.textSize = 9.5f * density
                        canvas.drawText(appTitle, iconCx + emblemRad + 6f * density, iconCy + 3.5f * density, textLeftPaint)

                        drawRectBorder(canvas, cardRect, radius, isFocused, density)
                    }
                }

                idx++
            }
        }
    }

    private fun drawRectBorder(
        canvas: Canvas,
        rect: RectF,
        radius: Float,
        isFocused: Boolean,
        density: Float
    ) {
        if (isFocused) {
            focusStrokePaint.color = theme.cardStyle.focusedBorderColor
            focusStrokePaint.strokeWidth = 2.6f * density
            canvas.drawRoundRect(rect, radius, radius, focusStrokePaint)
            if (theme.cardStyle.focusedBorderAccentColor != theme.cardStyle.focusedBorderColor) {
                focusAccentPaint.color = theme.cardStyle.focusedBorderAccentColor
                focusAccentPaint.strokeWidth = 1.2f * density
                insetRect.set(rect)
                insetRect.inset(2.2f * density, 2.2f * density)
                canvas.drawRoundRect(
                    insetRect,
                    (radius - 1.5f * density).coerceAtLeast(1f),
                    (radius - 1.5f * density).coerceAtLeast(1f),
                    focusAccentPaint
                )
            }
        } else {
            strokePaint.color = theme.cardStyle.normalBorderColor
            strokePaint.strokeWidth = 1.0f * density
            canvas.drawRoundRect(rect, radius, radius, strokePaint)
        }
    }
}
