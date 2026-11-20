package com.example.model

enum class WallpaperType {
    FAMILY_PHOTO,
    AURORA_NIGHT,
    DEEP_SPACE,
    CYBER_NEON,
    OLED_BLACK,
    CUSTOM_PHOTO
}

enum class ThemeAccent(val label: String, val hex: Long) {
    CYAN_CYBER("Cyan Cyber", 0xFF00E5FF),
    EMERALD_FOREST("Emerald Forest", 0xFF10B981),
    ROYAL_INDIGO("Royal Indigo", 0xFF818CF8),
    SUNSET_AMBER("Sunset Amber", 0xFFF59E0B),
    CRIMSON_RED("Crimson Red", 0xFFEF4444),
    MONOLITH_WHITE("Pure Monolith", 0xFFF1F5F9)
}

enum class AppDockSize(val label: String, val iconSizeDp: Int, val itemWidthDp: Int) {
    MINI_48("Extra Kecil", 48, 120),
    COMPACT_56("Kompak Persegi Panjang", 56, 136),
    MEDIUM_68("Sedang", 68, 150)
}

data class LauncherSettings(
    val wallpaperType: WallpaperType = WallpaperType.FAMILY_PHOTO,
    val customWallpaperUri: String? = null,
    val themeAccent: ThemeAccent = ThemeAccent.CYAN_CYBER,
    val appDockSize: AppDockSize = AppDockSize.COMPACT_56,
    val dimLevel: Float = 0.20f,
    val selectedCity: String = "Medan (WIB)",
    val useAutoLocation: Boolean = true,
    val showAnalogClock: Boolean = true
)
