package com.example

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * Model untuk 1 dari 8 Slot Aplikasi Favorit di Dock & Laci Aplikasi Aero TV.
 */
data class FavoriteSlot(
    val index: Int,            // 0..7
    val packageName: String?,  // null jika slot kosong
    val appModel: AppModel? = null // referensi data app terinstal
)

/**
 * Pengelola 8 Slot Favorit TV:
 * - Tepat 8 slot favorit.
 * - Menyimpan otomatis ke SharedPreferences dalam format JSON.
 * - Mendukung tambah, hapus, ganti aplikasi, dan mengatur urutan (tukar posisi).
 * - Pre-populasi otomatis saat pertama kali dibuka dengan aplikasi terbaik yang terpasang di STB.
 */
class FavoritesManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("b866f_favorites_prefs", Context.MODE_PRIVATE)

    private val KEY_FAVORITES_8 = "key_favorite_slots_v350"
    val TOTAL_SLOTS = 8

    // List berukuran 8 berisi packageName (atau null untuk slot kosong)
    private val slots = MutableList<String?>(TOTAL_SLOTS) { null }

    fun init(installedApps: List<AppModel>) {
        val savedJson = prefs.getString(KEY_FAVORITES_8, null)
        if (!savedJson.isNullOrEmpty()) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until TOTAL_SLOTS) {
                    if (i < array.length()) {
                        val pkg = array.optString(i, "")
                        slots[i] = if (pkg.isNotEmpty()) pkg else null
                    } else {
                        slots[i] = null
                    }
                }
            } catch (e: Exception) {
                // fallback to default
                populateDefaults(installedApps)
            }
        } else {
            populateDefaults(installedApps)
        }
    }

    private fun populateDefaults(installedApps: List<AppModel>) {
        for (i in 0 until TOTAL_SLOTS) {
            slots[i] = null
        }

        val prioritizedKeywords = listOf(
            "youtube", "cloudstream", "gvision", "bit tv", "tizentube",
            "ppsspp", "netflix", "vidio", "disney", "prime"
        )

        var slotIndex = 0
        val usedPkgs = mutableSetOf<String>()

        // 1. Masukkan aplikasi prioritas yang ada di STB
        for (kw in prioritizedKeywords) {
            if (slotIndex >= TOTAL_SLOTS) break
            val match = installedApps.find {
                (it.appName.lowercase().contains(kw) || it.packageName.lowercase().contains(kw)) &&
                        !usedPkgs.contains(it.packageName)
            }
            if (match != null) {
                slots[slotIndex] = match.packageName
                usedPkgs.add(match.packageName)
                slotIndex++
            }
        }

        // 2. Jika masih ada slot kosong, isi dengan aplikasi terpasang berikutnya
        for (app in installedApps) {
            if (slotIndex >= TOTAL_SLOTS) break
            if (!usedPkgs.contains(app.packageName)) {
                slots[slotIndex] = app.packageName
                usedPkgs.add(app.packageName)
                slotIndex++
            }
        }

        save()
    }

    fun getFavoriteSlots(installedApps: List<AppModel>): List<FavoriteSlot> {
        val appMap = installedApps.associateBy { it.packageName }
        return (0 until TOTAL_SLOTS).map { idx ->
            val pkg = slots[idx]
            val model = if (pkg != null) appMap[pkg] else null
            FavoriteSlot(index = idx, packageName = pkg, appModel = model)
        }
    }

    fun getSlotPackage(index: Int): String? {
        if (index in 0 until TOTAL_SLOTS) {
            return slots[index]
        }
        return null
    }

    fun setSlot(index: Int, packageName: String?) {
        if (index in 0 until TOTAL_SLOTS) {
            // Hapus duplikasi jika app ini ada di slot lain
            if (packageName != null) {
                for (i in 0 until TOTAL_SLOTS) {
                    if (i != index && slots[i] == packageName) {
                        slots[i] = null
                    }
                }
            }
            slots[index] = packageName
            save()
        }
    }

    fun removeSlot(index: Int) {
        if (index in 0 until TOTAL_SLOTS) {
            slots[index] = null
            save()
        }
    }

    fun swapSlots(fromIndex: Int, toIndex: Int): Boolean {
        if (fromIndex in 0 until TOTAL_SLOTS && toIndex in 0 until TOTAL_SLOTS && fromIndex != toIndex) {
            val temp = slots[fromIndex]
            slots[fromIndex] = slots[toIndex]
            slots[toIndex] = temp
            save()
            return true
        }
        return false
    }

    fun addFavoriteToFirstEmptySlot(packageName: String): Boolean {
        if (isFavorite(packageName)) return false
        val emptyIndex = slots.indexOfFirst { it == null }
        if (emptyIndex != -1) {
            slots[emptyIndex] = packageName
            save()
            return true
        }
        return false
    }

    fun isFavorite(packageName: String): Boolean {
        return slots.contains(packageName)
    }

    private fun save() {
        val array = JSONArray()
        for (i in 0 until TOTAL_SLOTS) {
            array.put(slots[i] ?: "")
        }
        prefs.edit().putString(KEY_FAVORITES_8, array.toString()).apply()
    }
}
