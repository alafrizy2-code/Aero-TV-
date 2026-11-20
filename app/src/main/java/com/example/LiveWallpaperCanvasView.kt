package com.example

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Custom View untuk 4 Mode Live Wallpaper Bawaan berbasis Canvas (Tanpa Video/Lag):
 * 1. CAMPFIRE (Api Unggun): Partikel api oranye beterbangan ke atas, bara api kecil, efek asap tipis, background sunset Pantai.
 * 2. RAIN (Hujan): Tetes hujan jatuh terus + efek riak air di bawah, background pemandangan Danau Toba.
 * 3. GALAXY (Galaksi / Bintang): Bintang-bintang berkelip + nebula ungu-biru bergerak perlahan.
 * 4. CLOUDS (Awan Bergerak): Awan putih bergerak lambat di langit biru gelap.
 *
 * Mendukung pengaturan Kecepatan Animasi (0.5x - 2.0x) dan Jumlah Partikel (0.5x - 2.0x).
 */
class LiveWallpaperCanvasView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class LiveType {
        CAMPFIRE,
        RAIN,
        GALAXY,
        CLOUDS,
        OFF
    }

    var liveType: LiveType = LiveType.OFF
        set(value) {
            field = value
            if (value != LiveType.OFF) {
                visibility = VISIBLE
                startAnimation()
            } else {
                visibility = GONE
                stopAnimation()
            }
            invalidate()
        }

    var animSpeedMultiplier: Float = 1.0f
    var particleDensityMultiplier: Float = 1.0f

    private var animTime = 0f
    private var animator: ValueAnimator? = null
    private val random = Random(12345)

    // Paints
    private val bgPaint = Paint()
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }
    private val mountainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#1B2A38")
    }

    // Particle models
    private class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float,
        var life: Float,
        var maxLife: Float,
        var color: Int
    )

    private class Star(
        var x: Float,
        var y: Float,
        var size: Float,
        var blinkOffset: Float,
        var blinkSpeed: Float
    )

    private class Ripple(
        var x: Float,
        var y: Float,
        var radius: Float,
        var alpha: Float
    )

    private val fireParticles = mutableListOf<Particle>()
    private val stars = mutableListOf<Star>()
    private val ripples = mutableListOf<Ripple>()
    private val rainDrops = mutableListOf<Particle>()
    private val clouds = mutableListOf<Particle>()

    init {
        initParticles()
    }

    private fun initParticles() {
        fireParticles.clear()
        stars.clear()
        ripples.clear()
        rainDrops.clear()
        clouds.clear()

        // Bintang untuk Galaksi
        for (i in 0 until 140) {
            stars.add(
                Star(
                    x = random.nextFloat(),
                    y = random.nextFloat(),
                    size = 1f + random.nextFloat() * 2.8f,
                    blinkOffset = random.nextFloat() * 6.28f,
                    blinkSpeed = 1.2f + random.nextFloat() * 2.5f
                )
            )
        }

        // Tetes Hujan untuk Danau Toba
        for (i in 0 until 90) {
            rainDrops.add(
                Particle(
                    x = random.nextFloat(),
                    y = random.nextFloat(),
                    vx = -0.002f,
                    vy = 0.015f + random.nextFloat() * 0.015f,
                    size = 12f + random.nextFloat() * 14f,
                    life = 1f,
                    maxLife = 1f,
                    color = Color.parseColor("#8060A5FA")
                )
            )
        }

        // Awan untuk Awan Bergerak
        for (i in 0 until 7) {
            clouds.add(
                Particle(
                    x = random.nextFloat(),
                    y = 0.05f + random.nextFloat() * 0.65f,
                    vx = 0.0003f + random.nextFloat() * 0.0006f,
                    vy = 0f,
                    size = 90f + random.nextFloat() * 140f,
                    life = 1f,
                    maxLife = 1f,
                    color = if (random.nextBoolean()) Color.parseColor("#55FFFFFF") else Color.parseColor("#77CBD5E1")
                )
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (liveType != LiveType.OFF) {
            startAnimation()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }

    fun setWallpaperType(type: LiveType) {
        liveType = type
    }

    fun getWallpaperType(): LiveType = liveType

    fun startAnimation() {
        if (animator?.isRunning == true) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val dt = 0.030f * animSpeedMultiplier
                animTime += dt
                updatePhysics(dt)
                invalidate()
            }
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
    }

    private fun updatePhysics(dt: Float) {
        val w = width.toFloat().coerceAtLeast(1f)
        val h = height.toFloat().coerceAtLeast(1f)

        when (liveType) {
            LiveType.CAMPFIRE -> {
                // Emisi partikel api baru di dasar tengah
                val maxParticles = (65 * particleDensityMultiplier).toInt()
                if (fireParticles.size < maxParticles) {
                    val colors = intArrayOf(
                        Color.parseColor("#FFFFD166"),
                        Color.parseColor("#FFF4A261"),
                        Color.parseColor("#FFE76F51"),
                        Color.parseColor("#E6EF476F"),
                        Color.parseColor("#44999999") // Asap tipis
                    )
                    fireParticles.add(
                        Particle(
                            x = 0.5f + (random.nextFloat() - 0.5f) * 0.08f,
                            y = 0.96f,
                            vx = (random.nextFloat() - 0.5f) * 0.003f,
                            vy = -(0.006f + random.nextFloat() * 0.012f),
                            size = 8f + random.nextFloat() * 16f,
                            life = 1.0f,
                            maxLife = 1.0f,
                            color = colors[random.nextInt(colors.size)]
                        )
                    )
                }

                val it = fireParticles.iterator()
                while (it.hasNext()) {
                    val p = it.next()
                    p.x += p.vx + sin(animTime * 3f + p.y * 10f) * 0.001f
                    p.y += p.vy
                    p.life -= 0.022f * animSpeedMultiplier
                    p.size *= 0.985f
                    if (p.life <= 0f || p.y < 0.35f) {
                        it.remove()
                    }
                }
            }

            LiveType.RAIN -> {
                // Hujan Danau Toba & riak air
                for (drop in rainDrops) {
                    drop.y += drop.vy * animSpeedMultiplier
                    drop.x += drop.vx * animSpeedMultiplier
                    if (drop.x < 0f) drop.x = 1.0f
                    if (drop.y > 0.88f) {
                        drop.y = 0.05f
                        drop.x = random.nextFloat()
                        // Buat riak air di danau
                        if (ripples.size < 25) {
                            ripples.add(Ripple(drop.x * w, 0.88f * h + (random.nextFloat() * 0.10f * h), 2f, 0.7f))
                        }
                    }
                }

                val rit = ripples.iterator()
                while (rit.hasNext()) {
                    val r = rit.next()
                    r.radius += 1.2f * animSpeedMultiplier
                    r.alpha -= 0.025f * animSpeedMultiplier
                    if (r.alpha <= 0f) {
                        rit.remove()
                    }
                }
            }

            LiveType.CLOUDS -> {
                for (cloud in clouds) {
                    cloud.x += cloud.vx * animSpeedMultiplier
                    if (cloud.x > 1.3f) {
                        cloud.x = -0.3f
                        cloud.y = 0.05f + random.nextFloat() * 0.65f
                    }
                }
            }

            else -> {}
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        when (liveType) {
            LiveType.CAMPFIRE -> drawCampfire(canvas, w, h)
            LiveType.RAIN -> drawLakeTobaRain(canvas, w, h)
            LiveType.GALAXY -> drawGalaxy(canvas, w, h)
            LiveType.CLOUDS -> drawMovingClouds(canvas, w, h)
            LiveType.OFF -> {}
        }
    }

    /**
     * 1. Api Unggun: Background sunset di pantai + api dan bara beterbangan + asap
     */
    private fun drawCampfire(canvas: Canvas, w: Float, h: Float) {
        // Background Sunset Pantai Gradient
        val sunsetShader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.parseColor("#1B1A47"), // Langit atas senja ungu tua
                Color.parseColor("#6A2C70"), // Ungu merah
                Color.parseColor("#B83B5E"), // Magenta senja
                Color.parseColor("#F08A5D"), // Jingga sunset
                Color.parseColor("#101018")  // Pantai siluet pasir malam
            ),
            floatArrayOf(0.0f, 0.35f, 0.60f, 0.82f, 1.0f),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = sunsetShader
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Cahaya bara api di tanah
        val glowShader = RadialGradient(
            w * 0.5f, h * 0.94f, w * 0.28f,
            Color.parseColor("#80FF9E00"),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        particlePaint.shader = glowShader
        canvas.drawCircle(w * 0.5f, h * 0.94f, w * 0.28f, particlePaint)
        particlePaint.shader = null

        // Siluet tumpukan kayu api
        linePaint.color = Color.parseColor("#150D0A")
        linePaint.strokeWidth = 14f
        linePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(w * 0.44f, h * 0.97f, w * 0.56f, h * 0.93f, linePaint)
        canvas.drawLine(w * 0.56f, h * 0.97f, w * 0.44f, h * 0.93f, linePaint)
        canvas.drawLine(w * 0.47f, h * 0.97f, w * 0.53f, h * 0.92f, linePaint)

        // Partikel api & bara
        for (p in fireParticles) {
            val alpha = (p.life * 255).toInt().coerceIn(0, 255)
            val baseColor = p.color
            val c = Color.argb(
                alpha,
                Color.red(baseColor),
                Color.green(baseColor),
                Color.blue(baseColor)
            )
            particlePaint.color = c
            canvas.drawCircle(p.x * w, p.y * h, p.size, particlePaint)
        }
    }

    /**
     * 2. Hujan: Pemandangan Danau Toba Sumatera Utara + tetes hujan + riak air
     */
    private fun drawLakeTobaRain(canvas: Canvas, w: Float, h: Float) {
        // Langit mendung Danau Toba
        val skyShader = LinearGradient(
            0f, 0f, 0f, h,
            intArrayOf(
                Color.parseColor("#131C26"), // Langit badai gelap
                Color.parseColor("#26384A"), // Awan tebal kabut
                Color.parseColor("#1C3243"), // Air Danau Toba
                Color.parseColor("#0C1722")
            ),
            floatArrayOf(0.0f, 0.45f, 0.65f, 1.0f),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = skyShader
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Siluet Pegunungan Danau Toba (Bukit Barisan)
        val mountainPath = Path().apply {
            moveTo(0f, h * 0.58f)
            lineTo(w * 0.18f, h * 0.46f)
            lineTo(w * 0.38f, h * 0.54f)
            lineTo(w * 0.58f, h * 0.42f)
            lineTo(w * 0.78f, h * 0.50f)
            lineTo(w, h * 0.44f)
            lineTo(w, h * 0.62f)
            lineTo(0f, h * 0.62f)
            close()
        }
        canvas.drawPath(mountainPath, mountainPaint)

        // Garis pemisah Danau Toba
        val waterGrad = LinearGradient(
            0f, h * 0.60f, 0f, h,
            Color.parseColor("#331F384C"),
            Color.parseColor("#E60B141C"),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = waterGrad
        canvas.drawRect(0f, h * 0.60f, w, h, bgPaint)

        // Riak air di permukaan danau
        for (r in ripples) {
            ripplePaint.color = Color.parseColor("#4DE2E8F0")
            ripplePaint.alpha = (r.alpha * 255).toInt().coerceIn(0, 255)
            canvas.drawOval(r.x - r.radius * 2.2f, r.y - r.radius * 0.6f, r.x + r.radius * 2.2f, r.y + r.radius * 0.6f, ripplePaint)
        }

        // Tetes air hujan
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeWidth = 2.2f
        linePaint.color = Color.parseColor("#66A5C9EB")
        for (drop in rainDrops) {
            val px = drop.x * w
            val py = drop.y * h
            canvas.drawLine(px, py, px - 3f, py + drop.size, linePaint)
        }
    }

    /**
     * 3. Galaksi: Bintang-bintang berkelip + nebula ungu-biru bergerak perlahan
     */
    private fun drawGalaxy(canvas: Canvas, w: Float, h: Float) {
        // Deep Space Background
        canvas.drawColor(Color.parseColor("#060714"))

        // Nebula 1 (Ungu Swirling)
        val neb1X = w * 0.35f + sin(animTime * 0.2f) * (w * 0.08f)
        val neb1Y = h * 0.45f + cos(animTime * 0.25f) * (h * 0.08f)
        val neb1Shader = RadialGradient(
            neb1X, neb1Y, w * 0.45f,
            Color.parseColor("#447928CA"),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        particlePaint.shader = neb1Shader
        canvas.drawCircle(neb1X, neb1Y, w * 0.45f, particlePaint)

        // Nebula 2 (Biru Toska)
        val neb2X = w * 0.68f + cos(animTime * 0.18f) * (w * 0.06f)
        val neb2Y = h * 0.55f + sin(animTime * 0.22f) * (h * 0.06f)
        val neb2Shader = RadialGradient(
            neb2X, neb2Y, w * 0.40f,
            Color.parseColor("#330070F3"),
            Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        particlePaint.shader = neb2Shader
        canvas.drawCircle(neb2X, neb2Y, w * 0.40f, particlePaint)
        particlePaint.shader = null

        // Bintang-bintang berkelip
        particlePaint.color = Color.WHITE
        for (star in stars) {
            val blink = (sin(animTime * star.blinkSpeed + star.blinkOffset) + 1f) * 0.5f
            val alpha = (40 + (blink * 215)).toInt().coerceIn(0, 255)
            particlePaint.alpha = alpha
            canvas.drawCircle(star.x * w, star.y * h, star.size, particlePaint)
        }
    }

    /**
     * 4. Awan Bergerak: Awan putih bergerak lambat di langit biru gelap
     */
    private fun drawMovingClouds(canvas: Canvas, w: Float, h: Float) {
        // Langit biru gelap malam
        val skyShader = LinearGradient(
            0f, 0f, 0f, h,
            Color.parseColor("#0B132B"),
            Color.parseColor("#1C2541"),
            Shader.TileMode.CLAMP
        )
        bgPaint.shader = skyShader
        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Awan berlapis bergerak lambat
        for (c in clouds) {
            particlePaint.color = c.color
            val cx = c.x * w
            val cy = c.y * h
            val r = c.size

            canvas.drawCircle(cx, cy, r * 0.55f, particlePaint)
            canvas.drawCircle(cx - r * 0.40f, cy + r * 0.12f, r * 0.40f, particlePaint)
            canvas.drawCircle(cx + r * 0.40f, cy + r * 0.12f, r * 0.40f, particlePaint)
            canvas.drawRoundRect(cx - r * 0.65f, cy, cx + r * 0.65f, cy + r * 0.45f, r * 0.25f, r * 0.25f, particlePaint)
        }
    }
}
