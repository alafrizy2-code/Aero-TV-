package com.example.theme

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import com.example.AppCategory
import com.example.AppModel
import java.util.Locale

data class ProjectivyWatchNextItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val packageName: String,
    val posterUri: String?,
    val intentUri: String?,
    val progressFraction: Float,
    val sourceName: String
)

data class ProjectivyContentSource(
    val id: String,
    val label: String,
    val packageName: String? = null,
    val isExternalTvChannel: Boolean = false
)

interface ProjectivyExternalWatchNextSource {
    suspend fun fetchWatchNextItems(): List<ProjectivyWatchNextItem>
    suspend fun fetchContentSources(): List<ProjectivyContentSource> = emptyList()
}

data class ProjectivyContentSnapshot(
    val watchNextItems: List<ProjectivyWatchNextItem>,
    val externalSources: List<ProjectivyContentSource>
)

/**
 * Penyedia data nyata untuk Projectivy Home Screen:
 * - Membaca Android TV Watch Next (content://android.media.tv/watch_next_program)
 *   dan Channels / PreviewPrograms jika tersedia & diizinkan oleh sistem.
 * - Mendukung sumber ekstensi nyata melalui ProjectivyExternalWatchNextSource.
 * - TIDAK PERNAH membuat konten film/rekomendasi fiktif: jika tidak ada sumber nyata,
 *   mengembalikan daftar kosong agar UI menampilkan area Watch Next kosong yang siap dikembangkan.
 */
object ProjectivyWatchNextProvider {

    private const val PREFS_THEME = "aero_theme_prefs"
    private const val KEY_PROJECTIVY_PINNED_PACKAGES = "key_projectivy_pinned_packages"

    private val WATCH_NEXT_URI: Uri = Uri.parse("content://android.media.tv/watch_next_program")
    private val PREVIEW_CHANNELS_URI: Uri = Uri.parse("content://android.media.tv/channel")
    private val PREVIEW_PROGRAMS_URI: Uri = Uri.parse("content://android.media.tv/preview_program")

    private val externalSources = mutableListOf<ProjectivyExternalWatchNextSource>()

    fun registerExternalSource(source: ProjectivyExternalWatchNextSource) {
        if (!externalSources.contains(source)) {
            externalSources.add(source)
        }
    }

    fun clearExternalSources() {
        externalSources.clear()
    }

    suspend fun loadWatchNextAndSources(context: Context): ProjectivyContentSnapshot {
        val watchNext = mutableListOf<ProjectivyWatchNextItem>()
        val sources = mutableListOf<ProjectivyContentSource>()
        val seenSourceIds = HashSet<String>()

        // 1. Query Android TV Watch Next Programs (nyata dari sistem bila tersedia)
        val systemWatchNext = querySystemWatchNext(context)
        watchNext.addAll(systemWatchNext)
        for (item in systemWatchNext) {
            val sid = "pkg:${item.packageName}"
            if (item.sourceName.isNotBlank() && seenSourceIds.add(sid)) {
                sources.add(
                    ProjectivyContentSource(
                        id = sid,
                        label = item.sourceName,
                        packageName = item.packageName,
                        isExternalTvChannel = true
                    )
                )
            }
        }

        // 2. Query Android TV Channels & Preview Programs (nyata dari sistem bila tersedia)
        val channelNames = querySystemChannels(context)
        for ((chId, chInfo) in channelNames) {
            val sid = "ch:$chId"
            if (seenSourceIds.add(sid)) {
                sources.add(
                    ProjectivyContentSource(
                        id = sid,
                        label = chInfo.first,
                        packageName = chInfo.second,
                        isExternalTvChannel = true
                    )
                )
            }
        }
        if (watchNext.isEmpty() && channelNames.isNotEmpty()) {
            watchNext.addAll(querySystemPreviewPrograms(context, channelNames))
        }

        // 3. Sumber ekstensi nyata tambahan (jika didaftarkan)
        for (ext in externalSources) {
            try {
                val extItems = ext.fetchWatchNextItems()
                watchNext.addAll(extItems)
                for (src in ext.fetchContentSources()) {
                    if (seenSourceIds.add(src.id)) {
                        sources.add(src)
                    }
                }
                for (item in extItems) {
                    val sid = "ext:${item.sourceName.lowercase(Locale.US)}"
                    if (item.sourceName.isNotBlank() && seenSourceIds.add(sid)) {
                        sources.add(
                            ProjectivyContentSource(
                                id = sid,
                                label = item.sourceName,
                                packageName = item.packageName.ifBlank { null },
                                isExternalTvChannel = true
                            )
                        )
                    }
                }
            } catch (_: Exception) { }
        }

        return ProjectivyContentSnapshot(
            watchNextItems = watchNext.distinctBy { "${it.packageName}:${it.id}:${it.title}" },
            externalSources = sources
        )
    }

    private fun querySystemWatchNext(context: Context): List<ProjectivyWatchNextItem> {
        val result = mutableListOf<ProjectivyWatchNextItem>()
        val projection = arrayOf(
            "_id",
            "title",
            "episode_title",
            "short_description",
            "poster_art_uri",
            "thumbnail_uri",
            "intent_uri",
            "package_name",
            "last_playback_position_millis",
            "duration_millis"
        )
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                WATCH_NEXT_URI,
                projection,
                null,
                null,
                "last_engagement_time_utc_millis DESC"
            )
            if (cursor != null) {
                while (cursor.moveToNext() && result.size < 20) {
                    val id = cursor.getLongOrDefault("_id", 0L)
                    val title = cursor.getStringOrNull("title")?.trim().orEmpty()
                    val epTitle = cursor.getStringOrNull("episode_title")?.trim().orEmpty()
                    val shortDesc = cursor.getStringOrNull("short_description")?.trim().orEmpty()
                    val posterUri = cursor.getStringOrNull("thumbnail_uri")
                        ?: cursor.getStringOrNull("poster_art_uri")
                    val intentUri = cursor.getStringOrNull("intent_uri")
                    val pkg = cursor.getStringOrNull("package_name")?.trim().orEmpty()
                    val posMs = cursor.getLongOrDefault("last_playback_position_millis", 0L)
                    val durMs = cursor.getLongOrDefault("duration_millis", 0L)

                    if (title.isNotEmpty() || !posterUri.isNullOrBlank()) {
                        val appLabel = resolvePackageLabel(context, pkg)
                        val progress = if (durMs > 0L && posMs > 0L) {
                            (posMs.toFloat() / durMs.toFloat()).coerceIn(0.02f, 1f)
                        } else {
                            0f
                        }
                        val sub = listOf(appLabel, epTitle.ifBlank { shortDesc })
                            .filter { it.isNotBlank() }
                            .joinToString(" • ")

                        result.add(
                            ProjectivyWatchNextItem(
                                id = id,
                                title = title.ifBlank { appLabel.ifBlank { "Watch Next" } },
                                subtitle = sub,
                                packageName = pkg,
                                posterUri = posterUri,
                                intentUri = intentUri,
                                progressFraction = progress,
                                sourceName = appLabel
                            )
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
            // Izin READ_EPG_DATA tidak diberikan oleh firmware TV -> kembalikan kosong dengan aman
        } catch (_: Exception) {
        } finally {
            try { cursor?.close() } catch (_: Exception) { }
        }
        return result
    }

    private fun querySystemChannels(context: Context): Map<Long, Pair<String, String?>> {
        val channels = linkedMapOf<Long, Pair<String, String?>>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                PREVIEW_CHANNELS_URI,
                arrayOf("_id", "display_name", "package_name", "browsable"),
                null,
                null,
                null
            )
            if (cursor != null) {
                while (cursor.moveToNext() && channels.size < 12) {
                    val id = cursor.getLongOrDefault("_id", -1L)
                    val displayName = cursor.getStringOrNull("display_name")?.trim().orEmpty()
                    val pkg = cursor.getStringOrNull("package_name")?.trim()
                    val browsable = cursor.getLongOrDefault("browsable", 1L)
                    if (id >= 0L && displayName.isNotBlank() && browsable != 0L) {
                        channels[id] = Pair(displayName, pkg)
                    }
                }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        } finally {
            try { cursor?.close() } catch (_: Exception) { }
        }
        return channels
    }

    private fun querySystemPreviewPrograms(
        context: Context,
        channels: Map<Long, Pair<String, String?>>
    ): List<ProjectivyWatchNextItem> {
        val result = mutableListOf<ProjectivyWatchNextItem>()
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                PREVIEW_PROGRAMS_URI,
                arrayOf(
                    "_id",
                    "channel_id",
                    "title",
                    "short_description",
                    "poster_art_uri",
                    "thumbnail_uri",
                    "intent_uri",
                    "package_name",
                    "last_playback_position_millis",
                    "duration_millis"
                ),
                null,
                null,
                null
            )
            if (cursor != null) {
                while (cursor.moveToNext() && result.size < 16) {
                    val id = cursor.getLongOrDefault("_id", 0L)
                    val chId = cursor.getLongOrDefault("channel_id", -1L)
                    val chInfo = channels[chId] ?: continue
                    val title = cursor.getStringOrNull("title")?.trim().orEmpty()
                    val shortDesc = cursor.getStringOrNull("short_description")?.trim().orEmpty()
                    val posterUri = cursor.getStringOrNull("thumbnail_uri")
                        ?: cursor.getStringOrNull("poster_art_uri")
                    val intentUri = cursor.getStringOrNull("intent_uri")
                    val pkg = cursor.getStringOrNull("package_name")?.trim()
                        ?: chInfo.second.orEmpty()
                    val posMs = cursor.getLongOrDefault("last_playback_position_millis", 0L)
                    val durMs = cursor.getLongOrDefault("duration_millis", 0L)
                    if (title.isNotEmpty() || !posterUri.isNullOrBlank()) {
                        val progress = if (durMs > 0L && posMs > 0L) {
                            (posMs.toFloat() / durMs.toFloat()).coerceIn(0.02f, 1f)
                        } else {
                            0f
                        }
                        result.add(
                            ProjectivyWatchNextItem(
                                id = id,
                                title = title.ifBlank { chInfo.first },
                                subtitle = shortDesc.ifBlank { chInfo.first },
                                packageName = pkg,
                                posterUri = posterUri,
                                intentUri = intentUri,
                                progressFraction = progress,
                                sourceName = chInfo.first
                            )
                        )
                    }
                }
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        } finally {
            try { cursor?.close() } catch (_: Exception) { }
        }
        return result
    }

    private fun resolvePackageLabel(context: Context, packageName: String): String {
        if (packageName.isBlank()) return ""
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            ""
        } catch (_: Exception) {
            ""
        }
    }

    fun getPinnedPackages(context: Context): List<String> {
        val raw = context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .getString(KEY_PROJECTIVY_PINNED_PACKAGES, "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun isPinned(context: Context, packageName: String): Boolean {
        return getPinnedPackages(context).contains(packageName)
    }

    fun togglePinnedPackage(context: Context, packageName: String): Boolean {
        val current = getPinnedPackages(context).toMutableList()
        val nowPinned = if (current.contains(packageName)) {
            current.remove(packageName)
            false
        } else {
            current.add(0, packageName)
            true
        }
        context.getSharedPreferences(PREFS_THEME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROJECTIVY_PINNED_PACKAGES, current.joinToString("|"))
            .apply()
        return nowPinned
    }

    fun orderAppsForProjectivy(
        context: Context,
        apps: List<AppModel>,
        categoryFilter: AppCategory? = null
    ): List<AppModel> {
        val filtered = if (categoryFilter == null) apps else apps.filter { it.category == categoryFilter }
        val pinnedOrder = getPinnedPackages(context)
        if (pinnedOrder.isEmpty()) return filtered
        val pinIndex = pinnedOrder.withIndex().associate { (i, pkg) -> pkg to i }
        val pinned = filtered.filter { pinIndex.containsKey(it.packageName) }
            .sortedBy { pinIndex[it.packageName] ?: Int.MAX_VALUE }
        val rest = filtered.filter { !pinIndex.containsKey(it.packageName) }
        return pinned + rest
    }

    private fun Cursor.getStringOrNull(colName: String): String? {
        val idx = getColumnIndex(colName)
        if (idx < 0 || isNull(idx)) return null
        return getString(idx)
    }

    private fun Cursor.getLongOrDefault(colName: String, fallback: Long): Long {
        val idx = getColumnIndex(colName)
        if (idx < 0 || isNull(idx)) return fallback
        return getLong(idx)
    }
}
