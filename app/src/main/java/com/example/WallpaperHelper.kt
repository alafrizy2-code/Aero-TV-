package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import java.util.concurrent.Executors

object WallpaperHelper {
    private const val TAG = "WallpaperHelper"
    private const val PREFS_NAME = "b866f_launcher_prefs"
    private const val KEY_WALLPAPER_URI = "key_wallpaper_uri"
    private const val KEY_CLOCK_WALLPAPER_URI = "key_clock_wallpaper_uri"

    private val decodeExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var cachedCustomWallpaperBitmap: Bitmap? = null
    @Volatile
    private var cachedCustomWallpaperUri: String? = null

    @Volatile
    private var cachedDefaultWallpaperBitmap: Bitmap? = null

    @Volatile
    private var cachedClockBitmap: Bitmap? = null
    @Volatile
    private var cachedClockUri: String? = null

    /**
     * Memuat wallpaper ke ImageView secara non-blocking dengan caching memori dan downsampling RGB_565
     * agar frame pertama & onResume instan (0ms) tanpa memblokir SurfaceSyncGroup.
     */
    fun loadWallpaper(context: Context, imageView: ImageView) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedUriString = prefs.getString(KEY_WALLPAPER_URI, null)
        val targetTag = savedUriString ?: "default_family_bg"
        imageView.tag = targetTag

        if (!savedUriString.isNullOrEmpty()) {
            val cached = cachedCustomWallpaperBitmap
            if (savedUriString == cachedCustomWallpaperUri && cached != null && !cached.isRecycled) {
                imageView.setImageBitmap(cached)
                return
            }

            decodeExecutor.execute {
                val decoded = decodeSampledUriBitmap(appCtx, Uri.parse(savedUriString), 1920, 1080)
                    ?: decodeDefaultFamilyBg(appCtx)
                if (decoded != null) {
                    if (decoded !== cachedDefaultWallpaperBitmap) {
                        cachedCustomWallpaperBitmap = decoded
                        cachedCustomWallpaperUri = savedUriString
                    }
                    mainHandler.post {
                        if (imageView.tag == targetTag && !decoded.isRecycled) {
                            imageView.setImageBitmap(decoded)
                        }
                    }
                }
            }
            return
        }

        cachedCustomWallpaperBitmap = null
        cachedCustomWallpaperUri = null

        val cachedDefault = cachedDefaultWallpaperBitmap
        if (cachedDefault != null && !cachedDefault.isRecycled) {
            imageView.setImageBitmap(cachedDefault)
            return
        }

        decodeExecutor.execute {
            val decoded = decodeDefaultFamilyBg(appCtx)
            if (decoded != null) {
                mainHandler.post {
                    if (imageView.tag == targetTag && !decoded.isRecycled) {
                        imageView.setImageBitmap(decoded)
                    }
                }
            }
        }
    }

    private fun decodeDefaultFamilyBg(context: Context): Bitmap? {
        cachedDefaultWallpaperBitmap?.let { if (!it.isRecycled) return it }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeResource(context.resources, R.drawable.custom_family_bg, bounds)
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, 1920, 1080)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeResource(context.resources, R.drawable.custom_family_bg, opts)?.also {
                cachedDefaultWallpaperBitmap = it
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal decode default wallpaper", e)
            null
        }
    }

    private fun decodeSampledUriBitmap(context: Context, uri: Uri, reqW: Int, reqH: Int): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, reqW, reqH)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, opts)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal memuat custom wallpaper dari Uri: $uri", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Menyimpan custom wallpaper dari hasil file picker penyimpanan (ACTION_OPEN_DOCUMENT).
     * Mengambil persistable permission agar tetap berlaku setelah STB di-reboot.
     */
    fun saveCustomWallpaperUri(context: Context, uri: Uri): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil persistable permission atau menyimpan Uri wallpaper", e)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_WALLPAPER_URI, uri.toString()).apply()
            false
        }
    }

    /**
     * Mengembalikan wallpaper ke foto keluarga bawaan.
     */
    fun resetToDefaultWallpaper(context: Context, imageView: ImageView) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_WALLPAPER_URI).apply()
        cachedCustomWallpaperBitmap = null
        cachedCustomWallpaperUri = null
        loadWallpaper(context, imageView)
    }

    /**
     * Memuat wallpaper khusus untuk Dial Jam Analog (AnalogClockView) secara non-blocking.
     */
    fun loadClockWallpaper(context: Context, clockView: AnalogClockView) {
        val appCtx = context.applicationContext
        val prefs = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedClockUri = prefs.getString(KEY_CLOCK_WALLPAPER_URI, null)

        if (!savedClockUri.isNullOrEmpty()) {
            val cached = cachedClockBitmap
            if (savedClockUri == cachedClockUri && cached != null && !cached.isRecycled) {
                clockView.setClockWallpaper(cached)
                return
            }

            decodeExecutor.execute {
                val bitmap = decodeSampledUriBitmap(appCtx, Uri.parse(savedClockUri), 256, 256)
                mainHandler.post {
                    if (bitmap != null) {
                        cachedClockBitmap = bitmap
                        cachedClockUri = savedClockUri
                        clockView.setClockWallpaper(bitmap)
                    } else {
                        clockView.setClockWallpaper(null)
                    }
                }
            }
            return
        }
        cachedClockBitmap = null
        cachedClockUri = null
        // Jika belum diset / null, clockView memakai dial bawaan (transparan kaca + tick marks)
        clockView.setClockWallpaper(null)
    }

    /**
     * Menyimpan wallpaper khusus dial jam analog dari file picker penyimpanan.
     */
    fun saveClockWallpaperUri(context: Context, uri: Uri): Boolean {
        return try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CLOCK_WALLPAPER_URI, uri.toString()).apply()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Gagal mengambil persistable permission untuk clock wallpaper", e)
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_CLOCK_WALLPAPER_URI, uri.toString()).apply()
            false
        }
    }

    /**
     * Mengembalikan wallpaper jam analog ke dial default.
     */
    fun resetClockWallpaper(context: Context, clockView: AnalogClockView) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CLOCK_WALLPAPER_URI).apply()
        clockView.setClockWallpaper(null)
    }

    /**
     * Intent untuk memilih gambar dari penyimpanan lokal STB / USB Flashdisk.
     */
    fun createWallpaperPickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}
