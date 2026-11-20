package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider
import com.example.theme.AtvHomeController
import com.example.theme.AtvViewMode
import com.example.theme.ThemeId
import com.example.theme.ThemeLayoutManager
import com.example.theme.ThemeManager
import com.example.theme.ThemeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp-xhdpi")
class AtvThemeTest {

    private fun sampleApp(
        pkg: String,
        name: String,
        cat: AppCategory = AppCategory.MEDIA_VIDEO,
        isTv: Boolean = true
    ): AppModel {
        return AppModel(
            packageName = pkg,
            appName = name,
            icon = ColorDrawable(Color.parseColor("#1E88E5")),
            launchIntent = null,
            banner = null,
            category = cat,
            isTvApp = isTv
        )
    }

    @Test
    fun `use home screen default toggle and theme storage for atv is persistent and compatible with other themes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        ThemeManager.restoreDefaultTheme(context)
        assertFalse(ThemeManager.shouldUseAtvHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.GOOGLE_TV)
        ThemeManager.setGoogleTvUseHomeScreenDefault(context, true)
        assertFalse(ThemeManager.shouldUseAtvHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.NEXFY)
        ThemeManager.setNexfyUseHomeScreenDefault(context, true)
        assertFalse(ThemeManager.shouldUseAtvHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.PROJECTIVY)
        ThemeManager.setProjectivyUseHomeScreenDefault(context, true)
        assertFalse(ThemeManager.shouldUseAtvHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.ATV_LAUNCHER)
        ThemeManager.setAtvUseHomeScreenDefault(context, true)
        assertEquals(ThemeId.ATV_LAUNCHER, ThemeManager.getActiveThemeId(context))
        assertTrue(ThemeManager.shouldUseAtvHomeLayout(context))
        assertFalse(ThemeManager.shouldUseGoogleTvHomeLayout(context))
        assertFalse(ThemeManager.shouldUseNexfyHomeLayout(context))
        assertFalse(ThemeManager.shouldUseProjectivyHomeLayout(context))

        // Matikan opsi -> kembali ke Aero TV dasar sementara pengaturan tema lain tetap terjaga
        ThemeManager.setAtvUseHomeScreenDefault(context, false)
        assertFalse(ThemeManager.isAtvUseHomeScreenDefault(context))
        assertFalse(ThemeManager.shouldUseAtvHomeLayout(context))
        assertTrue(ThemeManager.isGoogleTvUseHomeScreenDefault(context))
        assertTrue(ThemeManager.isNexfyUseHomeScreenDefault(context))
        assertTrue(ThemeManager.isProjectivyUseHomeScreenDefault(context))
    }

    @Test
    fun `atv card settings and favorites ordering persist accurately`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ThemeManager.resetAtvCardSettings(context)

        val custom = ThemeManager.AtvCardSettings(
            columns = 7,
            cardSizeScale = 1.12f,
            spacingDp = 14,
            cornerRadiusDp = 10,
            bgColorMode = ThemeManager.AtvBgColorMode.ATV_PALETTE,
            bgAlphaPercent = 75,
            focusEffect = ThemeManager.AtvFocusEffect.ORANGE_BORDER_ZOOM,
            iconSizeDp = 54,
            showFavoritesOnlyOnHome = false
        )
        ThemeManager.saveAtvCardSettings(context, custom)

        val loaded = ThemeManager.getAtvCardSettings(context)
        assertEquals(7, loaded.columns)
        assertEquals(1.12f, loaded.cardSizeScale, 0.01f)
        assertEquals(14, loaded.spacingDp)
        assertEquals(10, loaded.cornerRadiusDp)
        assertEquals(ThemeManager.AtvBgColorMode.ATV_PALETTE, loaded.bgColorMode)
        assertEquals(75, loaded.bgAlphaPercent)
        assertEquals(ThemeManager.AtvFocusEffect.ORANGE_BORDER_ZOOM, loaded.focusEffect)
        assertEquals(54, loaded.iconSizeDp)

        val apps = listOf(
            sampleApp("com.netflix.ninja", "Netflix"),
            sampleApp("com.google.android.youtube.tv", "YouTube"),
            sampleApp("com.vidio.android.tv", "Vidio")
        )
        assertTrue(ThemeManager.toggleAtvFavorite(context, "com.vidio.android.tv"))
        assertTrue(ThemeManager.toggleAtvFavorite(context, "com.google.android.youtube.tv"))
        // Geser YouTube ke posisi pertama favorit
        assertTrue(ThemeManager.moveAtvFavorite(context, "com.google.android.youtube.tv", -1))

        val ordered = ThemeManager.orderAppsForAtv(context, apps, favoritesOnly = false)
        assertEquals("com.google.android.youtube.tv", ordered[0].packageName)
        assertEquals("com.vidio.android.tv", ordered[1].packageName)
        assertEquals("com.netflix.ninja", ordered[2].packageName)

        // Bersihkan untuk tes lain
        ThemeManager.resetAtvCardSettings(context)
    }

    @Test
    fun `atv home screen and applications 6-col grid render icon left name right, support card size settings, app launch, dynamic apps, search, and back`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ThemeManager.resetAtvCardSettings(context)

        val root = LayoutInflater.from(context).inflate(R.layout.layout_atv_host, FrameLayout(context), false)
        val initialApps = listOf(
            sampleApp("com.atv.launcher", "ATV Launcher"),
            sampleApp("com.cloudtv.x64", "CloudTV X64"),
            sampleApp("com.fawesome.tv", "Fawesome"),
            sampleApp("com.alphainventor.filemanager", "File Manager +"),
            sampleApp("com.gvision.lite", "Gvision Lite V3"),
            sampleApp("com.gvision.tv", "Gvision TV"),
            sampleApp("com.iptv.player", "IPTV Player")
        )

        var launchedPkg: String? = null
        val controller = AtvHomeController(
            context = context,
            rootView = root,
            isDrawerStandaloneMode = false,
            onAppClick = { launchedPkg = it.packageName },
            onOpenWifi = {},
            onOpenBluetooth = {},
            onOpenSettings = {},
            onOpenVoiceSearch = {},
            onRefreshWeather = {}
        )
        controller.updateInstalledApps(initialApps)
        controller.updateSystemStatus(true, "HomeWiFi", true, "Cibinong • 29°C")

        val layoutHomeBody = root.findViewById<View>(R.id.layoutAtvHomeBody)
        val layoutAppsBody = root.findViewById<View>(R.id.layoutAtvAppsBody)
        val layoutAppsHeaderGroup = root.findViewById<View>(R.id.layoutAtvAppsHeaderGroup)
        val rvHomeGrid = root.findViewById<RecyclerView>(R.id.rvAtvHomeGrid)
        val rvAppsGrid = root.findViewById<RecyclerView>(R.id.rvAtvAppsGrid)
        val tvClock = root.findViewById<TextView>(R.id.tvAtvClock)
        val tvWeatherBadge = root.findViewById<TextView>(R.id.tvAtvWeatherBadge)
        val etSearch = root.findViewById<EditText>(R.id.etAtvSearch)

        // 1 & 3. Home Screen aktif + Grid 6 kolom default pada 1920x1080 + Status & Cuaca
        assertEquals(View.VISIBLE, layoutHomeBody.visibility)
        assertEquals(View.GONE, layoutAppsBody.visibility)
        assertEquals(6, (rvHomeGrid.layoutManager as GridLayoutManager).spanCount)
        assertEquals(7, rvHomeGrid.adapter?.itemCount)
        assertTrue(tvClock.text.isNotBlank())
        assertEquals("Cibinong • 29°C", tvWeatherBadge.text.toString())

        // 4 & 6. Verifikasi kartu landscape: ikon kiri + nama kanan + peluncuran aplikasi
        val holder = rvHomeGrid.adapter!!.createViewHolder(FrameLayout(context), 0)
        rvHomeGrid.adapter!!.bindViewHolder(holder, 0)
        val cardIcon = holder.itemView.findViewById<ImageView>(R.id.ivAtvCardIcon)
        val cardName = holder.itemView.findViewById<TextView>(R.id.tvAtvCardName)
        assertNotNull(cardIcon.drawable)
        assertEquals("ATV Launcher", cardName.text.toString())
        holder.itemView.performClick()
        assertEquals("com.atv.launcher", launchedPkg)

        // 2. Pindah ke halaman Applications: judul "Applications" tampil di kiri atas + grid 6 kolom
        controller.switchMode(AtvViewMode.APPLICATIONS, focusContent = false)
        assertEquals(View.GONE, layoutHomeBody.visibility)
        assertEquals(View.VISIBLE, layoutAppsBody.visibility)
        assertEquals(View.VISIBLE, layoutAppsHeaderGroup.visibility)
        assertEquals(6, (rvAppsGrid.layoutManager as GridLayoutManager).spanCount)
        assertEquals(7, rvAppsGrid.adapter?.itemCount)

        // 7. Deteksi aplikasi baru secara dinamis
        val updatedApps = initialApps + sampleApp("com.ott.navigator", "OTT Navigator")
        controller.updateInstalledApps(updatedApps)
        assertEquals(8, rvHomeGrid.adapter?.itemCount)
        assertEquals(8, rvAppsGrid.adapter?.itemCount)

        // 8. Pengaturan ukuran & kolom kartu langsung diterapkan tanpa restart
        val updatedSettings = ThemeManager.getAtvCardSettings(context).copy(columns = 5, cardSizeScale = 1.24f)
        ThemeManager.saveAtvCardSettings(context, updatedSettings)
        controller.applyCardSettingsAndLayoutManagers()
        assertEquals(5, (rvAppsGrid.layoutManager as GridLayoutManager).spanCount)

        // Pencarian & tombol Back kembali ke Applications lalu ke Home
        controller.openOrToggleSearch()
        etSearch.setText("V3")
        assertEquals(1, rvAppsGrid.adapter?.itemCount)
        assertTrue(controller.handleBackPressed()) // menutup search
        assertEquals(8, rvAppsGrid.adapter?.itemCount)
        assertTrue(controller.handleBackPressed()) // kembali ke Home
        assertEquals(View.VISIBLE, layoutHomeBody.visibility)
        assertEquals(View.GONE, layoutAppsBody.visibility)

        // 12. Mode OFF pada Aero TV dasar tetap merender kartu landscape ATV via ThemeLayoutManager
        val rvBasic = TvHorizontalRecyclerView(context)
        val atvTheme = ThemeRegistry.getThemeById(ThemeId.ATV_LAUNCHER)
        val basicAdapter = ThemeLayoutManager.applyHomeLayout(
            context = context,
            recyclerView = rvBasic,
            headerTextView = null,
            apps = initialApps,
            theme = atvTheme,
            onAppClick = {},
            onRequestAppDrawer = {}
        )
        assertEquals(7, basicAdapter.itemCount)
    }
}
