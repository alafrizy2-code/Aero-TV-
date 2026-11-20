package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Looper
import android.view.View
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import com.example.theme.LauncherThemeDefinition
import com.example.theme.ThemeAndIconSettingsDialog
import com.example.theme.ThemeId
import com.example.theme.ThemeManager
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
class ThemeSwitchFlowTest {

    private fun sampleApps(): List<AppModel> = (1..8).map { idx ->
        AppModel(
            packageName = "com.example.sample$idx",
            appName = "Sample $idx",
            icon = ColorDrawable(Color.parseColor("#2563EB")),
            launchIntent = null,
            banner = null,
            category = AppCategory.MEDIA_VIDEO,
            isTvApp = true
        )
    }

    @Test
    fun `theme settings dialog fits 1080p tv height and switches themes immediately on card click and toggle`() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).create().start().resume().get()
        ThemeManager.restoreDefaultTheme(activity)

        val appliedHistory = mutableListOf<LauncherThemeDefinition>()
        var closedCount = 0
        val dialog = ThemeAndIconSettingsDialog(
            context = activity,
            installedApps = sampleApps(),
            onThemeApplied = { appliedHistory.add(it) },
            onOpenAppDrawer = {},
            onClosed = { closedCount++ }
        )
        dialog.show()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val decor = dialog.window?.decorView
        assertNotNull(decor)
        decor!!

        // Pastikan tombol footer tetap terukur dan terlihat pada layar TV 1080p (960x540dp xhdpi = 1920x1080px)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY)
        decor.measure(widthSpec, heightSpec)
        decor.layout(0, 0, 1920, 1080)

        val btnToggle = decor.findViewWithTag<Button>("btn_toggle_home_default")
        val btnApply = decor.findViewWithTag<Button>("btn_apply_theme")
        val btnRestore = decor.findViewWithTag<Button>("btn_restore_default")
        assertNotNull(btnToggle)
        assertNotNull(btnApply)
        assertNotNull(btnRestore)
        assertEquals(View.VISIBLE, btnApply.visibility)
        assertEquals(View.VISIBLE, btnRestore.visibility)

        val applyLoc = IntArray(2)
        val restoreLoc = IntArray(2)
        btnApply.getLocationOnScreen(applyLoc)
        btnRestore.getLocationOnScreen(restoreLoc)
        assertTrue("Apply button must fit within 1080p height", applyLoc[1] + btnApply.height <= 1080)
        assertTrue("Restore button must fit within 1080p height", restoreLoc[1] + btnRestore.height <= 1080)

        // Uji klik kartu tema langsung mengaktifkan tema + memicu onThemeApplied
        val customThemes = listOf(
            ThemeId.GOOGLE_TV,
            ThemeId.NEXFY,
            ThemeId.PROJECTIVY,
            ThemeId.ATV_LAUNCHER,
            ThemeId.WOLF_LAUNCHER
        )
        for (themeId in customThemes) {
            val card = decor.findViewWithTag<View>("theme_card_${themeId.name}")
            assertNotNull("Card for $themeId should exist", card)
            card.performClick()
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            assertEquals(themeId, ThemeManager.getActiveThemeId(activity))
            assertEquals(themeId, appliedHistory.last().id)
            assertEquals(View.VISIBLE, btnToggle.visibility)
        }

        // Kembali ke DEFAULT dulu, lalu klik card Wolf (focus) atau klik toggle saat memilih ATV
        btnRestore.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(ThemeId.DEFAULT, ThemeManager.getActiveThemeId(activity))
        assertEquals(ThemeId.DEFAULT, appliedHistory.last().id)

        // Simulasikan D-Pad pindah fokus ke kartu ATV (tanpa klik kartu) lalu langsung klik Use Home Screen Default
        val atvCard = decor.findViewWithTag<View>("theme_card_${ThemeId.ATV_LAUNCHER.name}")
        atvCard.requestFocus()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        // Default ATV toggle = true -> klik pertama jadi false (OFF), klik kedua jadi true (ON)
        btnToggle.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(ThemeId.ATV_LAUNCHER, ThemeManager.getActiveThemeId(activity))
        assertFalse(ThemeManager.isAtvUseHomeScreenDefault(activity))
        assertFalse(ThemeManager.shouldUseAtvHomeLayout(activity))
        assertEquals(ThemeId.ATV_LAUNCHER, appliedHistory.last().id)

        btnToggle.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertEquals(ThemeId.ATV_LAUNCHER, ThemeManager.getActiveThemeId(activity))
        assertTrue(ThemeManager.isAtvUseHomeScreenDefault(activity))
        assertTrue(ThemeManager.shouldUseAtvHomeLayout(activity))
        assertEquals(ThemeId.ATV_LAUNCHER, appliedHistory.last().id)

        // Klik Apply Theme menutup dialog dan mempertahankan tema aktif
        btnApply.performClick()
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        assertFalse(dialog.isShowing)
        assertEquals(1, closedCount)
        assertEquals(ThemeId.ATV_LAUNCHER, ThemeManager.getActiveThemeId(activity))
    }

    @Test
    fun `main activity switches home screen host views for all 5 themes on vs off and syncs on resume`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        ThemeManager.restoreDefaultTheme(context)
        ThemeManager.setGoogleTvUseHomeScreenDefault(context, true)
        ThemeManager.setNexfyUseHomeScreenDefault(context, true)
        ThemeManager.setProjectivyUseHomeScreenDefault(context, true)
        ThemeManager.setAtvUseHomeScreenDefault(context, true)
        ThemeManager.setWolfUseHomeScreenDefault(context, true)
        AppCache.cachedApps = sampleApps()

        val controller = Robolectric.buildActivity(MainActivity::class.java).create().start().resume()
        val activity = controller.get()
        Shadows.shadowOf(Looper.getMainLooper()).idle()

        val rvBottom = activity.findViewById<View>(R.id.rvBottomApps)
        val topLeft = activity.findViewById<View>(R.id.layoutTopLeft)
        val topRight = activity.findViewById<View>(R.id.layoutTopRightContainer)

        assertEquals(View.VISIBLE, rvBottom.visibility)
        assertEquals(View.VISIBLE, topLeft.visibility)
        assertEquals(View.VISIBLE, topRight.visibility)

        val expectedHosts = listOf(
            ThemeId.GOOGLE_TV to R.id.googleTvHostRoot,
            ThemeId.NEXFY to R.id.nexfyHostRoot,
            ThemeId.PROJECTIVY to R.id.projectivyHostRoot,
            ThemeId.ATV_LAUNCHER to R.id.atvHostRoot,
            ThemeId.WOLF_LAUNCHER to R.id.wolfHostRoot
        )

        // Setiap tema dengan Use Home Screen Default = ON harus menyembunyikan Aero TV base dan menampilkan host tema
        for ((themeId, hostRootId) in expectedHosts) {
            ThemeManager.setActiveTheme(activity, themeId)
            controller.newIntent(Intent(Intent.ACTION_MAIN)).resume()
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            assertEquals(View.GONE, rvBottom.visibility)
            assertEquals(View.GONE, topLeft.visibility)
            assertEquals(View.GONE, topRight.visibility)
            val hostRoot = activity.findViewById<View>(hostRootId)
            assertNotNull("Host root $hostRootId should inflate when $themeId is ON", hostRoot)
            assertEquals(View.VISIBLE, hostRoot.visibility)

            // Matikan Use Home Screen Default -> kembali ke posisi default Aero TV
            when (themeId) {
                ThemeId.GOOGLE_TV -> ThemeManager.setGoogleTvUseHomeScreenDefault(activity, false)
                ThemeId.NEXFY -> ThemeManager.setNexfyUseHomeScreenDefault(activity, false)
                ThemeId.PROJECTIVY -> ThemeManager.setProjectivyUseHomeScreenDefault(activity, false)
                ThemeId.ATV_LAUNCHER -> ThemeManager.setAtvUseHomeScreenDefault(activity, false)
                ThemeId.WOLF_LAUNCHER -> ThemeManager.setWolfUseHomeScreenDefault(activity, false)
                else -> Unit
            }
            controller.newIntent(Intent(Intent.ACTION_MAIN)).resume()
            Shadows.shadowOf(Looper.getMainLooper()).idle()

            assertEquals(View.GONE, hostRoot.visibility)
            assertEquals(View.VISIBLE, rvBottom.visibility)
            assertEquals(View.VISIBLE, topLeft.visibility)
            assertEquals(View.VISIBLE, topRight.visibility)
        }

        // Pulihkan agar tidak mencemari state tes lain
        ThemeManager.restoreDefaultTheme(activity)
        ThemeManager.setGoogleTvUseHomeScreenDefault(activity, true)
        ThemeManager.setNexfyUseHomeScreenDefault(activity, true)
        ThemeManager.setProjectivyUseHomeScreenDefault(activity, true)
        ThemeManager.setAtvUseHomeScreenDefault(activity, true)
        ThemeManager.setWolfUseHomeScreenDefault(activity, true)
        controller.pause().stop().destroy()
    }
}
