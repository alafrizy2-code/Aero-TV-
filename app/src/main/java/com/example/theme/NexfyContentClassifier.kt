package com.example.theme

import android.content.Context
import com.example.AppCategory
import com.example.AppModel
import java.util.Locale

/**
 * 8 Kategori horizontal sesuai referensi Nexfy Launcher Home Screen:
 * Semua, Video & Film, Alat, Musik, Lainnya, Jaringan, Sosial, Game.
 */
enum class NexfyCategory(val label: String) {
    SEMUA("Semua"),
    VIDEO_FILM("Video & Film"),
    ALAT("Alat"),
    MUSIK("Musik"),
    LAINNYA("Lainnya"),
    JARINGAN("Jaringan"),
    SOSIAL("Sosial"),
    GAME("Game")
}

/**
 * Pengklasifikasi aplikasi terpasang nyata (PackageManager) ke kategori Nexfy
 * serta penyimpanan daftar sematan/pin favorit Nexfy secara permanen.
 */
object NexfyContentClassifier {

    private const val PREFS_THEME = "aero_theme_prefs"
    private const val KEY_NEXFY_PINNED_PACKAGES = "key_nexfy_pinned_packages"

    fun classify(app: AppModel): NexfyCategory {
        val pkg = app.packageName.lowercase(Locale.US)
        val name = app.appName.lowercase(Locale.US)
        val combined = "$pkg $name"

        // 1. Game
        if (app.category == AppCategory.GAMES ||
            containsAny(combined, "game", "ppsspp", "retroarch", "aether", "dolphin", "minecraft", "roblox", "pubg", "mobilelegends", "freefire", "arcade", "emulator")
        ) {
            return NexfyCategory.GAME
        }

        // 2. Musik
        if (app.category == AppCategory.MUSIC_AUDIO ||
            containsAny(combined, "spotify", "music", "musik", "resso", "joox", "radio", "soundcloud", "audiomack", "deezer", "tidal", "applemusic")
        ) {
            return NexfyCategory.MUSIK
        }

        // 3. Sosial
        if (containsAny(combined, "whatsapp", "telegram", "tiktok", "musically", "facebook", "instagram", "twitter", "discord", "messenger", "wechat", "threads", "reddit", "snapchat", "bigo")) {
            return NexfyCategory.SOSIAL
        }

        // 4. Jaringan
        if (containsAny(combined, "chrome", "browser", "firefox", "opera", "edge", "puffin", "tvbro", "speedtest", "wifi", "vpn", "wireguard", "openvpn", "zerotier", "network", "ftp", "smb", "sendfilestotv", "downloader", "ssh", "ping")) {
            return NexfyCategory.JARINGAN
        }

        // 5. Video & Film
        if (app.category == AppCategory.MEDIA_VIDEO ||
            containsAny(combined, "youtube", "smarttube", "tizentube", "netflix", "vidio", "disney", "hotstar", "prime", "vlc", "kodi", "plex", "stremio", "cloudstream", "vision", "iptv", "iqiyi", "wetv", "bilibili", "crunchyroll", "twitch", "mxtech", "fawesome", "pluto", "tubi", "loktv", "cvision", "iontv", "cinema", "movie", "film", "live tv") ||
            app.isTvApp
        ) {
            return NexfyCategory.VIDEO_FILM
        }

        // 6. Alat
        if (containsAny(combined, "file", "explorer", "xplore", "estrongs", "settings", "pengaturan", "cleaner", "boost", "monitor", "cpu", "apk", "installer", "aptoide", "vending", "play store", "clock", "calc", "calendar", "keyboard", "remote", "launcher", "dolby", "terminal", "adb")) {
            return NexfyCategory.ALAT
        }

        // 7. Lainnya
        return NexfyCategory.LAINNYA
    }

    fun getPinnedPackages(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getString(KEY_NEXFY_PINNED_PACKAGES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isPinned(context: Context, packageName: String): Boolean {
        return getPinnedPackages(context).contains(packageName)
    }

    fun togglePinnedPackage(context: Context, packageName: String): Boolean {
        val current = getPinnedPackages(context).toMutableList()
        val nowPinned = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(0, packageName)
            true
        }
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NEXFY_PINNED_PACKAGES, current.joinToString("|"))
            .apply()
        return nowPinned
    }

    fun orderAndFilterApps(
        context: Context,
        apps: List<AppModel>,
        category: NexfyCategory
    ): List<AppModel> {
        val pinnedOrder = getPinnedPackages(context)
        val filtered = if (category == NexfyCategory.SEMUA) {
            apps
        } else {
            apps.filter { classify(it) == category }
        }
        if (pinnedOrder.isEmpty()) return filtered

        val pinnedIndexMap = pinnedOrder.withIndex().associate { (idx, pkg) -> pkg to idx }
        val pinned = filtered.filter { pinnedIndexMap.containsKey(it.packageName) }
            .sortedBy { pinnedIndexMap[it.packageName] ?: Int.MAX_VALUE }
        val others = filtered.filter { !pinnedIndexMap.containsKey(it.packageName) }
        return pinned + others
    }

    private fun containsAny(haystack: String, vararg needles: String): Boolean {
        for (needle in needles) {
            if (haystack.contains(needle)) return true
        }
        return false
    }
}
