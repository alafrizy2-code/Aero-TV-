package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class LauncherAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "LauncherAccessibility"
        private var lastInterceptTime = 0L
        private const val INTERCEPT_DEBOUNCE_MS = 600L

        // Daftar paket launcher pabrik yang biasa terpasang di STB ZTE B866F / Android TV
        private val FACTORY_LAUNCHER_PACKAGES = setOf(
            "com.zte.zolauncher",
            "com.google.android.tvlauncher",
            "com.android.tv.launcher",
            "com.google.android.apps.tv.launcherx",
            "com.fiberhome.launcher",
            "com.fiberhome.tv.launcher",
            "com.zte.home",
            "com.utstar.launcher"
        )

        /**
         * Memeriksa apakah Layanan Aksesibilitas B866F Launcher sudah diaktifkan di Pengaturan STB.
         */
        fun isAccessibilityServiceEnabled(context: Context): Boolean {
            return try {
                val expectedServiceName = "${context.packageName}/${LauncherAccessibilityService::class.java.canonicalName}"
                val enabledServicesSetting = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: return false

                val colonSplitter = TextUtils.SimpleStringSplitter(':')
                colonSplitter.setString(enabledServicesSetting)
                while (colonSplitter.hasNext()) {
                    val componentName = colonSplitter.next()
                    if (componentName.equals(expectedServiceName, ignoreCase = true) ||
                        componentName.contains(LauncherAccessibilityService::class.java.simpleName)
                    ) {
                        return true
                    }
                }
                false
            } catch (e: Exception) {
                false
            }
        }

        /**
         * Memeriksa apakah izin WRITE_SETTINGS (Mengubah Setelan Sistem) sudah diberikan.
         */
        fun canWriteSettings(context: Context): Boolean {
            return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Settings.System.canWrite(context)
            } else {
                true
            }
        }

        /**
         * Mencoba mengaktifkan Layanan Aksesibilitas secara programatik jika memiliki hak WRITE_SECURE_SETTINGS / System / Root.
         */
        fun tryEnableAccessibilityProgrammatically(context: Context): Boolean {
            return try {
                val expectedComponent = "${context.packageName}/${LauncherAccessibilityService::class.java.name}"
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""

                val newEnabled = if (enabledServices.isEmpty()) {
                    expectedComponent
                } else if (!enabledServices.contains(expectedComponent)) {
                    "$enabledServices:$expectedComponent"
                } else {
                    enabledServices
                }

                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                    newEnabled
                )
                Settings.Secure.putString(
                    context.contentResolver,
                    Settings.Secure.ACCESSIBILITY_ENABLED,
                    "1"
                )
                Log.d(TAG, "Berhasil mengaktifkan Aksesibilitas via Settings.Secure!")
                true
            } catch (e: Exception) {
                Log.w(TAG, "Gagal mengaktifkan secara programatik (perlu izin manual di UI): ${e.message}")
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "LauncherAccessibilityService connected successfully")
        try {
            val info = serviceInfo
            if (info != null) {
                info.flags = info.flags or
                        android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                        android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                        android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
                serviceInfo = info
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Info configuration notice: ${e.message}")
        }
    }

    /**
     * Mencegat tombol HOME pada Remote Control STB B866F.
     * Ketika tombol HOME ditekan, kita langsung meluncurkan B866F Launcher dan
     * mengonsumsi event tombol tersebut agar tidak diambil oleh launcher bawaan ZTE.
     */
    override fun onKeyEvent(event: KeyEvent): Boolean {
        try {
            val keyCode = event.keyCode

            if (keyCode == KeyEvent.KEYCODE_HOME) {
                if (event.action == KeyEvent.ACTION_UP) {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastInterceptTime > INTERCEPT_DEBOUNCE_MS) {
                        lastInterceptTime = now
                        launchLauncherActivity()
                    }
                }
                return true // Konsumsi event tombol HOME
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error in onKeyEvent", e)
        }

        return super.onKeyEvent(event)
    }

    /**
     * Memantau jendela aplikasi yang aktif di layar TV.
     * Jika launcher bawaan ZTE / IndiHome mencoba muncul, alihkan ke B866F Launcher.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        try {
            if (event == null) return

            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val pkgName = event.packageName?.toString() ?: return

                if (pkgName != packageName && FACTORY_LAUNCHER_PACKAGES.contains(pkgName)) {
                    val now = SystemClock.uptimeMillis()
                    if (now - lastInterceptTime > 1200L) {
                        lastInterceptTime = now
                        Log.d(TAG, "Terdeteksi launcher bawaan $pkgName aktif. Mengalihkan ke B866F Launcher...")
                        launchLauncherActivity()
                    }
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error in onAccessibilityEvent", e)
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "LauncherAccessibilityService interrupted")
    }

    private fun launchLauncherActivity() {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal meluncurkan MainActivity dari AccessibilityService", e)
        }
    }
}
