package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AgentRunDto
import com.example.data.model.AttachmentDto
import com.example.data.model.CursorDto
import com.example.data.model.CycleDto
import com.example.data.model.FeedCountsDto
import com.example.data.model.FeedItemDto
import com.example.data.model.IssueDetailDto
import com.example.data.model.IssueSearchHitDto
import com.example.data.model.ProjectBoardResponse
import com.example.data.model.WorklogDto
import com.example.data.repository.ItsAPlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class FeedFilter(val apiValue: String?, val label: String) {
    ALL(null, "All"),
    COMMENTS("comments", "Comments"),
    HISTORY("history", "Activity"),
    WORKLOG("worklog", "Time")
}

data class IssueDetailUiState(
    val issueId: Int = 0,
    val issue: IssueDetailDto? = null,
    val boardScaffold: ProjectBoardResponse? = null,
    val projectCycles: List<CycleDto> = emptyList(),
    val feedItems: List<FeedItemDto> = emptyList(),
    val feedCounts: FeedCountsDto = FeedCountsDto(),
    val feedFilter: FeedFilter = FeedFilter.ALL,
    val feedNextCursor: CursorDto? = null,
    val worklogs: List<WorklogDto> = emptyList(),
    val attachments: List<AttachmentDto> = emptyList(),
    val agentRuns: List<AgentRunDto> = emptyList(),
    val linkSearchResults: List<IssueSearchHitDto> = emptyList(),
    val isLoading: Boolean = false,
    val isUploading: Boolean = false,
    val isMutating: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val deleted: Boolean = false
)

class IssueDetailViewModel(
    private val repository: ItsAPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IssueDetailUiState())
    val uiState: StateFlow<IssueDetailUiState> = _uiState.asStateFlow()

    private var issueSyncJob: Job? = null
    private var lastIssueRev: String? = null

    fun loadIssue(
        issueId: Int,
        knownBoard: ProjectBoardResponse? = null,
        knownProjectRef: String? = null,
        silent: Boolean = false
    ) {
        if (issueId <= 0) return
        val switchingIssue = _uiState.value.issueId != issueId
        if (switchingIssue) {
            lastIssueRev = null
            _uiState.value = IssueDetailUiState(
                issueId = issueId,
                boardScaffold = knownBoard,
                isLoading = true
            )
            startIssueSync(issueId)
        } else if (!silent) {
            _uiState.update { it.copy(isLoading = true, error = null) }
        }

        viewModelScope.launch {
            val issueDef = async { repository.getIssueDetail(issueId) }
            val feedDef = async { repository.getIssueFeed(issueId, _uiState.value.feedFilter.apiValue) }
            val countsDef = async { repository.getIssueFeedCounts(issueId) }
            val worklogsDef = async { repository.listWorklogs(issueId) }
            val attachmentsDef = async { repository.listAttachments(issueId) }
            val runsDef = async { repository.listAgentRuns(issueId) }

            val issueRes = issueDef.await()
            val issue = issueRes.getOrElse { err ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = err.message ?: "Failed to load issue."
                    )
                }
                return@launch
            }

            // Ensure we have the matching ProjectBoardResponse (columns, labels, assignees, customFields)
            var scaffold = _uiState.value.boardScaffold
            if (scaffold == null || scaffold.project.id != issue.projectId) {
                val targetRef = knownProjectRef ?: repository.listProjects().getOrNull()
                    ?.firstOrNull { it.id == issue.projectId }?.routeKey
                if (targetRef != null) {
                    scaffold = repository.getProjectBoard(targetRef).getOrNull()
                }
            }

            val cycles = if (scaffold?.project?.cyclesEnabled == true) {
                repository.listPlannedCycles(scaffold.project.routeKey).getOrElse { emptyList() }
            } else {
                emptyList()
            }

            val feedPage = feedDef.await().getOrNull()
            val counts = countsDef.await().getOrElse { _uiState.value.feedCounts }
            val worklogs = worklogsDef.await().getOrElse { emptyList() }
            val attachments = attachmentsDef.await().getOrElse { emptyList() }
            val runs = runsDef.await().getOrNull()?.items ?: emptyList()

            // A slower request for a previously opened issue must not repaint the
            // screen the user has since navigated to.
            if (_uiState.value.issueId != issueId) return@launch

            _uiState.update {
                it.copy(
                    issueId = issueId,
                    issue = issue,
                    boardScaffold = scaffold,
                    projectCycles = cycles,
                    feedItems = feedPage?.items ?: it.feedItems,
                    feedNextCursor = feedPage?.nextCursor,
                    feedCounts = counts,
                    worklogs = worklogs,
                    attachments = attachments,
                    agentRuns = runs,
                    isLoading = false,
                    error = null
                )
            }
        }
    }

    fun selectFeedFilter(filter: FeedFilter) {
        val issueId = _uiState.value.issueId
        if (issueId <= 0) return
        _uiState.update { it.copy(feedFilter = filter) }
        viewModelScope.launch {
            repository.getIssueFeed(issueId, filter.apiValue).onSuccess { page ->
                _uiState.update {
                    it.copy(feedItems = page.items, feedNextCursor = page.nextCursor)
                }
            }
        }
    }

    fun loadMoreFeed() {
        val state = _uiState.value
        val cursor = state.feedNextCursor ?: return
        viewModelScope.launch {
            repository.getIssueFeed(state.issueId, state.feedFilter.apiValue, cursor.takeIf { it.isValid }?.toQueryParam()).onSuccess { page ->
                _uiState.update {
                    it.copy(
                        feedItems = it.feedItems + page.items,
                        feedNextCursor = page.nextCursor
                    )
                }
            }
        }
    }

    fun updateIssue(patch: Map<String, Any?>, successMsg: String? = null, onBoardDirty: () -> Unit = {}) {
        val issueId = _uiState.value.issueId
        if (issueId <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            repository.updateIssueFields(issueId, patch).fold(
                onSuccess = {
                    _uiState.update { it.copy(isMutating = false) }
                    if (successMsg != null) postMessage(successMsg)
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                    onBoardDirty()
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isMutating = false) }
                    postMessage(err.message ?: "Update failed.")
                }
            )
        }
    }

    fun updateCustomField(fieldId: Int, value: String? = null, optionIds: List<Int>? = null) {
        val issueId = _uiState.value.issueId
        if (issueId <= 0) return
        viewModelScope.launch {
            repository.setCustomFieldValue(issueId, fieldId, value = value, optionIds = optionIds).fold(
                onSuccess = {
                    postMessage("Field updated")
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err -> postMessage(err.message ?: "Failed to update field.") }
            )
        }
    }

    fun toggleWatch(currentlyWatching: Boolean) {
        val issueId = _uiState.value.issueId
        if (issueId <= 0) return
        viewModelScope.launch {
            val res = if (currentlyWatching) repository.unwatchIssue(issueId) else repository.watchIssue(issueId)
            res.fold(
                onSuccess = {
                    postMessage(if (currentlyWatching) "Stopped watching issue" else "Watching issue")
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err -> postMessage(err.message ?: "Could not update watch state.") }
            )
        }
    }

    fun archiveOrRestore(isArchived: Boolean, onBoardDirty: () -> Unit) {
        val issueId = _uiState.value.issueId
        if (issueId <= 0) return
        viewModelScope.launch {
            val res = if (isArchived) repository.restoreIssue(issueId) else repository.archiveIssue(issueId)
            res.fold(
                onSuccess = {
                    postMessage(if (isArchived) "Issue restored" else "Issue archived")
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                    onBoardDirty()
                },
                onFailure = { err -> postMessage(err.message ?: "Action failed.") }
            )
        }
    }

    fun deleteIssue(onBoardDirty: () -> Unit) {
        val issueId = _uiState.value.issueId
        if (issueId <= 0) return
        viewModelScope.launch {
            repository.deleteIssue(issueId).fold(
                onSuccess = {
                    onBoardDirty()
                    _uiState.update { it.copy(deleted = true) }
                },
                onFailure = { err -> postMessage(err.message ?: "Delete failed.") }
            )
        }
    }

    // --- Comments & Reactions ---

    fun postComment(body: String, replyToId: Int? = null, onSent: () -> Unit = {}) {
        val issueId = _uiState.value.issueId
        if (issueId <= 0 || body.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isMutating = true) }
            repository.createComment(issueId, body, replyToId).fold(
                onSuccess = {
                    _uiState.update { it.copy(isMutating = false) }
                    onSent()
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isMutating = false) }
                    postMessage(err.message ?: "Failed to post comment.")
                }
            )
        }
    }

    fun editComment(commentId: Int, body: String) {
        val issueId = _uiState.value.issueId
        if (body.isBlank()) return
        viewModelScope.launch {
            repository.updateComment(commentId, body).fold(
                onSuccess = {
                    postMessage("Comment updated")
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err -> postMessage(err.message ?: "Could not edit comment.") }
            )
        }
    }

    fun deleteComment(commentId: Int) {
        val issueId = _uiState.value.issueId
        viewModelScope.launch {
            repository.deleteComment(commentId).fold(
                onSuccess = {
                    postMessage("Comment deleted")
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err -> postMessage(err.message ?: "Could not delete comment.") }
            )
        }
    }

    fun toggleReaction(commentId: Int, emoji: String, alreadyReacted: Boolean) {
        viewModelScope.launch {
            val res = if (alreadyReacted) {
                repository.removeReaction(commentId, emoji)
            } else {
                repository.addReaction(commentId, emoji)
            }
            res.fold(
                onSuccess = { updatedReactions ->
                    _uiState.update { state ->
                        state.copy(
                            feedItems = state.feedItems.map { item ->
                                if (item.id == commentId) {
                                    item.copy(reactions = updatedReactions)
                                } else {
                                    item
                                }
                            }
                        )
                    }
                },
                onFailure = { err -> postMessage(err.message ?: "Reaction failed.") }
            )
        }
    }

    // --- Checklists ---

    fun addChecklist(title: String) {
        val issueId = _uiState.value.issueId
        if (issueId <= 0 || title.isBlank()) return
        viewModelScope.launch {
            repository.createChecklist(issueId, title).fold(
                onSuccess = { loadIssue(issueId, _uiState.value.boardScaffold, silent = true) },
                onFailure = { err -> postMessage(err.message ?: "Could not create checklist.") }
            )
        }
    }

    fun deleteChecklist(checklistId: Int) {
        val issueId = _uiState.value.issueId
        viewModelScope.launch {
            repository.deleteChecklist(checklistId).fold(
                onSuccess = { loadIssue(issueId, _uiState.value.boardScaffold, silent = true) },
                onFailure = { err -> postMessage(err.message ?: "Could not delete checklist.") }
            )
        }
    }

    fun addChecklistItem(checklistId: Int, content: String) {
        val issueId = _uiState.value.issueId
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.createChecklistItem(checklistId, content).fold(
                onSuccess = { loadIssue(issueId, _uiState.value.boardScaffold, silent = true) },
                onFailure = { err -> postMessage(err.message ?: "Could not add checklist item.") }
            )
        }
    }

    fun toggleChecklistItem(itemId: Int, done: Boolean) {
        val issueId = _uiState.value.issueId
        viewModelScope.launch {
            repository.toggleChecklistItem(itemId, done).fold(
                onSuccess = { loadIssue(issueId, _uiState.value.boardScaffold, silent = true) },
                onFailure = { err -> postMessage(err.message ?: "Could not update item.") }
            )
        }
    }

    fun deleteChecklistItem(itemId: Int) {
        val issueId = _uiState.value.issueId
        viewModelScope.launch {
            repository.deleteChecklistItem(itemId).fold(
                onSuccess = { loadIssue(issueId, _uiState.value.boardScaffold, silent = true) },
                onFailure = { err -> postMessage(err.message ?: "Could not delete item.") }
            )
        }
    }

    // --- Subtasks & Links ---

    fun createSubtask(title: String, onBoardDirty: () -> Unit) {
        val issue = _uiState.value.issue ?: return
        val scaffold = _uiState.value.boardScaffold ?: return
        val defaultColId = scaffold.columns.firstOrNull { it.stateType == "unstarted" }?.id
            ?: scaffold.columns.firstOrNull()?.id ?: issue.columnId
        if (title.isBlank()) return

        viewModelScope.launch {
            repository.createIssue(
                projectRef = scaffold.project.routeKey,
                columnId = defaultColId,
                title = title,
                parentId = issue.id
            ).fold(
                onSuccess = {
                    postMessage("Added subtask ${it.identifier}")
                    loadIssue(issue.id, scaffold, silent = true)
                    onBoardDirty()
                },
                onFailure = { err -> postMessage(err.message ?: "Could not create subtask.") }
            )
        }
    }

    fun searchLinkCandidates(query: String) {
        val scaffold = _uiState.value.boardScaffold ?: return
        if (query.trim().length < 2) {
            _uiState.update { it.copy(linkSearchResults = emptyList()) }
            return
        }
        viewModelScope.launch {
            repository.searchProjectIssues(scaffold.project.routeKey, query.trim()).onSuccess { hits ->
                val currentId = _uiState.value.issueId
                _uiState.update {
                    it.copy(linkSearchResults = hits.filter { h -> h.id != currentId })
                }
            }
        }
    }

    fun addIssueLink(targetIssueId: Int, kind: String) {
        val issueId = _uiState.value.issueId
        viewModelScope.launch {
            repository.createIssueLink(issueId, targetIssueId, kind).fold(
                onSuccess = {
                    postMessage("Link added")
                    _uiState.update { it.copy(linkSearchResults = emptyList()) }
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err -> postMessage(err.message ?: "Could not link issue.") }
            )
        }
    }

    fun deleteIssueLink(linkId: Int) {
        val issueId = _uiState.value.issueId
        viewModelScope.launch {
            repository.deleteIssueLink(issueId, linkId).fold(
                onSuccess = { loadIssue(issueId, _uiState.value.boardScaffold, silent = true) },
                onFailure = { err -> postMessage(err.message ?: "Could not remove link.") }
            )
        }
    }

    // --- Worklogs ---

    fun addWorklog(minutes: Int, spentOn: String, note: String?) {
        val issueId = _uiState.value.issueId
        if (minutes <= 0) return
        viewModelScope.launch {
            repository.createWorklog(issueId, minutes, spentOn, note).fold(
                onSuccess = {
                    postMessage("Logged ${minutes}m")
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err -> postMessage(err.message ?: "Could not log time.") }
            )
        }
    }

    fun deleteWorklog(worklogId: Int) {
        val issueId = _uiState.value.issueId
        viewModelScope.launch {
            repository.deleteWorklog(worklogId).fold(
                onSuccess = {
                    postMessage("Worklog deleted")
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err -> postMessage(err.message ?: "Could not delete worklog.") }
            )
        }
    }

    // --- Attachments ---

    fun uploadAttachment(uri: Uri) {
        val issueId = _uiState.value.issueId
        if (issueId <= 0) return
        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true) }
            repository.uploadAttachmentFromUri(issueId, uri).fold(
                onSuccess = {
                    _uiState.update { it.copy(isUploading = false) }
                    postMessage("Uploaded ${it.filename}")
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isUploading = false) }
                    postMessage(err.message ?: "Upload failed.")
                }
            )
        }
    }

    fun deleteAttachment(publicId: String) {
        val issueId = _uiState.value.issueId
        viewModelScope.launch {
            repository.deleteAttachment(publicId).fold(
                onSuccess = {
                    postMessage("Attachment removed")
                    loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                },
                onFailure = { err -> postMessage(err.message ?: "Could not remove attachment.") }
            )
        }
    }

    fun resolveAttachmentUrl(relativeOrAbsolute: String?): String? {
        return repository.clientManager.resolveAttachmentUrl(relativeOrAbsolute)
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun stopSync() {
        issueSyncJob?.cancel()
    }

    private fun postMessage(msg: String) {
        _uiState.update { it.copy(message = msg) }
    }

    private fun startIssueSync(issueId: Int) {
        issueSyncJob?.cancel()
        issueSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(6_000L)
                val scope = "issue:$issueId"
                repository.getSyncRevs(listOf(scope)).onSuccess { revs ->
                    val rev = revs[scope]
                    if (rev != null) {
                        if (lastIssueRev != null && rev != lastIssueRev) {
                            loadIssue(issueId, _uiState.value.boardScaffold, silent = true)
                        }
                        lastIssueRev = rev
                    }
                }
            }
        }
    }

    override fun onCleared() {
        issueSyncJob?.cancel()
        super.onCleared()
    }
}
