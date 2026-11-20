package com.example

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.sin

/**
 * Custom View untuk Animasi Gelombang Suara (Voice Wave) Google TV Style:
 * 5 bar vertikal dengan warna dinamis (Biru, Merah, Kuning, Hijau) berosilasi halus saat mendengarkan.
 */
class VoiceWaveView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val colors = intArrayOf(
        Color.parseColor("#4285F4"), // Google Blue
        Color.parseColor("#EA4335"), // Google Red
        Color.parseColor("#FBBC05"), // Google Yellow
        Color.parseColor("#34A853"), // Google Green
        Color.parseColor("#4285F4")
    )

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private var animTime = 0f
    private var animator: ValueAnimator? = null
    private var currentRmsBoost = 1.0f
    var isListening: Boolean = false
        set(value) {
            field = value
            if (value) start() else stop()
            invalidate()
        }

    init {
        val density = context.resources.displayMetrics.density
        barPaint.strokeWidth = 6f * density
    }

    fun startWave() {
        currentRmsBoost = 1.0f
        isListening = true
    }

    fun stopWave() {
        currentRmsBoost = 1.0f
        isListening = false
    }

    fun updateRms(rmsdB: Float) {
        // Normalize rmsdB (-2..12) smoothly to boost bar amplitude
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.4f, 1.8f)
        currentRmsBoost = normalized
    }

    private fun start() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                animTime += 0.15f
                invalidate()
            }
            start()
        }
    }

    private fun stop() {
        animator?.cancel()
        animator = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isListening) start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stop()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val count = colors.size
        val gap = w / (count + 1)
        val cy = h * 0.5f

        for (i in 0 until count) {
            val cx = gap * (i + 1)
            val wave = if (isListening) {
                (((sin(animTime + i * 0.9f) + 1f) * 0.5f) * currentRmsBoost).coerceIn(0.15f, 1.0f)
            } else {
                0.15f
            }
            val minBarHeight = h * 0.18f
            val maxBarHeight = h * 0.85f
            val barH = minBarHeight + (maxBarHeight - minBarHeight) * wave

            barPaint.color = colors[i]
            canvas.drawLine(cx, cy - barH * 0.5f, cx, cy + barH * 0.5f, barPaint)
        }
    }
}
