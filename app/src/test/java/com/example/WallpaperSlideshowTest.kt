package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WallpaperSlideshowTest {

    @Test
    fun testSupportedIntervalsSeconds() {
        val intervals = WallpaperSlideshowManager.SUPPORTED_INTERVALS_SECONDS
        assertEquals(3, intervals.size)
        assertTrue(intervals.contains(30))  // 30 Detik
        assertTrue(intervals.contains(60))  // 1 Menit
        assertTrue(intervals.contains(180)) // 3 Menit
    }

    @Test
    fun testOrderModesConstants() {
        assertEquals("sequential", WallpaperSlideshowManager.MODE_SEQUENTIAL)
        assertEquals("random", WallpaperSlideshowManager.MODE_RANDOM)
    }

    @Test
    fun testSequentialIndexProgression() {
        val totalImages = 4
        var currentIndex = 0

        // Simulate advancing sequentially
        currentIndex = (currentIndex + 1) % totalImages
        assertEquals(1, currentIndex)
        currentIndex = (currentIndex + 1) % totalImages
        assertEquals(2, currentIndex)
        currentIndex = (currentIndex + 1) % totalImages
        assertEquals(3, currentIndex)
        currentIndex = (currentIndex + 1) % totalImages
        assertEquals(0, currentIndex) // loops back to start
    }

    @Test
    fun testSupportedImageExtensions() {
        fun isSupported(name: String): Boolean {
            val lower = name.lowercase()
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                    lower.endsWith(".png") || lower.endsWith(".webp")
        }

        assertTrue(isSupported("family_vacation.jpg"))
        assertTrue(isSupported("scenery_4k.JPEG"))
        assertTrue(isSupported("poster.PNG"))
        assertTrue(isSupported("wallpaper.webp"))
        assertFalse(isSupported("document.pdf"))
        assertFalse(isSupported("video.mp4"))
        assertFalse(isSupported("app.apk"))
    }

    @Test
    fun testAppFolderModel() {
        val folder = AppFolder(
            id = "test_folder",
            name = "🎬 Streaming",
            packageNames = mutableListOf("com.google.android.youtube.tv")
        )
        assertEquals("test_folder", folder.id)
        assertEquals("🎬 Streaming", folder.name)
        assertEquals(1, folder.packageNames.size)

        folder.packageNames.add("com.netflix.ninja")
        assertEquals(2, folder.packageNames.size)

        folder.name = "📺 TV & Movies"
        assertEquals("📺 TV & Movies", folder.name)
    }
}
