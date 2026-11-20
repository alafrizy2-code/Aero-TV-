package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Widget Jam Analog Top-Center:
 * - Menampilkan jarum jam, menit, dan detik secara real-time.
 * - Mendukung custom wallpaper/background dial jam yang bisa dipilih user dari penyimpanan.
 */
class AnalogClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var clockBitmap: Bitmap? = null
    private var bitmapShader: BitmapShader? = null
    private val shaderMatrix = Matrix()

    private val dialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#44000000")
    }

    private val dialBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.parseColor("#80FFFFFF")
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#B3FFFFFF")
    }

    private val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 7f
        color = Color.WHITE
    }

    private val minuteHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 5f
        color = Color.WHITE
    }

    private val secondHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.5f
        color = Color.parseColor("#EF4444")
    }

    private val pivotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#EF4444")
    }

    private val pivotCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }

    private val calendar = Calendar.getInstance()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTicking = false

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isTicking) return
            invalidate()
            mainHandler.postDelayed(this, 1000)
        }
    }

    fun setClockWallpaper(bitmap: Bitmap?) {
        this.clockBitmap = bitmap
        if (bitmap != null) {
            bitmapShader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            bmpPaint.shader = bitmapShader
        } else {
            bitmapShader = null
            bmpPaint.shader = null
        }
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startTicking()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopTicking()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) startTicking() else stopTicking()
    }

    private fun startTicking() {
        if (!isTicking) {
            isTicking = true
            mainHandler.removeCallbacks(tickRunnable)
            mainHandler.postDelayed(tickRunnable, 1000)
        }
    }

    private fun stopTicking() {
        isTicking = false
        mainHandler.removeCallbacks(tickRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val radius = min(cx, cy) - 6f
        if (radius <= 0) return

        // 1. Gambar Dial / Wallpaper Jam Analog
        if (clockBitmap != null && bitmapShader != null) {
            val bmp = clockBitmap!!
            val scale = (radius * 2f) / min(bmp.width, bmp.height).toFloat()
            shaderMatrix.reset()
            shaderMatrix.setScale(scale, scale)
            shaderMatrix.postTranslate(cx - (bmp.width * scale) / 2f, cy - (bmp.height * scale) / 2f)
            bitmapShader!!.setLocalMatrix(shaderMatrix)
            canvas.drawCircle(cx, cy, radius, bmpPaint)
            // Border tipis elegan
            canvas.drawCircle(cx, cy, radius, dialBorderPaint)
        } else {
            // Dial transparan kaca bawaan
            canvas.drawCircle(cx, cy, radius, dialPaint)
            canvas.drawCircle(cx, cy, radius, dialBorderPaint)

            // Titik penanda 12 jam
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30).toDouble())
                val startX = (cx + (radius - 12f) * sin(angle)).toFloat()
                val startY = (cy - (radius - 12f) * cos(angle)).toFloat()
                val stopX = (cx + (radius - 4f) * sin(angle)).toFloat()
                val stopY = (cy - (radius - 4f) * cos(angle)).toFloat()
                canvas.drawLine(startX, startY, stopX, stopY, tickPaint)
            }
        }

        // 2. Ambil Waktu Sistem STB
        calendar.timeInMillis = System.currentTimeMillis()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        // 3. Hitung Sudut Derajat Jarum Jam, Menit, Detik
        val hourAngle = Math.toRadians(((hours % 12 + minutes / 60f) * 30).toDouble())
        val minuteAngle = Math.toRadians(((minutes + seconds / 60f) * 6).toDouble())
        val secondAngle = Math.toRadians((seconds * 6).toDouble())

        // 4. Gambar Jarum Jam (Hour Hand)
        val hourLength = radius * 0.50f
        val hourX = (cx + hourLength * sin(hourAngle)).toFloat()
        val hourY = (cy - hourLength * cos(hourAngle)).toFloat()
        val hourTailX = (cx - 10f * sin(hourAngle)).toFloat()
        val hourTailY = (cy + 10f * cos(hourAngle)).toFloat()
        canvas.drawLine(hourTailX, hourTailY, hourX, hourY, hourHandPaint)

        // 5. Gambar Jarum Menit (Minute Hand)
        val minLength = radius * 0.72f
        val minX = (cx + minLength * sin(minuteAngle)).toFloat()
        val minY = (cy - minLength * cos(minuteAngle)).toFloat()
        val minTailX = (cx - 12f * sin(minuteAngle)).toFloat()
        val minTailY = (cy + 12f * cos(minuteAngle)).toFloat()
        canvas.drawLine(minTailX, minTailY, minX, minY, minuteHandPaint)

        // 6. Gambar Jarum Detik (Second Hand)
        val secLength = radius * 0.85f
        val secX = (cx + secLength * sin(secondAngle)).toFloat()
        val secY = (cy - secLength * cos(secondAngle)).toFloat()
        val secTailX = (cx - 16f * sin(secondAngle)).toFloat()
        val secTailY = (cy + 16f * cos(secondAngle)).toFloat()
        canvas.drawLine(secTailX, secTailY, secX, secY, secondHandPaint)

        // 7. Pusat Poros Jarum (Center Pivot)
        canvas.drawCircle(cx, cy, 6f, pivotPaint)
        canvas.drawCircle(cx, cy, 2.5f, pivotCenterPaint)
    }
}
