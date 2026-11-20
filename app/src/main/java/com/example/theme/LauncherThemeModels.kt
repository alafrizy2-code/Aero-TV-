package com.example.theme

enum class ThemeId {
    DEFAULT,
    GOOGLE_TV,
    NEXFY,
    PROJECTIVY,
    ATV_LAUNCHER,
    WOLF_LAUNCHER
}

enum class ThemeLayoutType {
    HORIZONTAL_SINGLE_ROW,
    HORIZONTAL_TWO_ROW_GRID
}

enum class CardShapeType {
    GLASS_PILL_RECT,       // Default Aero TV: Kartu kaca horizontal 224x96 (ikon kiri, teks kanan)
    FULL_CIRCLE_TILE,      // Google TV: Kartu bulat penuh tanpa kotak latar belakang + label di bawah
    SQUIRCLE_GLASS_TILE,   // Nexfy: Ubin kaca squircle elegan + ikon tengah + label di bawah dalam kartu
    BANNER_16_9_FULL_BLEED,// Projectivy: Banner persegi panjang 16:9 penuh tanpa kotak latar ikon default
    ATV_BANNER_GRID_TILE,  // ATV Launcher: Ubin banner persegi panjang klasik 2 baris penuh warna/brand
    WOLF_BANNER_TILE       // Wolf Launcher: Banner persegi panjang 2 baris dengan fokus bevel putih-cyan
}

enum class CardLabelPlacement {
    INSIDE_RIGHT,          // Default style (ikon kiri, teks kanan)
    INSIDE_BOTTOM_STACK,   // Nexfy (ikon di atas, teks di bawah di dalam squircle)
    BANNER_INTEGRATED,     // ATV & Wolf (banner full-bleed sudah memuat logo + judul/artwork)
    BELOW_CARD_ALWAYS,     // Google TV (caption selalu terlihat di bawah kartu bulat)
    BELOW_CARD_FOCUS_ONLY  // Projectivy (caption bersih muncul di bawah banner hanya saat fokus)
}

enum class IconFrameStyle {
    ORIGINAL_CLEAN,        // Ikon asli bersih tanpa kotak paksa
    FULL_CIRCLE_ART,       // Google TV: artwork/ikon penuh mengisi lingkaran kartu
    FULL_BANNER_ART        // Projectivy, ATV, Wolf: menggunakan banner 16:9 penuh atau banner brand sintetis
}

enum class FocusEffectStyle {
    TURQUOISE_GLOW_FRAME,
    GOOGLE_TV_WHITE_RING,
    NEXFY_NEON_DUAL_BORDER,
    PROJECTIVY_LUMINOUS_HALO,
    ATV_ORANGE_CRISP_OUTLINE,
    WOLF_WHITE_CYAN_BEVEL
}

data class ThemeLayoutConfig(
    val layoutType: ThemeLayoutType,
    val spanCount: Int,
    val cardWidthPx1080: Float,
    val cardHeightPx1080: Float,
    val gapHorizontalPx1080: Float,
    val gapVerticalPx1080: Float,
    val edgePaddingHorizontalPx1080: Float,
    val edgePaddingVerticalPx1080: Float,
    val bottomMarginDp: Int,
    val headerTitle: String?,
    val supportsDownAppDrawer: Boolean,
    val drawerColumns: Int
)

data class ThemeCardStyleConfig(
    val shapeType: CardShapeType,
    val cornerRadiusPx1080: Float,
    val normalBgStartColor: Int,
    val normalBgEndColor: Int,
    val focusedBgStartColor: Int,
    val focusedBgEndColor: Int,
    val normalBorderColor: Int,
    val normalBorderWidthPx1080: Float,
    val focusedBorderColor: Int,
    val focusedBorderAccentColor: Int,
    val focusedBorderWidthPx1080: Float,
    val labelPlacement: CardLabelPlacement,
    val labelColorNormal: Int,
    val labelColorFocused: Int,
    val labelSizePx1080: Float,
    val hasTopSheen: Boolean
)

data class ThemeIconConfig(
    val iconSizePx1080: Float,
    val frameStyle: IconFrameStyle,
    val useOnlineCatalog: Boolean
)

data class ThemeFocusConfig(
    val effectStyle: FocusEffectStyle,
    val scaleFocused: Float,
    val animDurationMs: Long,
    val elevationFocused: Float,
    val elevationNormal: Float,
    val unfocusedCardAlpha: Float
)

data class LauncherThemeDefinition(
    val id: ThemeId,
    val name: String,
    val subtitle: String,
    val description: String,
    val previewPrimaryColor: Int,
    val previewAccentColor: Int,
    val layout: ThemeLayoutConfig,
    val cardStyle: ThemeCardStyleConfig,
    val iconConfig: ThemeIconConfig,
    val focusConfig: ThemeFocusConfig
)
