package com.example

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object WeatherHelper {
    private const val TAG = "WeatherHelper"
    private const val PREFS_WEATHER = "aero_weather_cache_prefs"
    private const val KEY_LAST_LAT = "last_lat"
    private const val KEY_LAST_LON = "last_lon"
    private const val KEY_LAST_KECAMATAN = "last_kecamatan"

    // Thread terpisah untuk antrean cuaca & callback lokasi agar TIDAK PERNAH terjadi deadlock
    private val weatherExecutor = Executors.newSingleThreadExecutor()
    private val callbackExecutor = Executors.newCachedThreadPool()
    private val mainHandler = Handler(Looper.getMainLooper())

    private const val FALLBACK_LAT = -6.2088
    private const val FALLBACK_LON = 106.8456
    private const val FALLBACK_NAME = "Lokasi Perangkat"

    data class LocationData(
        val lat: Double,
        val lon: Double,
        val districtName: String,
        val source: String = "Fallback"
    )

    private data class WeatherResult(
        val tempC: Int,
        val weatherCode: Int,
        val isDay: Int
    )

    data class WeatherSnapshot(
        val districtName: String,
        val tempC: Int?,
        val condition: String,
        val weatherCode: Int,
        val isDay: Int,
        val formattedText: String
    )

    @Volatile
    private var cachedWeatherString: String? = null
    @Volatile
    private var cachedTempC: Int? = null
    @Volatile
    private var cachedCondition: String? = null
    @Volatile
    private var cachedDistrictName: String? = null
    @Volatile
    private var cachedWeatherCode: Int? = null
    @Volatile
    private var cachedIsDay: Int = 1
    @Volatile
    private var lastFetchTime: Long = 0L
    @Volatile
    private var isFetching: Boolean = false
    @Volatile
    private var pendingForceRefresh: Boolean = false

    private var lastTextViewRef: WeakReference<TextView>? = null
    private var lastOnWeatherCodeFetched: ((Int, Int) -> Unit)? = null
    private var lastOnResult: ((String) -> Unit)? = null

    fun invalidateCache() {
        cachedWeatherString = null
        cachedTempC = null
        cachedCondition = null
        cachedDistrictName = null
        cachedWeatherCode = null
        lastFetchTime = 0L
    }

    fun getCachedDistrictName(context: Context): String? {
        return cachedDistrictName ?: context.getSharedPreferences(PREFS_WEATHER, Context.MODE_PRIVATE)
            .getString(KEY_LAST_KECAMATAN, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun getCachedSnapshot(context: Context): WeatherSnapshot? {
        val text = cachedWeatherString ?: return null
        val code = cachedWeatherCode ?: return null
        val district = cachedDistrictName ?: getCachedDistrictName(context) ?: FALLBACK_NAME
        val condition = cachedCondition ?: mapWeatherCodeToIndonesian(code)
        return WeatherSnapshot(
            districtName = district,
            tempC = cachedTempC,
            condition = condition,
            weatherCode = code,
            isDay = cachedIsDay,
            formattedText = text
        )
    }

    /**
     * Mendeteksi lokasi perangkat saat ini secara berlapis tanpa memblokir UI:
     * 1. FusedLocationProviderClient (lastLocation & Wi-Fi/Balanced lalu High Accuracy)
     * 2. LocationManager sistem (FUSED / NETWORK / GPS / PASSIVE + permintaan lokasi aktif)
     * 3. IP Geolocation HTTPS (khusus STB Android TV tanpa GPS atau saat Wi-Fi location belum merespons)
     * 4. Koordinat terakhir yang tersimpan di SharedPreferences
     */
    @SuppressLint("MissingPermission")
    fun detectDeviceLocation(context: Context): LocationData {
        var bestLocation: Location? = null

        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            // 1. FusedLocationProviderClient (menggunakan Wi-Fi scanning & Google Location Services)
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                val lastLoc = try {
                    Tasks.await(fusedClient.lastLocation, 1200, TimeUnit.MILLISECONDS)
                } catch (_: Exception) {
                    null
                }
                if (isValidLocation(lastLoc)) {
                    bestLocation = lastLoc
                }

                // Jika lastLocation kosong atau berumur > 10 menit, minta lokasi aktif berbasis Wi-Fi terlebih dahulu
                val isStale = bestLocation == null || (System.currentTimeMillis() - bestLocation.time) > 10 * 60 * 1000L
                if (isStale) {
                    val wifiCts = CancellationTokenSource()
                    val wifiLoc = try {
                        Tasks.await(
                            fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, wifiCts.token),
                            2500,
                            TimeUnit.MILLISECONDS
                        )
                    } catch (_: Exception) {
                        wifiCts.cancel()
                        null
                    }
                    if (isValidLocation(wifiLoc)) {
                        bestLocation = wifiLoc
                    } else if (bestLocation == null && hasFine) {
                        val fineCts = CancellationTokenSource()
                        val fineLoc = try {
                            Tasks.await(
                                fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, fineCts.token),
                                2000,
                                TimeUnit.MILLISECONDS
                            )
                        } catch (_: Exception) {
                            fineCts.cancel()
                            null
                        }
                        if (isValidLocation(fineLoc)) {
                            bestLocation = fineLoc
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "FusedLocationProvider tidak tersedia/gagal", e)
            }

            // 2. LocationManager sistem (termasuk FUSED_PROVIDER API 31+ dan NETWORK_PROVIDER untuk Wi-Fi)
            try {
                val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                if (lm != null) {
                    val providers = buildList {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            add(LocationManager.FUSED_PROVIDER)
                        }
                        add(LocationManager.NETWORK_PROVIDER)
                        add(LocationManager.GPS_PROVIDER)
                        add(LocationManager.PASSIVE_PROVIDER)
                    }

                    for (provider in providers) {
                        try {
                            val enabled = try {
                                lm.isProviderEnabled(provider)
                            } catch (_: Exception) {
                                false
                            }
                            if (enabled) {
                                val loc = lm.getLastKnownLocation(provider)
                                if (isValidLocation(loc)) {
                                    if (bestLocation == null || loc!!.time > bestLocation.time) {
                                        bestLocation = loc
                                    }
                                }
                            }
                        } catch (_: Exception) { }
                    }

                    // Jika masih null, minta 1 lokasi aktif menggunakan callbackExecutor terpisah (TANPA deadlock!)
                    if (bestLocation == null) {
                        val activeProviders = buildList {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                add(LocationManager.FUSED_PROVIDER)
                            }
                            add(LocationManager.NETWORK_PROVIDER)
                            add(LocationManager.GPS_PROVIDER)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            for (provider in activeProviders) {
                                try {
                                    if (lm.isProviderEnabled(provider)) {
                                        val latch = CountDownLatch(1)
                                        val signal = android.os.CancellationSignal()
                                        lm.getCurrentLocation(provider, signal, callbackExecutor) { loc ->
                                            if (isValidLocation(loc)) {
                                                bestLocation = loc
                                            }
                                            latch.countDown()
                                        }
                                        latch.await(2500, TimeUnit.MILLISECONDS)
                                        signal.cancel()
                                        if (bestLocation != null) break
                                    }
                                } catch (_: Exception) { }
                            }
                        } else {
                            bestLocation = requestLegacyLocation(lm, activeProviders)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "LocationManager gagal", e)
            }
        }

        if (bestLocation != null) {
            val lat = bestLocation!!.latitude
            val lon = bestLocation!!.longitude
            val kecamatan = resolveKecamatanName(context, lat, lon)
            if (!kecamatan.isNullOrBlank()) {
                saveLastKnownLocation(context, lat, lon, kecamatan)
                return LocationData(lat, lon, kecamatan, "GPS/Wi-Fi")
            }
        }

        // 3. IP Geolocation untuk Android TV / STB tanpa Play Services / ketika GPS/Wi-Fi location tidak mengunci
        val ipLoc = fetchLocationFromIp()
        if (ipLoc != null) {
            val kecamatan = resolveKecamatanName(context, ipLoc.lat, ipLoc.lon)
                ?.takeIf { it.isNotBlank() }
                ?: cleanDistrictPrefix(ipLoc.districtName)
            saveLastKnownLocation(context, ipLoc.lat, ipLoc.lon, kecamatan)
            return LocationData(ipLoc.lat, ipLoc.lon, kecamatan, "IP")
        }

        // 4. Koordinat terakhir yang tersimpan di SharedPreferences
        val prefs = context.getSharedPreferences(PREFS_WEATHER, Context.MODE_PRIVATE)
        val savedName = prefs.getString(KEY_LAST_KECAMATAN, null)
        if (!savedName.isNullOrBlank()) {
            val savedLat = prefs.getFloat(KEY_LAST_LAT, FALLBACK_LAT.toFloat()).toDouble()
            val savedLon = prefs.getFloat(KEY_LAST_LON, FALLBACK_LON.toFloat()).toDouble()
            return LocationData(savedLat, savedLon, savedName, "Cache")
        }

        return LocationData(FALLBACK_LAT, FALLBACK_LON, FALLBACK_NAME, "Fallback")
    }

    @SuppressLint("MissingPermission")
    private fun requestLegacyLocation(lm: LocationManager, providers: List<String>): Location? {
        for (provider in providers) {
            try {
                if (!lm.isProviderEnabled(provider)) continue
                var result: Location? = null
                val latch = CountDownLatch(1)
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (isValidLocation(location)) {
                            result = location
                        }
                        latch.countDown()
                    }
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                mainHandler.post {
                    try {
                        lm.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                    } catch (_: Exception) {
                        latch.countDown()
                    }
                }
                latch.await(2500, TimeUnit.MILLISECONDS)
                mainHandler.post {
                    try {
                        lm.removeUpdates(listener)
                    } catch (_: Exception) { }
                }
                if (result != null) return result
            } catch (_: Exception) { }
        }
        return null
    }

    private fun isValidLocation(loc: Location?): Boolean {
        return loc != null && (loc.latitude != 0.0 || loc.longitude != 0.0) &&
            !loc.latitude.isNaN() && !loc.longitude.isNaN()
    }

    private fun saveLastKnownLocation(context: Context, lat: Double, lon: Double, kecamatan: String) {
        context.getSharedPreferences(PREFS_WEATHER, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_LAST_LAT, lat.toFloat())
            .putFloat(KEY_LAST_LON, lon.toFloat())
            .putString(KEY_LAST_KECAMATAN, kecamatan)
            .apply()
    }

    /**
     * Mengubah koordinat (lat, lon) menjadi NAMA KECAMATAN SAJA.
     */
    private fun resolveKecamatanName(context: Context, lat: Double, lon: Double): String? {
        // A. Android Geocoder (mencari pola "Kec. <Nama>" atau locality Kecamatan)
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("id", "ID"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val fullLine = addr.getAddressLine(0) ?: ""
                    val kecMatch = Regex("(?:Kecamatan|Kec\\.?)\\s+([^,\\d]+)", RegexOption.IGNORE_CASE)
                        .find(fullLine)
                    val matchedName = kecMatch?.groupValues?.getOrNull(1)?.trim()
                    if (!matchedName.isNullOrBlank()) {
                        return cleanDistrictPrefix(matchedName)
                    }

                    val locality = addr.locality?.trim()
                    if (!locality.isNullOrBlank() && !isCityOrRegencyName(locality)) {
                        return cleanDistrictPrefix(locality)
                    }
                }
            }
        } catch (_: Exception) { }

        // B. OpenStreetMap Nominatim Reverse Geocoding (akurat untuk tingkat Kecamatan di Indonesia)
        try {
            val nominatimUrl = "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon&zoom=14&addressdetails=1&accept-language=id"
            val conn = (URL(nominatimUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4500
                readTimeout = 4500
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AeroTVLauncher/6.4")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(body)
                val address = root.optJSONObject("address")
                if (address != null) {
                    val candidates = listOf(
                        address.optString("city_district"),
                        address.optString("district"),
                        address.optString("municipality"),
                        address.optString("suburb"),
                        address.optString("town"),
                        address.optString("county"),
                        address.optString("village")
                    )
                    for (candidate in candidates) {
                        val cleaned = cleanDistrictPrefix(candidate)
                        if (cleaned.isNotBlank() && !isCityOrRegencyName(cleaned)) {
                            return cleaned
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Nominatim reverse geocode gagal", e)
        }

        // C. Cadangan Reverse Geocoding: BigDataCloud (gratis & tanpa API key)
        try {
            val bdcUrl = "https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=$lat&longitude=$lon&localityLanguage=id"
            val conn = (URL(bdcUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val locality = cleanDistrictPrefix(json.optString("locality", ""))
                if (locality.isNotBlank() && !isCityOrRegencyName(locality)) {
                    return locality
                }
                val adminArray = json.optJSONObject("localityInfo")?.optJSONArray("administrative")
                if (adminArray != null) {
                    for (i in adminArray.length() - 1 downTo 0) {
                        val obj = adminArray.optJSONObject(i) ?: continue
                        val level = obj.optInt("adminLevel", 0)
                        val name = cleanDistrictPrefix(obj.optString("name", ""))
                        if (level in 6..7 && name.isNotBlank() && !isCityOrRegencyName(name)) {
                            return name
                        }
                    }
                }
            }
        } catch (_: Exception) { }

        // D. Fallback terakhir dari Geocoder jika masih belum ketemu
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("id", "ID"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(lat, lon, 1)
                if (!addresses.isNullOrEmpty()) {
                    val addr = addresses[0]
                    val fallback = addr.locality ?: addr.subLocality ?: addr.subAdminArea
                    if (!fallback.isNullOrBlank()) {
                        return cleanDistrictPrefix(fallback)
                    }
                }
            }
        } catch (_: Exception) { }

        return null
    }

    private fun isCityOrRegencyName(name: String): Boolean {
        return name.startsWith("Kota ", ignoreCase = true) ||
            name.startsWith("Kabupaten ", ignoreCase = true) ||
            name.startsWith("Kab.", ignoreCase = true)
    }

    private fun cleanDistrictPrefix(raw: String): String {
        return raw.trim()
            .replace(Regex("^(?:Kecamatan|Kec\\.?)\\s+", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun fetchLocationFromIp(): LocationData? {
        // 1. ipwho.is
        try {
            val conn = (URL("https://ipwho.is/").openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                if (json.optBoolean("success", true)) {
                    val lat = json.optDouble("latitude", Double.NaN)
                    val lon = json.optDouble("longitude", Double.NaN)
                    val city = json.optString("city", "").ifBlank { json.optString("region", FALLBACK_NAME) }
                    if (!lat.isNaN() && !lon.isNaN()) {
                        return LocationData(lat, lon, city, "IP")
                    }
                }
            }
        } catch (_: Exception) { }

        // 2. ipapi.co
        try {
            val conn = (URL("https://ipapi.co/json/").openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AeroTVLauncher/6.4")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val lat = json.optDouble("latitude", Double.NaN)
                val lon = json.optDouble("longitude", Double.NaN)
                val city = json.optString("city", "").ifBlank { json.optString("region", FALLBACK_NAME) }
                if (!lat.isNaN() && !lon.isNaN()) {
                    return LocationData(lat, lon, city, "IP")
                }
            }
        } catch (_: Exception) { }

        // 3. geojs.io
        try {
            val conn = (URL("https://get.geojs.io/v1/ip/geo.json").openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AeroTVLauncher/6.4")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val lat = json.optString("latitude", "").toDoubleOrNull() ?: Double.NaN
                val lon = json.optString("longitude", "").toDoubleOrNull() ?: Double.NaN
                val city = json.optString("city", "").ifBlank { json.optString("region", FALLBACK_NAME) }
                if (!lat.isNaN() && !lon.isNaN()) {
                    return LocationData(lat, lon, city, "IP")
                }
            }
        } catch (_: Exception) { }

        return null
    }

    @Suppress("DEPRECATION")
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork
        if (network != null) {
            val capabilities = cm.getNetworkCapabilities(network)
            if (capabilities != null && (
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    )
            ) {
                return true
            }
        }
        return cm.activeNetworkInfo?.isConnected == true
    }

    /**
     * Memuat cuaca real-time aktual berdasarkan lokasi perangkat saat ini (nama kecamatan),
     * lalu menyinkronkan teks cuaca DAN animasi cuaca (kode WMO + siang/malam).
     */
    fun fetchWeather(
        context: Context,
        textView: TextView,
        forceRefresh: Boolean = false,
        onWeatherCodeFetched: ((weatherCode: Int, isDay: Int) -> Unit)? = null,
        onResult: ((String) -> Unit)? = null
    ) {
        lastTextViewRef = WeakReference(textView)
        lastOnWeatherCodeFetched = onWeatherCodeFetched
        lastOnResult = onResult

        val cachedStr = cachedWeatherString
        val cachedCode = cachedWeatherCode
        if (cachedStr != null && cachedCode != null) {
            textView.text = cachedStr
            onWeatherCodeFetched?.invoke(cachedCode, cachedIsDay)
            onResult?.invoke(cachedStr)
        }

        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedStr != null && cachedCode != null && (now - lastFetchTime) < 10 * 60 * 1000L) {
            return
        }
        if (isFetching) {
            if (forceRefresh) {
                pendingForceRefresh = true
            }
            return
        }
        isFetching = true

        val appCtx = context.applicationContext
        weatherExecutor.execute {
            try {
                val location = detectDeviceLocation(appCtx)
                val lat = location.lat
                val lon = location.lon
                val kecamatan = location.districtName

                val weatherResult = fetchOpenMeteo(lat, lon) ?: fetchMetNo(lat, lon)

                if (weatherResult != null) {
                    val condition = mapWeatherCodeToIndonesian(weatherResult.weatherCode)
                    // Format: [Kecamatan] [Suhu]°C [Kondisi]
                    val weatherString = "$kecamatan ${weatherResult.tempC}°C $condition"
                    cachedDistrictName = kecamatan
                    cachedTempC = weatherResult.tempC
                    cachedCondition = condition
                    cachedWeatherString = weatherString
                    cachedWeatherCode = weatherResult.weatherCode
                    cachedIsDay = weatherResult.isDay
                    lastFetchTime = System.currentTimeMillis()

                    mainHandler.post {
                        lastTextViewRef?.get()?.text = weatherString
                        lastOnWeatherCodeFetched?.invoke(weatherResult.weatherCode, weatherResult.isDay)
                        lastOnResult?.invoke(weatherString)
                    }
                } else {
                    val statusLabel = if (!isOnline(appCtx)) {
                        "$kecamatan • Offline"
                    } else {
                        "$kecamatan • Cuaca tidak tersedia"
                    }
                    mainHandler.post {
                        val tv = lastTextViewRef?.get()
                        if (tv != null && cachedWeatherString == null) {
                            tv.text = statusLabel
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gagal memuat cuaca live", e)
                mainHandler.post {
                    val tv = lastTextViewRef?.get()
                    if (tv != null && cachedWeatherString == null) {
                        val fallbackDistrict = getCachedDistrictName(appCtx) ?: FALLBACK_NAME
                        tv.text = if (!isOnline(appCtx)) {
                            "$fallbackDistrict • Offline"
                        } else {
                            "$fallbackDistrict • Gagal memuat cuaca"
                        }
                    }
                }
            } finally {
                isFetching = false
                if (pendingForceRefresh) {
                    pendingForceRefresh = false
                    mainHandler.post {
                        val tv = lastTextViewRef?.get()
                        if (tv != null) {
                            fetchWeather(
                                context = appCtx,
                                textView = tv,
                                forceRefresh = true,
                                onWeatherCodeFetched = lastOnWeatherCodeFetched,
                                onResult = lastOnResult
                            )
                        }
                    }
                }
            }
        }
    }

    private fun fetchOpenMeteo(lat: Double, lon: Double): WeatherResult? {
        return try {
            val urlString =
                "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code,is_day&timezone=auto"
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(response)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").roundToInt()
                val weatherCode = current.getInt("weather_code")
                val isDay = current.optInt("is_day", 1)
                WeatherResult(temp, weatherCode, isDay)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Open-Meteo gagal", e)
            null
        }
    }

    private fun fetchMetNo(lat: Double, lon: Double): WeatherResult? {
        return try {
            val latRounded = String.format(Locale.US, "%.4f", lat)
            val lonRounded = String.format(Locale.US, "%.4f", lon)
            val urlString =
                "https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$latRounded&lon=$lonRounded"
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "AeroTVLauncher/6.4")
            }
            if (conn.responseCode == 200) {
                val response = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(response)
                val firstSeries = json.getJSONObject("properties")
                    .getJSONArray("timeseries")
                    .getJSONObject(0)
                val data = firstSeries.getJSONObject("data")
                val temp = data.getJSONObject("instant")
                    .getJSONObject("details")
                    .getDouble("air_temperature")
                    .roundToInt()
                val symbol = data.optJSONObject("next_1_hours")
                    ?.optJSONObject("summary")
                    ?.optString("symbol_code", "")
                    ?: data.optJSONObject("next_6_hours")
                        ?.optJSONObject("summary")
                        ?.optString("symbol_code", "")
                    ?: "fair_day"

                val isDay = if (symbol.contains("night", ignoreCase = true)) 0 else 1
                val wmoCode = mapMetSymbolToWmo(symbol)
                WeatherResult(temp, wmoCode, isDay)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "MET Norway fallback gagal", e)
            null
        }
    }

    private fun mapMetSymbolToWmo(symbol: String): Int {
        val s = symbol.lowercase(Locale.US)
        return when {
            s.contains("thunder") -> 95
            s.contains("heavyrain") -> 81
            s.contains("lightrain") || s.contains("drizzle") -> 51
            s.contains("rain") || s.contains("sleet") -> 63
            s.contains("snow") -> 73
            s.contains("fog") -> 45
            s.contains("cloudy") && !s.contains("partly") -> 3
            s.contains("partlycloudy") || s.contains("fair") -> 2
            s.contains("clearsky") -> 0
            else -> 2
        }
    }

    private fun mapWeatherCodeToIndonesian(code: Int): String {
        return when (code) {
            0 -> "Cerah"
            1, 2 -> "Cerah Berawan"
            3 -> "Berawan"
            45, 48 -> "Berkabut"
            51, 53, 55, 56, 57 -> "Gerimis"
            61, 63, 65, 66, 67 -> "Hujan"
            71, 73, 75, 77 -> "Hujan Es"
            80, 81, 82 -> "Hujan Lebat"
            85, 86 -> "Hujan Lebat"
            95, 96, 99 -> "Badai Petir"
            else -> "Cerah Berawan"
        }
    }
}
