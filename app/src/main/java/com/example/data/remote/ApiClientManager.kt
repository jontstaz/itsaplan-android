package com.example.data.remote

import com.example.data.local.AuthMode
import com.example.data.model.ApiErrorBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(
    val statusCode: Int,
    override val message: String,
    val errorCode: String? = null
) : Exception(message)

/**
 * Manages the dynamic OkHttp + Retrofit client pointing at whichever self-hosted
 * itsaplan API URL the user configured, injecting either x-api-key or Cookie auth.
 * Sensitive headers are never logged.
 */
class ApiClientManager {

    @Volatile
    private var currentBaseUrl: String = ""

    @Volatile
    private var currentAuthMode: AuthMode = AuthMode.NONE

    @Volatile
    private var currentApiKey: String = ""

    @Volatile
    private var currentSessionCookie: String = ""

    @Volatile
    private var cachedApi: ItsAPlanApi? = null

    @Volatile
    private var cachedOkHttp: OkHttpClient? = null

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val errorAdapter = moshi.adapter(ApiErrorBody::class.java)

    val baseUrl: String get() = currentBaseUrl

    fun configure(
        serverUrl: String,
        authMode: AuthMode,
        apiKey: String = "",
        sessionCookie: String = ""
    ) {
        val normalized = normalizeServerUrl(serverUrl)
            ?: throw IllegalArgumentException("Invalid server URL. Use http:// or https://")

        val baseChanged = normalized != currentBaseUrl
        currentBaseUrl = normalized
        currentAuthMode = authMode
        currentApiKey = apiKey.trim()
        currentSessionCookie = sessionCookie.trim()

        if (baseChanged || cachedApi == null) {
            rebuildClient(normalized)
        }
    }

    fun getApi(): ItsAPlanApi {
        return cachedApi ?: throw IllegalStateException("Server not configured")
    }

    fun getOkHttpClient(): OkHttpClient {
        return cachedOkHttp ?: buildOkHttpClient().also { cachedOkHttp = it }
    }

    /**
     * Creates a temporary one-shot Retrofit client to probe a candidate server URL
     * or perform sign-in before committing credentials.
     */
    fun createTransientApi(
        serverUrl: String,
        authMode: AuthMode = AuthMode.NONE,
        apiKey: String = "",
        sessionCookie: String = ""
    ): ItsAPlanApi {
        val normalized = normalizeServerUrl(serverUrl)
            ?: throw IllegalArgumentException("Please enter a valid http:// or https:// URL")

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val reqBuilder = chain.request().newBuilder()
                    .header("Accept", "application/json")
                val path = chain.request().url.encodedPath
                val isAuthRoute = path.startsWith("/api/auth/")
                when (authMode) {
                    AuthMode.API_KEY -> {
                        if (apiKey.isNotBlank() && !isAuthRoute) {
                            reqBuilder.header("x-api-key", apiKey.trim())
                        }
                    }
                    AuthMode.SESSION -> {
                        if (sessionCookie.isNotBlank()) {
                            reqBuilder.header("Cookie", sessionCookie.trim())
                        }
                    }
                    AuthMode.NONE -> Unit
                }
                chain.proceed(reqBuilder.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("$normalized/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ItsAPlanApi::class.java)
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val reqBuilder = original.newBuilder()
                .header("Accept", "application/json")

            val path = original.url.encodedPath
            val isAuthRoute = path.startsWith("/api/auth/")

            when (currentAuthMode) {
                AuthMode.API_KEY -> {
                    val key = currentApiKey
                    if (key.isNotBlank() && !isAuthRoute) {
                        reqBuilder.header("x-api-key", key)
                    }
                }
                AuthMode.SESSION -> {
                    val cookie = currentSessionCookie
                    if (cookie.isNotBlank()) {
                        reqBuilder.header("Cookie", cookie)
                    }
                }
                AuthMode.NONE -> Unit
            }

            chain.proceed(reqBuilder.build())
        }

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .build()
    }

    private fun rebuildClient(normalizedBaseUrl: String) {
        val okHttp = cachedOkHttp ?: buildOkHttpClient().also { cachedOkHttp = it }
        cachedApi = Retrofit.Builder()
            .baseUrl("$normalizedBaseUrl/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ItsAPlanApi::class.java)
    }

    fun resolveAttachmentUrl(rawOrRelativeUrl: String?): String? {
        if (rawOrRelativeUrl.isNullOrBlank()) return null
        val trimmed = rawOrRelativeUrl.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        if (currentBaseUrl.isBlank()) return null
        return if (trimmed.startsWith("/")) "$currentBaseUrl$trimmed" else "$currentBaseUrl/$trimmed"
    }

    fun parseError(throwable: Throwable): ApiException {
        return when (throwable) {
            is ApiException -> throwable
            is HttpException -> {
                val status = throwable.code()
                val rawBody = try {
                    throwable.response()?.errorBody()?.string()
                } catch (_: Exception) {
                    null
                }
                val parsed = rawBody?.let {
                    try {
                        errorAdapter.fromJson(it)
                    } catch (_: Exception) {
                        null
                    }
                }
                val message = parsed?.error ?: parsed?.message ?: when (status) {
                    401 -> "Authentication required or session expired."
                    403 -> "You don't have permission for this action."
                    404 -> "The requested resource was not found."
                    409 -> "Conflict: action rejected by server rules."
                    429 -> "Rate limit reached. Please wait a moment."
                    in 500..599 -> "Server error ($status). Check your itsaplan instance."
                    else -> "Request failed (HTTP $status)."
                }
                ApiException(statusCode = status, message = message, errorCode = parsed?.code)
            }
            is IOException -> ApiException(
                statusCode = 0,
                message = "Could not reach server. Verify the URL and network connection."
            )
            else -> ApiException(
                statusCode = -1,
                message = throwable.message ?: "Unexpected error occurred."
            )
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /**
         * Validates and normalizes a user-provided server URL.
         * Strictly allows only http:// or https:// with a non-empty host.
         */
        fun normalizeServerUrl(input: String): String? {
            val trimmed = input.trim().trimEnd('/')
            if (trimmed.isEmpty()) return null
            val withScheme = if (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                trimmed
            } else {
                "https://$trimmed"
            }
            val parsed = withScheme.toHttpUrlOrNull() ?: return null
            if (parsed.scheme != "http" && parsed.scheme != "https") return null
            if (parsed.host.isBlank()) return null
            return parsed.toString().trimEnd('/')
        }

        fun isInsecureHttpUrl(url: String): Boolean {
            val parsed = normalizeServerUrl(url)?.toHttpUrlOrNull() ?: return false
            if (parsed.scheme != "http") return false
            val host = parsed.host.lowercase()
            return host != "localhost" && host != "127.0.0.1" && host != "10.0.2.2"
        }

        /**
         * Serializes a map into JSON RequestBody while preserving explicit nulls
         * (critical for PATCH endpoints that clear nullable fields like assigneeUserId = null).
         */
        fun jsonBody(vararg pairs: Pair<String, Any?>): RequestBody {
            val obj = JSONObject()
            for ((key, value) in pairs) {
                when (value) {
                    null -> obj.put(key, JSONObject.NULL)
                    is List<*> -> {
                        val arr = JSONArray()
                        value.forEach { item -> arr.put(item ?: JSONObject.NULL) }
                        obj.put(key, arr)
                    }
                    else -> obj.put(key, value)
                }
            }
            return obj.toString().toRequestBody(JSON_MEDIA_TYPE)
        }

        fun jsonBodyFromMap(map: Map<String, Any?>): RequestBody {
            val obj = JSONObject()
            for ((key, value) in map) {
                when (value) {
                    null -> obj.put(key, JSONObject.NULL)
                    is List<*> -> {
                        val arr = JSONArray()
                        value.forEach { item -> arr.put(item ?: JSONObject.NULL) }
                        obj.put(key, arr)
                    }
                    else -> obj.put(key, value)
                }
            }
            return obj.toString().toRequestBody(JSON_MEDIA_TYPE)
        }

        /**
         * Extracts `name=value` cookie pairs from Set-Cookie headers returned by better-auth.
         */
        fun extractCookieHeader(setCookies: List<String>): String {
            return setCookies
                .mapNotNull { header ->
                    val firstPart = header.substringBefore(';').trim()
                    if (firstPart.contains('=')) firstPart else null
                }
                .joinToString("; ")
        }
    }
}
