package com.example.parentalapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.parentalapp.network.AppConfig
import com.example.parentalapp.network.DeviceRegisterRequest
import com.example.parentalapp.network.LoginRequest
import com.example.parentalapp.network.RegisterRequest
import com.example.parentalapp.network.RetrofitInstance
import com.example.parentalapp.network.SosRequest
import com.example.parentalapp.network.TokenManager
import com.example.parentalapp.ui.AddChildScreen
import com.example.parentalapp.ui.ChatScreen
import com.example.parentalapp.ui.ChildAppTheme
import com.example.parentalapp.ui.ChildSettingsManager
import com.example.parentalapp.ui.DashboardScreen
import com.example.parentalapp.ui.LoginScreen
import com.example.parentalapp.ui.NotificationHelper
import com.example.parentalapp.ui.RegisterScreen
import com.example.parentalapp.ui.SettingsScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.HttpException

enum class AppScreen {
    Login, Register, Dashboard, Chat, Pairing, Settings
}

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "child_prefs"

    private fun saveDeviceId(context: Context, email: String, deviceId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString("device_id_$email", deviceId).apply()
    }

    private fun loadDeviceId(context: Context, email: String): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("device_id_$email", null)
    }

    private fun getHardwareId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    private suspend fun refreshTokenIfNeeded() {
        if (TokenManager.isTokenExpired()) {
            try {
                val response = RetrofitInstance.api.refreshToken()
                TokenManager.saveToken(response.access_token)
                Log.d("API_SUCCESS", "Token odświeżony")
            } catch (e: Exception) {
                Log.e("API_ERROR", "Nie udało się odświeżyć tokenu: ${e.message}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppConfig.init(this)
        NotificationHelper.createChannels(this)
        ChildSettingsManager.load(this) // Wczytaj ustawienia przy starcie

        setContent {
            val context = LocalContext.current
            val currentSettings = ChildSettingsManager.settings // Reaktywny odczyt motywu

            // Motyw aplikacji zgodnie z ustawieniem
            val isDark = when (currentSettings.appTheme) {
                ChildAppTheme.DARK -> true
                ChildAppTheme.LIGHT -> false
                ChildAppTheme.SYSTEM -> isSystemInDarkTheme()
            }
            MaterialTheme(colorScheme = if (isDark) darkColorScheme() else lightColorScheme()) {

                var currentScreen by remember { mutableStateOf(AppScreen.Login) }
                var childDeviceId by remember { mutableStateOf<String?>(null) }
                var guardianName by remember { mutableStateOf<String?>(null) }
                val seenMessageIds = remember { mutableSetOf<String>() }

                val permissionsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { permissions ->
                    val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
                    if (locationGranted) {
                        val serviceIntent = Intent(context, LocationTrackingService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                }

                // Polling wiadomości co 10s — aktywny tylko na dashboardzie
                LaunchedEffect(currentScreen) {
                    if (currentScreen != AppScreen.Dashboard) return@LaunchedEffect

                    while (isActive) {
                        val deviceId = childDeviceId
                        if (deviceId != null && TokenManager.token != null) {
                            try {
                                val history = RetrofitInstance.api.getMessageHistory(deviceId)
                                val sorted = history.sortedBy { it.sent_at }

                                val freshFromGuardian = sorted.filter { msg ->
                                    msg.receiver_device_id == deviceId && msg.id !in seenMessageIds
                                }

                                // Powiadomienia tylko jeśli flaga włączona
                                if (seenMessageIds.isNotEmpty() && currentSettings.messageNotificationsEnabled) {
                                    freshFromGuardian.forEach { msg ->
                                        NotificationHelper.showMessageNotification(context, msg.content)
                                    }
                                }

                                sorted.forEach { seenMessageIds.add(it.id) }
                                Log.d("POLLING", "Sprawdzono wiadomości: ${sorted.size}, nowych: ${freshFromGuardian.size}")

                            } catch (e: Exception) {
                                Log.d("POLLING", "Błąd pollingu wiadomości: ${e.message}")
                            }
                        }
                        delay(10000)
                    }
                }

                when (currentScreen) {
                    AppScreen.Login -> {
                        LoginScreen(
                            onLoginClick = { email, password ->
                                lifecycleScope.launch {
                                    try {
                                        val response = RetrofitInstance.api.login(LoginRequest(email, password))
                                        TokenManager.saveToken(response.access_token)

                                        val savedDeviceId = loadDeviceId(context, email)
                                        if (savedDeviceId != null) {
                                            childDeviceId = savedDeviceId
                                            Log.d("API_SUCCESS", "Używam zapisanego device_id dla $email: $savedDeviceId")
                                        } else {
                                            val hardwareId = getHardwareId(context)
                                            val device = RetrofitInstance.api.registerDevice(
                                                DeviceRegisterRequest(
                                                    device_name = "Telefon dziecka",
                                                    hardware_id = hardwareId
                                                )
                                            )
                                            childDeviceId = device.id
                                            saveDeviceId(context, email, device.id)
                                            Log.d("API_SUCCESS", "Zarejestrowano device_id dla $email: ${device.id}")
                                        }

                                        Toast.makeText(context, "Zalogowano pomyślnie!", Toast.LENGTH_SHORT).show()
                                        currentScreen = AppScreen.Dashboard
                                    } catch (e: HttpException) {
                                        Log.e("API_ERROR", "HTTP ${e.code()}: ${e.response()?.errorBody()?.string()}")
                                        Toast.makeText(context, "Błąd: Nieprawidłowy email lub hasło.", Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Log.e("API_ERROR", "Exception: ${e.message}")
                                        Toast.makeText(context, "Błąd sieci: Brak połączenia z serwerem.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onRegisterClick = { currentScreen = AppScreen.Register }
                        )
                    }

                    AppScreen.Register -> {
                        RegisterScreen(
                            onRegisterClick = { email, password, username ->
                                lifecycleScope.launch {
                                    try {
                                        val response = RetrofitInstance.api.register(
                                            RegisterRequest(email = email, password = password, username = username, role = "child")
                                        )
                                        Toast.makeText(context, "Konto ${response.username} utworzone!", Toast.LENGTH_SHORT).show()
                                        currentScreen = AppScreen.Login
                                    } catch (e: HttpException) {
                                        val body = e.response()?.errorBody()?.string()
                                        Log.e("API_ERROR", "HTTP ${e.code()}: $body")
                                        val msg = when (e.code()) {
                                            409 -> "Ten email jest już zajęty."
                                            422 -> "Sprawdź poprawność danych."
                                            else -> "Błąd rejestracji: ${e.code()}"
                                        }
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        Log.e("API_ERROR", "Exception: ${e.message}")
                                        Toast.makeText(context, "Błąd połączenia. Sprawdź sieć.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            onNavigateBack = { currentScreen = AppScreen.Login }
                        )
                    }

                    AppScreen.Dashboard -> {
                        LaunchedEffect(childDeviceId) {
                            refreshTokenIfNeeded()

                            val deviceId = childDeviceId
                            if (deviceId != null) {
                                try {
                                    val guardian = RetrofitInstance.api.getMyGuardian(deviceId)
                                    guardianName = guardian.username
                                    Log.d("API_SUCCESS", "Rodzic: ${guardian.username}")
                                } catch (e: Exception) {
                                    Log.d("API_INFO", "Brak parowania lub błąd: ${e.message}")
                                    guardianName = null
                                }
                            }

                            val permissionsToRequest = mutableListOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
                        }

                        DashboardScreen(
                            guardianName = guardianName,
                            onNavigateToPairing = { currentScreen = AppScreen.Pairing },
                            onNavigateToChat = { currentScreen = AppScreen.Chat },
                            onNavigateToSettings = { currentScreen = AppScreen.Settings },
                            onSosTriggered = {
                                val deviceId = childDeviceId
                                if (deviceId != null) {
                                    lifecycleScope.launch {
                                        try {
                                            RetrofitInstance.api.triggerSos(SosRequest(deviceId))
                                            Toast.makeText(context, "Alarm SOS wysłany do rodzica!", Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            Log.e("API_ERROR", "Nie udało się wysłać alarmu SOS: ${e.message}")
                                            Toast.makeText(context, "Błąd wysyłania alarmu! Sprawdź sieć.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "Błąd: Brak przypisanego urządzenia", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onLogoutClick = {
                                TokenManager.token = null
                                childDeviceId = null
                                guardianName = null
                                seenMessageIds.clear()
                                val serviceIntent = Intent(context, LocationTrackingService::class.java)
                                context.stopService(serviceIntent)
                                currentScreen = AppScreen.Login
                            }
                        )
                    }

                    AppScreen.Chat -> {
                        ChatScreen(
                            childDeviceId = childDeviceId ?: "",
                            onNavigateBack = { currentScreen = AppScreen.Dashboard }
                        )
                    }

                    AppScreen.Pairing -> {
                        val deviceId = childDeviceId ?: ""
                        AddChildScreen(
                            childDeviceId = deviceId,
                            onNavigateBack = { currentScreen = AppScreen.Dashboard },
                            onPaired = {
                                Toast.makeText(context, "Sparowano z rodzicem!", Toast.LENGTH_SHORT).show()
                                currentScreen = AppScreen.Dashboard
                            }
                        )
                    }

                    AppScreen.Settings -> {
                        SettingsScreen(
                            onNavigateBack = { currentScreen = AppScreen.Dashboard },
                            onLogoutClick = {
                                TokenManager.token = null
                                childDeviceId = null
                                guardianName = null
                                seenMessageIds.clear()
                                val serviceIntent = Intent(context, LocationTrackingService::class.java)
                                context.stopService(serviceIntent)
                                currentScreen = AppScreen.Login
                            }
                        )
                    }
                }
            }
        }
    }
}