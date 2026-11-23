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

private val Context.recentStore: DataStore<Preferences> by preferencesDataStore(name = "itsaplan_recent")

data class RecentIssue(
    val id: Int,
    val identifier: String,
    val title: String,
    val projectRef: String?
)

class RecentIssuesStore(private val context: Context) {
    private val KEY = stringPreferencesKey("recent_issues")
    private val MAX = 8

    suspend fun record(issue: RecentIssue) {
        val current = load().toMutableList()
        current.removeAll { it.id == issue.id }
        current.add(0, issue)
        val trimmed = current.take(MAX)
        val arr = JSONArray()
        trimmed.forEach { r ->
            arr.put(JSONObject().apply {
                put("id", r.id)
                put("identifier", r.identifier)
                put("title", r.title)
                put("projectRef", r.projectRef ?: JSONObject.NULL)
            })
        }
        context.recentStore.edit { prefs -> prefs[KEY] = arr.toString() }
    }

    suspend fun load(): List<RecentIssue> {
        val raw = context.recentStore.data.first()[KEY].orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                RecentIssue(
                    id = o.optInt("id"),
                    identifier = o.optString("identifier"),
                    title = o.optString("title"),
                    projectRef = if (o.isNull("projectRef")) null else o.optString("projectRef")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Drops the recent-issue history; used when an account signs out. */
    suspend fun clearAll() {
        context.recentStore.edit { prefs -> prefs.remove(KEY) }
    }
}
