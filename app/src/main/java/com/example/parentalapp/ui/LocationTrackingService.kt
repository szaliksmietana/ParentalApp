package com.example.parentalapp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.example.parentalapp.network.LocationCreateRequest
import com.example.parentalapp.network.RetrofitInstance
import com.example.parentalapp.ui.ChildSettingsManager
import com.example.parentalapp.ui.NotificationHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val PREFS_NAME = "child_prefs"

    private fun loadDeviceId(): String? {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.entries.firstOrNull { it.key.startsWith("device_id_") }?.value as? String
    }

    private fun getBatteryLevel(): Int? {
        return try {
            val bm = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            if (level < 0) null else level
        } catch (e: Exception) {
            null
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("LocationService", "GPS: Lat=${location.latitude}, Lng=${location.longitude}")

                    val deviceId = loadDeviceId()
                    if (deviceId == null) {
                        Log.w("LocationService", "Brak device_id — pomijam wysyłanie")
                        continue
                    }

                    val battery = getBatteryLevel()
                    val accuracy = if (location.hasAccuracy()) location.accuracy.toDouble() else null

                    serviceScope.launch {
                        try {
                            RetrofitInstance.api.postLocation(
                                LocationCreateRequest(
                                    device_id = deviceId,
                                    latitude = location.latitude,
                                    longitude = location.longitude,
                                    accuracy_meters = accuracy,
                                    battery_level = battery
                                )
                            )
                            Log.d("LocationService", "Wysłano: ${location.latitude}, ${location.longitude}, bateria: $battery%")
                        } catch (e: Exception) {
                            Log.e("LocationService", "Błąd wysyłania: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ChildSettingsManager.load(this) // Wczytaj ustawienia przy starcie serwisu
        NotificationHelper.createChannels(this)
        startForeground(1, NotificationHelper.buildLocationForegroundNotification(this))
        startLocationUpdates()
        return START_STICKY
    }

    private fun startLocationUpdates() {
        // Interwał GPS z ustawień
        val intervalMs = ChildSettingsManager.settings.gpsInterval.ms
        val minIntervalMs = (intervalMs * 0.66).toLong() // min = 2/3 głównego interwału

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(minIntervalMs)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
            Log.d("LocationService", "GPS interwał: ${intervalMs / 1000}s")
        } catch (e: SecurityException) {
            Log.e("LocationService", "Brak uprawnień: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}