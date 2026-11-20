package com.example

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom View untuk Animasi Cuaca Dinamis Real-Time sesuai WMO Weather Code & Siang/Malam:
 * - SUNNY: Matahari kuning cerah berputar + glow hangat.
 * - CLEAR_NIGHT: Bulan sabit kuning lembut + bintang berkelip.
 * - PARTLY_CLOUDY: Matahari di belakang awan bergerak perlahan.
 * - PARTLY_CLOUDY_NIGHT: Bulan sabit + awan bergerak perlahan.
 * - CLOUDY: Awan berlapis bergerak perlahan.
 * - RAIN: Awan hujan + tetesan air jatuh + splash.
 * - THUNDERSTORM: Awan gelap + hujan + kilatan petir menyambar.
 * - FOGGY: Awan + garis kabut melayang horizontal.
 */
class WeatherAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class WeatherType {
        RAIN,
        SUNNY,
        CLEAR_NIGHT,
        PARTLY_CLOUDY,
        PARTLY_CLOUDY_NIGHT,
        CLOUDY,
        THUNDERSTORM,
        FOGGY
    }

    private var currentWeatherType: WeatherType = WeatherType.PARTLY_CLOUDY

    // Paints
    private val sunPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FBBF24")
        style = Paint.Style.FILL
    }
    private val sunGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4DF59E0B")
        style = Paint.Style.FILL
    }
    private val sunRayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F59E0B")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FDE68A")
        style = Paint.Style.FILL
    }
    private val moonGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FDE68A")
        style = Paint.Style.FILL
    }
    private val moonCutoutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E60F172A")
        style = Paint.Style.FILL
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FEF08A")
        style = Paint.Style.FILL
    }
    private val cloudPaintMain = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#94A3B8")
        style = Paint.Style.FILL
    }
    private val cloudPaintSecondary = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E2E8F0")
        style = Paint.Style.FILL
    }
    private val cloudPaintDark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#334155")
        style = Paint.Style.FILL
    }
    private val rainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60A5FA")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val splashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#93C5FD")
        style = Paint.Style.STROKE
    }
    private val lightningPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FACC15")
        style = Paint.Style.FILL_AND_STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fogPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#99FFFFFF")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private class Drop(
        var x: Float,
        var y: Float,
        var speed: Float,
        var length: Float,
        var splashRadius: Float = 0f,
        var splashAlpha: Float = 0f
    )

    private val drops = mutableListOf<Drop>()
    private val random = Random(42)

    private val moonPath = Path()
    private val cutoutPath = Path()
    private val boltPath = Path()
    private val cloudRect = RectF()

    private var animTime = 0f
    private var animator: ValueAnimator? = null
    private var isWindowVisible = true

    init {
        val density = context.resources.displayMetrics.density
        sunRayPaint.strokeWidth = 2.6f * density
        rainPaint.strokeWidth = 2f * density
        splashPaint.strokeWidth = 1.2f * density
        lightningPaint.strokeWidth = 2.5f * density
        fogPaint.strokeWidth = 2.6f * density

        for (i in 0 until 14) {
            drops.add(
                Drop(
                    x = random.nextFloat(),
                    y = random.nextFloat(),
                    speed = 0.015f + random.nextFloat() * 0.02f,
                    length = 6f + random.nextFloat() * 6f
                )
            )
        }
    }

    fun setWeatherType(type: WeatherType) {
        if (currentWeatherType != type) {
            currentWeatherType = type
            invalidate()
        }
    }

    fun setWeatherByCode(weatherCode: Int, isDay: Int = 1) {
        val daytime = (isDay == 1)
        val type = when (weatherCode) {
            0 -> if (daytime) WeatherType.SUNNY else WeatherType.CLEAR_NIGHT
            1, 2 -> if (daytime) WeatherType.PARTLY_CLOUDY else WeatherType.PARTLY_CLOUDY_NIGHT
            3 -> WeatherType.CLOUDY
            45, 48 -> WeatherType.FOGGY
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86 -> WeatherType.RAIN
            95, 96, 99 -> WeatherType.THUNDERSTORM
            else -> if (daytime) WeatherType.PARTLY_CLOUDY else WeatherType.PARTLY_CLOUDY_NIGHT
        }
        setWeatherType(type)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isWindowVisible = windowVisibility == VISIBLE && isShown
        if (isWindowVisible) {
            post { if (isAttachedToWindow && isWindowVisible) startAnimation() }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        isWindowVisible = (visibility == VISIBLE) && isShown
        if (isWindowVisible) {
            startAnimation()
        } else {
            stopAnimation()
        }
    }

    private fun startAnimation() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                animTime += 0.035f
                updateParticles()
                invalidate()
            }
            start()
        }
    }

    private fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun updateParticles() {
        for (drop in drops) {
            drop.y += drop.speed
            if (drop.splashAlpha > 0f) {
                drop.splashRadius += 0.4f
                drop.splashAlpha -= 0.08f
            }
            if (drop.y > 1.0f) {
                drop.y = 0.35f
                drop.x = 0.15f + random.nextFloat() * 0.70f
                drop.splashRadius = 1f
                drop.splashAlpha = 0.8f
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        when (currentWeatherType) {
            WeatherType.SUNNY -> drawSunny(canvas, w, h, w * 0.5f, h * 0.5f, 1.0f)
            WeatherType.CLEAR_NIGHT -> drawClearNight(canvas, w, h, w * 0.5f, h * 0.5f, 1.0f)
            WeatherType.PARTLY_CLOUDY -> drawPartlyCloudy(canvas, w, h, daytime = true)
            WeatherType.PARTLY_CLOUDY_NIGHT -> drawPartlyCloudy(canvas, w, h, daytime = false)
            WeatherType.RAIN -> drawRain(canvas, w, h, isThunder = false)
            WeatherType.CLOUDY -> drawCloudy(canvas, w, h)
            WeatherType.THUNDERSTORM -> drawRain(canvas, w, h, isThunder = true)
            WeatherType.FOGGY -> drawFoggy(canvas, w, h)
        }
    }

    private fun drawSunny(canvas: Canvas, w: Float, h: Float, cx: Float, cyBase: Float, scaleFactor: Float) {
        val floatOffset = sin(animTime * 1.8f) * (h * 0.04f)
        val cy = cyBase + floatOffset
        val baseRadius = minOf(w, h) * 0.24f * scaleFactor

        canvas.drawCircle(cx, cy, baseRadius * 1.55f, sunGlowPaint)

        val rayCount = 8
        val rayInner = baseRadius * 1.25f
        val rayOuter = baseRadius * 1.65f
        val rotationAngle = (animTime * 20f) % 360f

        for (i in 0 until rayCount) {
            val angleDeg = rotationAngle + (i * (360f / rayCount))
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val x1 = cx + (cos(angleRad) * rayInner).toFloat()
            val y1 = cy + (sin(angleRad) * rayInner).toFloat()
            val x2 = cx + (cos(angleRad) * rayOuter).toFloat()
            val y2 = cy + (sin(angleRad) * rayOuter).toFloat()
            canvas.drawLine(x1, y1, x2, y2, sunRayPaint)
        }

        canvas.drawCircle(cx, cy, baseRadius, sunPaint)
    }

    private fun drawClearNight(canvas: Canvas, w: Float, h: Float, cx: Float, cyBase: Float, scaleFactor: Float) {
        val floatOffset = sin(animTime * 1.4f) * (h * 0.03f)
        val cy = cyBase + floatOffset
        val r = minOf(w, h) * 0.24f * scaleFactor

        canvas.drawCircle(cx, cy, r * 1.45f, moonGlowPaint)

        // Bulan sabit tanpa alokasi objek di onDraw
        moonPath.reset()
        moonPath.addCircle(cx, cy, r, Path.Direction.CW)
        cutoutPath.reset()
        cutoutPath.addCircle(cx + r * 0.48f, cy - r * 0.28f, r * 0.85f, Path.Direction.CW)
        moonPath.op(cutoutPath, Path.Op.DIFFERENCE)
        canvas.drawPath(moonPath, moonPaint)

        // Bintang berkelip lembut
        val alpha1 = (120 + (sin(animTime * 3.0f) * 110)).toInt().coerceIn(40, 255)
        val alpha2 = (120 + (cos(animTime * 2.4f) * 110)).toInt().coerceIn(40, 255)
        starPaint.alpha = alpha1
        canvas.drawCircle(cx + r * 1.15f, cy - r * 0.65f, r * 0.14f, starPaint)
        starPaint.alpha = alpha2
        canvas.drawCircle(cx - r * 1.1f, cy - r * 0.75f, r * 0.11f, starPaint)
    }

    private fun drawPartlyCloudy(canvas: Canvas, w: Float, h: Float, daytime: Boolean) {
        if (daytime) {
            drawSunny(canvas, w, h, w * 0.36f, h * 0.36f, 0.72f)
        } else {
            drawClearNight(canvas, w, h, w * 0.36f, h * 0.36f, 0.72f)
        }
        val sway = sin(animTime * 1.2f) * (w * 0.05f)
        drawCloudShape(canvas, w * 0.56f + sway, h * 0.56f, minOf(w, h) * 0.22f, cloudPaintSecondary)
    }

    private fun drawRain(canvas: Canvas, w: Float, h: Float, isThunder: Boolean) {
        val sway = sin(animTime * 1.5f) * (w * 0.035f)
        val cloudCx = w * 0.5f + sway
        val cloudCy = h * 0.35f
        val cloudRadius = minOf(w, h) * 0.22f

        if (isThunder) {
            val flashPhase = (animTime * 1.2f) % 3.0f
            if (flashPhase < 0.25f || (flashPhase in 0.35f..0.50f)) {
                boltPath.reset()
                val bx = cloudCx
                val by = cloudCy + cloudRadius * 0.5f
                boltPath.moveTo(bx, by)
                boltPath.lineTo(bx - 6f, by + 16f)
                boltPath.lineTo(bx + 2f, by + 16f)
                boltPath.lineTo(bx - 4f, by + 32f)
                canvas.drawPath(boltPath, lightningPaint)
            }
        }

        val cloudBottom = cloudCy + cloudRadius * 0.6f
        for (drop in drops) {
            val dropX = drop.x * w
            val dropY = cloudBottom + (drop.y * (h - cloudBottom))

            if (dropY in cloudBottom..h) {
                canvas.drawLine(dropX, dropY, dropX - 1.5f, dropY + drop.length, rainPaint)
            }

            if (drop.splashAlpha > 0f) {
                splashPaint.alpha = (drop.splashAlpha * 255).toInt().coerceIn(0, 255)
                canvas.drawCircle(dropX, h - 2f, drop.splashRadius * 3f, splashPaint)
            }
        }

        val paint = if (isThunder) cloudPaintDark else cloudPaintMain
        drawCloudShape(canvas, cloudCx, cloudCy, cloudRadius, paint)
    }

    private fun drawCloudy(canvas: Canvas, w: Float, h: Float) {
        val cloudWidth = w * 0.6f
        val offset1 = ((animTime * 6f) % (w + cloudWidth)) - (cloudWidth * 0.5f)
        drawCloudShape(canvas, offset1, h * 0.40f, minOf(w, h) * 0.18f, cloudPaintMain)

        val offset2 = (((animTime * 10f) + w * 0.4f) % (w + cloudWidth)) - (cloudWidth * 0.5f)
        drawCloudShape(canvas, offset2, h * 0.48f, minOf(w, h) * 0.22f, cloudPaintSecondary)
    }

    private fun drawFoggy(canvas: Canvas, w: Float, h: Float) {
        drawCloudShape(canvas, w * 0.5f, h * 0.32f, minOf(w, h) * 0.18f, cloudPaintMain)

        val line1X = sin(animTime * 0.8f) * (w * 0.08f)
        val line2X = cos(animTime * 0.6f) * (w * 0.10f)
        val line3X = sin(animTime * 0.7f + 1f) * (w * 0.07f)

        canvas.drawLine(w * 0.15f + line1X, h * 0.60f, w * 0.85f + line1X, h * 0.60f, fogPaint)
        canvas.drawLine(w * 0.25f + line2X, h * 0.74f, w * 0.75f + line2X, h * 0.74f, fogPaint)
        canvas.drawLine(w * 0.18f + line3X, h * 0.88f, w * 0.82f + line3X, h * 0.88f, fogPaint)
    }

    private fun drawCloudShape(canvas: Canvas, cx: Float, cy: Float, radius: Float, paint: Paint) {
        canvas.drawCircle(cx, cy - radius * 0.2f, radius, paint)
        canvas.drawCircle(cx - radius * 0.75f, cy + radius * 0.1f, radius * 0.70f, paint)
        canvas.drawCircle(cx + radius * 0.75f, cy + radius * 0.1f, radius * 0.70f, paint)
        cloudRect.set(
            cx - radius * 1.1f,
            cy - radius * 0.1f,
            cx + radius * 1.1f,
            cy + radius * 0.65f
        )
        canvas.drawRoundRect(cloudRect, radius * 0.5f, radius * 0.5f, paint)
    }
}
