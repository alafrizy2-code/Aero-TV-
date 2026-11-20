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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Foreground Overlay Service untuk menampilkan widget jam digital kecil (format 24 jam HH : MM)
 * di pojok kiri atas secara persisten di atas semua aplikasi (YouTube, Netflix, browser, game, dll)
 * pada Android TV.
 *
 * - Menggunakan pil glassmorphism gelap berkontras tinggi dengan teks putih tebal + shadow
 *   agar selalu terbaca jelas baik di layar film gelap maupun layar aplikasi terang.
 * - Ukuran dan padding disamakan persis dengan NetworkSpeedOverlayService di pojok kanan atas.
 * - Otomatis sembunyi saat berada di MainActivity (karena MainActivity sudah menampilkan
 *   jam besar di widget header) dan langsung tampil saat berpindah ke aplikasi lain.
 */
class DigitalClockOverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "aero_tv_digital_clock_channel"
        private const val NOTIF_ID = 5202

        @Volatile
        var isLauncherForeground: Boolean = true
            set(value) {
                field = value
                activeInstance?.updateVisibility()
            }

        @Volatile
        var isOverlayEnabledPref: Boolean = true

        @Volatile
        private var activeInstance: DigitalClockOverlayService? = null

        fun startIfPermitted(context: Context) {
            if (!isOverlayEnabledPref) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                return
            }
            try {
                val intent = Intent(context, DigitalClockOverlayService::class.java)
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
                context.stopService(Intent(context, DigitalClockOverlayService::class.java))
            } catch (e: Exception) { }
        }
    }

    private var windowManager: WindowManager? = null
    private var clockTextView: TextView? = null
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH : mm", Locale.getDefault())

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateClockTime()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        startForegroundNotification()
        setupOverlayWindow()
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
                "Jam Digital Overlay",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Aero TV Clock Overlay")
            .setContentText("Menampilkan jam digital di pojok kiri atas")
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
            setColor(Color.parseColor("#B30F172A")) // Gelap semi-transparan berkontras tinggi untuk layar terang/gelap
            setStroke((1 * density).toInt(), Color.parseColor("#59FFFFFF"))
        }

        val tv = TextView(this).apply {
            text = "00 : 00"
            setTextColor(Color.WHITE)
            textSize = 12f
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
            gravity = Gravity.TOP or Gravity.START
            x = (24 * density).toInt()
            y = (16 * density).toInt()
        }

        try {
            windowManager?.addView(tv, params)
            clockTextView = tv
        } catch (e: Exception) {
            // Ignored
        }
    }

    fun updateVisibility() {
        handler.post {
            clockTextView?.visibility = if (isLauncherForeground || !isOverlayEnabledPref) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private fun updateClockTime() {
        val formatted = timeFormat.format(Date())
        clockTextView?.text = formatted
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateRunnable)
        activeInstance = null
        clockTextView?.let {
            try {
                windowManager?.removeView(it)
            } catch (e: Exception) { }
        }
        clockTextView = null
    }
}
