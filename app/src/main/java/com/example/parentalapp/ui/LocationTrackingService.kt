package com.example.parentalapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*

class LocationTrackingService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreate() {
        super.onCreate()
        // Inicjalizacja klienta Google Play Services do lokalizacji
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    // TUTAJ DODAMY RETROFIT:
                    // Wysyłanie paczki JSON (szerokość, długość) do serwera Python/PostgreSQL
                    Log.d("LocationService", "Współrzędne dziecka: Lat: ${location.latitude}, Lng: ${location.longitude}")
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        // Android wymaga, aby usługa w tle wyświetlała stałe powiadomienie
        val notification = NotificationCompat.Builder(this, "LOCATION_CHANNEL_ID")
            .setContentTitle("Parental App")
            .setContentText("Śledzenie lokalizacji w tle jest aktywne")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
        startLocationUpdates()

        // START_STICKY gwarantuje, że jeśli system ubije apkę z braku RAMu, spróbuje ją zrestartować
        return START_STICKY
    }

    private fun startLocationUpdates() {
        // Ustawienia GPS: wysoka dokładność, odpytywanie co 15 sekund
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
            .setMinUpdateIntervalMillis(10000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Brak uprawnień do lokalizacji: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "LOCATION_CHANNEL_ID",
                "Lokalizacja",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Klasyczna usługa w tle, nie wymaga bindowania
    }
}