package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.random.Random

/**
 * Pengelola Wallpaper Slideshow khusus Android TV (ZTE B866F).
 *
 * Fitur Utama:
 * 1. Mendukung pemilihan folder via SAF (ACTION_OPEN_DOCUMENT_TREE) dan Browser Folder Internal TV (Direct Path).
 * 2. Persistable URI permission & local path persistence agar tetap berlaku setelah reboot.
 * 3. Deteksi otomatis file gambar (jpg, jpeg, png, webp).
 * 4. Pilihan interval pergantian: 30 Detik, 1 Menit, 3 Menit.
 * 5. Mode urutan: Berurutan (Sequential) & Acak (Random / Shuffle).
 * 6. Transisi Fade crossfade halus antara 2 layer ImageView.
 * 7. Penyimpanan konfigurasi di SharedPreferences.
 * 8. Optimalisasi RAM & GPU: decodeSampledBitmap targeting 1080p dengan RGB_565.
 * 9. Center Crop 16:9 tanpa distorsi.
 * 10. Navigasi ramah remote Android TV (D-pad & tombol OK).
 */
class WallpaperSlideshowManager(
    private val context: Context,
    private val ivBase: ImageView,
    private val ivNext: ImageView
) {
    companion object {
        private const val TAG = "WallpaperSlideshow"
        private const val PREFS_NAME = "b866f_launcher_prefs"

        const val KEY_SLIDESHOW_ENABLED = "key_slideshow_enabled"
        const val KEY_FOLDER_URI = "key_slideshow_folder_uri"
        const val KEY_FOLDER_NAME = "key_slideshow_folder_name"
        const val KEY_INTERVAL_SECONDS = "key_slideshow_interval_seconds"
        const val KEY_ORDER_MODE = "key_slideshow_order_mode"
        const val KEY_CURRENT_INDEX = "key_slideshow_current_index"
        const val KEY_TRANSITION_EFFECT = "key_slideshow_transition_effect"

        const val MODE_SEQUENTIAL = "sequential"
        const val MODE_RANDOM = "random"

        const val TRANSITION_FADE = "fade"
        const val TRANSITION_SLIDE = "slide"
        const val TRANSITION_ZOOM = "zoom"

        // Interval yang didukung sesuai permintaan: 10 Detik, 30 Detik, 1 Menit (60s), 3 Menit (180s)
        val SUPPORTED_INTERVALS_SECONDS = intArrayOf(10, 30, 60, 180)
    }

    var onSlideChanged: ((index: Int, total: Int) -> Unit)? = null

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val imageUris = mutableListOf<Uri>()
    private var slideshowJob: Job? = null
    private var currentIndex = 0
    private var currentBitmap: Bitmap? = null

    init {
        currentIndex = prefs.getInt(KEY_CURRENT_INDEX, 0)
    }

    /**
     * Membuat Intent untuk memilih satu folder gambar via SAF (ACTION_OPEN_DOCUMENT_TREE).
     */
    fun createFolderPickerIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            putExtra(Intent.EXTRA_LOCAL_ONLY, true)
        }
    }

    /**
     * Memproses hasil seleksi folder dari SAF.
     */
    suspend fun onFolderSelected(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal mengambil persistable permission: ${e.message}")
        }

        val folderName = getFolderDisplayName(treeUri)

        prefs.edit()
            .putString(KEY_FOLDER_URI, treeUri.toString())
            .putString(KEY_FOLDER_NAME, folderName)
            .putBoolean(KEY_SLIDESHOW_ENABLED, true)
            .putInt(KEY_CURRENT_INDEX, 0)
            .apply()

        currentIndex = 0
        scanImagesInFolder(treeUri)
    }

    /**
     * Memproses hasil seleksi folder langsung dari Browser Folder Internal STB / USB Flashdisk.
     */
    suspend fun onLocalFolderSelected(folderPath: String, displayName: String): Int = withContext(Dispatchers.IO) {
        val uriStr = if (folderPath.startsWith("file://")) folderPath else "file://$folderPath"

        prefs.edit()
            .putString(KEY_FOLDER_URI, uriStr)
            .putString(KEY_FOLDER_NAME, displayName)
            .putBoolean(KEY_SLIDESHOW_ENABLED, true)
            .putInt(KEY_CURRENT_INDEX, 0)
            .apply()

        currentIndex = 0
        scanImagesFromLocalPath(folderPath.removePrefix("file://"))
    }

    /**
     * Memindai file gambar yang didukung (jpg, jpeg, png, webp, heic) dari direktori lokal.
     * Menggunakan walkTopDown(maxDepth=4) agar seluruh subfolder (misal DCIM/Camera, Pictures/Wallpaper) terdeteksi.
     */
    fun scanImagesFromLocalPath(path: String): Int {
        val detected = mutableListOf<Uri>()
        try {
            val dir = File(path)
            if (dir.exists()) {
                if (dir.isFile && isSupportedFileName(dir.name)) {
                    detected.add(Uri.fromFile(dir))
                } else if (dir.isDirectory) {
                    dir.walkTopDown().maxDepth(4).forEach { file ->
                        if (file.isFile && isSupportedFileName(file.name) && file.length() > 0) {
                            detected.add(Uri.fromFile(file))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat memindai gambar dari path lokal: $path", e)
        }

        // Fallback: Jika direct File traversal menghasilkan 0 (karena Scoped Storage di Android 11+), gunakan MediaStore
        if (detected.isEmpty()) {
            val mediaStoreUris = queryMediaStoreForFolder(path)
            detected.addAll(mediaStoreUris)
        }

        synchronized(imageUris) {
            imageUris.clear()
            imageUris.addAll(detected)
        }

        Log.d(TAG, "Ditemukan ${detected.size} file gambar di path lokal: $path")
        return detected.size
    }

    /**
     * Memindai file gambar yang didukung dari folder SAF.
     * Mendukung rekursif ke subfolder, translasi ke direct file path, dan fallback ke MediaStore.
     */
    suspend fun scanImagesInFolder(treeUri: Uri): Int = withContext(Dispatchers.IO) {
        val detected = mutableListOf<Uri>()
        try {
            val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDoc != null && rootDoc.exists()) {
                scanDocumentFileRecursive(rootDoc, detected, depth = 0, maxDepth = 3)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat memindai gambar dari folder SAF", e)
        }

        // Jika DocumentFile SAF mengembalikan 0 (sering terjadi akibat bug DocumentsUI STB), coba selesaikan path lokalnya
        if (detected.isEmpty()) {
            val localPath = resolveLocalPathFromUri(treeUri)
            if (localPath != null) {
                val directDir = File(localPath)
                if (directDir.exists()) {
                    directDir.walkTopDown().maxDepth(4).forEach { file ->
                        if (file.isFile && isSupportedFileName(file.name) && file.length() > 0) {
                            detected.add(Uri.fromFile(file))
                        }
                    }
                }
            }
        }

        // Fallback MediaStore jika masih 0
        if (detected.isEmpty()) {
            val folderName = getFolderDisplayName(treeUri)
            val mediaStoreUris = queryMediaStoreForFolder(folderName)
            detected.addAll(mediaStoreUris)
        }

        synchronized(imageUris) {
            imageUris.clear()
            imageUris.addAll(detected)
        }

        Log.d(TAG, "Ditemukan ${detected.size} file gambar di folder SAF: $treeUri")
        detected.size
    }

    private fun scanDocumentFileRecursive(
        doc: DocumentFile,
        outList: MutableList<Uri>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return
        val files = doc.listFiles()
        for (file in files) {
            if (file.isFile && isSupportedImage(file)) {
                outList.add(file.uri)
            } else if (file.isDirectory && depth < maxDepth) {
                scanDocumentFileRecursive(file, outList, depth + 1, maxDepth)
            }
        }
    }

    private fun resolveLocalPathFromUri(uri: Uri): String? {
        try {
            if (uri.scheme == "file") return uri.path
            val path = uri.path ?: return null
            if (path.contains("primary:")) {
                val rel = path.substringAfter("primary:")
                return "/storage/emulated/0/$rel"
            }
            if (path.contains(":")) {
                val segment = path.substringAfterLast(":")
                val drive = path.substringBeforeLast(":").substringAfterLast("/")
                if (drive.matches(Regex("[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}"))) {
                    return "/storage/$drive/$segment"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Gagal resolve path: ${e.message}")
        }
        return null
    }

    private fun queryMediaStoreForFolder(folderHint: String): List<Uri> {
        val list = mutableListOf<Uri>()
        try {
            val cleanHint = folderHint.substringAfterLast("/").trim()
            val projection = arrayOf(
                android.provider.MediaStore.Images.Media._ID,
                android.provider.MediaStore.Images.Media.DATA
            )
            val cursor = context.contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${android.provider.MediaStore.Images.Media.DATE_MODIFIED} DESC"
            )
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                val dataCol = c.getColumnIndex(android.provider.MediaStore.Images.Media.DATA)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val dataPath = if (dataCol != -1) c.getString(dataCol) else null
                    val uri = Uri.withAppendedPath(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    if (dataPath != null) {
                        if (dataPath.contains(cleanHint, ignoreCase = true) && isSupportedFileName(dataPath)) {
                            list.add(uri)
                        }
                    } else {
                        list.add(uri)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore query error: ${e.message}")
        }
        return list
    }

    private fun isSupportedImage(file: DocumentFile): Boolean {
        val type = file.type?.lowercase()?.trim()
        if (type != null && (type.startsWith("image/") || type == "image/*")) {
            return true
        }
        val name = file.name ?: return false
        return isSupportedFileName(name)
    }

    private fun isSupportedFileName(name: String?): Boolean {
        if (name.isNullOrBlank()) return false
        val lower = Uri.decode(name).lowercase().trim()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".webp") ||
                lower.endsWith(".bmp") || lower.endsWith(".gif") ||
                lower.endsWith(".heic")
    }

    private fun getFolderDisplayName(treeUri: Uri): String {
        return try {
            val doc = DocumentFile.fromTreeUri(context, treeUri)
            doc?.name ?: treeUri.lastPathSegment ?: "Folder Foto"
        } catch (e: Exception) {
            "Folder Foto"
        }
    }

    /**
     * Menjalankan siklus otomatis Wallpaper Slideshow.
     */
    fun startSlideshow(scope: CoroutineScope) {
        if (slideshowJob?.isActive == true && imageUris.isNotEmpty()) {
            return
        }
        stopSlideshow()

        if (!isSlideshowEnabled()) return
        val savedUriStr = getSavedFolderUri() ?: return

        slideshowJob = scope.launch(Dispatchers.Main) {
            if (imageUris.isEmpty()) {
                val count = withContext(Dispatchers.IO) {
                    if (savedUriStr.startsWith("file://")) {
                        scanImagesFromLocalPath(savedUriStr.removePrefix("file://"))
                    } else {
                        scanImagesInFolder(Uri.parse(savedUriStr))
                    }
                }
                if (count == 0) {
                    Log.w(TAG, "Folder tidak memiliki gambar yang didukung.")
                    return@launch
                }
            }

            // Tampilkan foto saat ini segera saat launcher dibuka jika belum ada bitmap
            if (currentBitmap == null) {
                displayCurrentOrNext(animated = false)
            }

            // Loop berganti otomatis berdasarkan interval detik yang dipilih (30s, 60s, 180s)
            while (isActive) {
                val intervalSeconds = getIntervalSeconds()
                val delayMs = intervalSeconds * 1000L
                delay(delayMs)

                if (!isActive) break
                advanceToNextImage(animated = true)
            }
        }
    }

    /**
     * Menghentikan siklus otomatis.
     */
    fun stopSlideshow() {
        slideshowJob?.cancel()
        slideshowJob = null
    }

    /**
     * Memajukan ke foto berikutnya (Sequential atau Random) dengan transisi Fade.
     */
    fun advanceToNextImage(animated: Boolean = true) {
        synchronized(imageUris) {
            if (imageUris.isEmpty()) return

            val mode = getOrderMode()
            if (mode == MODE_RANDOM && imageUris.size > 1) {
                var next = Random.nextInt(imageUris.size)
                while (next == currentIndex) {
                    next = Random.nextInt(imageUris.size)
                }
                currentIndex = next
            } else {
                currentIndex = (currentIndex + 1) % imageUris.size
            }
            prefs.edit().putInt(KEY_CURRENT_INDEX, currentIndex).apply()
        }

        displayCurrentOrNext(animated = animated)
    }

    /**
     * Menampilkan foto saat ini ke ImageView dengan optimasi decoding dan transisi crossfade.
     */
    private fun displayCurrentOrNext(animated: Boolean) {
        val uri = synchronized(imageUris) {
            if (imageUris.isEmpty()) null
            else imageUris.getOrNull(currentIndex) ?: imageUris.firstOrNull()
        } ?: return

        CoroutineScope(Dispatchers.IO).launch {
            val bitmap = decodeSampledBitmapFromUri(uri, reqWidth = 1920, reqHeight = 1080)
            withContext(Dispatchers.Main) {
                if (bitmap != null) {
                    applyBitmapTransition(bitmap, animated)
                } else {
                    Log.w(TAG, "Gagal mendecode bitmap dari URI: $uri, beralih ke berikutnya...")
                    advanceToNextImage(animated = false)
                }
            }
        }
    }

    /**
     * Transisi Fade halus (Crossfade) antara ivBase dan ivNext.
     */
    /**
     * Efek Transisi: Fade (memudar), Slide (geser), Zoom (perlahan membesar).
     */
    private fun applyBitmapTransition(newBitmap: Bitmap, animated: Boolean) {
        onSlideChanged?.invoke(currentIndex, imageUris.size)

        if (!animated) {
            ivBase.setImageBitmap(newBitmap)
            ivBase.translationX = 0f
            ivBase.scaleX = 1f
            ivBase.scaleY = 1f
            ivNext.visibility = View.GONE
            ivNext.alpha = 0f
            ivNext.translationX = 0f
            ivNext.scaleX = 1f
            ivNext.scaleY = 1f
            currentBitmap = newBitmap
            return
        }

        val effect = getTransitionEffect()
        val screenW = ivBase.width.takeIf { it > 0 }?.toFloat() ?: 1920f

        ivNext.setImageBitmap(newBitmap)
        ivNext.visibility = View.VISIBLE

        when (effect) {
            TRANSITION_SLIDE -> {
                ivNext.alpha = 1f
                ivNext.scaleX = 1f
                ivNext.scaleY = 1f
                ivNext.translationX = screenW

                ivNext.animate()
                    .translationX(0f)
                    .setDuration(700L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator(1.2f))
                    .start()

                ivBase.animate()
                    .translationX(-screenW)
                    .setDuration(700L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator(1.2f))
                    .withEndAction {
                        ivBase.setImageBitmap(newBitmap)
                        ivBase.translationX = 0f
                        ivNext.visibility = View.GONE
                        ivNext.translationX = 0f
                        currentBitmap = newBitmap
                    }
                    .start()
            }
            TRANSITION_ZOOM -> {
                ivNext.alpha = 0f
                ivNext.scaleX = 1.18f
                ivNext.scaleY = 1.18f
                ivNext.translationX = 0f

                ivNext.animate()
                    .alpha(1f)
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(900L)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .withEndAction {
                        ivBase.setImageBitmap(newBitmap)
                        ivNext.visibility = View.GONE
                        ivNext.alpha = 0f
                        currentBitmap = newBitmap
                    }
                    .start()
            }
            else -> { // TRANSITION_FADE
                ivNext.alpha = 0f
                ivNext.translationX = 0f
                ivNext.scaleX = 1f
                ivNext.scaleY = 1f

                ivNext.animate()
                    .alpha(1f)
                    .setDuration(800L)
                    .withEndAction {
                        ivBase.setImageBitmap(newBitmap)
                        ivNext.visibility = View.GONE
                        ivNext.alpha = 0f
                        currentBitmap = newBitmap
                    }
                    .start()
            }
        }
    }

    fun nextSlide() {
        advanceToNextImage(animated = true)
    }

    fun prevSlide() {
        synchronized(imageUris) {
            if (imageUris.isEmpty()) return
            currentIndex = if (currentIndex - 1 < 0) imageUris.size - 1 else currentIndex - 1
            prefs.edit().putInt(KEY_CURRENT_INDEX, currentIndex).apply()
        }
        displayCurrentOrNext(animated = true)
    }

    fun advanceToPreviousImage(animated: Boolean = true) {
        prevSlide()
    }

    fun setOnSlideChangedListener(listener: (Int, Int) -> Unit) {
        onSlideChanged = listener
    }

    fun getCurrentIndex(): Int = currentIndex
    fun getTotalImages(): Int = imageUris.size

    fun getTransitionEffect(): String = prefs.getString(KEY_TRANSITION_EFFECT, TRANSITION_FADE) ?: TRANSITION_FADE

    fun setTransitionEffect(effect: String) {
        prefs.edit().putString(KEY_TRANSITION_EFFECT, effect).apply()
    }

    /**
     * Optimasi decoding gambar untuk Android TV (ZTE B866F):
     * 1. Hitung inSampleSize berdasarkan resolusi layar 1080p (1920x1080).
     * 2. Gunakan Bitmap.Config.RGB_565 (menghemat RAM 50% dibanding ARGB_8888).
     * 3. Mencegah OutOfMemoryError jika folder berisi foto kamera resolusi tinggi (>24MP).
     */
    private fun decodeSampledBitmapFromUri(
        uri: Uri,
        reqWidth: Int = 1920,
        reqHeight: Int = 1080
    ): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            // RGB_565: 2 bytes per pixel tanpa alpha channel, hemat memori pada STB
            options.inPreferredConfig = Bitmap.Config.RGB_565
            options.inDither = true

            openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Gagal mendecode sampled bitmap dari $uri", e)
            null
        }
    }

    private fun openInputStream(uri: Uri) = try {
        if (uri.scheme == "file") {
            FileInputStream(File(uri.path ?: ""))
        } else {
            context.contentResolver.openInputStream(uri)
        }
    } catch (e: Exception) {
        null
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    // ==========================================
    // GETTERS & SETTERS SETELAN SLIDESHOW
    // ==========================================

    fun isSlideshowEnabled(): Boolean {
        return prefs.getBoolean(KEY_SLIDESHOW_ENABLED, false) && !getSavedFolderUri().isNullOrEmpty()
    }

    fun setSlideshowEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SLIDESHOW_ENABLED, enabled).apply()
    }

    fun getSavedFolderUri(): String? {
        return prefs.getString(KEY_FOLDER_URI, null)
    }

    fun getSavedFolderName(): String {
        return prefs.getString(KEY_FOLDER_NAME, null) ?: "Belum dipilih"
    }

    fun getIntervalSeconds(): Int {
        val stored = prefs.getInt(KEY_INTERVAL_SECONDS, -1)
        if (stored in SUPPORTED_INTERVALS_SECONDS) {
            return stored
        }
        // Migrasi dari format lama atau default: 60 detik (1 menit)
        return 60
    }

    fun setIntervalSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_INTERVAL_SECONDS, seconds).apply()
    }

    fun getOrderMode(): String {
        return prefs.getString(KEY_ORDER_MODE, MODE_SEQUENTIAL) ?: MODE_SEQUENTIAL
    }

    fun setOrderMode(mode: String) {
        prefs.edit().putString(KEY_ORDER_MODE, mode).apply()
    }

    fun getImageCount(): Int {
        return synchronized(imageUris) { imageUris.size }
    }

    /**
     * Menghapus konfigurasi slideshow dan mengembalikan wallpaper ke statis.
     */
    fun clearSlideshowSettings() {
        stopSlideshow()
        synchronized(imageUris) { imageUris.clear() }
        prefs.edit()
            .putBoolean(KEY_SLIDESHOW_ENABLED, false)
            .remove(KEY_FOLDER_URI)
            .remove(KEY_FOLDER_NAME)
            .remove(KEY_CURRENT_INDEX)
            .apply()
    }
}
