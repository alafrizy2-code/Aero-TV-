package com.example.theme

import com.example.AppCategory
import com.example.AppModel
import com.example.model.FootballMatch
import com.example.model.MatchStatus

/**
 * Arsitektur modular sumber konten & tab Google TV agar dapat dikembangkan lebih lanjut
 * (mis. integrasi Android TV Channels / Watch Next di masa depan) tanpa menampilkan data film palsu.
 */
enum class GoogleTvTabId {
    HOME,
    APPS
}

data class GoogleTvTabSpec(
    val id: GoogleTvTabId,
    val title: String,
    val isAvailable: Boolean
)

/**
 * Kontrak ekstensi untuk penyedia rekomendasi nyata (mis. TvContractCompat PreviewPrograms).
 */
interface GoogleTvRecommendationSource {
    suspend fun fetchRecommendations(): List<GoogleTvTopPickItem>
}

sealed class GoogleTvHeroItem {
    data class InstalledAppHero(
        val app: AppModel,
        val badgeLabel: String,
        val subtitle: String
    ) : GoogleTvHeroItem()

    data class LiveMatchHero(
        val match: FootballMatch
    ) : GoogleTvHeroItem()

    data class EmptySourceNotice(
        val title: String,
        val subtitle: String
    ) : GoogleTvHeroItem()
}

sealed class GoogleTvTopPickItem {
    data class FootballCard(
        val match: FootballMatch
    ) : GoogleTvTopPickItem()

    data class QuickAppCard(
        val app: AppModel,
        val subtitle: String
    ) : GoogleTvTopPickItem()

    data class LauncherActionCard(
        val actionId: String,
        val title: String,
        val subtitle: String,
        val badge: String
    ) : GoogleTvTopPickItem()

    data class RecommendationPlaceholder(
        val title: String,
        val subtitle: String
    ) : GoogleTvTopPickItem()
}

object GoogleTvContentProvider {

    const val ACTION_VOICE_SEARCH = "action_voice_search"
    const val ACTION_ALL_APPS = "action_all_apps"
    const val ACTION_FOOTBALL_CENTER = "action_football_center"
    const val ACTION_WALLPAPER_SETTINGS = "action_wallpaper_settings"

    private val externalSources = mutableListOf<GoogleTvRecommendationSource>()

    fun registerExternalSource(source: GoogleTvRecommendationSource) {
        if (!externalSources.contains(source)) {
            externalSources.add(source)
        }
    }

    /**
     * Hanya mendaftarkan tab yang benar-benar berfungsi saat ini (Beranda & Aplikasi).
     * Tab Live / Library disiapkan secara arsitektural bila sumbernya ditambahkan nanti.
     */
    fun getAvailableTabs(): List<GoogleTvTabSpec> {
        return listOf(
            GoogleTvTabSpec(GoogleTvTabId.HOME, "Beranda", isAvailable = true),
            GoogleTvTabSpec(GoogleTvTabId.APPS, "Aplikasi", isAvailable = true)
        )
    }

    /**
     * Membangun daftar Banner Unggulan dari sumber nyata di perangkat:
     * 1. Pertandingan sepakbola LIVE / mendatang terdekat (ESPN nyata, jika tersedia)
     * 2. Aplikasi streaming/media yang benar-benar terpasang di perangkat
     */
    fun buildHeroItems(
        installedApps: List<AppModel>,
        footballMatches: List<FootballMatch>
    ): List<GoogleTvHeroItem> {
        val result = mutableListOf<GoogleTvHeroItem>()

        // 1. Jika ada pertandingan sepakbola LIVE atau terdekat nyata dari FootballScheduleRepository
        val spotlightMatch = footballMatches.firstOrNull { it.status == MatchStatus.LIVE }
            ?: footballMatches.firstOrNull()
        if (spotlightMatch != null) {
            result.add(GoogleTvHeroItem.LiveMatchHero(spotlightMatch))
        }

        // 2. Prioritaskan aplikasi TV / Video yang benar-benar terpasang di STB
        val mediaApps = installedApps.filter {
            it.category == AppCategory.MEDIA_VIDEO || it.isTvApp || it.banner != null
        }.take(6)

        val heroApps = if (mediaApps.isNotEmpty()) mediaApps else installedApps.take(5)
        for (app in heroApps) {
            val badge = if (app.isTvApp) "Aplikasi Android TV • Terpasang" else "Aplikasi Terpasang"
            val sub = "${app.category.label} • Tekan OK untuk membuka ${app.appName}"
            result.add(GoogleTvHeroItem.InstalledAppHero(app, badge, sub))
        }

        if (result.isEmpty()) {
            result.add(
                GoogleTvHeroItem.EmptySourceNotice(
                    title = "Menunggu Sumber Konten & Aplikasi",
                    subtitle = "Belum ada aplikasi terpasang yang terdeteksi. Daftar akan terisi otomatis dari perangkat."
                )
            )
        }

        return result
    }

    /**
     * Membangun baris "Pilihan teratas untuk Anda" hanya dari sumber data nyata proyek
     * (shortcut sistem nyata, jadwal bola nyata, dan aplikasi terpasang nyata — tanpa film palsu).
     */
    suspend fun buildTopPickItems(
        installedApps: List<AppModel>,
        footballMatches: List<FootballMatch>,
        weatherSummary: String
    ): List<GoogleTvTopPickItem> {
        val items = mutableListOf<GoogleTvTopPickItem>()

        // Ekstensi sumber rekomendasi eksternal nyata bila terdaftar nanti
        for (src in externalSources) {
            try {
                items.addAll(src.fetchRecommendations())
            } catch (_: Exception) { }
        }

        // Kartu nyata Jadwal Sepakbola (ESPN)
        if (footballMatches.isNotEmpty()) {
            for (match in footballMatches.take(3)) {
                items.add(GoogleTvTopPickItem.FootballCard(match))
            }
        }

        // Kartu utilitas nyata launcher (Cuaca kecamatan real-time, Pencarian Suara, Semua Aplikasi, Wallpaper)
        items.add(
            GoogleTvTopPickItem.LauncherActionCard(
                actionId = ACTION_ALL_APPS,
                title = "Semua Aplikasi",
                subtitle = "${installedApps.size} aplikasi terpasang",
                badge = "Aplikasi"
            )
        )
        items.add(
            GoogleTvTopPickItem.LauncherActionCard(
                actionId = ACTION_VOICE_SEARCH,
                title = "Pencarian Suara",
                subtitle = weatherSummary.ifBlank { "Cari aplikasi dengan suara" },
                badge = "Asisten"
            )
        )
        items.add(
            GoogleTvTopPickItem.LauncherActionCard(
                actionId = ACTION_WALLPAPER_SETTINGS,
                title = "Wallpaper Utama",
                subtitle = "Ganti atau atur slideshow",
                badge = "Tampilan"
            )
        )

        // Tambahkan aplikasi video/TV nyata yang terpasang sebagai kartu pilihan cepat
        val featuredApps = installedApps
            .filter { it.category == AppCategory.MEDIA_VIDEO || it.isTvApp }
            .ifEmpty { installedApps }
            .take(6)
        for (app in featuredApps) {
            items.add(
                GoogleTvTopPickItem.QuickAppCard(
                    app = app,
                    subtitle = app.category.label
                )
            )
        }

        if (items.isEmpty()) {
            items.add(
                GoogleTvTopPickItem.RecommendationPlaceholder(
                    title = "Kanal Rekomendasi",
                    subtitle = "Belum ada sumber rekomendasi eksternal terhubung"
                )
            )
        }

        return items
    }
}
