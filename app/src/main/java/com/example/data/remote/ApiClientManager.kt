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

    /**
     * The origin (scheme://host:port) the stored credentials belong to. Auth headers are
     * only ever attached to requests aimed at exactly this origin, so switching servers
     * can never leak the previous key or cookie to a different host.
     */
    @Volatile
    private var credentialOrigin: String = ""

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

        requireAcceptableTransport(normalized)
        configuredHost = normalized.toHttpUrlOrNull()?.host
        val baseChanged = normalized != currentBaseUrl
        currentBaseUrl = normalized
        credentialOrigin = if (authMode == AuthMode.NONE) "" else originOf(normalized)
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

        requireAcceptableTransport(normalized)
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            // Never follow a redirect: a 3xx could otherwise re-send the credentials we
            // attach here to a host the caller never configured.
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(
                authInterceptor(
                    modeProvider = { authMode },
                    keyProvider = { apiKey },
                    cookieProvider = { sessionCookie },
                    originProvider = { originOf(normalized) }
                )
            )
            .build()

        return Retrofit.Builder()
            .baseUrl("$normalized/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ItsAPlanApi::class.java)
    }

    private fun buildOkHttpClient(): OkHttpClient {
        val authInterceptor = authInterceptor(
            modeProvider = { currentAuthMode },
            keyProvider = { currentApiKey },
            cookieProvider = { currentSessionCookie },
            originProvider = { credentialOrigin }
        )

        return OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(authInterceptor)
            .build()
    }

    /**
     * Attaches credentials only when the request targets the origin those credentials
     * were configured for. Anything else (redirects, absolute URLs baked into a payload)
     * goes out unauthenticated.
     */
    private fun authInterceptor(
        modeProvider: () -> AuthMode,
        keyProvider: () -> String,
        cookieProvider: () -> String,
        originProvider: () -> String
    ): Interceptor = Interceptor { chain ->
        val original = chain.request()
        val reqBuilder = original.newBuilder()
            .header("Accept", "application/json")

        val expectedOrigin = originProvider()
        if (expectedOrigin.isNotBlank() && originOf(original.url) == expectedOrigin) {
            val path = original.url.encodedPath
            val isAuthRoute = path.startsWith("/api/auth/")
            when (modeProvider()) {
                AuthMode.API_KEY -> {
                    val key = keyProvider()
                    if (key.isNotBlank() && !isAuthRoute) reqBuilder.header("x-api-key", key)
                }
                AuthMode.SESSION -> {
                    val cookie = cookieProvider()
                    if (cookie.isNotBlank()) reqBuilder.header("Cookie", cookie)
                }
                AuthMode.NONE -> Unit
            }
        }

        chain.proceed(reqBuilder.build())
    }

    /** Drops every credential the shared client still holds. */
    fun clearAuth() {
        currentAuthMode = AuthMode.NONE
        currentApiKey = ""
        currentSessionCookie = ""
        credentialOrigin = ""
    }

    private fun originOf(url: String): String =
        url.toHttpUrlOrNull()?.let { originOf(it) } ?: ""

    private fun originOf(url: okhttp3.HttpUrl): String =
        "${url.scheme}://${url.host}:${url.port}"

    /**
     * Plain HTTP is only allowed for the loopback and emulator hosts that
     * res/xml/network_security_config.xml likewise permits. Everything else has to be
     * https, so a mistyped URL cannot silently send an API key in the clear.
     */
    private fun requireAcceptableTransport(normalized: String) = assertAcceptableTransport(normalized)

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

    /** Builds the same ApiException the typed endpoints produce, for bodyless responses. */
    fun exceptionFor(status: Int, rawBody: String?, action: String? = null): ApiException {
        val parsed = rawBody?.let {
            try {
                errorAdapter.fromJson(it)
            } catch (_: Exception) {
                null
            }
        }
        val message = parsed?.error ?: parsed?.message ?: defaultStatusMessage(status)
        return ApiException(
            statusCode = status,
            message = if (action != null) "$action: $message" else message,
            errorCode = parsed?.code
        )
    }

    private fun defaultStatusMessage(status: Int): String = when (status) {
        401 -> "Authentication required or session expired."
        403 -> "You don't have permission for this action."
        404 -> "The requested resource was not found."
        409 -> "Conflict: action rejected by server rules."
        429 -> "Rate limit reached. Please wait a moment."
        in 500..599 -> "Server error ($status). Check your itsaplan instance."
        else -> "Request failed (HTTP $status)."
    }

    fun parseError(throwable: Throwable): ApiException {
        // Cancellation must propagate so callers can tell "aborted" from "failed".
        if (throwable is kotlinx.coroutines.CancellationException) throw throwable
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
                val message = parsed?.error ?: parsed?.message ?: defaultStatusMessage(status)
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
        /**
         * Host of the server the app is configured against. Markdown rendering uses it to
         * decide which images may be fetched without a third-party request.
         */
        @Volatile
        var configuredHost: String? = null
            private set

        fun currentServerHost(): String? = configuredHost

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

        /**
         * Rejects plain HTTP for anything but loopback/emulator hosts, so a mistyped URL
         * cannot send credentials in the clear. Mirrors network_security_config.xml.
         */
        fun assertAcceptableTransport(normalized: String) {
            val url = normalizeServerUrl(normalized)?.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("Invalid server URL.")
            if (url.scheme == "https") return
            if (isLocalDevelopmentHost(url.host)) return
            throw IllegalArgumentException(
                "This address uses plain HTTP. Use an https:// URL, or connect to localhost/10.0.2.2 for local development."
            )
        }

        /** Hosts allowed to speak plain HTTP for local development. */
        fun isLocalDevelopmentHost(host: String): Boolean {
            val h = host.lowercase()
            return h == "localhost" || h == "127.0.0.1" || h == "::1" || h == "10.0.2.2"
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
