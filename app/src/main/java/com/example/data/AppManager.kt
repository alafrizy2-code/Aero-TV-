package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import com.example.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppManager(private val context: Context) {

    suspend fun getInstalledApps(favorites: Set<String>): List<AppInfo> = withContext(Dispatchers.IO) {
        val packageManager = context.packageManager
        val appsMap = mutableMapOf<String, AppInfo>()

        // 1. Query Leanback Launcher apps (Android TV specific)
        val tvIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        }
        val tvApps = packageManager.queryIntentActivities(tvIntent, 0)
        for (resolveInfo in tvApps) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) continue // Skip our launcher

            val label = resolveInfo.loadLabel(packageManager)?.toString() ?: pkg
            val icon = resolveInfo.loadIcon(packageManager)
            val isFav = favorites.contains(pkg)
            val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            appsMap[pkg] = AppInfo(
                packageName = pkg,
                activityName = resolveInfo.activityInfo.name,
                label = label,
                isFavorite = isFav,
                isSystemApp = isSystem,
                icon = icon
            )
        }

        // 2. Query Standard Launcher apps as well (for sideloaded or general utility apps)
        val standardIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val standardApps = packageManager.queryIntentActivities(standardIntent, 0)
        for (resolveInfo in standardApps) {
            val pkg = resolveInfo.activityInfo.packageName
            if (pkg == context.packageName) continue
            if (!appsMap.containsKey(pkg)) {
                val label = resolveInfo.loadLabel(packageManager)?.toString() ?: pkg
                val icon = resolveInfo.loadIcon(packageManager)
                val isFav = favorites.contains(pkg)
                val isSystem = (resolveInfo.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

                appsMap[pkg] = AppInfo(
                    packageName = pkg,
                    activityName = resolveInfo.activityInfo.name,
                    label = label,
                    isFavorite = isFav,
                    isSystemApp = isSystem,
                    icon = icon
                )
            }
        }

        // Sort: favorites first, then alphabetically
        appsMap.values.sortedWith(
            compareByDescending<AppInfo> { it.isFavorite }
                .thenBy { it.label.lowercase() }
        )
    }

    fun launchApp(app: AppInfo): Boolean {
        return try {
            val launchIntent = context.packageManager.getLeanbackLaunchIntentForPackage(app.packageName)
                ?: context.packageManager.getLaunchIntentForPackage(app.packageName)

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                Toast.makeText(context, "Tidak dapat membuka ${app.label}", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun openAppInfo(packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka info aplikasi", Toast.LENGTH_SHORT).show()
        }
    }

    fun uninstallApp(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal mencopot aplikasi", Toast.LENGTH_SHORT).show()
        }
    }

    // Quick System Settings Shortcuts
    fun openTvSettings() {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Pengaturan sistem tidak tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWifiSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openTvSettings()
        }
    }

    fun openBluetoothSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openTvSettings()
        }
    }

    fun openDisplaySettings() {
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openTvSettings()
        }
    }

    fun openManageAppsSettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            openTvSettings()
        }
    }

    fun openDefaultHomeSettings() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
            } else {
                Intent(Settings.ACTION_SETTINGS)
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            openTvSettings()
        }
    }
}
