package io.github.jontstaz.itsaplan.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import io.github.jontstaz.itsaplan.data.local.AuthMode
import io.github.jontstaz.itsaplan.data.local.ConnectionStore
import io.github.jontstaz.itsaplan.data.local.DraftStore
import io.github.jontstaz.itsaplan.data.local.RecentIssuesStore
import io.github.jontstaz.itsaplan.data.local.WidgetCache
import io.github.jontstaz.itsaplan.data.local.StoredConnection
import io.github.jontstaz.itsaplan.data.model.AttachmentDto
import io.github.jontstaz.itsaplan.data.model.AuthConfigResponse
import io.github.jontstaz.itsaplan.data.model.BoardIssueDto
import io.github.jontstaz.itsaplan.data.model.ChecklistDto
import io.github.jontstaz.itsaplan.data.model.ChecklistItemDto
import io.github.jontstaz.itsaplan.data.model.CommentReactionDto
import io.github.jontstaz.itsaplan.data.model.CrossProjectIssueHitDto
import io.github.jontstaz.itsaplan.data.model.CurrentUserDto
import io.github.jontstaz.itsaplan.data.model.CycleDto
import io.github.jontstaz.itsaplan.data.model.CyclePageResponse
import io.github.jontstaz.itsaplan.data.model.DocumentDto
import io.github.jontstaz.itsaplan.data.model.DocumentSummaryDto
import io.github.jontstaz.itsaplan.data.model.FeedCountsDto
import io.github.jontstaz.itsaplan.data.model.FeedItemDto
import io.github.jontstaz.itsaplan.data.model.FeedPageResponse
import io.github.jontstaz.itsaplan.data.model.HealthResponse
import io.github.jontstaz.itsaplan.data.model.IssueAgentRunPageDto
import io.github.jontstaz.itsaplan.data.model.IssueDetailDto
import io.github.jontstaz.itsaplan.data.model.InitiativeDto
import io.github.jontstaz.itsaplan.data.model.InitiativePageResponse
import io.github.jontstaz.itsaplan.data.model.IssueLinkDto
import io.github.jontstaz.itsaplan.data.model.IssueResponseDto
import io.github.jontstaz.itsaplan.data.model.IssueSearchHitDto
import io.github.jontstaz.itsaplan.data.model.IssueWatcherDto
import io.github.jontstaz.itsaplan.data.model.LabelDto
import io.github.jontstaz.itsaplan.data.model.MemberPageResponse
import io.github.jontstaz.itsaplan.data.model.NotificationPageResponse
import io.github.jontstaz.itsaplan.data.model.ProjectBoardResponse
import io.github.jontstaz.itsaplan.data.model.ProjectDetailDto
import io.github.jontstaz.itsaplan.data.model.ProjectListItem
import io.github.jontstaz.itsaplan.data.model.TeamDetailDto
import io.github.jontstaz.itsaplan.data.model.TeamDto
import io.github.jontstaz.itsaplan.data.model.WorklogDto
import io.github.jontstaz.itsaplan.data.remote.ApiClientManager
import io.github.jontstaz.itsaplan.data.remote.ApiClientManager.Companion.extractCookieHeader
import io.github.jontstaz.itsaplan.data.remote.ApiClientManager.Companion.jsonBody
import io.github.jontstaz.itsaplan.data.remote.ApiClientManager.Companion.jsonBodyFromMap
import io.github.jontstaz.itsaplan.data.remote.ApiException
import io.github.jontstaz.itsaplan.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

private const val MAX_UPLOAD_BYTES = 25L * 1024 * 1024

data class ServerProbeResult(
    val normalizedUrl: String,
    val health: HealthResponse,
    val authConfig: AuthConfigResponse
)

class ItsAPlanRepository(
    private val context: Context,
    val store: ConnectionStore,
    val clientManager: ApiClientManager
) {
    val connectionFlow: Flow<StoredConnection> = store.connectionFlow

    /**
     * Clears account-scoped local caches when the signed-in account or server changes,
     * so a draft, recent issue or widget entry from one account can never surface under
     * another (or be submitted to a different server).
     */
    private suspend fun onSessionEstablished(serverUrl: String, userId: String) {
        val scope = "${serverUrl.lowercase().trimEnd('/')}|$userId"
        val previous = store.accountScope()
        if (previous.isNotEmpty() && previous != scope) {
            DraftStore(context).clearAll()
            RecentIssuesStore(context).clearAll()
            WidgetCache(context).clear()
        }
        store.setAccountScope(scope)
    }

    /**
     * Retrofit hands back `Response<…>` for bodyless endpoints without throwing on
     * non-2xx, so without this a 403 or 500 would be reported to the UI as success.
     */
    private fun retrofit2.Response<*>.requireSuccess(action: String) {
        if (isSuccessful) return
        throw clientManager.exceptionFor(code(), errorBody()?.string(), action)
    }

    private suspend fun <T> safeCall(block: suspend () -> T): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(block())
        } catch (t: Throwable) {
            Result.failure(clientManager.parseError(t))
        }
    }

    // --- Connection & Auth ---

    suspend fun probeServer(serverUrl: String): Result<ServerProbeResult> = safeCall {
        val normalized = ApiClientManager.normalizeServerUrl(serverUrl)
            ?: throw ApiException(0, "Enter a valid http:// or https:// server URL.")
        val api = clientManager.createTransientApi(normalized)
        val health = api.getHealth()
        val authConfig = api.getAuthConfig()
        ServerProbeResult(normalizedUrl = normalized, health = health, authConfig = authConfig)
    }

    suspend fun connectWithApiKey(serverUrl: String, apiKey: String): Result<CurrentUserDto> = safeCall {
        val normalized = ApiClientManager.normalizeServerUrl(serverUrl)
            ?: throw ApiException(0, "Enter a valid http:// or https:// server URL.")
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isBlank()) throw ApiException(400, "API key cannot be empty.")

        val transient = clientManager.createTransientApi(
            serverUrl = normalized,
            authMode = AuthMode.API_KEY,
            apiKey = trimmedKey
        )
        val me = transient.getMe()
        if (!me.authenticated || me.user == null) {
            throw ApiException(401, "API key was rejected by the server.")
        }
        store.saveApiKeyConnection(normalized, trimmedKey)
        clientManager.configure(normalized, AuthMode.API_KEY, apiKey = trimmedKey)
        onSessionEstablished(normalized, me.user.id)
        me.user
    }

    suspend fun connectWithPassword(
        serverUrl: String,
        identifier: String,
        password: String
    ): Result<CurrentUserDto> = safeCall {
        val normalized = ApiClientManager.normalizeServerUrl(serverUrl)
            ?: throw ApiException(0, "Enter a valid http:// or https:// server URL.")
        val id = identifier.trim()
        if (id.isBlank() || password.isEmpty()) {
            throw ApiException(400, "Enter both your email/username and password.")
        }

        val transient = clientManager.createTransientApi(normalized)
        val isEmail = id.contains('@')
        val response = if (isEmail) {
            transient.signInEmail(jsonBody("email" to id, "password" to password))
        } else {
            transient.signInUsername(jsonBody("username" to id, "password" to password))
        }

        if (!response.isSuccessful) {
            val code = response.code()
            val errMsg = when (code) {
                401, 403 -> "Invalid email/username or password."
                429 -> "Too many login attempts. Please wait a moment."
                else -> "Sign-in failed (HTTP $code)."
            }
            throw ApiException(code, errMsg)
        }

        val cookies = extractCookieHeader(response.headers().values("Set-Cookie"))
        if (cookies.isBlank()) {
            throw ApiException(401, "Server did not return a session cookie. Try signing in with an API Key instead.")
        }

        val verifiedApi = clientManager.createTransientApi(
            serverUrl = normalized,
            authMode = AuthMode.SESSION,
            sessionCookie = cookies
        )
        val me = verifiedApi.getMe()
        if (!me.authenticated || me.user == null) {
            throw ApiException(401, "Session verification failed. Try using a Personal API Key.")
        }

        store.saveSessionConnection(normalized, cookies)
        clientManager.configure(normalized, AuthMode.SESSION, sessionCookie = cookies)
        onSessionEstablished(normalized, me.user.id)
        me.user
    }

    suspend fun restoreSavedSession(): Result<CurrentUserDto?> = safeCall {
        val saved = store.getSnapshot()
        if (!saved.hasCredentials) return@safeCall null
        clientManager.configure(
            serverUrl = saved.serverUrl,
            authMode = saved.authMode,
            apiKey = saved.apiKey,
            sessionCookie = saved.sessionCookie
        )
        val me = clientManager.getApi().getMe()
        if (me.authenticated && me.user != null) {
            onSessionEstablished(saved.serverUrl, me.user.id)
            me.user
        } else {
            store.clearAuth(keepServerUrl = true)
            null
        }
    }

    suspend fun signOut(): Result<Unit> = safeCall {
        val saved = store.getSnapshot()
        if (saved.authMode == AuthMode.SESSION && saved.hasCredentials) {
            try {
                clientManager.getApi().signOut(jsonBody())
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: Exception) {
                // Always clear local credentials even if server sign-out fails
            }
        }
        // Retire the session before anything else: if a later cleanup step fails, the
        // shared client still must not keep answering with the old credentials.
        clientManager.clearAuth()
        store.clearAuth(keepServerUrl = true)
        // Account-scoped local data must not survive into the next sign-in.
        DraftStore(context).clearAll()
        RecentIssuesStore(context).clearAll()
        WidgetCache(context).clear()
    }

    suspend fun saveLastProjectRef(ref: String) = store.saveLastProjectRef(ref)
    suspend fun saveThemeMode(mode: ThemeMode) = store.saveThemeMode(mode)
    suspend fun saveLiveSyncEnabled(enabled: Boolean) = store.saveLiveSyncEnabled(enabled)

    // --- Teams & Projects ---

    suspend fun listTeams(): Result<List<TeamDto>> = safeCall {
        clientManager.getApi().listTeams()
    }

    suspend fun getTeam(teamId: Int): Result<TeamDetailDto> = safeCall {
        clientManager.getApi().getTeam(teamId)
    }

    suspend fun listTeamMembers(teamId: Int): Result<MemberPageResponse> = safeCall {
        clientManager.getApi().listTeamMembers(teamId)
    }

    suspend fun listProjects(query: String? = null, teamId: Int? = null): Result<List<ProjectListItem>> = safeCall {
        clientManager.getApi().listProjects(query = query?.takeIf { it.isNotBlank() }, teamId = teamId, permissions = true)
    }

    suspend fun createProject(
        teamId: Int,
        key: String,
        name: String,
        description: String?,
        preset: String
    ): Result<ProjectDetailDto> = safeCall {
        val map = mutableMapOf<String, Any?>(
            "key" to key.trim().uppercase(),
            "name" to name.trim(),
            "preset" to preset
        )
        if (!description.isNullOrBlank()) {
            map["description"] = description.trim()
        }
        clientManager.getApi().createProject(teamId, jsonBodyFromMap(map))
    }

    suspend fun getProjectBoard(projectRef: String): Result<ProjectBoardResponse> = safeCall {
        clientManager.getApi().getProjectBoard(projectRef)
    }

    suspend fun updateProjectPreferences(
        projectRef: String,
        isFavorite: Boolean? = null,
        isHidden: Boolean? = null
    ): Result<Unit> = safeCall {
        val map = mutableMapOf<String, Any?>()
        if (isFavorite != null) map["isFavorite"] = isFavorite
        if (isHidden != null) map["isHidden"] = isHidden
        clientManager.getApi().updateProjectPreferences(projectRef, jsonBodyFromMap(map))
        Unit
    }

    // --- Issues ---

    suspend fun getBoardIssues(projectRef: String): Result<List<BoardIssueDto>> = safeCall {
        clientManager.getApi().getBoardIssues(projectRef).issues
    }

    suspend fun listArchivedIssues(projectRef: String): Result<List<IssueResponseDto>> = safeCall {
        clientManager.getApi().listArchivedIssues(projectRef)
    }

    suspend fun searchProjectIssues(projectRef: String, query: String): Result<List<IssueSearchHitDto>> = safeCall {
        clientManager.getApi().searchProjectIssues(projectRef, query)
    }

    suspend fun listCrossProjectIssues(
        assigneeUserId: String? = null,
        priority: String? = null,
        stateTypes: String? = null,
        limit: Int = 100
    ): Result<List<CrossProjectIssueHitDto>> = safeCall {
        clientManager.getApi().listCrossProjectIssues(
            assigneeUserId = assigneeUserId,
            priority = priority,
            stateTypes = stateTypes,
            limit = limit
        )
    }

    suspend fun searchCrossProjectIssues(query: String): Result<List<CrossProjectIssueHitDto>> = safeCall {
        clientManager.getApi().searchCrossProjectIssues(query = query, limit = 40)
    }

    suspend fun createIssue(
        projectRef: String,
        columnId: Int,
        title: String,
        description: String? = null,
        typeId: Int? = null,
        cycleId: Int? = null,
        assigneeUserId: String? = null,
        delegateUserId: String? = null,
        parentId: Int? = null,
        priority: String = "none",
        estimatePoints: Double? = null,
        estimateMinutes: Int? = null,
        dueDate: String? = null,
        labelIds: List<Int> = emptyList()
    ): Result<IssueResponseDto> = safeCall {
        val map = mutableMapOf<String, Any?>(
            "columnId" to columnId,
            "title" to title.trim()
        )
        if (priority != "none" && priority.isNotBlank()) map["priority"] = priority
        if (!description.isNullOrBlank()) map["description"] = description.trim()
        if (typeId != null) map["typeId"] = typeId
        if (cycleId != null) map["cycleId"] = cycleId
        if (!assigneeUserId.isNullOrBlank()) map["assigneeUserId"] = assigneeUserId
        if (!delegateUserId.isNullOrBlank()) map["delegateUserId"] = delegateUserId
        if (parentId != null) map["parentId"] = parentId
        if (estimatePoints != null) map["estimatePoints"] = estimatePoints
        if (estimateMinutes != null) map["estimateMinutes"] = estimateMinutes
        if (!dueDate.isNullOrBlank()) map["dueDate"] = dueDate.trim()
        if (labelIds.isNotEmpty()) map["labelIds"] = labelIds

        clientManager.getApi().createIssue(projectRef, jsonBodyFromMap(map))
    }

    suspend fun getIssueDetail(issueId: Int): Result<IssueDetailDto> = safeCall {
        clientManager.getApi().getIssueDetail(issueId)
    }

    suspend fun updateIssueFields(issueId: Int, patch: Map<String, Any?>): Result<IssueResponseDto> = safeCall {
        clientManager.getApi().updateIssue(issueId, jsonBodyFromMap(patch))
    }

    suspend fun archiveIssue(issueId: Int, subtasksMode: String = "detach"): Result<Unit> = safeCall {
        clientManager.getApi().archiveIssue(issueId, jsonBody("subtasks" to subtasksMode)).requireSuccess("Could not archive issue")
        Unit
    }

    suspend fun restoreIssue(issueId: Int): Result<Unit> = safeCall {
        clientManager.getApi().restoreIssue(issueId).requireSuccess("Could not restore issue")
        Unit
    }

    suspend fun deleteIssue(issueId: Int, subtasksMode: String? = "detach"): Result<Unit> = safeCall {
        clientManager.getApi().deleteIssue(issueId, subtasksMode).requireSuccess("Could not delete issue")
        Unit
    }

    suspend fun setCustomFieldValue(
        issueId: Int,
        fieldId: Int,
        value: String? = null,
        valueEnd: String? = null,
        optionIds: List<Int>? = null
    ): Result<Unit> = safeCall {
        val map = mutableMapOf<String, Any?>()
        if (optionIds != null) {
            map["optionIds"] = optionIds
        } else {
            map["value"] = value
            if (valueEnd != null) map["valueEnd"] = valueEnd
        }
        clientManager.getApi().setCustomFieldValue(issueId, fieldId, jsonBodyFromMap(map))
        Unit
    }

    // --- Feed, Comments, Reactions ---

    suspend fun getIssueFeed(
        issueId: Int,
        filter: String? = null,
        cursor: String? = null
    ): Result<FeedPageResponse> = safeCall {
        clientManager.getApi().getIssueFeed(
            issueId = issueId,
            limit = 40,
            cursor = cursor,
            filter = filter,
            order = "desc"
        )
    }

    suspend fun getIssueFeedCounts(issueId: Int): Result<FeedCountsDto> = safeCall {
        clientManager.getApi().getIssueFeedCounts(issueId)
    }

    suspend fun createComment(issueId: Int, body: String, replyToId: Int? = null): Result<FeedItemDto> = safeCall {
        val map = mutableMapOf<String, Any?>("body" to body.trim())
        if (replyToId != null) map["replyToId"] = replyToId
        clientManager.getApi().createComment(issueId, jsonBodyFromMap(map))
    }

    suspend fun updateComment(commentId: Int, body: String): Result<FeedItemDto> = safeCall {
        clientManager.getApi().updateComment(commentId, jsonBody("body" to body.trim()))
    }

    suspend fun deleteComment(commentId: Int): Result<Unit> = safeCall {
        clientManager.getApi().deleteComment(commentId).requireSuccess("Could not delete comment")
        Unit
    }

    suspend fun addReaction(commentId: Int, emoji: String): Result<List<CommentReactionDto>> = safeCall {
        clientManager.getApi().addReaction(commentId, jsonBody("emoji" to emoji))
    }

    suspend fun removeReaction(commentId: Int, emoji: String): Result<List<CommentReactionDto>> = safeCall {
        clientManager.getApi().removeReaction(commentId, emoji)
    }

    // --- Watchers ---

    suspend fun watchIssue(issueId: Int): Result<Unit> = safeCall {
        clientManager.getApi().watchIssue(issueId)
        Unit
    }

    suspend fun unwatchIssue(issueId: Int): Result<Unit> = safeCall {
        clientManager.getApi().unwatchIssue(issueId)
        Unit
    }

    // --- Checklists ---

    suspend fun createChecklist(issueId: Int, title: String): Result<ChecklistDto> = safeCall {
        clientManager.getApi().createChecklist(issueId, jsonBody("title" to title.trim()))
    }

    suspend fun deleteChecklist(checklistId: Int): Result<Unit> = safeCall {
        clientManager.getApi().deleteChecklist(checklistId).requireSuccess("Could not delete checklist")
        Unit
    }

    suspend fun createChecklistItem(checklistId: Int, content: String): Result<ChecklistItemDto> = safeCall {
        clientManager.getApi().createChecklistItem(checklistId, jsonBody("content" to content.trim()))
    }

    suspend fun toggleChecklistItem(itemId: Int, done: Boolean): Result<ChecklistItemDto> = safeCall {
        clientManager.getApi().updateChecklistItem(itemId, jsonBody("done" to done))
    }

    suspend fun deleteChecklistItem(itemId: Int): Result<Unit> = safeCall {
        clientManager.getApi().deleteChecklistItem(itemId).requireSuccess("Could not delete checklist item")
        Unit
    }

    // --- Worklogs ---

    suspend fun listWorklogs(issueId: Int): Result<List<WorklogDto>> = safeCall {
        clientManager.getApi().listWorklogs(issueId)
    }

    suspend fun createWorklog(
        issueId: Int,
        minutes: Int,
        spentOn: String,
        note: String?
    ): Result<WorklogDto> = safeCall {
        val map = mutableMapOf<String, Any?>(
            "minutes" to minutes,
            "spentOn" to spentOn
        )
        if (!note.isNullOrBlank()) map["note"] = note.trim()
        clientManager.getApi().createWorklog(issueId, jsonBodyFromMap(map))
    }

    suspend fun deleteWorklog(worklogId: Int): Result<Unit> = safeCall {
        clientManager.getApi().deleteWorklog(worklogId).requireSuccess("Could not delete worklog")
        Unit
    }

    // --- Links ---

    suspend fun createIssueLink(issueId: Int, targetIssueId: Int, kind: String): Result<IssueLinkDto> = safeCall {
        clientManager.getApi().createIssueLink(
            issueId,
            jsonBody("targetIssueId" to targetIssueId, "kind" to kind)
        )
    }

    suspend fun deleteIssueLink(issueId: Int, linkId: Int): Result<Unit> = safeCall {
        clientManager.getApi().deleteIssueLink(issueId, linkId).requireSuccess("Could not remove link")
        Unit
    }

    // --- Attachments ---

    suspend fun listAttachments(issueId: Int): Result<List<AttachmentDto>> = safeCall {
        clientManager.getApi().listAttachments(issueId)
    }

    suspend fun uploadAttachmentFromUri(issueId: Int, uri: Uri): Result<AttachmentDto> = safeCall {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        var fileName = "attachment"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) {
                    val queried = cursor.getString(idx)
                    if (!queried.isNullOrBlank()) fileName = queried
                }
            }
        }
        // Cap while streaming: reading first and checking afterwards would let an
        // unbounded provider stream exhaust the heap before the limit is applied.
        val bytes = resolver.openInputStream(uri)?.use { readCapped(it, MAX_UPLOAD_BYTES) }
            ?: throw ApiException(400, "Could not read selected file.")
        val requestBody = bytes.toRequestBody(mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", fileName, requestBody)
        clientManager.getApi().uploadAttachment(issueId, part)
    }

    private fun readCapped(input: java.io.InputStream, maxBytes: Long): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(64 * 1024)
        var total = 0L
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw ApiException(413, "File exceeds the 25 MB upload limit.")
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    suspend fun deleteAttachment(publicId: String): Result<Unit> = safeCall {
        clientManager.getApi().deleteAttachment(publicId).requireSuccess("Could not delete attachment")
        Unit
    }

    // --- Agent Runs ---

    suspend fun listAgentRuns(issueId: Int): Result<IssueAgentRunPageDto> = safeCall {
        clientManager.getApi().listAgentRuns(issueId)
    }

    // --- Labels ---

    suspend fun createLabel(projectRef: String, name: String, color: String?): Result<LabelDto> = safeCall {
        val map = mutableMapOf<String, Any?>("name" to name.trim())
        if (!color.isNullOrBlank()) map["color"] = color
        clientManager.getApi().createLabel(projectRef, jsonBodyFromMap(map))
    }

    // --- Cycles ---

    suspend fun listPlannedCycles(projectRef: String): Result<List<CycleDto>> = safeCall {
        clientManager.getApi().listCycles(projectRef, status = "planned")
    }

    suspend fun listCompletedCycles(projectRef: String, page: Int = 1): Result<CyclePageResponse> = safeCall {
        clientManager.getApi().listCompletedCycles(projectRef, page = page, pageSize = 20)
    }

    suspend fun listInitiatives(
        projectRef: String,
        status: String? = null,
        search: String? = null,
        page: Int = 1
    ): Result<InitiativePageResponse> = safeCall {
        clientManager.getApi().listInitiatives(
            projectRef,
            status = status,
            search = search?.takeIf { it.isNotBlank() },
            page = page
        )
    }

    suspend fun createInitiative(
        projectRef: String,
        title: String,
        description: String?,
        status: String?,
        targetDate: String?
    ): Result<InitiativeDto> = safeCall {
        val map = mutableMapOf<String, Any?>("title" to title.trim())
        description?.takeIf { it.isNotBlank() }?.let { map["description"] = it.trim() }
        status?.takeIf { it.isNotBlank() }?.let { map["status"] = it }
        targetDate?.takeIf { it.isNotBlank() }?.let { map["targetDate"] = it.trim() }
        clientManager.getApi().createInitiative(projectRef, jsonBodyFromMap(map))
    }

    suspend fun updateInitiativeStatus(initiativeId: Int, status: String): Result<InitiativeDto> = safeCall {
        clientManager.getApi().updateInitiative(
            initiativeId,
            jsonBodyFromMap(mapOf("status" to status))
        )
    }

    suspend fun deleteInitiative(initiativeId: Int): Result<Unit> = safeCall {
        clientManager.getApi().deleteInitiative(initiativeId).requireSuccess("Could not delete initiative")
    }

    suspend fun createCycle(
        projectRef: String,
        name: String,
        goal: String?,
        startDate: String,
        endDate: String
    ): Result<CycleDto> = safeCall {
        val map = mutableMapOf<String, Any?>(
            "name" to name.trim(),
            "startDate" to startDate.trim(),
            "endDate" to endDate.trim()
        )
        if (!goal.isNullOrBlank()) map["goal"] = goal.trim()
        clientManager.getApi().createCycle(projectRef, jsonBodyFromMap(map))
    }

    suspend fun updateCycle(cycleId: Int, patch: Map<String, Any?>): Result<CycleDto> = safeCall {
        clientManager.getApi().updateCycle(cycleId, jsonBodyFromMap(patch))
    }

    suspend fun deleteCycle(cycleId: Int): Result<Unit> = safeCall {
        clientManager.getApi().deleteCycle(cycleId).requireSuccess("Could not delete cycle")
        Unit
    }

    suspend fun finishCycle(cycleId: Int): Result<CycleDto> = safeCall {
        clientManager.getApi().finishCycle(cycleId)
    }

    suspend fun startNextCycle(cycleId: Int): Result<CycleDto> = safeCall {
        clientManager.getApi().startNextCycle(cycleId).cycle
    }

    suspend fun transferCycleIssues(cycleId: Int, targetCycleId: Int?): Result<Unit> = safeCall {
        clientManager.getApi().transferCycleIssues(cycleId, jsonBody("targetCycleId" to targetCycleId))
        Unit
    }

    // --- Notifications ---

    suspend fun listNotifications(
        includeRead: Boolean = true,
        includeSnoozed: Boolean = false,
        types: String? = null,
        cursor: String? = null
    ): Result<NotificationPageResponse> = safeCall {
        clientManager.getApi().listNotifications(
            limit = 40,
            cursor = cursor,
            types = types,
            includeRead = includeRead,
            includeSnoozed = includeSnoozed
        )
    }

    suspend fun getUnreadCount(): Result<Int> = safeCall {
        clientManager.getApi().getUnreadNotificationCount().unread
    }

    suspend fun markNotificationRead(id: Int, read: Boolean): Result<Unit> = safeCall {
        clientManager.getApi().setNotificationRead(id, jsonBody("read" to read)).requireSuccess("Could not update notification")
        Unit
    }

    suspend fun markAllNotificationsRead(): Result<Unit> = safeCall {
        clientManager.getApi().markAllNotificationsRead(jsonBody()).requireSuccess("Could not mark notifications read")
        Unit
    }

    suspend fun snoozeNotification(id: Int, untilIso: String?): Result<Unit> = safeCall {
        clientManager.getApi().snoozeNotification(id, jsonBody("until" to untilIso)).requireSuccess("Could not snooze notification")
        Unit
    }

    suspend fun deleteNotification(id: Int): Result<Unit> = safeCall {
        clientManager.getApi().deleteNotification(id).requireSuccess("Could not delete notification")
        Unit
    }

    // --- Live Sync ---

    suspend fun getSyncRevs(scopes: List<String>): Result<Map<String, String>> = safeCall {
        if (scopes.isEmpty()) return@safeCall emptyMap()
        clientManager.getApi().getSyncRevs(scopes.joinToString(",")).revs
    }

    // --- Documents (Docs) ---

    suspend fun listDocuments(projectRef: String, query: String? = null, archived: Boolean = false): Result<List<DocumentSummaryDto>> = safeCall {
        clientManager.getApi().listDocuments(
            projectRef,
            query = query?.takeIf { it.isNotBlank() },
            archived = if (archived) "true" else null
        )
    }

    suspend fun getDocument(projectRef: String, documentId: Int): Result<DocumentDto> = safeCall {
        clientManager.getApi().getDocument(projectRef, documentId)
    }

    suspend fun createDocument(
        projectRef: String,
        title: String,
        content: String? = null,
        parentId: Int? = null
    ): Result<DocumentDto> = safeCall {
        val map = mutableMapOf<String, Any?>("title" to title.trim())
        if (!content.isNullOrBlank()) map["content"] = content
        if (parentId != null) map["parentId"] = parentId
        clientManager.getApi().createDocument(projectRef, jsonBodyFromMap(map))
    }

    suspend fun updateDocument(
        projectRef: String,
        documentId: Int,
        version: Int,
        title: String? = null,
        content: String? = null
    ): Result<DocumentDto> = safeCall {
        val map = mutableMapOf<String, Any?>("version" to version)
        if (title != null) map["title"] = title.trim()
        if (content != null) map["content"] = content
        clientManager.getApi().updateDocument(projectRef, documentId, jsonBodyFromMap(map))
    }

    suspend fun archiveDocument(projectRef: String, documentId: Int, version: Int): Result<Unit> = safeCall {
        clientManager.getApi().archiveDocument(projectRef, documentId, jsonBody("version" to version)).requireSuccess("Could not archive document")
        Unit
    }

    suspend fun restoreDocument(projectRef: String, documentId: Int, version: Int): Result<Unit> = safeCall {
        clientManager.getApi().restoreDocument(projectRef, documentId, jsonBody("version" to version))
        Unit
    }
}
