package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.AppDockSize
import com.example.model.LauncherSettings
import com.example.model.ThemeAccent
import com.example.model.WallpaperType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aero_tv_launcher_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _favorites = MutableStateFlow(loadFavorites())
    val favorites: StateFlow<Set<String>> = _favorites.asStateFlow()

    private fun loadSettings(): LauncherSettings {
        val wallpaperName = prefs.getString("wallpaper_type", WallpaperType.FAMILY_PHOTO.name)
            ?: WallpaperType.FAMILY_PHOTO.name
        val wallpaperType = try {
            WallpaperType.valueOf(wallpaperName)
        } catch (e: Exception) {
            WallpaperType.FAMILY_PHOTO
        }

        val themeName = prefs.getString("theme_accent", ThemeAccent.CYAN_CYBER.name)
            ?: ThemeAccent.CYAN_CYBER.name
        val themeAccent = try {
            ThemeAccent.valueOf(themeName)
        } catch (e: Exception) {
            ThemeAccent.CYAN_CYBER
        }

        val dockSizeName = prefs.getString("dock_size", AppDockSize.COMPACT_56.name)
            ?: AppDockSize.COMPACT_56.name
        val appDockSize = try {
            AppDockSize.valueOf(dockSizeName)
        } catch (e: Exception) {
            AppDockSize.COMPACT_56
        }

        return LauncherSettings(
            wallpaperType = wallpaperType,
            customWallpaperUri = prefs.getString("custom_wallpaper_uri", null),
            themeAccent = themeAccent,
            appDockSize = appDockSize,
            dimLevel = prefs.getFloat("dim_level", 0.35f),
            selectedCity = prefs.getString("selected_city", "Jakarta (WIB)") ?: "Jakarta (WIB)",
            useAutoLocation = prefs.getBoolean("use_auto_location", true)
        )
    }

    private fun loadFavorites(): Set<String> {
        return prefs.getStringSet("favorite_packages", emptySet()) ?: emptySet()
    }

    fun updateSettings(newSettings: LauncherSettings) {
        prefs.edit()
            .putString("wallpaper_type", newSettings.wallpaperType.name)
            .putString("custom_wallpaper_uri", newSettings.customWallpaperUri)
            .putString("theme_accent", newSettings.themeAccent.name)
            .putString("dock_size", newSettings.appDockSize.name)
            .putFloat("dim_level", newSettings.dimLevel)
            .putString("selected_city", newSettings.selectedCity)
            .putBoolean("use_auto_location", newSettings.useAutoLocation)
            .apply()
        _settings.value = newSettings
    }

    fun toggleFavorite(packageName: String) {
        val current = _favorites.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        prefs.edit().putStringSet("favorite_packages", current).apply()
        _favorites.value = current
    }

    fun setCustomWallpaper(uriString: String) {
        val updated = _settings.value.copy(
            wallpaperType = WallpaperType.CUSTOM_PHOTO,
            customWallpaperUri = uriString
        )
        updateSettings(updated)
    }
}
