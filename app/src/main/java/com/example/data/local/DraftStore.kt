package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.draftStore: DataStore<Preferences> by preferencesDataStore(name = "itsaplan_drafts")

/**
 * Persists user drafts so backgrounding or process death doesn't lose a typed
 * comment or an in-progress doc edit. Keyed by issue id and document id.
 */
class DraftStore(private val context: Context) {

    private fun commentKey(issueId: Int) = stringPreferencesKey("draft_comment_$issueId")
    private fun docKey(docId: Int) = stringPreferencesKey("draft_doc_$docId")
    private fun issueDescKey(issueId: Int) = stringPreferencesKey("draft_issue_desc_$issueId")

    suspend fun loadCommentDraft(issueId: Int): String =
        context.draftStore.data.first()[commentKey(issueId)].orEmpty()

    suspend fun saveCommentDraft(issueId: Int, text: String) {
        context.draftStore.edit { prefs ->
            if (text.isBlank()) prefs.remove(commentKey(issueId))
            else prefs[commentKey(issueId)] = text
        }
    }

    suspend fun loadDocDraft(docId: Int): String =
        context.draftStore.data.first()[docKey(docId)].orEmpty()

    suspend fun saveDocDraft(docId: Int, content: String) {
        context.draftStore.edit { prefs ->
            if (content.isBlank()) prefs.remove(docKey(docId))
            else prefs[docKey(docId)] = content
        }
    }

    suspend fun loadIssueDescriptionDraft(issueId: Int): String =
        context.draftStore.data.first()[issueDescKey(issueId)].orEmpty()

    suspend fun saveIssueDescriptionDraft(issueId: Int, text: String) {
        context.draftStore.edit { prefs ->
            if (text.isBlank()) prefs.remove(issueDescKey(issueId))
            else prefs[issueDescKey(issueId)] = text
        }
    }

    /** Drops every draft; used when an account signs out. */
    suspend fun clearAll() {
        context.draftStore.edit { it.clear() }
    }
}
