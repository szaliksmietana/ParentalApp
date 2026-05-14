package com.example.parentalapp.network

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.Properties

object AppConfig {
    private var baseUrl: String = "http://10.0.2.2:2244/" // fallback

    fun init(context: Context) {
        try {
            val props = Properties()
            context.assets.open("config.properties").use { props.load(it) }
            baseUrl = props.getProperty("BASE_URL", baseUrl)
        } catch (e: Exception) {
        }
    }

    fun getBaseUrl() = baseUrl
}

object TokenManager {
    var token: String? = null
}

data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val role: String = "parent"
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UserResponse(
    val id: String,
    val email: String,
    val username: String,
    val role: String
)

data class TokenResponse(
    val access_token: String,
    val token_type: String
)

data class DeviceRegisterRequest(
    val device_name: String? = "Telefon rodzica",
    val platform: String = "android"
)

data class DeviceResponse(
    val id: String,
    val device_name: String?,
    val platform: String
)

data class PairedChildResponse(
    val pair_id: String,
    val child_device_id: String,
    val child_user_id: String,
    val username: String,
    val device_name: String?,
    val last_seen: String?,
    val paired_at: String
)

data class ConfirmPairingRequest(
    val code: String,
    val guardian_device_id: String
)

data class PairingConfirmResponse(
    val id: String,
    val child_device_id: String,
    val guardian_device_id: String,
    val paired_at: String,
    val is_active: Boolean
)

interface FamilyGuardApi {
    @Headers("Connection: close")
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @Headers("Connection: close")
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @Headers("Connection: close")
    @POST("devices/register")
    suspend fun registerDevice(@Body request: DeviceRegisterRequest): DeviceResponse

    @Headers("Connection: close")
    @GET("devices")
    suspend fun getMyDevices(): List<DeviceResponse>

    @Headers("Connection: close")
    @GET("pairing/my-children")
    suspend fun getChildren(@Query("device_id") deviceId: String): List<PairedChildResponse>

    @Headers("Connection: close")
    @POST("pairing/confirm")
    suspend fun confirmPairing(@Body request: ConfirmPairingRequest): PairingConfirmResponse
}

object RetrofitInstance {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            TokenManager.token?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(requestBuilder.build())
        }.build()

    val api: FamilyGuardApi by lazy {
        Retrofit.Builder()
            .baseUrl(AppConfig.getBaseUrl())
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FamilyGuardApi::class.java)
    }
}