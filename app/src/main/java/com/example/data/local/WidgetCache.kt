package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.widgetStore: DataStore<Preferences> by preferencesDataStore(name = "itsaplan_widget")

data class WidgetIssue(
    val id: Int,
    val identifier: String,
    val title: String
)

/**
 * Cache backing the home-screen widget. The app process populates this on every
 * board / my-issues refresh; the widget reads it without doing its own network
 * calls (which would need API keys in a separate process).
 */
class WidgetCache(private val context: Context) {
    private val KEY_MY_ISSUES = stringPreferencesKey("my_issues_json")

    suspend fun saveMyIssues(issues: List<WidgetIssue>) {
        val arr = JSONArray()
        issues.forEach { issue ->
            arr.put(JSONObject().apply {
                put("id", issue.id)
                put("identifier", issue.identifier)
                put("title", issue.title)
            })
        }
        context.widgetStore.edit { prefs ->
            prefs[KEY_MY_ISSUES] = arr.toString()
        }
    }

    suspend fun loadMyIssues(): List<WidgetIssue> {
        val raw = context.widgetStore.data.first()[KEY_MY_ISSUES].orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                WidgetIssue(
                    id = o.optInt("id"),
                    identifier = o.optString("identifier"),
                    title = o.optString("title")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Empties the widget cache so a signed-out device shows no account titles. */
    /** False until the app has written the cache once, so a new widget can say so. */
    suspend fun hasLoaded(): Boolean =
        context.widgetStore.data.first().contains(KEY_MY_ISSUES)

    suspend fun clear() {
        context.widgetStore.edit { prefs -> prefs.remove(KEY_MY_ISSUES) }
    }
}
