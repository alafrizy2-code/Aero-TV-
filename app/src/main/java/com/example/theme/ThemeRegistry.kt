package com.example.theme

import android.graphics.Color

/**
 * Registri Deklaratif Tema Launcher.
 * Setiap tema memiliki bentuk, proporsi ukuran, gaya kartu penuh (bulat tanpa background untuk Google TV,
 * banner persegi panjang full-bleed untuk Projectivy / ATV / Wolf, squircle kaca untuk Nexfy,
 * serta mempertahankan Default Theme Aero TV tetap utuh 100%).
 */
object ThemeRegistry {

    val allThemes: List<LauncherThemeDefinition> = listOf(
        // 0. DEFAULT THEME (Tema Bawaan Aero TV - Wajib Dipertahankan Utuh)
        LauncherThemeDefinition(
            id = ThemeId.DEFAULT,
            name = "Default Theme (Aero TV)",
            subtitle = "Tema Bawaan • Kartu Kaca Horizontal",
            description = "Desain bawaan Aero TV: 1 baris horizontal di bawah layar (224×96 px), latar kaca transparan, ikon di kiri, label putih di kanan, serta garis fokus cyan #2DD4BF.",
            previewPrimaryColor = Color.parseColor("#0B101B"),
            previewAccentColor = Color.parseColor("#2DD4BF"),
            layout = ThemeLayoutConfig(
                layoutType = ThemeLayoutType.HORIZONTAL_SINGLE_ROW,
                spanCount = 1,
                cardWidthPx1080 = 224f,
                cardHeightPx1080 = 96f,
                gapHorizontalPx1080 = 16f,
                gapVerticalPx1080 = 16f,
                edgePaddingHorizontalPx1080 = 76f,
                edgePaddingVerticalPx1080 = 16f,
                bottomMarginDp = 20,
                headerTitle = null,
                supportsDownAppDrawer = false,
                drawerColumns = 6
            ),
            cardStyle = ThemeCardStyleConfig(
                shapeType = CardShapeType.GLASS_PILL_RECT,
                cornerRadiusPx1080 = 4f,
                normalBgStartColor = Color.parseColor("#880B101B"),
                normalBgEndColor = Color.parseColor("#880B101B"),
                focusedBgStartColor = Color.parseColor("#8858687E"),
                focusedBgEndColor = Color.parseColor("#8858687E"),
                normalBorderColor = Color.parseColor("#44FFFFFF"),
                normalBorderWidthPx1080 = 1.5f,
                focusedBorderColor = Color.parseColor("#2DD4BF"),
                focusedBorderAccentColor = Color.parseColor("#2DD4BF"),
                focusedBorderWidthPx1080 = 4.5f,
                labelPlacement = CardLabelPlacement.INSIDE_RIGHT,
                labelColorNormal = Color.WHITE,
                labelColorFocused = Color.WHITE,
                labelSizePx1080 = 20f,
                hasTopSheen = false
            ),
            iconConfig = ThemeIconConfig(
                iconSizePx1080 = 46f,
                frameStyle = IconFrameStyle.ORIGINAL_CLEAN,
                useOnlineCatalog = false
            ),
            focusConfig = ThemeFocusConfig(
                effectStyle = FocusEffectStyle.TURQUOISE_GLOW_FRAME,
                scaleFocused = 1.12f,
                animDurationMs = 110L,
                elevationFocused = 16f,
                elevationNormal = 2f,
                unfocusedCardAlpha = 1.0f
            )
        ),

        // 1. GOOGLE TV THEME (Kartu Bulat Penuh Tanpa Latar Kotak)
        LauncherThemeDefinition(
            id = ThemeId.GOOGLE_TV,
            name = "Google TV Theme",
            subtitle = "Home Google TV (ON/OFF) • Google TV Apps • Ikon Bulat",
            description = "Desain otentik Google TV dengan opsi khusus 'Use Home Screen Default' (ON: tata letak penuh Google TV Home + tab Beranda & Aplikasi; OFF: tata letak dasar Aero TV dengan deretan ikon bulat Google TV) serta halaman Google TV Apps.",
            previewPrimaryColor = Color.parseColor("#111827"),
            previewAccentColor = Color.parseColor("#FFFFFF"),
            layout = ThemeLayoutConfig(
                layoutType = ThemeLayoutType.HORIZONTAL_SINGLE_ROW,
                spanCount = 1,
                cardWidthPx1080 = 122f,
                cardHeightPx1080 = 122f,
                gapHorizontalPx1080 = 26f,
                gapVerticalPx1080 = 22f,
                edgePaddingHorizontalPx1080 = 76f,
                edgePaddingVerticalPx1080 = 18f,
                bottomMarginDp = 16,
                headerTitle = "Aplikasi Anda  •  Tekan ↓ untuk App Drawer",
                supportsDownAppDrawer = true,
                drawerColumns = 8
            ),
            cardStyle = ThemeCardStyleConfig(
                shapeType = CardShapeType.FULL_CIRCLE_TILE,
                cornerRadiusPx1080 = 61f,
                normalBgStartColor = Color.TRANSPARENT,
                normalBgEndColor = Color.TRANSPARENT,
                focusedBgStartColor = Color.TRANSPARENT,
                focusedBgEndColor = Color.TRANSPARENT,
                normalBorderColor = Color.parseColor("#33FFFFFF"),
                normalBorderWidthPx1080 = 1.5f,
                focusedBorderColor = Color.parseColor("#FFFFFF"),
                focusedBorderAccentColor = Color.parseColor("#E0F2FE"),
                focusedBorderWidthPx1080 = 4.0f,
                labelPlacement = CardLabelPlacement.BELOW_CARD_ALWAYS,
                labelColorNormal = Color.parseColor("#E2E8F0"),
                labelColorFocused = Color.WHITE,
                labelSizePx1080 = 17f,
                hasTopSheen = false
            ),
            iconConfig = ThemeIconConfig(
                iconSizePx1080 = 122f,
                frameStyle = IconFrameStyle.FULL_CIRCLE_ART,
                useOnlineCatalog = true
            ),
            focusConfig = ThemeFocusConfig(
                effectStyle = FocusEffectStyle.GOOGLE_TV_WHITE_RING,
                scaleFocused = 1.14f,
                animDurationMs = 135L,
                elevationFocused = 18f,
                elevationNormal = 2f,
                unfocusedCardAlpha = 0.94f
            )
        ),

        // 2. NEXFY LAUNCHER THEME (Home Nexfy ON/OFF • All Apps 6 Kolom • Kartu Landscape Kaca)
        LauncherThemeDefinition(
            id = ThemeId.NEXFY,
            name = "Nexfy Launcher Theme",
            subtitle = "Home Nexfy (ON/OFF) • All Apps 6 Kolom • Kartu Landscape",
            description = "Desain otentik Nexfy Launcher: bar status sistem & navigasi 8 kategori di atas, wallpaper terbuka luas di tengah, kartu landscape dengan ikon & nama aplikasi di bawah, opsi 'Use Home Screen Default' (ON/OFF), dan halaman All Apps 6 kolom.",
            previewPrimaryColor = Color.parseColor("#0B1120"),
            previewAccentColor = Color.parseColor("#38BDF8"),
            layout = ThemeLayoutConfig(
                layoutType = ThemeLayoutType.HORIZONTAL_SINGLE_ROW,
                spanCount = 1,
                cardWidthPx1080 = 276f,
                cardHeightPx1080 = 156f,
                gapHorizontalPx1080 = 20f,
                gapVerticalPx1080 = 18f,
                edgePaddingHorizontalPx1080 = 72f,
                edgePaddingVerticalPx1080 = 16f,
                bottomMarginDp = 18,
                headerTitle = "Semua Aplikasi  •  Tekan ↓ untuk All Apps",
                supportsDownAppDrawer = true,
                drawerColumns = 6
            ),
            cardStyle = ThemeCardStyleConfig(
                shapeType = CardShapeType.SQUIRCLE_GLASS_TILE,
                cornerRadiusPx1080 = 16f,
                normalBgStartColor = Color.parseColor("#B30F172A"),
                normalBgEndColor = Color.parseColor("#CC1E1B4B"),
                focusedBgStartColor = Color.parseColor("#E6172554"),
                focusedBgEndColor = Color.parseColor("#E6312E81"),
                normalBorderColor = Color.parseColor("#33FFFFFF"),
                normalBorderWidthPx1080 = 1.5f,
                focusedBorderColor = Color.parseColor("#FFFFFF"),
                focusedBorderAccentColor = Color.parseColor("#38BDF8"),
                focusedBorderWidthPx1080 = 4.0f,
                labelPlacement = CardLabelPlacement.BANNER_INTEGRATED,
                labelColorNormal = Color.parseColor("#E2E8F0"),
                labelColorFocused = Color.WHITE,
                labelSizePx1080 = 16f,
                hasTopSheen = true
            ),
            iconConfig = ThemeIconConfig(
                iconSizePx1080 = 64f,
                frameStyle = IconFrameStyle.FULL_BANNER_ART,
                useOnlineCatalog = true
            ),
            focusConfig = ThemeFocusConfig(
                effectStyle = FocusEffectStyle.NEXFY_NEON_DUAL_BORDER,
                scaleFocused = 1.09f,
                animDurationMs = 110L,
                elevationFocused = 18f,
                elevationNormal = 4f,
                unfocusedCardAlpha = 0.94f
            )
        ),

        // 3. PROJECTIVY LAUNCHER THEME (Home Projectivy ON/OFF • TV Apps 6 Kolom • Watch Next)
        LauncherThemeDefinition(
            id = ThemeId.PROJECTIVY,
            name = "Projectivy Launcher Theme",
            subtitle = "Home Projectivy (ON/OFF) • TV Apps 6 Kolom • Watch Next",
            description = "Desain otentik Projectivy Launcher: deretan Apps minimalis, kategori sumber konten & Watch Next nyata di atas wallpaper luas, opsi 'Use Home Screen Default' (ON/OFF), serta halaman TV Apps 6 kolom dengan ikon di tengah kartu & outline putih fokus.",
            previewPrimaryColor = Color.parseColor("#090D16"),
            previewAccentColor = Color.parseColor("#FFFFFF"),
            layout = ThemeLayoutConfig(
                layoutType = ThemeLayoutType.HORIZONTAL_SINGLE_ROW,
                spanCount = 1,
                cardWidthPx1080 = 288f,
                cardHeightPx1080 = 162f,
                gapHorizontalPx1080 = 20f,
                gapVerticalPx1080 = 20f,
                edgePaddingHorizontalPx1080 = 76f,
                edgePaddingVerticalPx1080 = 18f,
                bottomMarginDp = 14,
                headerTitle = "Apps  •  Tekan ↓ untuk TV Apps",
                supportsDownAppDrawer = true,
                drawerColumns = 6
            ),
            cardStyle = ThemeCardStyleConfig(
                shapeType = CardShapeType.BANNER_16_9_FULL_BLEED,
                cornerRadiusPx1080 = 12f,
                normalBgStartColor = Color.TRANSPARENT,
                normalBgEndColor = Color.TRANSPARENT,
                focusedBgStartColor = Color.TRANSPARENT,
                focusedBgEndColor = Color.TRANSPARENT,
                normalBorderColor = Color.parseColor("#22FFFFFF"),
                normalBorderWidthPx1080 = 1.0f,
                focusedBorderColor = Color.parseColor("#FFFFFF"),
                focusedBorderAccentColor = Color.parseColor("#F59E0B"),
                focusedBorderWidthPx1080 = 4.0f,
                labelPlacement = CardLabelPlacement.BELOW_CARD_FOCUS_ONLY,
                labelColorNormal = Color.TRANSPARENT,
                labelColorFocused = Color.WHITE,
                labelSizePx1080 = 18f,
                hasTopSheen = false
            ),
            iconConfig = ThemeIconConfig(
                iconSizePx1080 = 288f,
                frameStyle = IconFrameStyle.FULL_BANNER_ART,
                useOnlineCatalog = true
            ),
            focusConfig = ThemeFocusConfig(
                effectStyle = FocusEffectStyle.PROJECTIVY_LUMINOUS_HALO,
                scaleFocused = 1.12f,
                animDurationMs = 125L,
                elevationFocused = 24f,
                elevationNormal = 2f,
                unfocusedCardAlpha = 0.90f
            )
        ),

        // 4. ATV LAUNCHER THEME (Home ATV ON/OFF • Applications 6 Kolom • Kartu Landscape Berwarna)
        LauncherThemeDefinition(
            id = ThemeId.ATV_LAUNCHER,
            name = "ATV Launcher Theme",
            subtitle = "Home ATV (ON/OFF) • Applications 6 Kolom • Kartu Landscape",
            description = "Desain otentik ATV Launcher terbaru: area wallpaper terbuka luas di atas, grid Applications 6 kolom kartu landscape modern berwarna (ikon di kiri, nama di kanan), pengaturan lengkap kartu & fokus, opsi 'Use Home Screen Default' (ON/OFF), dan susunan aplikasi favorit.",
            previewPrimaryColor = Color.parseColor("#111827"),
            previewAccentColor = Color.parseColor("#1E88E5"),
            layout = ThemeLayoutConfig(
                layoutType = ThemeLayoutType.HORIZONTAL_TWO_ROW_GRID,
                spanCount = 2,
                cardWidthPx1080 = 264f,
                cardHeightPx1080 = 152f,
                gapHorizontalPx1080 = 16f,
                gapVerticalPx1080 = 14f,
                edgePaddingHorizontalPx1080 = 72f,
                edgePaddingVerticalPx1080 = 12f,
                bottomMarginDp = 14,
                headerTitle = "Applications  •  Tekan ↓ di Baris Bawah untuk Halaman Applications",
                supportsDownAppDrawer = true,
                drawerColumns = 6
            ),
            cardStyle = ThemeCardStyleConfig(
                shapeType = CardShapeType.ATV_BANNER_GRID_TILE,
                cornerRadiusPx1080 = 4f,
                normalBgStartColor = Color.parseColor("#EB1E88E5"),
                normalBgEndColor = Color.parseColor("#EB1565C0"),
                focusedBgStartColor = Color.parseColor("#FF2196F3"),
                focusedBgEndColor = Color.parseColor("#FF1976D2"),
                normalBorderColor = Color.parseColor("#2EFFFFFF"),
                normalBorderWidthPx1080 = 1.0f,
                focusedBorderColor = Color.parseColor("#FFFFFF"),
                focusedBorderAccentColor = Color.parseColor("#E0F2FE"),
                focusedBorderWidthPx1080 = 4.0f,
                labelPlacement = CardLabelPlacement.INSIDE_RIGHT,
                labelColorNormal = Color.WHITE,
                labelColorFocused = Color.WHITE,
                labelSizePx1080 = 19f,
                hasTopSheen = true
            ),
            iconConfig = ThemeIconConfig(
                iconSizePx1080 = 66f,
                frameStyle = IconFrameStyle.ORIGINAL_CLEAN,
                useOnlineCatalog = true
            ),
            focusConfig = ThemeFocusConfig(
                effectStyle = FocusEffectStyle.ATV_ORANGE_CRISP_OUTLINE,
                scaleFocused = 1.09f,
                animDurationMs = 95L,
                elevationFocused = 18f,
                elevationNormal = 3f,
                unfocusedCardAlpha = 1.0f
            )
        ),

        // 5. WOLF LAUNCHER THEME (Home Wolf ON/OFF • Cuaca & Storage • Dock Transparan + All Apps)
        LauncherThemeDefinition(
            id = ThemeId.WOLF_LAUNCHER,
            name = "Wolf Launcher Theme",
            subtitle = "Home Wolf (ON/OFF) • Cuaca & Storage • Dock Transparan",
            description = "Desain otentik Wolf Launcher: widget cuaca & penyimpanan perangkat aktual di kiri atas, jam digital besar & status sistem di kanan atas, dock transparan membulat di bawah + tombol All Apps, serta opsi 'Use Home Screen Default' (ON/OFF).",
            previewPrimaryColor = Color.parseColor("#0E1726"),
            previewAccentColor = Color.parseColor("#22D3EE"),
            layout = ThemeLayoutConfig(
                layoutType = ThemeLayoutType.HORIZONTAL_TWO_ROW_GRID,
                spanCount = 2,
                cardWidthPx1080 = 248f,
                cardHeightPx1080 = 124f,
                gapHorizontalPx1080 = 12f,
                gapVerticalPx1080 = 12f,
                edgePaddingHorizontalPx1080 = 76f,
                edgePaddingVerticalPx1080 = 12f,
                bottomMarginDp = 14,
                headerTitle = "Wolf Launcher  •  Tekan ↓ untuk App Drawer",
                supportsDownAppDrawer = true,
                drawerColumns = 6
            ),
            cardStyle = ThemeCardStyleConfig(
                shapeType = CardShapeType.WOLF_BANNER_TILE,
                cornerRadiusPx1080 = 6f,
                normalBgStartColor = Color.TRANSPARENT,
                normalBgEndColor = Color.TRANSPARENT,
                focusedBgStartColor = Color.TRANSPARENT,
                focusedBgEndColor = Color.TRANSPARENT,
                normalBorderColor = Color.parseColor("#3838BDF8"),
                normalBorderWidthPx1080 = 1.5f,
                focusedBorderColor = Color.parseColor("#FFFFFF"),
                focusedBorderAccentColor = Color.parseColor("#22D3EE"),
                focusedBorderWidthPx1080 = 4.5f,
                labelPlacement = CardLabelPlacement.BANNER_INTEGRATED,
                labelColorNormal = Color.WHITE,
                labelColorFocused = Color.WHITE,
                labelSizePx1080 = 18f,
                hasTopSheen = true
            ),
            iconConfig = ThemeIconConfig(
                iconSizePx1080 = 248f,
                frameStyle = IconFrameStyle.FULL_BANNER_ART,
                useOnlineCatalog = true
            ),
            focusConfig = ThemeFocusConfig(
                effectStyle = FocusEffectStyle.WOLF_WHITE_CYAN_BEVEL,
                scaleFocused = 1.11f,
                animDurationMs = 100L,
                elevationFocused = 18f,
                elevationNormal = 3f,
                unfocusedCardAlpha = 0.96f
            )
        )
    )

    fun getThemeById(id: ThemeId): LauncherThemeDefinition {
        return allThemes.find { it.id == id } ?: allThemes.first()
    }
}
