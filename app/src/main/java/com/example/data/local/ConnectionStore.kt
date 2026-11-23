package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "itsaplan_prefs")

enum class AuthMode {
    NONE,
    API_KEY,
    SESSION
}

data class StoredConnection(
    val serverUrl: String = "",
    val authMode: AuthMode = AuthMode.NONE,
    val apiKey: String = "",
    val sessionCookie: String = "",
    val lastProjectRef: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val liveSyncEnabled: Boolean = true
) {
    /** Never let a decrypted key or cookie reach a log, crash report or screenshot. */
    override fun toString(): String =
        "StoredConnection(serverUrl=$serverUrl, authMode=$authMode, hasApiKey=${apiKey.isNotBlank()}, " +
            "hasSessionCookie=${sessionCookie.isNotBlank()}, lastProjectRef=$lastProjectRef, " +
            "themeMode=$themeMode, liveSyncEnabled=$liveSyncEnabled)"

    val hasCredentials: Boolean
        get() = serverUrl.isNotBlank() && when (authMode) {
            AuthMode.API_KEY -> apiKey.isNotBlank()
            AuthMode.SESSION -> sessionCookie.isNotBlank()
            AuthMode.NONE -> false
        }
}

class ConnectionStore(private val context: Context) {
    private object Keys {
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTH_MODE = stringPreferencesKey("auth_mode")
        val ENC_API_KEY = stringPreferencesKey("enc_api_key")
        val ENC_SESSION_COOKIE = stringPreferencesKey("enc_session_cookie")
        val LAST_PROJECT_REF = stringPreferencesKey("last_project_ref")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LIVE_SYNC_ENABLED = booleanPreferencesKey("live_sync_enabled")
    }

    val connectionFlow: Flow<StoredConnection> = context.dataStore.data.map { prefs ->
        val mode = when (prefs[Keys.AUTH_MODE]) {
            AuthMode.API_KEY.name -> AuthMode.API_KEY
            AuthMode.SESSION.name -> AuthMode.SESSION
            else -> AuthMode.NONE
        }
        val theme = when (prefs[Keys.THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
        StoredConnection(
            serverUrl = prefs[Keys.SERVER_URL].orEmpty(),
            authMode = mode,
            apiKey = CryptoHelper.decrypt(prefs[Keys.ENC_API_KEY].orEmpty()),
            sessionCookie = CryptoHelper.decrypt(prefs[Keys.ENC_SESSION_COOKIE].orEmpty()),
            lastProjectRef = prefs[Keys.LAST_PROJECT_REF].orEmpty(),
            themeMode = theme,
            liveSyncEnabled = prefs[Keys.LIVE_SYNC_ENABLED] ?: true
        )
    }

    suspend fun getSnapshot(): StoredConnection = connectionFlow.first()

    suspend fun saveApiKeyConnection(serverUrl: String, apiKey: String) {
        val encKey = CryptoHelper.encrypt(apiKey.trim())
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = serverUrl.trim().trimEnd('/')
            prefs[Keys.AUTH_MODE] = AuthMode.API_KEY.name
            prefs[Keys.ENC_API_KEY] = encKey
            prefs.remove(Keys.ENC_SESSION_COOKIE)
        }
    }

    suspend fun saveSessionConnection(serverUrl: String, sessionCookie: String) {
        val encCookie = CryptoHelper.encrypt(sessionCookie.trim())
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = serverUrl.trim().trimEnd('/')
            prefs[Keys.AUTH_MODE] = AuthMode.SESSION.name
            prefs[Keys.ENC_SESSION_COOKIE] = encCookie
            prefs.remove(Keys.ENC_API_KEY)
        }
    }

    suspend fun updateSessionCookie(sessionCookie: String) {
        if (sessionCookie.isBlank()) return
        val encCookie = CryptoHelper.encrypt(sessionCookie.trim())
        context.dataStore.edit { prefs ->
            if (prefs[Keys.AUTH_MODE] == AuthMode.SESSION.name) {
                prefs[Keys.ENC_SESSION_COOKIE] = encCookie
            }
        }
    }

    suspend fun saveLastProjectRef(projectRef: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_PROJECT_REF] = projectRef
        }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun saveLiveSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LIVE_SYNC_ENABLED] = enabled
        }
    }

    suspend fun clearAuth(keepServerUrl: Boolean = true) {
        context.dataStore.edit { prefs ->
            if (!keepServerUrl) {
                prefs.remove(Keys.SERVER_URL)
            }
            prefs[Keys.AUTH_MODE] = AuthMode.NONE.name
            prefs.remove(Keys.ENC_API_KEY)
            prefs.remove(Keys.ENC_SESSION_COOKIE)
            prefs.remove(Keys.LAST_PROJECT_REF)
        }
    }
}
