package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.example.theme.ThemeAppDrawerDialog
import com.example.theme.ThemeId
import com.example.theme.ThemeLayoutManager
import com.example.theme.ThemeManager
import com.example.theme.ThemeRegistry
import com.example.theme.WolfHomeController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp-xhdpi")
class WolfThemeTest {

    private fun sampleApp(
        pkg: String,
        name: String,
        cat: AppCategory = AppCategory.MEDIA_VIDEO,
        isTv: Boolean = true
    ): AppModel {
        return AppModel(
            packageName = pkg,
            appName = name,
            icon = ColorDrawable(Color.parseColor("#1D4ED8")),
            launchIntent = null,
            banner = null,
            category = cat,
            isTvApp = isTv
        )
    }

    @Test
    fun `use home screen default toggle for wolf is persistent and isolated from other themes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        ThemeManager.restoreDefaultTheme(context)
        assertFalse(ThemeManager.shouldUseWolfHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.GOOGLE_TV)
        ThemeManager.setGoogleTvUseHomeScreenDefault(context, true)
        assertFalse(ThemeManager.shouldUseWolfHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.NEXFY)
        ThemeManager.setNexfyUseHomeScreenDefault(context, true)
        assertFalse(ThemeManager.shouldUseWolfHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.PROJECTIVY)
        ThemeManager.setProjectivyUseHomeScreenDefault(context, true)
        assertFalse(ThemeManager.shouldUseWolfHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.ATV_LAUNCHER)
        ThemeManager.setAtvUseHomeScreenDefault(context, true)
        assertFalse(ThemeManager.shouldUseWolfHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.WOLF_LAUNCHER)
        ThemeManager.setWolfUseHomeScreenDefault(context, true)
        assertEquals(ThemeId.WOLF_LAUNCHER, ThemeManager.getActiveThemeId(context))
        assertTrue(ThemeManager.isWolfUseHomeScreenDefault(context))
        assertTrue(ThemeManager.shouldUseWolfHomeLayout(context))
        assertFalse(ThemeManager.shouldUseGoogleTvHomeLayout(context))
        assertFalse(ThemeManager.shouldUseNexfyHomeLayout(context))
        assertFalse(ThemeManager.shouldUseProjectivyHomeLayout(context))
        assertFalse(ThemeManager.shouldUseAtvHomeLayout(context))

        // Matikan opsi -> kembali ke tata letak dasar Aero TV tanpa mengganggu tema lain
        ThemeManager.setWolfUseHomeScreenDefault(context, false)
        assertFalse(ThemeManager.isWolfUseHomeScreenDefault(context))
        assertFalse(ThemeManager.shouldUseWolfHomeLayout(context))
        assertTrue(ThemeManager.isGoogleTvUseHomeScreenDefault(context))
        assertTrue(ThemeManager.isNexfyUseHomeScreenDefault(context))
        assertTrue(ThemeManager.isProjectivyUseHomeScreenDefault(context))
        assertTrue(ThemeManager.isAtvUseHomeScreenDefault(context))
    }

    @Test
    fun `wolf display settings and dock favorites ordering persist and reset accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ThemeManager.resetWolfDisplaySettings(context)

        val custom = ThemeManager.WolfDisplaySettings(
            showWeatherWidget = false,
            showStorageInfo = true,
            showClockAndDate = false,
            widgetSizeScale = 1.2f,
            dockAlphaPercent = 72,
            iconSizeDp = 72,
            cardSpacingDp = 18,
            focusEffect = ThemeManager.WolfFocusEffect.WHITE_RING_ZOOM,
            visibleDockAppsCount = 8,
            dockLabelMode = ThemeManager.WolfDockLabelMode.ALWAYS
        )
        ThemeManager.saveWolfDisplaySettings(context, custom)

        val loaded = ThemeManager.getWolfDisplaySettings(context)
        assertFalse(loaded.showWeatherWidget)
        assertTrue(loaded.showStorageInfo)
        assertFalse(loaded.showClockAndDate)
        assertEquals(1.2f, loaded.widgetSizeScale, 0.01f)
        assertEquals(72, loaded.dockAlphaPercent)
        assertEquals(72, loaded.iconSizeDp)
        assertEquals(18, loaded.cardSpacingDp)
        assertEquals(ThemeManager.WolfFocusEffect.WHITE_RING_ZOOM, loaded.focusEffect)
        assertEquals(8, loaded.visibleDockAppsCount)
        assertEquals(ThemeManager.WolfDockLabelMode.ALWAYS, loaded.dockLabelMode)

        val apps = listOf(
            sampleApp("com.google.android.youtube.tv", "YouTube"),
            sampleApp("com.netflix.ninja", "Netflix"),
            sampleApp("com.vidio.android.tv", "Vidio"),
            sampleApp("com.spotify.tv.android", "Spotify")
        )
        assertTrue(ThemeManager.toggleWolfDockFavorite(context, "com.vidio.android.tv"))
        assertTrue(ThemeManager.toggleWolfDockFavorite(context, "com.spotify.tv.android"))
        assertTrue(ThemeManager.moveWolfDockFavorite(context, "com.spotify.tv.android", -1))

        val ordered = ThemeManager.orderAppsForWolfDock(context, apps, visibleCount = 3)
        assertEquals(3, ordered.size)
        assertEquals("com.spotify.tv.android", ordered[0].packageName)
        assertEquals("com.vidio.android.tv", ordered[1].packageName)
        assertEquals("com.google.android.youtube.tv", ordered[2].packageName)

        ThemeManager.resetWolfDisplaySettings(context)
        val reset = ThemeManager.getWolfDisplaySettings(context)
        assertTrue(reset.showWeatherWidget)
        assertTrue(reset.showStorageInfo)
        assertTrue(reset.showClockAndDate)
        assertEquals(9, reset.visibleDockAppsCount)
    }

    @Test
    fun `wolf home renders weather widget, real storage info, clock, system status, dock apps, and all apps drawer trigger`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ThemeManager.resetWolfDisplaySettings(context)

        val root = LayoutInflater.from(context).inflate(R.layout.layout_wolf_host, FrameLayout(context), false)
        val initialApps = (1..12).map { idx ->
            sampleApp("com.example.app$idx", "App $idx")
        }

        var launchedPkg: String? = null
        var drawerOpened = false
        val controller = WolfHomeController(
            context = context,
            rootView = root,
            onAppClick = { launchedPkg = it.packageName },
            onOpenAppDrawer = { drawerOpened = true },
            onOpenWifi = {},
            onOpenBluetooth = {},
            onOpenSettings = {},
            onRefreshWeather = {}
        )
        controller.updateInstalledApps(initialApps)
        controller.updateSystemStatus(
            wifiOnline = true,
            wifiSsid = "HomeFiber",
            bluetoothOn = true,
            weatherText = "Bandung 26°C Sebagian berawan",
            weatherCode = 2,
            isDay = 1
        )
        controller.updateNetworkSpeed("↓ 1.45 MB/s")
        controller.refreshStorageInfo(force = true)

        val cardWeather = root.findViewById<View>(R.id.cardWolfWeather)
        val tvWeatherLoc = root.findViewById<TextView>(R.id.tvWolfWeatherLocation)
        val tvWeatherTemp = root.findViewById<TextView>(R.id.tvWolfWeatherTemp)
        val tvWeatherCond = root.findViewById<TextView>(R.id.tvWolfWeatherCondition)

        val cardStorage = root.findViewById<View>(R.id.cardWolfStorage)
        val tvStorageAvail = root.findViewById<TextView>(R.id.tvWolfStorageAvailable)
        val tvStorageInternal = root.findViewById<TextView>(R.id.tvWolfStorageInternal)
        val tvRamAvail = root.findViewById<TextView>(R.id.tvWolfRamAvailable)

        val layoutClock = root.findViewById<View>(R.id.layoutWolfClockGroup)
        val tvClock = root.findViewById<TextView>(R.id.tvWolfClock)
        val tvDate = root.findViewById<TextView>(R.id.tvWolfDate)
        val tvNetSpeed = root.findViewById<TextView>(R.id.tvWolfNetSpeed)

        val rvDock = root.findViewById<RecyclerView>(R.id.rvWolfDockApps)
        val btnAllApps = root.findViewById<View>(R.id.btnWolfAllApps)

        // 1. Weather widget & real storage & clock & system status
        assertEquals(View.VISIBLE, cardWeather.visibility)
        assertEquals("BANDUNG", tvWeatherLoc.text.toString())
        assertEquals("26°", tvWeatherTemp.text.toString())
        assertEquals("Sebagian berawan", tvWeatherCond.text.toString())

        assertEquals(View.VISIBLE, cardStorage.visibility)
        assertTrue(tvStorageAvail.text.toString().contains("tersedia"))
        assertTrue(tvStorageInternal.text.toString().startsWith("Terpakai:"))
        assertTrue(tvRamAvail.text.toString().startsWith("Memori tersedia:"))

        assertEquals(View.VISIBLE, layoutClock.visibility)
        assertTrue(tvClock.text.isNotBlank())
        assertTrue(tvDate.text.isNotBlank())
        assertEquals("↓ 1.45 MB/s", tvNetSpeed.text.toString())

        // 2. Dock menampilkan 9 ikon default + klik item membuka aplikasi + tombol All Apps memanggil drawer
        assertEquals(9, rvDock.adapter?.itemCount)
        val holder = rvDock.adapter!!.createViewHolder(FrameLayout(context), 0)
        rvDock.adapter!!.bindViewHolder(holder, 0)
        holder.itemView.performClick()
        assertEquals("com.example.app1", launchedPkg)

        btnAllApps.performClick()
        assertTrue(drawerOpened)

        // 3. Pengaturan tampilan Wolf diterapkan langsung tanpa restart
        val updated = ThemeManager.getWolfDisplaySettings(context).copy(
            showWeatherWidget = false,
            showStorageInfo = false,
            showClockAndDate = false,
            visibleDockAppsCount = 6
        )
        ThemeManager.saveWolfDisplaySettings(context, updated)
        controller.applyDisplaySettings()
        assertEquals(View.GONE, cardWeather.visibility)
        assertEquals(View.GONE, cardStorage.visibility)
        assertEquals(View.GONE, layoutClock.visibility)
        assertEquals(6, rvDock.adapter?.itemCount)

        // 4. Deteksi aplikasi baru secara dinamis
        val newApps = listOf(sampleApp("com.brand.newapp", "Brand New App")) + initialApps
        controller.updateInstalledApps(newApps)
        val holderFirst = rvDock.adapter!!.createViewHolder(FrameLayout(context), 0)
        rvDock.adapter!!.bindViewHolder(holderFirst, 0)
        assertEquals("Brand New App", holderFirst.itemView.findViewById<TextView>(R.id.tvWolfDockAppName).text.toString())

        // 5. Mode OFF menggunakan ThemeLayoutManager tetap merender semua kartu Wolf dengan rapi
        val rvBasic = TvHorizontalRecyclerView(context)
        val wolfTheme = ThemeRegistry.getThemeById(ThemeId.WOLF_LAUNCHER)
        val basicAdapter = ThemeLayoutManager.applyHomeLayout(
            context = context,
            recyclerView = rvBasic,
            headerTextView = null,
            apps = initialApps,
            theme = wolfTheme,
            onAppClick = {},
            onRequestAppDrawer = {}
        )
        assertEquals(12, basicAdapter.itemCount)

        ThemeManager.resetWolfDisplaySettings(context)
    }

    @Test
    fun `wolf app drawer uses existing Aero TV drawer with live search and back dismissal`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().get()
        val wolfTheme = ThemeRegistry.getThemeById(ThemeId.WOLF_LAUNCHER)
        val apps = listOf(
            sampleApp("com.google.android.youtube.tv", "YouTube"),
            sampleApp("com.netflix.ninja", "Netflix"),
            sampleApp("com.vidio.android.tv", "Vidio"),
            sampleApp("com.spotify.tv.android", "Spotify")
        )
        var closedCalled = false
        var launchedAppPkg: String? = null
        val dialog = ThemeAppDrawerDialog(
            context = activity,
            apps = apps,
            theme = wolfTheme,
            onAppClick = { launchedAppPkg = it.packageName },
            onClosed = { closedCalled = true }
        )
        dialog.show()
        assertTrue(dialog.isShowing)

        val root = dialog.window?.decorView
        assertNotNull(root)
        val etSearch = root!!.findViewWithTag<EditText>("standard_drawer_search")
        val rvGrid = root.findViewWithTag<RecyclerView>("standard_drawer_grid")
        assertNotNull(etSearch)
        assertNotNull(rvGrid)
        assertEquals(4, rvGrid.adapter?.itemCount)

        // Pencarian real-time menyaring daftar aplikasi
        etSearch.setText("Net")
        assertEquals(1, rvGrid.adapter?.itemCount)

        // Tekan Back saat pencarian aktif -> bersihkan pencarian dulu
        dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK))
        dialog.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK))
        assertTrue(dialog.isShowing)
        assertEquals("", etSearch.text.toString())
        assertEquals(4, rvGrid.adapter?.itemCount)

        // Klik item meluncurkan aplikasi & menutup dialog
        val holder = rvGrid.adapter!!.createViewHolder(FrameLayout(activity), 2)
        rvGrid.adapter!!.bindViewHolder(holder, 2)
        holder.itemView.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals("com.vidio.android.tv", launchedAppPkg)
        assertFalse(dialog.isShowing)
        assertTrue(closedCalled)
    }
}
