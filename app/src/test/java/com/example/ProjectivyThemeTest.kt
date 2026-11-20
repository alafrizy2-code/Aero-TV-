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
import com.example.theme.ProjectivyContentSnapshot
import com.example.theme.ProjectivyContentSource
import com.example.theme.ProjectivyExternalWatchNextSource
import com.example.theme.ProjectivyHomeController
import com.example.theme.ProjectivyViewMode
import com.example.theme.ProjectivyWatchNextItem
import com.example.theme.ProjectivyWatchNextProvider
import com.example.theme.ThemeId
import com.example.theme.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w960dp-h540dp-xhdpi")
class ProjectivyThemeTest {

    @After
    fun tearDown() {
        ProjectivyWatchNextProvider.clearExternalSources()
    }

    private fun sampleApp(
        pkg: String,
        name: String,
        cat: AppCategory = AppCategory.TOOLS_SYSTEM,
        isTv: Boolean = false
    ): AppModel {
        return AppModel(
            packageName = pkg,
            appName = name,
            icon = ColorDrawable(Color.DKGRAY),
            launchIntent = null,
            banner = null,
            category = cat,
            isTvApp = isTv
        )
    }

    @Test
    fun `use home screen default toggle for projectivy is independent and persistent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        ThemeManager.restoreDefaultTheme(context)
        assertFalse(ThemeManager.shouldUseProjectivyHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.GOOGLE_TV)
        ThemeManager.setGoogleTvUseHomeScreenDefault(context, true)
        assertFalse(ThemeManager.shouldUseProjectivyHomeLayout(context))

        ThemeManager.setActiveTheme(context, ThemeId.PROJECTIVY)
        ThemeManager.setProjectivyUseHomeScreenDefault(context, true)
        assertTrue(ThemeManager.shouldUseProjectivyHomeLayout(context))
        assertFalse(ThemeManager.shouldUseGoogleTvHomeLayout(context))
        assertFalse(ThemeManager.shouldUseNexfyHomeLayout(context))

        // Matikan opsi -> kembali ke tata letak dasar Aero TV tanpa mengubah tema lain
        ThemeManager.setProjectivyUseHomeScreenDefault(context, false)
        assertFalse(ThemeManager.isProjectivyUseHomeScreenDefault(context))
        assertFalse(ThemeManager.shouldUseProjectivyHomeLayout(context))
        assertTrue(ThemeManager.isGoogleTvUseHomeScreenDefault(context))
    }

    @Test
    fun `watch next provider returns zero fictional data when no system or connected source exists`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ProjectivyWatchNextProvider.clearExternalSources()

        val snapshot = ProjectivyWatchNextProvider.loadWatchNextAndSources(context)
        assertTrue(snapshot.watchNextItems.isEmpty())
        assertTrue(snapshot.externalSources.isEmpty())
    }

    @Test
    fun `watch next provider loads real external connected source and pinning works`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ProjectivyWatchNextProvider.registerExternalSource(object : ProjectivyExternalWatchNextSource {
            override suspend fun fetchWatchNextItems(): List<ProjectivyWatchNextItem> {
                return listOf(
                    ProjectivyWatchNextItem(
                        id = 101L,
                        title = "Kanal Nyata",
                        subtitle = "MovieBoxTV • S1 E2",
                        packageName = "com.movieboxtv.app",
                        posterUri = null,
                        intentUri = null,
                        progressFraction = 0.45f,
                        sourceName = "MovieBoxTV"
                    )
                )
            }

            override suspend fun fetchContentSources(): List<ProjectivyContentSource> {
                return listOf(
                    ProjectivyContentSource(
                        id = "movieboxtv",
                        label = "MovieBoxTV",
                        packageName = "com.movieboxtv.app",
                        isExternalTvChannel = true
                    )
                )
            }
        })

        val snapshot = ProjectivyWatchNextProvider.loadWatchNextAndSources(context)
        assertEquals(1, snapshot.watchNextItems.size)
        assertEquals("Kanal Nyata", snapshot.watchNextItems.first().title)
        assertTrue(snapshot.externalSources.any { it.label == "MovieBoxTV" })

        val apps = listOf(
            sampleApp("com.netflix.ninja", "Netflix", AppCategory.MEDIA_VIDEO, true),
            sampleApp("com.movieboxtv.app", "MovieBoxTV", AppCategory.MEDIA_VIDEO, true)
        )
        assertTrue(ProjectivyWatchNextProvider.togglePinnedPackage(context, "com.movieboxtv.app"))
        val ordered = ProjectivyWatchNextProvider.orderAppsForProjectivy(context, apps)
        assertEquals("com.movieboxtv.app", ordered.first().packageName)
        assertFalse(ProjectivyWatchNextProvider.togglePinnedPackage(context, "com.movieboxtv.app"))
    }

    @Test
    fun `projectivy home and 6 column tv apps drawer handle empty watch next, real snapshot, dynamic apps and back`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = LayoutInflater.from(context).inflate(R.layout.layout_projectivy_host, FrameLayout(context), false)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        try {
            val initialApps = listOf(
                sampleApp("com.netflix.ninja", "Netflix", AppCategory.MEDIA_VIDEO, true),
                sampleApp("com.google.android.youtube.tv", "YouTube", AppCategory.MEDIA_VIDEO, true),
                sampleApp("com.vidio.android.tv", "Vidio", AppCategory.MEDIA_VIDEO, true),
                sampleApp("com.spotify.tv.android", "Spotify", AppCategory.MUSIC_AUDIO, true),
                sampleApp("com.alphainventor.filemanager", "File Manager +"),
                sampleApp("org.ppsspp.ppsspp", "PPSSPP", AppCategory.GAMES)
            )

            val controller = ProjectivyHomeController(
                context = context,
                rootView = root,
                scope = scope,
                isDrawerStandaloneMode = false,
                onAppClick = {},
                onOpenWifi = {},
                onOpenSettings = {},
                onOpenVoiceSearch = {},
                onRefreshWeather = {}
            )
            controller.updateInstalledApps(initialApps)
            controller.updateSystemStatus(true, "Rumah-WiFi", "Cibinong • 29°C")

            val rvHomeApps = root.findViewById<RecyclerView>(R.id.rvProjectivyHomeApps)
            val rvWatchNext = root.findViewById<RecyclerView>(R.id.rvProjectivyWatchNext)
            val layoutWatchNextEmpty = root.findViewById<View>(R.id.layoutProjectivyWatchNextEmpty)
            val rvTvAppsGrid = root.findViewById<RecyclerView>(R.id.rvProjectivyTvAppsGrid)
            val layoutHomeBody = root.findViewById<View>(R.id.layoutProjectivyHomeBody)
            val layoutTvAppsBody = root.findViewById<View>(R.id.layoutProjectivyTvAppsBody)
            val tvClock = root.findViewById<TextView>(R.id.tvProjectivyClock)
            val tvWeatherBadge = root.findViewById<TextView>(R.id.tvProjectivyWeatherBadge)

            // 1. Home Screen aktif: Apps row berisi TV Apps tile + 6 app nyata = 7
            assertEquals(View.VISIBLE, layoutHomeBody.visibility)
            assertEquals(View.GONE, layoutTvAppsBody.visibility)
            assertEquals(7, rvHomeApps.adapter?.itemCount)
            assertTrue(tvClock.text.isNotBlank())
            assertEquals("Cibinong • 29°C", tvWeatherBadge.text.toString())

            // 2. Watch Next kosong secara default (tanpa data fiktif)
            controller.applyWatchNextSnapshot(ProjectivyContentSnapshot(emptyList(), emptyList()))
            assertEquals(View.VISIBLE, layoutWatchNextEmpty.visibility)
            assertEquals(View.GONE, rvWatchNext.visibility)

            // 3. Saat sumber nyata terpasang, Watch Next menampilkan item nyata
            controller.applyWatchNextSnapshot(
                ProjectivyContentSnapshot(
                    watchNextItems = listOf(
                        ProjectivyWatchNextItem(
                            id = 77L,
                            title = "Dokumenter Alam",
                            subtitle = "Kodi • Ep 1",
                            packageName = "org.xbmc.kodi",
                            posterUri = null,
                            intentUri = null,
                            progressFraction = 0.3f,
                            sourceName = "Kodi"
                        )
                    ),
                    externalSources = listOf(
                        ProjectivyContentSource("ch:1", "Kodi", "org.xbmc.kodi", true)
                    )
                )
            )
            assertEquals(View.GONE, layoutWatchNextEmpty.visibility)
            assertEquals(View.VISIBLE, rvWatchNext.visibility)
            assertEquals(1, rvWatchNext.adapter?.itemCount)

            // 4. Switch ke TV Apps -> Grid 6 kolom dengan 6 aplikasi
            controller.switchMode(ProjectivyViewMode.TV_APPS, focusContent = false)
            assertEquals(View.VISIBLE, layoutTvAppsBody.visibility)
            val lm = rvTvAppsGrid.layoutManager as GridLayoutManager
            assertEquals(6, lm.spanCount)
            assertEquals(6, rvTvAppsGrid.adapter?.itemCount)

            // 5. Deteksi aplikasi baru secara dinamis
            val updatedApps = initialApps + sampleApp("com.disney.disneyplus", "Disney+", AppCategory.MEDIA_VIDEO, true)
            controller.updateInstalledApps(updatedApps)
            assertEquals(8, rvHomeApps.adapter?.itemCount)
            assertEquals(7, rvTvAppsGrid.adapter?.itemCount)

            // 6. Tombol Back kembali ke Home
            assertTrue(controller.handleBackPressed())
            assertEquals(View.VISIBLE, layoutHomeBody.visibility)
            assertEquals(View.GONE, layoutTvAppsBody.visibility)
        } finally {
            scope.cancel()
        }
    }
}
