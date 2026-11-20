package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.example.theme.NexfyCategory
import com.example.theme.NexfyContentClassifier
import com.example.theme.NexfyHomeController
import com.example.theme.NexfyViewMode
import com.example.theme.ThemeId
import com.example.theme.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp-xhdpi")
class NexfyThemeTest {

    private fun sampleApp(pkg: String, name: String, cat: AppCategory = AppCategory.TOOLS_SYSTEM, isTv: Boolean = false): AppModel {
        return AppModel(
            packageName = pkg,
            appName = name,
            icon = ColorDrawable(Color.BLUE),
            launchIntent = null,
            banner = null,
            category = cat,
            isTvApp = isTv
        )
    }

    @Test
    fun `use home screen default toggle for nexfy is independent from google tv and other themes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        ThemeManager.restoreDefaultTheme(context)
        assertFalse(ThemeManager.shouldUseNexfyHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.GOOGLE_TV)
        ThemeManager.setGoogleTvUseHomeScreenDefault(context, true)
        assertFalse(ThemeManager.shouldUseNexfyHomeLayout(context))
        assertTrue(ThemeManager.shouldUseGoogleTvHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.NEXFY)
        ThemeManager.setNexfyUseHomeScreenDefault(context, true)
        assertTrue(ThemeManager.shouldUseNexfyHomeLayout(context))
        assertFalse(ThemeManager.shouldUseGoogleTvHomeLayout(context))

        // Toggle Nexfy OFF -> kembali ke Aero TV dasar tanpa mengubah Google TV
        ThemeManager.setNexfyUseHomeScreenDefault(context, false)
        assertFalse(ThemeManager.isNexfyUseHomeScreenDefault(context))
        assertFalse(ThemeManager.shouldUseNexfyHomeLayout(context))
        assertTrue(ThemeManager.isGoogleTvUseHomeScreenDefault(context))
    }

    @Test
    fun `nexfy classifier maps real installed apps into all 8 categories and persists pins`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val youtube = sampleApp("com.google.android.youtube.tv", "YouTube", AppCategory.MEDIA_VIDEO, isTv = true)
        val fileMgr = sampleApp("com.alphainventor.filemanager", "File Manager +")
        val spotify = sampleApp("com.spotify.tv.android", "Spotify", AppCategory.MUSIC_AUDIO)
        val browser = sampleApp("com.android.chrome", "Chrome Browser")
        val telegram = sampleApp("org.telegram.messenger", "Telegram")
        val game = sampleApp("org.ppsspp.ppsspp", "PPSSPP", AppCategory.GAMES)
        val other = sampleApp("com.custom.utility", "My Custom Tool")

        assertEquals(NexfyCategory.VIDEO_FILM, NexfyContentClassifier.classify(youtube))
        assertEquals(NexfyCategory.ALAT, NexfyContentClassifier.classify(fileMgr))
        assertEquals(NexfyCategory.MUSIK, NexfyContentClassifier.classify(spotify))
        assertEquals(NexfyCategory.JARINGAN, NexfyContentClassifier.classify(browser))
        assertEquals(NexfyCategory.SOSIAL, NexfyContentClassifier.classify(telegram))
        assertEquals(NexfyCategory.GAME, NexfyContentClassifier.classify(game))
        assertEquals(NexfyCategory.LAINNYA, NexfyContentClassifier.classify(other))

        val list = listOf(youtube, fileMgr, spotify, browser, telegram, game, other)
        assertEquals(7, NexfyContentClassifier.orderAndFilterApps(context, list, NexfyCategory.SEMUA).size)

        // Pin Spotify ke depan
        assertTrue(NexfyContentClassifier.togglePinnedPackage(context, spotify.packageName))
        val ordered = NexfyContentClassifier.orderAndFilterApps(context, list, NexfyCategory.SEMUA)
        assertEquals("com.spotify.tv.android", ordered.first().packageName)

        // Lepas pin
        assertFalse(NexfyContentClassifier.togglePinnedPackage(context, spotify.packageName))
    }

    @Test
    fun `nexfy home and all apps controller handles 6 column grid mode switching dynamic updates and back`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = LayoutInflater.from(context).inflate(R.layout.layout_nexfy_host, FrameLayout(context), false)

        val initialApps = listOf(
            sampleApp("com.netflix.ninja", "Netflix", AppCategory.MEDIA_VIDEO, true),
            sampleApp("com.google.android.youtube.tv", "YouTube", AppCategory.MEDIA_VIDEO, true),
            sampleApp("com.spotify.tv.android", "Spotify", AppCategory.MUSIC_AUDIO, true),
            sampleApp("com.alphainventor.filemanager", "File Manager +"),
            sampleApp("com.android.chrome", "Chrome"),
            sampleApp("org.ppsspp.ppsspp", "PPSSPP", AppCategory.GAMES)
        )

        var clickedPkg: String? = null
        val controller = NexfyHomeController(
            context = context,
            rootView = root,
            isDrawerStandaloneMode = false,
            onAppClick = { clickedPkg = it.packageName },
            onOpenWifi = {},
            onOpenBluetooth = {},
            onOpenSettings = {},
            onOpenVoiceSearch = {},
            onRefreshWeather = {}
        )

        controller.updateInstalledApps(initialApps)
        controller.updateSystemStatus(
            wifiOnline = true,
            wifiSsid = "HomeFiber",
            bluetoothOn = true,
            weatherText = "Cerah 29°C"
        )

        // Verifikasi 8 tab kategori di Home
        val catBar = root.findViewById<android.widget.LinearLayout>(R.id.layoutNexfyCategoryBar)
        assertEquals(8, catBar.childCount)

        // Verifikasi Home Row punya 1 All Apps tile + 6 real apps = 7
        val rvHome = root.findViewById<RecyclerView>(R.id.rvNexfyHomeApps)
        assertEquals(7, rvHome.adapter?.itemCount)

        // Verifikasi status cuaca
        assertEquals("Cerah 29°C", root.findViewById<TextView>(R.id.tvNexfyWeather).text.toString())

        // Pindah ke All Apps
        controller.switchMode(NexfyViewMode.ALL_APPS, focusContent = false)
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layoutNexfyAllAppsBody).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.layoutNexfyHomeBody).visibility)

        val rvGrid = root.findViewById<RecyclerView>(R.id.rvNexfyAllAppsGrid)
        assertEquals(6, (rvGrid.layoutManager as GridLayoutManager).spanCount)
        assertEquals(6, rvGrid.adapter?.itemCount)

        // Simulasi aplikasi baru terpasang
        val updatedApps = initialApps + sampleApp("org.videolan.vlc", "VLC", AppCategory.MEDIA_VIDEO, true)
        controller.updateInstalledApps(updatedApps)
        assertEquals(7, rvGrid.adapter?.itemCount)
        assertEquals(8, rvHome.adapter?.itemCount)

        // Tekan Back di All Apps -> kembali ke Home Screen
        assertTrue(controller.handleBackPressed())
        assertEquals(View.VISIBLE, root.findViewById<View>(R.id.layoutNexfyHomeBody).visibility)
        assertEquals(View.GONE, root.findViewById<View>(R.id.layoutNexfyAllAppsBody).visibility)
    }
}
