package com.example

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Data Model untuk Folder Kategori Aplikasi di Dock Bawah Aero TV.
 */
data class AppFolder(
    val id: String,
    var name: String,
    val packageNames: MutableList<String> = mutableListOf()
)

/**
 * Pengelola Folder Kategori Aplikasi:
 * - Menyimpan struktur kategori dan aplikasi ke SharedPreferences (JSON).
 * - Mendukung tambah folder kategori baru dari menu Setting.
 * - Mengubah nama folder.
 * - Menghapus folder.
 * - Menambah dan menghapus aplikasi di dalam folder.
 */
class AppFolderManager(private val context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("b866f_folders_prefs", Context.MODE_PRIVATE)
    private val KEY_FOLDERS_JSON = "key_folders_json"

    private val folders = mutableListOf<AppFolder>()

    fun getFolders(): List<AppFolder> = folders

    fun initFolders(installedApps: List<AppModel>) {
        folders.clear()
        val jsonStr = prefs.getString(KEY_FOLDERS_JSON, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    val name = obj.getString("name")
                    val pkgsArray = obj.optJSONArray("packageNames")
                    val pkgs = mutableListOf<String>()
                    if (pkgsArray != null) {
                        for (j in 0 until pkgsArray.length()) {
                            pkgs.add(pkgsArray.getString(j))
                        }
                    }
                    folders.add(AppFolder(id, name, pkgs))
                }
            } catch (e: Exception) {
                folders.clear()
            }
        }

        // Jika belum ada folder tersimpan, buat kategori pintar default
        if (folders.isEmpty()) {
            val streamingPkgs = mutableListOf<String>()
            val gamesPkgs = mutableListOf<String>()
            val toolsPkgs = mutableListOf<String>()
            val othersPkgs = mutableListOf<String>()

            for (app in installedApps) {
                val lowerPkg = app.packageName.lowercase()
                val lowerName = app.appName.lowercase()

                when {
                    lowerPkg.contains("youtube") || lowerPkg.contains("netflix") || lowerPkg.contains("vidio") ||
                    lowerPkg.contains("stream") || lowerPkg.contains("vision") || lowerPkg.contains("tizen") ||
                    lowerPkg.contains("tv") || lowerName.contains("tv") || lowerName.contains("movie") || lowerName.contains("cinema") -> {
                        streamingPkgs.add(app.packageName)
                    }
                    lowerPkg.contains("game") || lowerPkg.contains("ppsspp") || lowerPkg.contains("retro") ||
                    lowerPkg.contains("play") || lowerName.contains("game") -> {
                        gamesPkgs.add(app.packageName)
                    }
                    lowerPkg.contains("settings") || lowerPkg.contains("browser") || lowerPkg.contains("chrome") ||
                    lowerPkg.contains("file") || lowerPkg.contains("tool") -> {
                        toolsPkgs.add(app.packageName)
                    }
                    else -> {
                        othersPkgs.add(app.packageName)
                    }
                }
            }

            // Pastikan folder tidak kosong saat pertama kali dibuka
            if (streamingPkgs.isEmpty() && installedApps.isNotEmpty()) {
                streamingPkgs.addAll(installedApps.take(4).map { it.packageName })
            }

            folders.add(AppFolder("folder_streaming", "🎬 Streaming & TV", streamingPkgs))
            folders.add(AppFolder("folder_games", "🎮 Games", gamesPkgs))
            folders.add(AppFolder("folder_tools", "🛠️ Alat & Sistem", toolsPkgs))
            if (othersPkgs.isNotEmpty()) {
                folders.add(AppFolder("folder_others", "📁 Lainnya", othersPkgs))
            }
            saveFolders()
        }
    }

    fun createFolder(name: String): AppFolder {
        val newFolder = AppFolder(
            id = "folder_" + System.currentTimeMillis(),
            name = name.trim().ifEmpty { "Kategori Baru" },
            packageNames = mutableListOf()
        )
        folders.add(newFolder)
        saveFolders()
        return newFolder
    }

    fun renameFolder(id: String, newName: String) {
        val folder = folders.find { it.id == id }
        if (folder != null) {
            folder.name = newName.trim().ifEmpty { folder.name }
            saveFolders()
        }
    }

    fun deleteFolder(id: String): Boolean {
        val removed = folders.removeAll { it.id == id }
        if (removed) {
            saveFolders()
        }
        return removed
    }

    fun addAppToFolder(folderId: String, packageName: String) {
        val folder = folders.find { it.id == folderId }
        if (folder != null && !folder.packageNames.contains(packageName)) {
            folder.packageNames.add(packageName)
            saveFolders()
        }
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        val folder = folders.find { it.id == folderId }
        if (folder != null) {
            folder.packageNames.remove(packageName)
            saveFolders()
        }
    }

    fun saveFolders() {
        val array = JSONArray()
        for (f in folders) {
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            val pkgsArray = JSONArray()
            for (pkg in f.packageNames) {
                pkgsArray.put(pkg)
            }
            obj.put("packageNames", pkgsArray)
            array.put(obj)
        }
        prefs.edit().putString(KEY_FOLDERS_JSON, array.toString()).apply()
    }
}
