package com.example.parentalapp.network

import android.content.Context
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.Properties
import java.util.concurrent.TimeUnit

object AppConfig {
    private var baseUrl: String = "http://10.0.2.2:2244/"

    fun init(context: Context) {
        try {
            val props = Properties()
            context.assets.open("config.properties").use { props.load(it) }
            baseUrl = props.getProperty("BASE_URL", baseUrl)
        } catch (e: Exception) {
            // Fallback
        }
    }

    fun getBaseUrl() = baseUrl
}

object TokenManager {
    var token: String? = null
    var tokenExpiry: Long = 0L

    fun isTokenExpired(): Boolean {
        return System.currentTimeMillis() > tokenExpiry - 5 * 60 * 1000
    }

    fun saveToken(accessToken: String) {
        token = accessToken
        tokenExpiry = System.currentTimeMillis() + 24 * 60 * 60 * 1000
    }
}

// --- Auth ---
data class RegisterRequest(
    val email: String,
    val password: String,
    val username: String,
    val role: String = "child"
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

// --- Devices ---
data class DeviceRegisterRequest(
    val device_name: String? = "Telefon dziecka",
    val platform: String = "android",
    val fcm_token: String? = null,
    val hardware_id: String? = null
)

data class DeviceResponse(
    val id: String,
    val user_id: String,
    val device_name: String?,
    val platform: String,
    val hardware_id: String?,
    val registered_at: String
)

// --- Pairing ---
data class GeneratePairingCodeRequest(
    val device_id: String
)

data class PairingCodeResponse(
    val code: String,
    val expires_at: String
)

data class PairingStatusResponse(
    val is_paired: Boolean,
    val pair_id: String?,
    val paired_at: String?
)

data class GuardianResponse(
    val pair_id: String,
    val guardian_device_id: String,
    val guardian_user_id: String,
    val username: String,
    val device_name: String?,
    val paired_at: String
)

// --- Location ---
data class LocationCreateRequest(
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy_meters: Double? = null,
    val battery_level: Int? = null
)

data class LocationResponse(
    val id: String,
    val device_id: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy_meters: Double?,
    val battery_level: Int?,
    val recorded_at: String
)

// --- Messages ---
data class MessageResponse(
    val id: String,
    val sender_device_id: String,
    val receiver_device_id: String,
    val content: String,
    val sent_at: String,
    val read_at: String?
)

data class SendMessageRequest(
    val sender_device_id: String,
    val receiver_device_id: String,
    val content: String
)

interface FamilyGuardApi {
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("auth/refresh")
    suspend fun refreshToken(): TokenResponse

    @POST("devices/register")
    suspend fun registerDevice(@Body request: DeviceRegisterRequest): DeviceResponse

    @POST("pairing/generate")
    suspend fun generatePairingCode(@Body request: GeneratePairingCodeRequest): PairingCodeResponse

    @GET("pairing/status")
    suspend fun getPairingStatus(@Query("device_id") deviceId: String): PairingStatusResponse

    @GET("pairing/my-guardian")
    suspend fun getMyGuardian(@Query("device_id") deviceId: String): GuardianResponse

    @POST("location")
    suspend fun postLocation(@Body request: LocationCreateRequest): LocationResponse

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): MessageResponse

    @POST("messages/{message_id}/read")
    suspend fun markAsRead(
        @Path("message_id") messageId: String,
        @Query("device_id") deviceId: String
    ): MessageResponse

    @GET("messages/history")
    suspend fun getMessageHistory(
        @Query("device_id") deviceId: String,
        @Query("limit") limit: Int = 40
    ): List<MessageResponse>
}

object RetrofitInstance {
    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .retryOnConnectionFailure(true)
        .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
                .removeHeader("Connection")
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