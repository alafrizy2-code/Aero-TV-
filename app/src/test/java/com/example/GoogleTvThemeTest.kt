package com.example

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.example.model.FootballMatch
import com.example.model.MatchStatus
import com.example.theme.GoogleTvContentProvider
import com.example.theme.GoogleTvHeroItem
import com.example.theme.GoogleTvTabId
import com.example.theme.GoogleTvTopPickItem
import com.example.theme.ThemeId
import com.example.theme.ThemeManager
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleTvThemeTest {

    @Test
    fun `use home screen default setting only activates google tv home when active theme is google tv and toggle is ON`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Default theme tidak pernah memakai layout Google TV Home meskipun toggle ON
        ThemeManager.restoreDefaultTheme(context)
        assertTrue(ThemeManager.isGoogleTvUseHomeScreenDefault(context))
        assertFalse(ThemeManager.shouldUseGoogleTvHomeLayout(context))

        // Tema lain (mis. Projectivy) juga tidak terpengaruh oleh opsi Google TV
        ThemeManager.setActiveTheme(context, ThemeId.PROJECTIVY)
        assertFalse(ThemeManager.shouldUseGoogleTvHomeLayout(context))

        // Aktifkan Google TV Theme dengan toggle ON -> Home Screen Google TV aktif
        ThemeManager.setActiveTheme(context, ThemeId.GOOGLE_TV)
        ThemeManager.setGoogleTvUseHomeScreenDefault(context, true)
        assertTrue(ThemeManager.shouldUseGoogleTvHomeLayout(context))

        // Matikan toggle OFF -> kembali ke tata letak dasar Aero TV sambil tetap menyimpan status OFF
        ThemeManager.setGoogleTvUseHomeScreenDefault(context, false)
        assertFalse(ThemeManager.isGoogleTvUseHomeScreenDefault(context))
        assertFalse(ThemeManager.shouldUseGoogleTvHomeLayout(context))

        // Pulihkan ON -> langsung terdeteksi kembali
        ThemeManager.setGoogleTvUseHomeScreenDefault(context, true)
        assertTrue(ThemeManager.shouldUseGoogleTvHomeLayout(context))
    }

    @Test
    fun `google tv content provider uses real data only and never injects fake movie items`() = runBlocking {
        val fakeInstalledSample = listOf(
            AppModel(
                packageName = "com.google.android.youtube.tv",
                appName = "YouTube",
                icon = ColorDrawable(Color.RED),
                launchIntent = null,
                banner = null,
                category = AppCategory.MEDIA_VIDEO,
                isTvApp = true
            )
        )
        val liveMatch = FootballMatch(
            id = "m1",
            leagueName = "Premier League",
            leagueCode = "eng.1",
            homeTeam = "Liverpool",
            awayTeam = "Arsenal",
            homeScore = 1,
            awayScore = 1,
            status = MatchStatus.LIVE,
            matchTimeWib = "22:30 WIB",
            matchDateWib = "Hari Ini",
            liveMinute = "60'"
        )

        val heroes = GoogleTvContentProvider.buildHeroItems(fakeInstalledSample, listOf(liveMatch))
        assertTrue(heroes.first() is GoogleTvHeroItem.LiveMatchHero)
        assertTrue(heroes.any { it is GoogleTvHeroItem.InstalledAppHero && it.app.appName == "YouTube" })

        val emptyHeroes = GoogleTvContentProvider.buildHeroItems(emptyList(), emptyList())
        assertEquals(1, emptyHeroes.size)
        assertTrue(emptyHeroes.first() is GoogleTvHeroItem.EmptySourceNotice)

        val picks = GoogleTvContentProvider.buildTopPickItems(fakeInstalledSample, listOf(liveMatch), "Cerah 29°C")
        assertTrue(picks.any { it is GoogleTvTopPickItem.FootballCard })
        assertTrue(picks.any { it is GoogleTvTopPickItem.QuickAppCard && it.app.appName == "YouTube" })
        assertTrue(picks.any { it is GoogleTvTopPickItem.LauncherActionCard && it.actionId == GoogleTvContentProvider.ACTION_ALL_APPS })

        val availableTabs = GoogleTvContentProvider.getAvailableTabs()
        assertEquals(listOf(GoogleTvTabId.HOME, GoogleTvTabId.APPS), availableTabs.map { it.id })
    }

    @Test
    fun `google tv host layout inflates cleanly without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parent = FrameLayout(context)
        val inflated = LayoutInflater.from(context).inflate(R.layout.layout_google_tv_host, parent, false)
        assertTrue(inflated.findViewById<android.view.View>(R.id.cardGtvHeroBanner) != null)
        assertTrue(inflated.findViewById<android.view.View>(R.id.rvGtvTopPicks) != null)
        assertTrue(inflated.findViewById<android.view.View>(R.id.rvGtvYourApps) != null)
        assertTrue(inflated.findViewById<android.view.View>(R.id.rvGtvInstalledGrid) != null)
    }
}
