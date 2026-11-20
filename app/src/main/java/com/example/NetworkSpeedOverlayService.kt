package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.util.Locale

/**
 * Foreground Overlay Service untuk menampilkan informasi kecepatan jaringan secara persisten
 * di atas semua aplikasi (YouTube, Netflix, browser, game, dll) pada STB Android TV.
 *
 * - Menggunakan pil glassmorphism gelap berkontras tinggi dengan teks putih tebal + shadow
 *   agar selalu terbaca jelas baik di layar film gelap maupun layar aplikasi terang.
 * - Otomatis sembunyi saat berada di MainActivity (karena MainActivity sudah menampilkan
 *   indikator kecepatan sejajar rapi di bar ikon pojok kanan atas) dan langsung tampil
 *   saat berpindah ke aplikasi lain.
 */
class NetworkSpeedOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "aero_tv_network_speed_channel"
        private const val NOTIF_ID = 5201

        @Volatile
        var isLauncherForeground: Boolean = true
            set(value) {
                field = value
                activeInstance?.updateVisibility()
            }

        @Volatile
        var isOverlayEnabledPref: Boolean = true

        @Volatile
        private var activeInstance: NetworkSpeedOverlayService? = null

        fun startIfPermitted(context: Context) {
            if (!isOverlayEnabledPref) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            try {
                val intent = Intent(context, NetworkSpeedOverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // Ignored if restricted
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, NetworkSpeedOverlayService::class.java))
            } catch (e: Exception) { }
        }
    }

    private var windowManager: WindowManager? = null
    private var speedTextView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var lastRxBytes = 0L
    private var lastTxBytes = 0L

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateNetworkSpeed()
            handler.postDelayed(this, 1500)
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        startForegroundNotification()
        setupOverlayWindow()
        lastRxBytes = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        lastTxBytes = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
        handler.post(updateRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        updateVisibility()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monitor Kecepatan Jaringan",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aero TV Network Monitor")
            .setContentText("Menampilkan kecepatan jaringan di atas aplikasi")
            .setSmallIcon(R.drawable.ic_wifi)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIF_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (_: Throwable) {
            // Ignored on restricted background transitions
        }
    }

    private fun setupOverlayWindow() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val density = resources.displayMetrics.density

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 14f * density
            setColor(Color.parseColor("#B30F172A")) // Gelap semi-transparan terbaca di layar terang maupun gelap
            setStroke((1 * density).toInt(), Color.parseColor("#59FFFFFF"))
        }

        val tv = TextView(this).apply {
            text = "↓ 0.0 KB/s"
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            background = bgDrawable
            setPadding(
                (10 * density).toInt(),
                (4 * density).toInt(),
                (10 * density).toInt(),
                (4 * density).toInt()
            )
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
            visibility = if (isLauncherForeground) View.GONE else View.VISIBLE
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = (24 * density).toInt()
            y = (16 * density).toInt()
        }

        try {
            windowManager?.addView(tv, params)
            speedTextView = tv
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun updateVisibility() {
        handler.post {
            speedTextView?.visibility = if (isLauncherForeground || !isOverlayEnabledPref) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private fun updateNetworkSpeed() {
        val currentRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
        val currentTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)

        val deltaBytes = ((currentRx - lastRxBytes).coerceAtLeast(0L) +
            (currentTx - lastTxBytes).coerceAtLeast(0L))
        lastRxBytes = currentRx
        lastTxBytes = currentTx

        // Dibagi 1.5 detik interval
        val bytesPerSec = (deltaBytes / 1.5).toLong()
        val formatted = when {
            bytesPerSec < 1024 * 1024 -> String.format(Locale.US, "↓ %.1f KB/s", bytesPerSec / 1024.0)
            else -> String.format(Locale.US, "↓ %.2f MB/s", bytesPerSec / (1024.0 * 1024.0))
        }
        speedTextView?.text = formatted
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        activeInstance = null
        speedTextView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) { }
        }
        speedTextView = null
    }
}
