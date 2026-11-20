package com.example

import android.content.Context
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manajer Live Wallpaper dari Penyimpanan (Video .mp4/.webm/.mov dan Gambar Bergerak .gif).
 * - Autoplay, loop tanpa henti, default muted, tanpa kontrol media.
 * - Pengaturan Kecepatan (0.5x, 1x, 1.5x, 2x).
 * - Mode Tampilan (Cover, Contain, Stretch).
 * - Kecerahan (Brightness 0-100% via dim overlay).
 * - Volume (Mute / Unmute).
 * - Multi-file library: list thumbnail/nama file, pilih, hapus, preview.
 * - Tersimpan permanen di SharedPreferences.
 */
class StorageLiveWallpaperManager(
    private val context: Context,
    private val mediaContainer: FrameLayout,
    private val surfaceView: SurfaceView,
    private val ivGif: ImageView,
    private val viewBrightnessDim: View
) {
    companion object {
        private const val TAG = "StorageLiveWallpaper"
        private const val PREFS_NAME = "storage_live_wallpaper_prefs"

        const val KEY_ACTIVE_URI = "key_active_uri"
        const val KEY_ACTIVE_NAME = "key_active_name"
        const val KEY_ACTIVE_IS_VIDEO = "key_active_is_video"
        const val KEY_ITEMS_JSON = "key_saved_items_json"

        const val KEY_SPEED = "key_speed"
        const val KEY_DISPLAY_MODE = "key_display_mode" // "cover", "contain", "stretch"
        const val KEY_BRIGHTNESS = "key_brightness" // 0 - 100
        const val KEY_IS_MUTED = "key_is_muted"

        const val MODE_COVER = "cover"
        const val MODE_CONTAIN = "contain"
        const val MODE_STRETCH = "stretch"

        const val MAX_FILE_SIZE_BYTES = 150 * 1024 * 1024L // 150 MB
    }

    data class MediaItem(
        val uriString: String,
        val fileName: String,
        val isVideo: Boolean,
        val fileSize: Long
    )

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var mediaPlayer: MediaPlayer? = null
    private var isSurfaceReady = false
    private var activeItem: MediaItem? = null

    private val gifImageLoader by lazy {
        ImageLoader.Builder(context)
            .components {
                if (Build.VERSION.SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .build()
    }

    init {
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                isSurfaceReady = true
                if (activeItem?.isVideo == true) {
                    startVideoPlayback(Uri.parse(activeItem!!.uriString))
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                isSurfaceReady = false
                releasePlayer()
            }
        })
    }

    fun getActiveItem(): MediaItem? {
        val uriStr = prefs.getString(KEY_ACTIVE_URI, null) ?: return null
        val name = prefs.getString(KEY_ACTIVE_NAME, "Live Wallpaper") ?: "Live Wallpaper"
        val isVideo = prefs.getBoolean(KEY_ACTIVE_IS_VIDEO, true)
        return MediaItem(uriStr, name, isVideo, 0L)
    }

    fun getSavedItems(): List<MediaItem> {
        val jsonStr = prefs.getString(KEY_ITEMS_JSON, "[]") ?: "[]"
        val list = mutableListOf<MediaItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MediaItem(
                        uriString = obj.getString("uri"),
                        fileName = obj.getString("name"),
                        isVideo = obj.getBoolean("isVideo"),
                        fileSize = obj.optLong("size", 0L)
                    )
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing saved live wallpaper items", e)
        }
        return list
    }

    private fun saveItems(items: List<MediaItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("uri", item.uriString)
                put("name", item.fileName)
                put("isVideo", item.isVideo)
                put("size", item.fileSize)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ITEMS_JSON, array.toString()).apply()
    }

    fun addItem(uri: Uri): MediaItem? {
        val fileName = queryFileName(uri) ?: "Live_Wallpaper_${System.currentTimeMillis()}"
        val fileSize = queryFileSize(uri)
        if (fileSize > MAX_FILE_SIZE_BYTES) {
            Toast.makeText(context, "File terlalu besar, pilih file lain (maks 150MB)", Toast.LENGTH_LONG).show()
            return null
        }

        val isVideo = isVideoFile(fileName, uri)
        val item = MediaItem(uri.toString(), fileName, isVideo, fileSize)

        val items = getSavedItems().toMutableList()
        // Hapus duplikasi URI jika sudah ada
        items.removeAll { it.uriString == item.uriString }
        items.add(0, item)
        saveItems(items)

        // Set langsung sebagai active
        setActiveItem(item)
        return item
    }

    fun deleteItem(item: MediaItem) {
        val items = getSavedItems().toMutableList()
        items.removeAll { it.uriString == item.uriString }
        saveItems(items)

        if (activeItem?.uriString == item.uriString) {
            val next = items.firstOrNull()
            if (next != null) {
                setActiveItem(next)
            } else {
                stop()
                prefs.edit().remove(KEY_ACTIVE_URI).remove(KEY_ACTIVE_NAME).apply()
            }
        }
    }

    fun setActiveItem(item: MediaItem) {
        activeItem = item
        prefs.edit()
            .putString(KEY_ACTIVE_URI, item.uriString)
            .putString(KEY_ACTIVE_NAME, item.fileName)
            .putBoolean(KEY_ACTIVE_IS_VIDEO, item.isVideo)
            .apply()

        applyCurrentActive()
    }

    fun applyCurrentActive() {
        val item = getActiveItem()
        activeItem = item
        if (item == null) {
            stop()
            return
        }

        mediaContainer.visibility = View.VISIBLE
        updateBrightness()

        if (item.isVideo) {
            ivGif.visibility = View.GONE
            surfaceView.visibility = View.VISIBLE
            if (isSurfaceReady) {
                startVideoPlayback(Uri.parse(item.uriString))
            }
        } else {
            surfaceView.visibility = View.GONE
            releasePlayer()
            ivGif.visibility = View.VISIBLE
            applyDisplayModeToImageView()
            ivGif.load(Uri.parse(item.uriString), gifImageLoader)
        }
    }

    private fun startVideoPlayback(uri: Uri) {
        try {
            releasePlayer()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, uri)
                setDisplay(surfaceView.holder)
                isLooping = true

                val isMuted = getIsMuted()
                val vol = if (isMuted) 0f else 1f
                setVolume(vol, vol)

                setOnPreparedListener { mp ->
                    adjustVideoScaling(mp.videoWidth, mp.videoHeight)
                    applyPlaybackSpeed(getSpeed())
                    mp.start()
                }

                setOnErrorListener { _, what, extra ->
                    Log.w(TAG, "MediaPlayer error: $what, $extra")
                    true
                }

                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memutar video live wallpaper", e)
        }
    }

    private fun adjustVideoScaling(videoWidth: Int, videoHeight: Int) {
        if (videoWidth <= 0 || videoHeight <= 0) return
        val displayMode = getDisplayMode()
        val containerWidth = mediaContainer.width.takeIf { it > 0 } ?: context.resources.displayMetrics.widthPixels
        val containerHeight = mediaContainer.height.takeIf { it > 0 } ?: context.resources.displayMetrics.heightPixels

        val videoRatio = videoWidth.toFloat() / videoHeight
        val containerRatio = containerWidth.toFloat() / containerHeight

        val layoutParams = surfaceView.layoutParams as FrameLayout.LayoutParams

        when (displayMode) {
            MODE_COVER -> {
                if (videoRatio > containerRatio) {
                    layoutParams.height = containerHeight
                    layoutParams.width = (containerHeight * videoRatio).toInt()
                } else {
                    layoutParams.width = containerWidth
                    layoutParams.height = (containerWidth / videoRatio).toInt()
                }
            }
            MODE_CONTAIN -> {
                if (videoRatio > containerRatio) {
                    layoutParams.width = containerWidth
                    layoutParams.height = (containerWidth / videoRatio).toInt()
                } else {
                    layoutParams.height = containerHeight
                    layoutParams.width = (containerHeight * videoRatio).toInt()
                }
            }
            MODE_STRETCH -> {
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }
        layoutParams.gravity = android.view.Gravity.CENTER
        surfaceView.layoutParams = layoutParams
    }

    private fun applyDisplayModeToImageView() {
        when (getDisplayMode()) {
            MODE_COVER -> ivGif.scaleType = ImageView.ScaleType.CENTER_CROP
            MODE_CONTAIN -> ivGif.scaleType = ImageView.ScaleType.FIT_CENTER
            MODE_STRETCH -> ivGif.scaleType = ImageView.ScaleType.FIT_XY
        }
    }

    fun setSpeed(speed: Float) {
        prefs.edit().putFloat(KEY_SPEED, speed).apply()
        applyPlaybackSpeed(speed)
    }

    fun getSpeed(): Float = prefs.getFloat(KEY_SPEED, 1.0f)

    private fun applyPlaybackSpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer?.isPlaying == true) {
                mediaPlayer?.playbackParams = PlaybackParams().apply { this.speed = speed }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mengatur kecepatan playback: ${e.message}")
        }
    }

    fun setDisplayMode(mode: String) {
        prefs.edit().putString(KEY_DISPLAY_MODE, mode).apply()
        applyCurrentActive()
    }

    fun getDisplayMode(): String = prefs.getString(KEY_DISPLAY_MODE, MODE_COVER) ?: MODE_COVER

    fun setBrightness(brightness: Int) {
        prefs.edit().putInt(KEY_BRIGHTNESS, brightness.coerceIn(0, 100)).apply()
        updateBrightness()
    }

    fun getBrightness(): Int = prefs.getInt(KEY_BRIGHTNESS, 100)

    private fun updateBrightness() {
        val brightness = getBrightness()
        // 100% = alpha 0 (tanpa peredupan), 0% = alpha 0.90 (sangat redup)
        val dimAlpha = ((100 - brightness) / 100f) * 0.88f
        viewBrightnessDim.alpha = dimAlpha
    }

    fun setIsMuted(isMuted: Boolean) {
        prefs.edit().putBoolean(KEY_IS_MUTED, isMuted).apply()
        val vol = if (isMuted) 0f else 1f
        mediaPlayer?.setVolume(vol, vol)
    }

    fun getIsMuted(): Boolean = prefs.getBoolean(KEY_IS_MUTED, true)

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
            }
        } catch (e: Exception) { }
    }

    fun resume() {
        try {
            if (mediaPlayer != null && mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
            } else if (activeItem != null) {
                applyCurrentActive()
            }
        } catch (e: Exception) { }
    }

    fun release() {
        stop()
    }

    fun createMediaPickerIntent(): android.content.Intent {
        return android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(android.content.Intent.EXTRA_MIME_TYPES, arrayOf("video/*", "image/gif"))
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
    }

    fun stop() {
        releasePlayer()
        ivGif.setImageDrawable(null)
        mediaContainer.visibility = View.GONE
    }

    private fun releasePlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // Ignored
        } finally {
            mediaPlayer = null
        }
    }

    private fun queryFileName(uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.lastPathSegment
        }
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx != -1) cursor.getString(idx) else null
                } else null
            }
        } catch (e: Exception) {
            uri.lastPathSegment
        }
    }

    private fun queryFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (idx != -1) cursor.getLong(idx) else 0L
                } else 0L
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    private fun isVideoFile(fileName: String, uri: Uri): Boolean {
        val lower = fileName.lowercase()
        if (lower.endsWith(".mp4") || lower.endsWith(".webm") || lower.endsWith(".mov") || lower.endsWith(".mkv")) {
            return true
        }
        val type = context.contentResolver.getType(uri)?.lowercase() ?: ""
        return type.startsWith("video/")
    }
}
