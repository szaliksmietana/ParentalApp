package com.example.parentalapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.parentalapp.network.TokenManager
import com.example.parentalapp.ui.AddChildScreen
import com.example.parentalapp.ui.ChatScreen
import com.example.parentalapp.ui.DashboardScreen
import com.example.parentalapp.ui.LoginScreen
import com.example.parentalapp.ui.RegisterScreen
import com.example.parentalapp.ui.SettingsScreen
import kotlinx.coroutines.launch
import retrofit2.HttpException

enum class AppScreen {
    Login, Register, Dashboard, Chat, Pairing, Settings
}

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "child_prefs"
    private val KEY_DEVICE_ID = "device_id"

    private fun saveDeviceId(context: Context, deviceId: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    private fun loadDeviceId(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_DEVICE_ID, null)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppConfig.init(this)

        setContent {
            var currentScreen by remember { mutableStateOf(AppScreen.Login) }
            var childDeviceId by remember { mutableStateOf<String?>(null) }
            val context = LocalContext.current

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

            when (currentScreen) {
                AppScreen.Login -> {
                    LoginScreen(
                        onLoginClick = { email, password ->
                            lifecycleScope.launch {
                                try {
                                    val response = RetrofitInstance.api.login(LoginRequest(email, password))
                                    TokenManager.token = response.access_token

                                    val savedDeviceId = loadDeviceId(context)
                                    if (savedDeviceId == null) {
                                        val device = RetrofitInstance.api.registerDevice(
                                            DeviceRegisterRequest(device_name = "Telefon dziecka")
                                        )
                                        saveDeviceId(context, device.id)
                                        childDeviceId = device.id
                                        Log.d("API_SUCCESS", "Zarejestrowano device dziecka: ${device.id}")
                                    } else {
                                        childDeviceId = savedDeviceId
                                        Log.d("API_SUCCESS", "Używam zapisanego device dziecka: $savedDeviceId")
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
                                        RegisterRequest(
                                            email = email,
                                            password = password,
                                            username = username,
                                            role = "child"
                                        )
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
                    LaunchedEffect(Unit) {
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
                        onNavigateToPairing = { currentScreen = AppScreen.Pairing },
                        onNavigateToChat = { currentScreen = AppScreen.Chat },
                        onNavigateToSettings = { currentScreen = AppScreen.Settings },
                        onLogoutClick = {
                            TokenManager.token = null
                            childDeviceId = null
                            val serviceIntent = Intent(context, LocationTrackingService::class.java)
                            context.stopService(serviceIntent)
                            currentScreen = AppScreen.Login
                        }
                    )
                }

                AppScreen.Chat -> {
                    ChatScreen(
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