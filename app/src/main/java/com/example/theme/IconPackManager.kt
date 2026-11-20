package com.example.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.LruCache
import android.widget.ImageView
import com.example.AppModel
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.roundToInt

/**
 * IconPackManager:
 * - Katalog Ikon Online + Cache Disk Lokal (Offline-Ready).
 * - Menghasilkan aset visual kartu yang sesungguhnya untuk setiap gaya launcher:
 *   1. FULL_CIRCLE_ART (Google TV): Kartu bulat penuh edge-to-edge tanpa kotak latar belakang.
 *   2. FULL_BANNER_ART (Projectivy, ATV Launcher, Wolf Launcher): Banner persegi panjang penuh
 *      menggunakan Leanback app.banner asli atau banner brand sintetis 16:9 / ATV penuh warna
 *      tanpa latar belakang ikon default.
 *   3. ORIGINAL_CLEAN (Default & Nexfy): Ikon bersih langsung tanpa bingkai paksa ganda.
 */
object IconPackManager {

    private val ioExecutor = Executors.newFixedThreadPool(3)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Bounded LruCache 8 MB agar RAM STB B866F tetap aman dan navigasi cepat
    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    // Bounded LruCache warna palet ubin ATV (hanya menyimpan pasangan Int start/end per packageName)
    private val atvColorCache = LruCache<String, Pair<Int, Int>>(160)

    private val ATV_VIVID_PALETTE = listOf(
        Pair(Color.parseColor("#1E88E5"), Color.parseColor("#1565C0")), // Biru ATV (ATV Launcher / Gvision TV)
        Pair(Color.parseColor("#2E7D32"), Color.parseColor("#1B5E20")), // Hijau Modern (CloudTV / OTT Navigator)
        Pair(Color.parseColor("#EF6C00"), Color.parseColor("#E65100")), // Oranye Hangat (Fawesome)
        Pair(Color.parseColor("#455A64"), Color.parseColor("#263238")), // Abu Gelap Modern (File Manager)
        Pair(Color.parseColor("#D84315"), Color.parseColor("#BF360C")), // Merah Oranye (Gvision Lite)
        Pair(Color.parseColor("#C62828"), Color.parseColor("#8E1C1C")), // Merah Crimson (IPTV Player)
        Pair(Color.parseColor("#5E35B1"), Color.parseColor("#4527A0")), // Ungu Modern (Browser)
        Pair(Color.parseColor("#0288D1"), Color.parseColor("#01579B")), // Biru Cyan (Netfly)
        Pair(Color.parseColor("#3949AB"), Color.parseColor("#283593")), // Indigo (Play My IPTV)
        Pair(Color.parseColor("#00897B"), Color.parseColor("#00695C"))  // Teal Hijau (Play Store)
    )

    private val PACKAGE_DOMAIN_CATALOG: Map<String, String> = mapOf(
        "com.google.android.youtube.tv" to "youtube.com",
        "com.google.android.youtube" to "youtube.com",
        "com.google.android.youtube.tvkids" to "youtubekids.com",
        "com.teamsmart.videomanager.tv" to "smarttubenext.com",
        "com.liskovsoft.smarttubetv" to "youtube.com",
        "com.netflix.ninja" to "netflix.com",
        "com.netflix.mediaclient" to "netflix.com",
        "com.amazon.amazonvideo.livingroom" to "primevideo.com",
        "com.amazon.avod.thirdpartyclient" to "primevideo.com",
        "com.disney.disneyplus" to "disneyplus.com",
        "in.startv.hotstar" to "hotstar.com",
        "in.startv.hotstar.dplus" to "hotstar.com",
        "com.spotify.tv.android" to "spotify.com",
        "com.spotify.music" to "spotify.com",
        "com.vidio.android" to "vidio.com",
        "com.vidio.android.tv" to "vidio.com",
        "com.visionplus.android" to "visionplus.id",
        "tv.twitch.android.app" to "twitch.tv",
        "com.plexapp.android" to "plex.tv",
        "org.xbmc.kodi" to "kodi.tv",
        "com.stremio.one" to "stremio.com",
        "com.stremio" to "stremio.com",
        "org.videolan.vlc" to "videolan.org",
        "com.mxtech.videoplayer.ad" to "mxplayer.in",
        "com.mxtech.videoplayer.pro" to "mxplayer.in",
        "com.iqiyi.i18n" to "iq.com",
        "com.iqiyi.i18n.tv" to "iq.com",
        "com.tencent.qqlivei18n" to "wetv.vip",
        "com.bstar.intl" to "bilibili.tv",
        "com.crunchyroll.crunchyroid" to "crunchyroll.com",
        "com.android.vending" to "play.google.com",
        "com.android.chrome" to "google.com",
        "com.apple.atve.androidtv.appletv" to "tv.apple.com",
        "com.hbo.hbonow" to "max.com",
        "com.wbd.stream" to "max.com",
        "com.tiktok.tv" to "tiktok.com",
        "com.zhiliaoapp.musically" to "tiktok.com",
        "org.telegram.messenger" to "telegram.org",
        "com.whatsapp" to "whatsapp.com",
        "com.github.android" to "github.com",
        "org.ppsspp.ppsspp" to "ppsspp.org",
        "com.retroarch" to "retroarch.com",
        "com.estrongs.android.pop" to "estrongs.com",
        "com.lonelycatgames.xplore" to "lonelycatgames.com",
        "com.yablio.sendfilestotv" to "sendfilestotv.app",
        "tv.pluto.android" to "pluto.tv",
        "com.tubitv" to "tubitv.com"
    )

    private val KEYWORD_DOMAIN_HINTS: List<Pair<String, String>> = listOf(
        "youtube" to "youtube.com",
        "smarttube" to "youtube.com",
        "tizentube" to "youtube.com",
        "netflix" to "netflix.com",
        "primevideo" to "primevideo.com",
        "amazonvideo" to "primevideo.com",
        "disney" to "disneyplus.com",
        "hotstar" to "hotstar.com",
        "spotify" to "spotify.com",
        "vidio" to "vidio.com",
        "visionplus" to "visionplus.id",
        "twitch" to "twitch.tv",
        "plex" to "plex.tv",
        "kodi" to "kodi.tv",
        "stremio" to "stremio.com",
        "vlc" to "videolan.org",
        "iqiyi" to "iq.com",
        "wetv" to "wetv.vip",
        "bilibili" to "bilibili.tv",
        "crunchyroll" to "crunchyroll.com",
        "chrome" to "google.com",
        "vending" to "play.google.com",
        "telegram" to "telegram.org",
        "whatsapp" to "whatsapp.com",
        "ppsspp" to "ppsspp.org",
        "tiktok" to "tiktok.com"
    )

    fun resolveOnlineIconUrl(packageName: String): String? {
        val pkgLower = packageName.lowercase(Locale.US)
        val domain = PACKAGE_DOMAIN_CATALOG[pkgLower]
            ?: KEYWORD_DOMAIN_HINTS.firstOrNull { (kw, _) -> pkgLower.contains(kw) }?.second
            ?: return null
        return "https://t2.gstatic.com/faviconV2?client=SOCIAL&type=FAVICON&fallback_opts=TYPE,SIZE,URL&url=https://$domain&size=128"
    }

    private fun getThemeIconsDir(context: Context, themeId: ThemeId): File {
        return File(context.filesDir, "theme_packs/${themeId.name.lowercase(Locale.US)}/icons").apply {
            if (!exists()) mkdirs()
        }
    }

    private fun getManifestFile(context: Context, themeId: ThemeId): File {
        return File(context.filesDir, "theme_packs/${themeId.name.lowercase(Locale.US)}/manifest.json")
    }

    private fun getIconDiskFile(context: Context, themeId: ThemeId, packageName: String): File {
        val safeName = packageName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(getThemeIconsDir(context, themeId), "$safeName.png")
    }

    fun isThemeDownloaded(context: Context, themeId: ThemeId): Boolean {
        if (themeId == ThemeId.DEFAULT) return true
        return getManifestFile(context, themeId).exists()
    }

    fun getDownloadedIconCount(context: Context, themeId: ThemeId): Int {
        if (themeId == ThemeId.DEFAULT) return 0
        val manifest = getManifestFile(context, themeId)
        if (manifest.exists()) {
            try {
                val json = JSONObject(manifest.readText())
                return json.optInt("iconCount", 0)
            } catch (_: Exception) { }
        }
        return getThemeIconsDir(context, themeId).listFiles()?.count { it.extension == "png" } ?: 0
    }

    fun downloadThemePack(
        context: Context,
        theme: LauncherThemeDefinition,
        installedApps: List<AppModel>,
        onProgress: (progressPercent: Int, statusText: String) -> Unit,
        onComplete: (downloadedCount: Int, matchedCatalogCount: Int, success: Boolean) -> Unit
    ) {
        val appCtx = context.applicationContext
        ioExecutor.execute {
            try {
                val targets = installedApps
                    .mapNotNull { app ->
                        val url = resolveOnlineIconUrl(app.packageName)
                        if (url != null) Pair(app.packageName, url) else null
                    }
                    .ifEmpty {
                        PACKAGE_DOMAIN_CATALOG.entries.take(16).mapNotNull { (pkg, _) ->
                            resolveOnlineIconUrl(pkg)?.let { Pair(pkg, it) }
                        }
                    }

                val total = targets.size.coerceAtLeast(1)
                var downloaded = 0
                var processed = 0

                for ((pkg, url) in targets) {
                    val outFile = getIconDiskFile(appCtx, theme.id, pkg)
                    if (outFile.exists() && outFile.length() > 100L) {
                        downloaded++
                    } else {
                        val bmp = downloadBitmapFromUrl(url)
                        if (bmp != null) {
                            try {
                                FileOutputStream(outFile).use { fos ->
                                    bmp.compress(Bitmap.CompressFormat.PNG, 92, fos)
                                }
                                bmp.recycle()
                                downloaded++
                            } catch (_: Exception) { }
                        }
                    }
                    processed++
                    val pct = ((processed * 100f) / total).roundToInt().coerceIn(5, 100)
                    mainHandler.post {
                        onProgress(pct, "Mengunduh ikon $processed/$total ($downloaded tersimpan)")
                    }
                }

                // Bersihkan cache memori tema ini agar ikon baru segera dirender ulang ke bentuk kartu
                evictThemeFromMemoryCache(theme.id)

                val manifest = getManifestFile(appCtx, theme.id)
                manifest.parentFile?.mkdirs()
                val json = JSONObject().apply {
                    put("themeId", theme.id.name)
                    put("themeName", theme.name)
                    put("downloadedAt", System.currentTimeMillis())
                    put("iconCount", downloaded)
                    put("catalogCandidates", targets.size)
                    put("offlineReady", true)
                }
                manifest.writeText(json.toString())

                mainHandler.post {
                    onComplete(downloaded, targets.size, downloaded > 0)
                }
            } catch (_: Exception) {
                mainHandler.post {
                    onComplete(0, 0, false)
                }
            }
        }
    }

    private fun evictThemeFromMemoryCache(themeId: ThemeId) {
        val prefix = "${themeId.name}:"
        val keys = memoryCache.snapshot().keys.filter { it.startsWith(prefix) }
        for (k in keys) {
            memoryCache.remove(k)
        }
    }

    /**
     * Memuat visual kartu aplikasi sesuai bentuk & karakter desain tema secara asinkron (anti-lag).
     */
    fun bindCardVisual(
        context: Context,
        imageView: ImageView,
        app: AppModel,
        theme: LauncherThemeDefinition
    ) {
        val visualTag = "${theme.id.name}:${theme.cardStyle.shapeType.name}:${app.packageName}"
        imageView.tag = visualTag

        // Default Theme mempertahankan ikon asli 100%
        if (theme.id == ThemeId.DEFAULT) {
            imageView.setImageDrawable(app.icon)
            return
        }

        // Cek cache memori (0ms)
        memoryCache.get(visualTag)?.let { cachedBmp ->
            if (!cachedBmp.isRecycled) {
                imageView.setImageBitmap(cachedBmp)
                return
            }
        }

        // Preview instan sementara di UI thread agar RecyclerView langsung tampil tanpa delay
        if (theme.iconConfig.frameStyle == IconFrameStyle.FULL_BANNER_ART && app.banner != null) {
            imageView.setImageDrawable(app.banner)
        } else {
            imageView.setImageDrawable(app.icon)
        }

        val appCtx = context.applicationContext
        val diskFile = getIconDiskFile(appCtx, theme.id, app.packageName)

        ioExecutor.execute {
            try {
                // Jika FULL_BANNER_ART dan aplikasi punya native Android TV Leanback banner -> pakai 100% langsung!
                if (theme.iconConfig.frameStyle == IconFrameStyle.FULL_BANNER_ART && app.banner != null) {
                    val bannerBmp = drawableToBannerBitmap(app.banner, 320, 180)
                    memoryCache.put(visualTag, bannerBmp)
                    mainHandler.post {
                        if (imageView.tag == visualTag && !bannerBmp.isRecycled) {
                            imageView.setImageBitmap(bannerBmp)
                        }
                    }
                    return@execute
                }

                // Ambil sumber ikon (prioritas: cache disk unduhan online -> ikon asli aplikasi)
                val diskBmp = if (diskFile.exists() && diskFile.length() > 100L) {
                    decodeSampledBitmapFromFile(diskFile, 128, 128)
                } else null

                val baseIconBmp = diskBmp ?: drawableToBitmap(app.icon, 128)
                val renderedBmp = renderCardArtwork(baseIconBmp, app, theme)
                if (diskBmp != null && diskBmp !== renderedBmp && !diskBmp.isRecycled) {
                    diskBmp.recycle()
                }
                if (baseIconBmp !== renderedBmp && !baseIconBmp.isRecycled) {
                    baseIconBmp.recycle()
                }

                memoryCache.put(visualTag, renderedBmp)
                mainHandler.post {
                    if (imageView.tag == visualTag && !renderedBmp.isRecycled) {
                        imageView.setImageBitmap(renderedBmp)
                    }
                }

                // Jika online catalog aktif dan ikon belum ada di disk, unduh di background lalu perbarui
                if (diskBmp == null && theme.iconConfig.useOnlineCatalog) {
                    val url = resolveOnlineIconUrl(app.packageName)
                    if (url != null && !diskFile.exists()) {
                        val netBmp = downloadBitmapFromUrl(url) ?: return@execute
                        diskFile.parentFile?.mkdirs()
                        FileOutputStream(diskFile).use { fos ->
                            netBmp.compress(Bitmap.CompressFormat.PNG, 92, fos)
                        }
                        val netRendered = renderCardArtwork(netBmp, app, theme)
                        if (netBmp !== netRendered && !netBmp.isRecycled) netBmp.recycle()
                        memoryCache.put(visualTag, netRendered)
                        mainHandler.post {
                            if (imageView.tag == visualTag && !netRendered.isRecycled) {
                                imageView.setImageBitmap(netRendered)
                            }
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun bindGoogleTvCircleIcon(
        context: Context,
        imageView: ImageView,
        app: AppModel
    ) {
        val googleTheme = ThemeRegistry.getThemeById(ThemeId.GOOGLE_TV)
        bindCardVisual(context, imageView, app, googleTheme)
    }

    /**
     * Memuat banner persegi panjang landscape ala Google TV Apps (Referensi 2).
     * Memprioritaskan Leanback app.banner asli Android TV; jika tidak tersedia,
     * membuat kartu landscape bersih dari ikon asli & nama aplikasi sistem.
     */
    fun bindGoogleTvLandscapeBanner(
        context: Context,
        imageView: ImageView,
        app: AppModel
    ) {
        val visualTag = "GOOGLE_TV_LANDSCAPE:${app.packageName}"
        imageView.tag = visualTag

        memoryCache.get(visualTag)?.let { cachedBmp ->
            if (!cachedBmp.isRecycled) {
                imageView.setImageBitmap(cachedBmp)
                return
            }
        }

        if (app.banner != null) {
            imageView.setImageDrawable(app.banner)
        } else {
            imageView.setImageDrawable(app.icon)
        }

        val appCtx = context.applicationContext
        val diskFile = getIconDiskFile(appCtx, ThemeId.GOOGLE_TV, app.packageName)

        ioExecutor.execute {
            try {
                if (app.banner != null) {
                    val bannerBmp = drawableToBannerBitmap(app.banner, 320, 180)
                    memoryCache.put(visualTag, bannerBmp)
                    mainHandler.post {
                        if (imageView.tag == visualTag && !bannerBmp.isRecycled) {
                            imageView.setImageBitmap(bannerBmp)
                        }
                    }
                    return@execute
                }

                val diskBmp = if (diskFile.exists() && diskFile.length() > 100L) {
                    decodeSampledBitmapFromFile(diskFile, 128, 128)
                } else null

                val baseIconBmp = diskBmp ?: drawableToBitmap(app.icon, 128)
                val renderedBmp = renderGoogleTvLandscapeCard(baseIconBmp, app.appName)
                if (diskBmp != null && diskBmp !== renderedBmp && !diskBmp.isRecycled) {
                    diskBmp.recycle()
                }
                if (baseIconBmp !== renderedBmp && !baseIconBmp.isRecycled) {
                    baseIconBmp.recycle()
                }

                memoryCache.put(visualTag, renderedBmp)
                mainHandler.post {
                    if (imageView.tag == visualTag && !renderedBmp.isRecycled) {
                        imageView.setImageBitmap(renderedBmp)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun renderGoogleTvLandscapeCard(iconBmp: Bitmap, appName: String): Bitmap {
        val w = 320
        val h = 180
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val palette = extractBrandPalette(iconBmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                palette.bgStart, palette.bgEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.72f, h * 0.42f, w * 0.55f,
                palette.glowOverlay, Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glowPaint)

        // Sorotan atas halus agar kartu terlihat modern ala Google TV Apps (Referensi 2)
        val topHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h * 0.45f,
                Color.parseColor("#1FFFFFFF"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.45f, topHighlight)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val iconSize = 68f
        val iconLeft = 28f
        val iconTop = (h - iconSize) / 2f
        canvas.drawBitmap(iconBmp, null, RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize), iconPaint)

        drawBannerTitleText(
            canvas = canvas,
            title = appName,
            textColor = palette.textColor,
            shadowColor = palette.shadowColor,
            left = iconLeft + iconSize + 18f,
            right = w - 20f,
            centerY = h / 2f,
            textSizePx = 25f,
            maxLines = 2
        )
        return out
    }

    /**
     * Memuat kartu landscape gaya Nexfy Launcher (Referensi 1 & 2).
     * Memprioritaskan Leanback app.banner asli Android TV; jika tidak tersedia,
     * merender kartu landscape kaca-brand yang ringan dan di-cache LruCache untuk B866F.
     */
    fun bindNexfyLandscapeCard(
        context: Context,
        imageView: ImageView,
        app: AppModel
    ) {
        val visualTag = "NEXFY_LANDSCAPE:${app.packageName}"
        imageView.tag = visualTag

        memoryCache.get(visualTag)?.let { cachedBmp ->
            if (!cachedBmp.isRecycled) {
                imageView.setImageBitmap(cachedBmp)
                return
            }
        }

        if (app.banner != null) {
            imageView.setImageDrawable(app.banner)
        } else {
            imageView.setImageDrawable(app.icon)
        }

        val appCtx = context.applicationContext
        val diskFile = getIconDiskFile(appCtx, ThemeId.NEXFY, app.packageName)

        ioExecutor.execute {
            try {
                if (app.banner != null) {
                    val bannerBmp = drawableToBannerBitmap(app.banner, 304, 172)
                    memoryCache.put(visualTag, bannerBmp)
                    mainHandler.post {
                        if (imageView.tag == visualTag && !bannerBmp.isRecycled) {
                            imageView.setImageBitmap(bannerBmp)
                        }
                    }
                    return@execute
                }

                val diskBmp = if (diskFile.exists() && diskFile.length() > 100L) {
                    decodeSampledBitmapFromFile(diskFile, 128, 128)
                } else null

                val baseIconBmp = diskBmp ?: drawableToBitmap(app.icon, 128)
                val renderedBmp = renderNexfyLandscapeCard(baseIconBmp, app.appName)
                if (diskBmp != null && diskBmp !== renderedBmp && !diskBmp.isRecycled) {
                    diskBmp.recycle()
                }
                if (baseIconBmp !== renderedBmp && !baseIconBmp.isRecycled) {
                    baseIconBmp.recycle()
                }

                memoryCache.put(visualTag, renderedBmp)
                mainHandler.post {
                    if (imageView.tag == visualTag && !renderedBmp.isRecycled) {
                        imageView.setImageBitmap(renderedBmp)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun renderNexfyLandscapeCard(iconBmp: Bitmap, appName: String): Bitmap {
        val w = 304
        val h = 172
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val palette = extractBrandPalette(iconBmp)

        // Latar gelap frosted/brand khas Nexfy Launcher (kontras tinggi & ramah GPU)
        val bgStart = darkenColor(palette.bgStart, 0.72f)
        val bgEnd = Color.parseColor("#E60F172A")
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                bgStart, bgEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.28f, h * 0.45f, w * 0.56f,
                palette.glowOverlay, Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), glowPaint)

        // Kilau kaca tipis di tepi atas kartu
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h * 0.36f,
                Color.parseColor("#24FFFFFF"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.36f, sheenPaint)

        // Ikon asli di kiri-tengah + nama aplikasi rapi di kanan
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val iconSize = 68f
        val iconLeft = 24f
        val iconTop = (h - iconSize) / 2f
        canvas.drawBitmap(iconBmp, null, RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize), iconPaint)

        drawBannerTitleText(
            canvas = canvas,
            title = appName,
            textColor = Color.WHITE,
            shadowColor = Color.parseColor("#CC000000"),
            left = iconLeft + iconSize + 16f,
            right = w - 18f,
            centerY = h / 2f,
            textSizePx = 23f,
            maxLines = 2
        )
        return out
    }

    /**
     * Memuat kartu landscape Projectivy Launcher dengan ikon di tengah kartu (Referensi 1 & 2).
     * Memprioritaskan Leanback app.banner asli Android TV; jika tidak tersedia,
     * merender kartu landscape full-bleed dengan ikon aplikasi tepat di tengah tanpa kotak latar default.
     */
    fun bindProjectivyCenteredCard(
        context: Context,
        imageView: ImageView,
        app: AppModel
    ) {
        val visualTag = "PROJECTIVY_CENTERED:${app.packageName}"
        imageView.tag = visualTag

        memoryCache.get(visualTag)?.let { cachedBmp ->
            if (!cachedBmp.isRecycled) {
                imageView.setImageBitmap(cachedBmp)
                return
            }
        }

        if (app.banner != null) {
            imageView.setImageDrawable(app.banner)
        } else {
            imageView.setImageDrawable(app.icon)
        }

        val appCtx = context.applicationContext
        val diskFile = getIconDiskFile(appCtx, ThemeId.PROJECTIVY, app.packageName)

        ioExecutor.execute {
            try {
                if (app.banner != null) {
                    val bannerBmp = drawableToBannerBitmap(app.banner, 300, 186)
                    memoryCache.put(visualTag, bannerBmp)
                    mainHandler.post {
                        if (imageView.tag == visualTag && !bannerBmp.isRecycled) {
                            imageView.setImageBitmap(bannerBmp)
                        }
                    }
                    return@execute
                }

                val diskBmp = if (diskFile.exists() && diskFile.length() > 100L) {
                    decodeSampledBitmapFromFile(diskFile, 128, 128)
                } else null

                val baseIconBmp = diskBmp ?: drawableToBitmap(app.icon, 128)
                val renderedBmp = renderProjectivyCenteredCard(baseIconBmp)
                if (diskBmp != null && diskBmp !== renderedBmp && !diskBmp.isRecycled) {
                    diskBmp.recycle()
                }
                if (baseIconBmp !== renderedBmp && !baseIconBmp.isRecycled) {
                    baseIconBmp.recycle()
                }

                memoryCache.put(visualTag, renderedBmp)
                mainHandler.post {
                    if (imageView.tag == visualTag && !renderedBmp.isRecycled) {
                        imageView.setImageBitmap(renderedBmp)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun renderProjectivyCenteredCard(iconBmp: Bitmap): Bitmap {
        val w = 300
        val h = 186
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val palette = extractBrandPalette(iconBmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                palette.bgStart, palette.bgEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Sorotan lembut di tengah di belakang ikon agar bersih dan elegan di layar TV
        val centerGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f, h * 0.5f, w * 0.48f,
                palette.glowOverlay, Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), centerGlowPaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val iconSize = h * 0.56f
        val iconLeft = (w - iconSize) / 2f
        val iconTop = (h - iconSize) / 2f
        canvas.drawBitmap(
            iconBmp,
            null,
            RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize),
            iconPaint
        )
        return out
    }

    /**
     * Memuat ikon bersih untuk sisi kiri kartu ATV Launcher (menggunakan cache unduhan online jika ada,
     * atau ikon asli Android tanpa kotak latar tambahan).
     */
    fun bindAtvLeftIcon(
        context: Context,
        imageView: ImageView,
        app: AppModel
    ) {
        val visualTag = "ATV_ICON:${app.packageName}"
        imageView.tag = visualTag

        memoryCache.get(visualTag)?.let { cachedBmp ->
            if (!cachedBmp.isRecycled) {
                imageView.setImageBitmap(cachedBmp)
                return
            }
        }

        imageView.setImageDrawable(app.icon)
        val appCtx = context.applicationContext
        val diskFile = getIconDiskFile(appCtx, ThemeId.ATV_LAUNCHER, app.packageName)
        if (!diskFile.exists() || diskFile.length() <= 100L) return

        ioExecutor.execute {
            try {
                val diskBmp = decodeSampledBitmapFromFile(diskFile, 128, 128) ?: return@execute
                memoryCache.put(visualTag, diskBmp)
                mainHandler.post {
                    if (imageView.tag == visualTag && !diskBmp.isRecycled) {
                        imageView.setImageBitmap(diskBmp)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    /**
     * Menentukan pasangan warna gradien modern untuk kartu ATV berdasarkan pengaturan warna pengguna
     * (AUTO_BRAND / ATV_PALETTE / DARK_GLASS / BLUE_TECH / SLATE_MONO) + transparansi.
     */
    fun getAtvTileColors(
        app: AppModel,
        settings: ThemeManager.AtvCardSettings,
        positionHint: Int = 0
    ): Pair<Int, Int> {
        val rawPair = when (settings.bgColorMode) {
            ThemeManager.AtvBgColorMode.DARK_GLASS ->
                Pair(Color.parseColor("#1E293B"), Color.parseColor("#0F172A"))
            ThemeManager.AtvBgColorMode.BLUE_TECH ->
                Pair(Color.parseColor("#1E88E5"), Color.parseColor("#1565C0"))
            ThemeManager.AtvBgColorMode.SLATE_MONO ->
                Pair(Color.parseColor("#475569"), Color.parseColor("#1E293B"))
            ThemeManager.AtvBgColorMode.ATV_PALETTE -> {
                val idx = kotlin.math.abs(app.packageName.hashCode() + positionHint) % ATV_VIVID_PALETTE.size
                ATV_VIVID_PALETTE[idx]
            }
            ThemeManager.AtvBgColorMode.AUTO_BRAND -> {
                atvColorCache.get(app.packageName) ?: run {
                    val computed = computeQuickAtvBrandPair(app)
                    atvColorCache.put(app.packageName, computed)
                    computed
                }
            }
        }

        val alpha = ((settings.bgAlphaPercent.coerceIn(30, 100) / 100f) * 255f).roundToInt().coerceIn(40, 255)
        return Pair(
            Color.argb(alpha, Color.red(rawPair.first), Color.green(rawPair.first), Color.blue(rawPair.first)),
            Color.argb(alpha, Color.red(rawPair.second), Color.green(rawPair.second), Color.blue(rawPair.second))
        )
    }

    private fun computeQuickAtvBrandPair(app: AppModel): Pair<Int, Int> {
        return try {
            val bmp = drawableToBitmap(app.icon, 48)
            val palette = extractBrandPalette(bmp)
            if (!bmp.isRecycled) bmp.recycle()
            val start = adjustColorBrightness(palette.bgStart, 0.96f)
            val end = darkenColor(palette.bgStart, 0.72f)
            Pair(start, end)
        } catch (_: Exception) {
            val idx = kotlin.math.abs(app.packageName.hashCode()) % ATV_VIVID_PALETTE.size
            ATV_VIVID_PALETTE[idx]
        }
    }

    private fun renderCardArtwork(
        iconBmp: Bitmap,
        app: AppModel,
        theme: LauncherThemeDefinition
    ): Bitmap {
        return when (theme.cardStyle.shapeType) {
            CardShapeType.FULL_CIRCLE_TILE -> renderFullCircleTile(iconBmp, app.icon)
            CardShapeType.SQUIRCLE_GLASS_TILE -> renderNexfyLandscapeCard(iconBmp, app.appName)
            CardShapeType.BANNER_16_9_FULL_BLEED -> renderProjectivyFullBanner(iconBmp, app.appName)
            CardShapeType.ATV_BANNER_GRID_TILE -> renderAtvFullBanner(iconBmp, app.appName)
            CardShapeType.WOLF_BANNER_TILE -> renderWolfFullBanner(iconBmp, app.appName)
            CardShapeType.GLASS_PILL_RECT -> Bitmap.createScaledBitmap(iconBmp, 128, 128, true)
        }
    }

    /**
     * Google TV: Kartu bulat penuh (160x160) tanpa kotak latar belakang.
     * Jika ikon merupakan AdaptiveIconDrawable atau ikon kotak penuh, mengisi lingkaran 100%;
     * jika ikon transparan dengan bentuk bebas, mengisi lingkaran dengan warna brand aplikasi yang bersih.
     */
    private fun renderFullCircleTile(iconBmp: Bitmap, rawDrawable: Drawable): Bitmap {
        val size = 160
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        // Jika Android AdaptiveIconDrawable, render penuh tanpa padding tambahan lalu zoom sedikit agar edge-to-edge bulat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && rawDrawable is AdaptiveIconDrawable) {
            val adaptiveBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val ac = Canvas(adaptiveBmp)
            val bleed = (size * 0.14f).roundToInt()
            rawDrawable.setBounds(-bleed, -bleed, size + bleed, size + bleed)
            rawDrawable.draw(ac)
            canvas.drawBitmap(adaptiveBmp, 0f, 0f, paint)
            adaptiveBmp.recycle()
            return out
        }

        val isEdgeOpaque = isBitmapEdgeOpaque(iconBmp)
        if (isEdgeOpaque) {
            val dst = RectF(0f, 0f, size.toFloat(), size.toFloat())
            canvas.drawBitmap(iconBmp, null, dst, paint)
            return out
        }

        // Ikon transparan -> beri warna lingkaran penuh dari palet brand ikon tanpa latar belakang luar
        val palette = extractBrandPalette(iconBmp)
        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, size.toFloat(), size.toFloat(),
                palette.bgStart, palette.bgEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, circlePaint)

        // Gambar lambang aplikasi jernih & besar di tengah lingkaran
        val pad = size * 0.19f
        val dst = RectF(pad, pad, size - pad, size - pad)
        canvas.drawBitmap(iconBmp, null, dst, paint)
        return out
    }

    /**
     * Projectivy Launcher: Banner landscape bersih dengan ikon aplikasi di tengah kartu.
     */
    private fun renderProjectivyFullBanner(iconBmp: Bitmap, appName: String): Bitmap {
        return renderProjectivyCenteredCard(iconBmp)
    }

    /**
     * ATV Launcher: Banner persegi panjang penuh gaya klasik (280x138) seperti grid Applications di foto referensi ATV.
     */
    private fun renderAtvFullBanner(iconBmp: Bitmap, appName: String): Bitmap {
        val w = 280
        val h = 138
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val palette = extractBrandPalette(iconBmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                palette.bgStart, palette.bgEnd,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Kilau diagonal halus khas banner ATV
        val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, h * 0.5f,
                Color.parseColor("#24FFFFFF"), Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.5f, sheenPaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val iconSize = 58f
        val iconLeft = 22f
        val iconTop = (h - iconSize) / 2f
        canvas.drawBitmap(iconBmp, null, RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize), iconPaint)

        drawBannerTitleText(
            canvas = canvas,
            title = appName,
            textColor = palette.textColor,
            shadowColor = palette.shadowColor,
            left = iconLeft + iconSize + 14f,
            right = w - 16f,
            centerY = h / 2f,
            textSizePx = 22f,
            maxLines = 2
        )
        return out
    }

    /**
     * Wolf Launcher: Banner persegi panjang 2 baris bernuansa kaya & tegas (300x150).
     */
    private fun renderWolfFullBanner(iconBmp: Bitmap, appName: String): Bitmap {
        val w = 300
        val h = 150
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val palette = extractBrandPalette(iconBmp)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, w.toFloat(), h.toFloat(),
                darkenColor(palette.bgStart, 0.78f),
                darkenColor(palette.bgEnd, 0.58f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), bgPaint)

        // Aksen cahaya diagonal di belakang logo
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.25f, h * 0.5f, w * 0.55f,
                palette.glowOverlay, Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), accentPaint)

        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val iconSize = 62f
        val iconLeft = 24f
        val iconTop = (h - iconSize) / 2f
        canvas.drawBitmap(iconBmp, null, RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize), iconPaint)

        drawBannerTitleText(
            canvas = canvas,
            title = appName,
            textColor = Color.WHITE,
            shadowColor = Color.parseColor("#CC000000"),
            left = iconLeft + iconSize + 16f,
            right = w - 18f,
            centerY = h / 2f,
            textSizePx = 23f,
            maxLines = 2
        )
        return out
    }

    @Suppress("DEPRECATION")
    private fun drawBannerTitleText(
        canvas: Canvas,
        title: String,
        textColor: Int,
        shadowColor: Int,
        left: Float,
        right: Float,
        centerY: Float,
        textSizePx: Float,
        maxLines: Int
    ) {
        val availWidth = (right - left).roundToInt().coerceAtLeast(40)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textColor
            textSize = textSizePx
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            setShadowLayer(4f, 1f, 2f, shadowColor)
        }
        val layout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(title, 0, title.length, tp, availWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setMaxLines(maxLines)
                .setEllipsize(android.text.TextUtils.TruncateAt.END)
                .build()
        } else {
            StaticLayout(title, tp, availWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false)
        }
        canvas.save()
        val top = centerY - (layout.height / 2f)
        canvas.translate(left, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private data class BrandPalette(
        val bgStart: Int,
        val bgEnd: Int,
        val glowOverlay: Int,
        val textColor: Int,
        val shadowColor: Int
    )

    private fun extractBrandPalette(bitmap: Bitmap): BrandPalette {
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var count = 0

        var rSatSum = 0L
        var gSatSum = 0L
        var bSatSum = 0L
        var satCount = 0

        val w = bitmap.width
        val h = bitmap.height
        val stepX = (w / 16).coerceAtLeast(1)
        val stepY = (h / 16).coerceAtLeast(1)

        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val px = bitmap.getPixel(x, y)
                val alpha = Color.alpha(px)
                if (alpha > 180) {
                    val r = Color.red(px)
                    val g = Color.green(px)
                    val b = Color.blue(px)
                    val maxC = maxOf(r, maxOf(g, b))
                    val minC = minOf(r, minOf(g, b))
                    val sat = maxC - minC
                    if (sat > 35 && maxC > 45) {
                        rSatSum += r
                        gSatSum += g
                        bSatSum += b
                        satCount++
                    }
                    if (!(r > 240 && g > 240 && b > 240)) {
                        rSum += r
                        gSum += g
                        bSum += b
                        count++
                    }
                }
                x += stepX
            }
            y += stepY
        }

        val baseColor = when {
            satCount > 0 -> Color.rgb((rSatSum / satCount).toInt(), (gSatSum / satCount).toInt(), (bSatSum / satCount).toInt())
            count > 0 -> Color.rgb((rSum / count).toInt(), (gSum / count).toInt(), (bSum / count).toInt())
            else -> Color.parseColor("#1E3A8A")
        }

        val startColor = adjustColorBrightness(baseColor, 1.05f)
        val endColor = darkenColor(baseColor, 0.56f)
        val lum = (0.299f * Color.red(startColor) + 0.587f * Color.green(startColor) + 0.114f * Color.blue(startColor)) / 255f
        val textCol = if (lum > 0.68f) Color.parseColor("#0F172A") else Color.WHITE
        val shadowCol = if (lum > 0.68f) Color.parseColor("#33FFFFFF") else Color.parseColor("#AA000000")
        val glow = Color.argb(72, Color.red(baseColor).coerceAtLeast(90), Color.green(baseColor).coerceAtLeast(90), Color.blue(baseColor).coerceAtLeast(90))

        return BrandPalette(startColor, endColor, glow, textCol, shadowCol)
    }

    private fun isBitmapEdgeOpaque(bmp: Bitmap): Boolean {
        val w = bmp.width
        val h = bmp.height
        if (w < 8 || h < 8) return false
        val samples = listOf(
            bmp.getPixel(3, 3),
            bmp.getPixel(w - 4, 3),
            bmp.getPixel(3, h - 4),
            bmp.getPixel(w - 4, h - 4),
            bmp.getPixel(w / 2, 2),
            bmp.getPixel(w / 2, h - 3)
        )
        return samples.count { Color.alpha(it) > 235 } >= 5
    }

    private fun darkenColor(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).roundToInt().coerceIn(8, 255)
        val g = (Color.green(color) * factor).roundToInt().coerceIn(8, 255)
        val b = (Color.blue(color) * factor).roundToInt().coerceIn(12, 255)
        return Color.rgb(r, g, b)
    }

    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val r = (Color.red(color) * factor).roundToInt().coerceIn(14, 245)
        val g = (Color.green(color) * factor).roundToInt().coerceIn(14, 245)
        val b = (Color.blue(color) * factor).roundToInt().coerceIn(18, 245)
        return Color.rgb(r, g, b)
    }

    private fun downloadBitmapFromUrl(urlString: String): Bitmap? {
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 3500
                readTimeout = 3500
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AeroTVLauncher/6.6")
            }
            if (conn.responseCode == 200) {
                conn.inputStream.use { stream ->
                    val opts = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val bmp = BitmapFactory.decodeStream(stream, null, opts)
                    if (bmp != null && bmp.width >= 16 && bmp.height >= 16) bmp else null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBitmapFromFile(file: File, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            var inSample = 1
            while ((bounds.outWidth / inSample) > reqW * 2 || (bounds.outHeight / inSample) > reqH * 2) {
                inSample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = inSample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeFile(file.absolutePath, opts)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToBitmap(drawable: Drawable, targetSize: Int): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
            val b = drawable.bitmap
            if (b.width <= targetSize && b.height <= targetSize) {
                return b.copy(Bitmap.Config.ARGB_8888, false)
            }
            return Bitmap.createScaledBitmap(b, targetSize, targetSize, true)
        }
        val bmp = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, targetSize, targetSize)
        drawable.draw(canvas)
        return bmp
    }

    private fun drawableToBannerBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val iw = drawable.intrinsicWidth
        val ih = drawable.intrinsicHeight
        if (iw > 0 && ih > 0) {
            // Center-crop ke rasio banner kartu agar penuh edge-to-edge
            val scale = maxOf(width.toFloat() / iw, height.toFloat() / ih)
            val dw = (iw * scale).roundToInt()
            val dh = (ih * scale).roundToInt()
            val dx = (width - dw) / 2
            val dy = (height - dh) / 2
            drawable.setBounds(dx, dy, dx + dw, dy + dh)
        } else {
            drawable.setBounds(0, 0, width, height)
        }
        drawable.draw(canvas)
        return bmp
    }
}
