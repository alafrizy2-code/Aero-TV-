package com.example.theme

import android.content.Context

/**
 * ThemeManager:
 * Pengelola pusat tema aktif launcher: menyimpan pilihan tema secara permanen di SharedPreferences,
 * melindungi tema lama sebagai Default Theme, dan menerapkan perubahan tema secara langsung tanpa restart.
 */
object ThemeManager {
    private const val PREFS_THEME = "aero_theme_prefs"
    private const val KEY_ACTIVE_THEME_ID = "key_active_theme_id"
    private const val KEY_GOOGLE_TV_HOME_DEFAULT = "key_google_tv_use_home_screen_default"
    private const val KEY_NEXFY_HOME_DEFAULT = "key_nexfy_use_home_screen_default"
    private const val KEY_PROJECTIVY_HOME_DEFAULT = "key_projectivy_use_home_screen_default"
    private const val KEY_ATV_HOME_DEFAULT = "key_atv_use_home_screen_default"
    private const val KEY_WOLF_HOME_DEFAULT = "key_wolf_use_home_screen_default"

    // Pengaturan Kartu ATV Launcher
    private const val KEY_ATV_COLUMNS = "key_atv_card_columns"
    private const val KEY_ATV_CARD_SIZE_SCALE = "key_atv_card_size_scale"
    private const val KEY_ATV_SPACING_DP = "key_atv_card_spacing_dp"
    private const val KEY_ATV_CORNER_RADIUS_DP = "key_atv_card_corner_radius_dp"
    private const val KEY_ATV_BG_COLOR_MODE = "key_atv_card_bg_color_mode"
    private const val KEY_ATV_BG_ALPHA_PERCENT = "key_atv_card_bg_alpha_percent"
    private const val KEY_ATV_FOCUS_EFFECT = "key_atv_card_focus_effect"
    private const val KEY_ATV_ICON_SIZE_DP = "key_atv_card_icon_size_dp"
    private const val KEY_ATV_FAVORITES_ONLY_HOME = "key_atv_favorites_only_home"
    private const val KEY_ATV_FAVORITE_PACKAGES = "key_atv_favorite_packages"

    // Pengaturan Tampilan Wolf Launcher
    private const val KEY_WOLF_SHOW_WEATHER = "key_wolf_show_weather"
    private const val KEY_WOLF_SHOW_STORAGE = "key_wolf_show_storage"
    private const val KEY_WOLF_SHOW_CLOCK = "key_wolf_show_clock"
    private const val KEY_WOLF_WIDGET_SCALE = "key_wolf_widget_scale"
    private const val KEY_WOLF_DOCK_ALPHA_PERCENT = "key_wolf_dock_alpha_percent"
    private const val KEY_WOLF_ICON_SIZE_DP = "key_wolf_icon_size_dp"
    private const val KEY_WOLF_CARD_SPACING_DP = "key_wolf_card_spacing_dp"
    private const val KEY_WOLF_FOCUS_EFFECT = "key_wolf_focus_effect"
    private const val KEY_WOLF_VISIBLE_DOCK_COUNT = "key_wolf_visible_dock_count"
    private const val KEY_WOLF_DOCK_LABEL_MODE = "key_wolf_dock_label_mode"
    private const val KEY_WOLF_DOCK_FAVORITES = "key_wolf_dock_favorites"

    enum class AtvBgColorMode(val label: String) {
        AUTO_BRAND("Otomatis Warna Ikon (Modern)"),
        ATV_PALETTE("Palet Warna ATV"),
        DARK_GLASS("Gelap Transparan"),
        BLUE_TECH("Biru Modern"),
        SLATE_MONO("Abu-abu Elegan")
    }

    enum class AtvFocusEffect(val label: String) {
        WHITE_OUTLINE_ZOOM("Outline Putih + Zoom"),
        ORANGE_BORDER_ZOOM("Bingkai Oranye ATV + Zoom"),
        CYAN_GLOW_ZOOM("Garis Cyan + Zoom"),
        BRIGHT_LIFT_ONLY("Zoom & Elevasi Tanpa Bingkai")
    }

    data class AtvCardSettings(
        val columns: Int = 6,
        val cardSizeScale: Float = 1.0f,
        val spacingDp: Int = 10,
        val cornerRadiusDp: Int = 4,
        val bgColorMode: AtvBgColorMode = AtvBgColorMode.AUTO_BRAND,
        val bgAlphaPercent: Int = 92,
        val focusEffect: AtvFocusEffect = AtvFocusEffect.WHITE_OUTLINE_ZOOM,
        val iconSizeDp: Int = 46,
        val showFavoritesOnlyOnHome: Boolean = false
    )

    enum class WolfFocusEffect(val label: String) {
        WHITE_CYAN_GLOW("Outline Putih-Cyan + Zoom"),
        WHITE_RING_ZOOM("Outline Putih Tegas + Zoom"),
        BOTTOM_ACCENT_BAR("Garis Bawah Putih + Zoom"),
        SOFT_LIFT_ONLY("Zoom & Elevasi Tanpa Bingkai")
    }

    enum class WolfDockLabelMode(val label: String) {
        ON_FOCUS("Saat Fokus Saja"),
        ALWAYS("Selalu Tampil"),
        HIDDEN("Sembunyikan Nama")
    }

    data class WolfDisplaySettings(
        val showWeatherWidget: Boolean = true,
        val showStorageInfo: Boolean = true,
        val showClockAndDate: Boolean = true,
        val widgetSizeScale: Float = 1.0f,
        val dockAlphaPercent: Int = 62,
        val iconSizeDp: Int = 58,
        val cardSpacingDp: Int = 14,
        val focusEffect: WolfFocusEffect = WolfFocusEffect.WHITE_CYAN_GLOW,
        val visibleDockAppsCount: Int = 9, // 0 = Semua aplikasi terpasang
        val dockLabelMode: WolfDockLabelMode = WolfDockLabelMode.ON_FOCUS
    )

    fun getActiveThemeId(context: Context): ThemeId {
        val raw = context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_THEME_ID, ThemeId.DEFAULT.name)
        return try {
            ThemeId.valueOf(raw ?: ThemeId.DEFAULT.name)
        } catch (_: Exception) {
            ThemeId.DEFAULT
        }
    }

    fun getActiveTheme(context: Context): LauncherThemeDefinition {
        return ThemeRegistry.getThemeById(getActiveThemeId(context))
    }

    fun setActiveTheme(context: Context, themeId: ThemeId): LauncherThemeDefinition {
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACTIVE_THEME_ID, themeId.name)
            .apply()
        return ThemeRegistry.getThemeById(themeId)
    }

    fun restoreDefaultTheme(context: Context): LauncherThemeDefinition {
        return setActiveTheme(context, ThemeId.DEFAULT)
    }

    /**
     * Pengaturan khusus Google TV Theme: Use Home Screen Default (ON/OFF).
     * Disimpan secara permanen di SharedPreferences tanpa memengaruhi tema lain.
     */
    fun isGoogleTvUseHomeScreenDefault(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getBoolean(KEY_GOOGLE_TV_HOME_DEFAULT, true)
    }

    fun setGoogleTvUseHomeScreenDefault(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_GOOGLE_TV_HOME_DEFAULT, enabled)
            .apply()
    }

    /**
     * Pengaturan khusus Nexfy Launcher Theme: Use Home Screen Default (ON/OFF).
     * Disimpan secara permanen dan terpisah dari tema Google TV maupun tema lainnya.
     */
    fun isNexfyUseHomeScreenDefault(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getBoolean(KEY_NEXFY_HOME_DEFAULT, true)
    }

    fun setNexfyUseHomeScreenDefault(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NEXFY_HOME_DEFAULT, enabled)
            .apply()
    }

    /**
     * Pengaturan khusus Projectivy Launcher Theme: Use Home Screen Default (ON/OFF).
     * Disimpan secara permanen dan terpisah dari Google TV maupun Nexfy Theme.
     */
    fun isProjectivyUseHomeScreenDefault(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getBoolean(KEY_PROJECTIVY_HOME_DEFAULT, true)
    }

    fun setProjectivyUseHomeScreenDefault(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_PROJECTIVY_HOME_DEFAULT, enabled)
            .apply()
    }

    /**
     * Pengaturan khusus ATV Launcher Theme: Use Home Screen Default (ON/OFF).
     * Disimpan secara permanen dan terpisah dari tema lain.
     */
    fun isAtvUseHomeScreenDefault(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ATV_HOME_DEFAULT, true)
    }

    fun setAtvUseHomeScreenDefault(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ATV_HOME_DEFAULT, enabled)
            .apply()
    }

    /**
     * Memeriksa apakah tema aktif saat ini menggunakan tata letak Home Screen khusus Google TV.
     */
    fun shouldUseGoogleTvHomeLayout(context: Context, activeTheme: LauncherThemeDefinition = getActiveTheme(context)): Boolean {
        return activeTheme.id == ThemeId.GOOGLE_TV && isGoogleTvUseHomeScreenDefault(context)
    }

    /**
     * Memeriksa apakah tema aktif saat ini menggunakan tata letak Home Screen khusus Nexfy.
     */
    fun shouldUseNexfyHomeLayout(context: Context, activeTheme: LauncherThemeDefinition = getActiveTheme(context)): Boolean {
        return activeTheme.id == ThemeId.NEXFY && isNexfyUseHomeScreenDefault(context)
    }

    /**
     * Memeriksa apakah tema aktif saat ini menggunakan tata letak Home Screen khusus Projectivy.
     */
    fun shouldUseProjectivyHomeLayout(context: Context, activeTheme: LauncherThemeDefinition = getActiveTheme(context)): Boolean {
        return activeTheme.id == ThemeId.PROJECTIVY && isProjectivyUseHomeScreenDefault(context)
    }

    /**
     * Memeriksa apakah tema aktif saat ini menggunakan tata letak Home Screen khusus ATV Launcher.
     */
    fun shouldUseAtvHomeLayout(context: Context, activeTheme: LauncherThemeDefinition = getActiveTheme(context)): Boolean {
        return activeTheme.id == ThemeId.ATV_LAUNCHER && isAtvUseHomeScreenDefault(context)
    }

    /**
     * Pengaturan khusus Wolf Launcher Theme: Use Home Screen Default (ON/OFF).
     * Disimpan secara permanen dan terpisah dari tema lain.
     */
    fun isWolfUseHomeScreenDefault(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getBoolean(KEY_WOLF_HOME_DEFAULT, true)
    }

    fun setWolfUseHomeScreenDefault(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WOLF_HOME_DEFAULT, enabled)
            .apply()
    }

    /**
     * Memeriksa apakah tema aktif saat ini menggunakan tata letak Home Screen khusus Wolf Launcher.
     */
    fun shouldUseWolfHomeLayout(context: Context, activeTheme: LauncherThemeDefinition = getActiveTheme(context)): Boolean {
        return activeTheme.id == ThemeId.WOLF_LAUNCHER && isWolfUseHomeScreenDefault(context)
    }

    fun getAtvCardSettings(context: Context): AtvCardSettings {
        val prefs = context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
        val colorMode = try {
            AtvBgColorMode.valueOf(prefs.getString(KEY_ATV_BG_COLOR_MODE, AtvBgColorMode.AUTO_BRAND.name) ?: AtvBgColorMode.AUTO_BRAND.name)
        } catch (_: Exception) {
            AtvBgColorMode.AUTO_BRAND
        }
        val focusEffect = try {
            AtvFocusEffect.valueOf(prefs.getString(KEY_ATV_FOCUS_EFFECT, AtvFocusEffect.WHITE_OUTLINE_ZOOM.name) ?: AtvFocusEffect.WHITE_OUTLINE_ZOOM.name)
        } catch (_: Exception) {
            AtvFocusEffect.WHITE_OUTLINE_ZOOM
        }
        return AtvCardSettings(
            columns = prefs.getInt(KEY_ATV_COLUMNS, 6).coerceIn(4, 8),
            cardSizeScale = prefs.getFloat(KEY_ATV_CARD_SIZE_SCALE, 1.0f).coerceIn(0.82f, 1.28f),
            spacingDp = prefs.getInt(KEY_ATV_SPACING_DP, 10).coerceIn(4, 24),
            cornerRadiusDp = prefs.getInt(KEY_ATV_CORNER_RADIUS_DP, 4).coerceIn(0, 20),
            bgColorMode = colorMode,
            bgAlphaPercent = prefs.getInt(KEY_ATV_BG_ALPHA_PERCENT, 92).coerceIn(30, 100),
            focusEffect = focusEffect,
            iconSizeDp = prefs.getInt(KEY_ATV_ICON_SIZE_DP, 46).coerceIn(30, 68),
            showFavoritesOnlyOnHome = prefs.getBoolean(KEY_ATV_FAVORITES_ONLY_HOME, false)
        )
    }

    fun saveAtvCardSettings(context: Context, settings: AtvCardSettings) {
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_ATV_COLUMNS, settings.columns.coerceIn(4, 8))
            .putFloat(KEY_ATV_CARD_SIZE_SCALE, settings.cardSizeScale.coerceIn(0.82f, 1.28f))
            .putInt(KEY_ATV_SPACING_DP, settings.spacingDp.coerceIn(4, 24))
            .putInt(KEY_ATV_CORNER_RADIUS_DP, settings.cornerRadiusDp.coerceIn(0, 20))
            .putString(KEY_ATV_BG_COLOR_MODE, settings.bgColorMode.name)
            .putInt(KEY_ATV_BG_ALPHA_PERCENT, settings.bgAlphaPercent.coerceIn(30, 100))
            .putString(KEY_ATV_FOCUS_EFFECT, settings.focusEffect.name)
            .putInt(KEY_ATV_ICON_SIZE_DP, settings.iconSizeDp.coerceIn(30, 68))
            .putBoolean(KEY_ATV_FAVORITES_ONLY_HOME, settings.showFavoritesOnlyOnHome)
            .apply()
    }

    fun resetAtvCardSettings(context: Context): AtvCardSettings {
        val defaults = AtvCardSettings()
        saveAtvCardSettings(context, defaults)
        return defaults
    }

    fun getAtvFavoritePackages(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getString(KEY_ATV_FAVORITE_PACKAGES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isAtvFavorite(context: Context, packageName: String): Boolean {
        return getAtvFavoritePackages(context).contains(packageName)
    }

    fun toggleAtvFavorite(context: Context, packageName: String): Boolean {
        val current = getAtvFavoritePackages(context).toMutableList()
        val nowFavorite = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        saveAtvFavoritePackages(context, current)
        return nowFavorite
    }

    fun moveAtvFavorite(context: Context, packageName: String, delta: Int): Boolean {
        val current = getAtvFavoritePackages(context).toMutableList()
        val idx = current.indexOf(packageName)
        if (idx < 0) return false
        val target = (idx + delta).coerceIn(0, current.lastIndex)
        if (target == idx) return false
        val item = current.removeAt(idx)
        current.add(target, item)
        saveAtvFavoritePackages(context, current)
        return true
    }

    private fun saveAtvFavoritePackages(context: Context, list: List<String>) {
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ATV_FAVORITE_PACKAGES, list.joinToString("|"))
            .apply()
    }

    fun orderAppsForAtv(
        context: Context,
        apps: List<com.example.AppModel>,
        favoritesOnly: Boolean = false
    ): List<com.example.AppModel> {
        val favs = getAtvFavoritePackages(context)
        if (favoritesOnly) {
            if (favs.isEmpty()) return apps.take(12)
            val favIndex = favs.withIndex().associate { (i, pkg) -> pkg to i }
            val matched = apps.filter { favIndex.containsKey(it.packageName) }
                .sortedBy { favIndex[it.packageName] ?: Int.MAX_VALUE }
            return if (matched.isNotEmpty()) matched else apps.take(12)
        }
        if (favs.isEmpty()) return apps
        val favIndex = favs.withIndex().associate { (i, pkg) -> pkg to i }
        val pinned = apps.filter { favIndex.containsKey(it.packageName) }
            .sortedBy { favIndex[it.packageName] ?: Int.MAX_VALUE }
        val rest = apps.filter { !favIndex.containsKey(it.packageName) }
        return pinned + rest
    }

    fun getWolfDisplaySettings(context: Context): WolfDisplaySettings {
        val prefs = context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
        val focusEffect = try {
            WolfFocusEffect.valueOf(
                prefs.getString(KEY_WOLF_FOCUS_EFFECT, WolfFocusEffect.WHITE_CYAN_GLOW.name)
                    ?: WolfFocusEffect.WHITE_CYAN_GLOW.name
            )
        } catch (_: Exception) {
            WolfFocusEffect.WHITE_CYAN_GLOW
        }
        val labelMode = try {
            WolfDockLabelMode.valueOf(
                prefs.getString(KEY_WOLF_DOCK_LABEL_MODE, WolfDockLabelMode.ON_FOCUS.name)
                    ?: WolfDockLabelMode.ON_FOCUS.name
            )
        } catch (_: Exception) {
            WolfDockLabelMode.ON_FOCUS
        }
        val rawVisibleCount = prefs.getInt(KEY_WOLF_VISIBLE_DOCK_COUNT, 9)
        val safeVisibleCount = if (rawVisibleCount <= 0) 0 else rawVisibleCount.coerceIn(4, 14)
        return WolfDisplaySettings(
            showWeatherWidget = prefs.getBoolean(KEY_WOLF_SHOW_WEATHER, true),
            showStorageInfo = prefs.getBoolean(KEY_WOLF_SHOW_STORAGE, true),
            showClockAndDate = prefs.getBoolean(KEY_WOLF_SHOW_CLOCK, true),
            widgetSizeScale = prefs.getFloat(KEY_WOLF_WIDGET_SCALE, 1.0f).coerceIn(0.80f, 1.35f),
            dockAlphaPercent = prefs.getInt(KEY_WOLF_DOCK_ALPHA_PERCENT, 62).coerceIn(15, 95),
            iconSizeDp = prefs.getInt(KEY_WOLF_ICON_SIZE_DP, 58).coerceIn(40, 84),
            cardSpacingDp = prefs.getInt(KEY_WOLF_CARD_SPACING_DP, 14).coerceIn(6, 30),
            focusEffect = focusEffect,
            visibleDockAppsCount = safeVisibleCount,
            dockLabelMode = labelMode
        )
    }

    fun saveWolfDisplaySettings(context: Context, settings: WolfDisplaySettings) {
        val safeVisibleCount = if (settings.visibleDockAppsCount <= 0) 0 else settings.visibleDockAppsCount.coerceIn(4, 14)
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_WOLF_SHOW_WEATHER, settings.showWeatherWidget)
            .putBoolean(KEY_WOLF_SHOW_STORAGE, settings.showStorageInfo)
            .putBoolean(KEY_WOLF_SHOW_CLOCK, settings.showClockAndDate)
            .putFloat(KEY_WOLF_WIDGET_SCALE, settings.widgetSizeScale.coerceIn(0.80f, 1.35f))
            .putInt(KEY_WOLF_DOCK_ALPHA_PERCENT, settings.dockAlphaPercent.coerceIn(15, 95))
            .putInt(KEY_WOLF_ICON_SIZE_DP, settings.iconSizeDp.coerceIn(40, 84))
            .putInt(KEY_WOLF_CARD_SPACING_DP, settings.cardSpacingDp.coerceIn(6, 30))
            .putString(KEY_WOLF_FOCUS_EFFECT, settings.focusEffect.name)
            .putInt(KEY_WOLF_VISIBLE_DOCK_COUNT, safeVisibleCount)
            .putString(KEY_WOLF_DOCK_LABEL_MODE, settings.dockLabelMode.name)
            .apply()
    }

    fun resetWolfDisplaySettings(context: Context): WolfDisplaySettings {
        val defaults = WolfDisplaySettings()
        saveWolfDisplaySettings(context, defaults)
        return defaults
    }

    fun getWolfDockFavoritePackages(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getString(KEY_WOLF_DOCK_FAVORITES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isWolfDockFavorite(context: Context, packageName: String): Boolean {
        return getWolfDockFavoritePackages(context).contains(packageName)
    }

    fun toggleWolfDockFavorite(context: Context, packageName: String): Boolean {
        val current = getWolfDockFavoritePackages(context).toMutableList()
        val added = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(packageName)
            true
        }
        saveWolfDockFavorites(context, current)
        return added
    }

    fun moveWolfDockFavorite(context: Context, packageName: String, delta: Int): Boolean {
        val current = getWolfDockFavoritePackages(context).toMutableList()
        val idx = current.indexOf(packageName)
        if (idx < 0) return false
        val target = (idx + delta).coerceIn(0, current.lastIndex)
        if (target == idx) return false
        val item = current.removeAt(idx)
        current.add(target, item)
        saveWolfDockFavorites(context, current)
        return true
    }

    private fun saveWolfDockFavorites(context: Context, list: List<String>) {
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_WOLF_DOCK_FAVORITES, list.joinToString("|"))
            .apply()
    }

    fun orderAppsForWolfDock(
        context: Context,
        apps: List<com.example.AppModel>,
        visibleCount: Int
    ): List<com.example.AppModel> {
        if (apps.isEmpty()) return emptyList()
        val favs = getWolfDockFavoritePackages(context)
        val ordered = if (favs.isEmpty()) {
            apps
        } else {
            val favIndex = favs.withIndex().associate { (i, pkg) -> pkg to i }
            val pinned = apps.filter { favIndex.containsKey(it.packageName) }
                .sortedBy { favIndex[it.packageName] ?: Int.MAX_VALUE }
            val rest = apps.filter { !favIndex.containsKey(it.packageName) }
            pinned + rest
        }
        return if (visibleCount > 0) ordered.take(visibleCount) else ordered
    }

    fun getAllThemes(): List<LauncherThemeDefinition> = ThemeRegistry.allThemes
}
