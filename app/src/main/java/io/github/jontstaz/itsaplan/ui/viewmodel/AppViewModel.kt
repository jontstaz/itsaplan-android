package io.github.jontstaz.itsaplan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jontstaz.itsaplan.data.local.StoredConnection
import io.github.jontstaz.itsaplan.data.model.BoardIssueDto
import io.github.jontstaz.itsaplan.data.model.CurrentUserDto
import io.github.jontstaz.itsaplan.data.model.CrossProjectIssueHitDto
import io.github.jontstaz.itsaplan.data.model.CycleDto
import io.github.jontstaz.itsaplan.data.model.IssueResponseDto
import io.github.jontstaz.itsaplan.data.model.ProjectBoardResponse
import io.github.jontstaz.itsaplan.data.model.ProjectListItem
import io.github.jontstaz.itsaplan.data.model.TeamDto
import io.github.jontstaz.itsaplan.data.remote.ApiException
import io.github.jontstaz.itsaplan.data.repository.ItsAPlanRepository
import io.github.jontstaz.itsaplan.data.repository.ServerProbeResult
import io.github.jontstaz.itsaplan.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Checking : AuthState
    data object Unauthenticated : AuthState
    data class Authenticated(val user: CurrentUserDto) : AuthState
}

enum class BoardViewMode {
    KANBAN,
    LIST,
    /** One line per issue under collapsible status headers. */
    COMPACT;

    /** Board → List → Compact → Board, or List ↔ Compact in a narrow pane. */
    fun next(kanbanAvailable: Boolean = true): BoardViewMode = when (this) {
        KANBAN -> LIST
        LIST -> COMPACT
        COMPACT -> if (kanbanAvailable) KANBAN else LIST
    }
}

data class BoardFilters(
    val searchQuery: String = "",
    val priority: String? = null,
    val assigneeUserId: String? = null,
    val labelId: Int? = null,
    val cycleId: Int? = null,
    val showArchived: Boolean = false
) {
    val activeCount: Int
        get() = listOfNotNull(
            priority,
            assigneeUserId,
            labelId,
            cycleId,
            if (showArchived) true else null
        ).size
}

data class AppUiState(
    val authState: AuthState = AuthState.Checking,
    val connection: StoredConnection = StoredConnection(),
    val probeResult: ServerProbeResult? = null,
    val isProbing: Boolean = false,
    val isAuthenticating: Boolean = false,
    val authError: String? = null,
    val teams: List<TeamDto> = emptyList(),
    val projects: List<ProjectListItem> = emptyList(),
    val selectedProject: ProjectListItem? = null,
    val projectBoard: ProjectBoardResponse? = null,
    val boardIssues: List<BoardIssueDto> = emptyList(),
    val archivedIssues: List<IssueResponseDto> = emptyList(),
    val projectCycles: List<CycleDto> = emptyList(),
    val boardViewMode: BoardViewMode = BoardViewMode.KANBAN,
    val boardFilters: BoardFilters = BoardFilters(),
    val isLoadingProjects: Boolean = false,
    val isLoadingBoard: Boolean = false,
    val boardError: String? = null,
    val unreadNotifications: Int = 0,
    val snackbarMessage: String? = null,
    val lastSyncedAtMs: Long = 0L,
    val globalSearchResults: List<CrossProjectIssueHitDto> = emptyList(),
    val isGlobalSearching: Boolean = false
)

class AppViewModel(
    val repository: ItsAPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    private var syncJob: Job? = null
    /** One in-flight create at a time: a double tap must not post the same issue twice. */
    private var createIssueJob: Job? = null
    private var globalSearchJob: Job? = null
    private var lastRevs: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch {
            repository.connectionFlow.collect { conn ->
                _uiState.update { it.copy(connection = conn) }
            }
        }
        restoreSession()
    }

    fun restoreSession() {
        viewModelScope.launch {
            _uiState.update { it.copy(authState = AuthState.Checking, authError = null) }
            val result = repository.restoreSavedSession()
            result.fold(
                onSuccess = { user ->
                    if (user != null) {
                        onAuthenticated(user)
                    } else {
                        _uiState.update { it.copy(authState = AuthState.Unauthenticated) }
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            authState = AuthState.Unauthenticated,
                            authError = err.message
                        )
                    }
                }
            )
        }
    }

    fun probeServer(url: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProbing = true, authError = null, probeResult = null) }
            repository.probeServer(url).fold(
                onSuccess = { probe ->
                    _uiState.update {
                        it.copy(
                            isProbing = false,
                            probeResult = probe,
                            authError = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isProbing = false,
                            probeResult = null,
                            authError = err.message ?: "Could not reach server."
                        )
                    }
                }
            )
        }
    }

    fun signInWithApiKey(serverUrl: String, apiKey: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, authError = null) }
            repository.connectWithApiKey(serverUrl, apiKey).fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isAuthenticating = false) }
                    onAuthenticated(user)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            authError = err.message ?: "Authentication failed."
                        )
                    }
                }
            )
        }
    }

    fun signInWithPassword(serverUrl: String, identifier: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isAuthenticating = true, authError = null) }
            repository.connectWithPassword(serverUrl, identifier, password).fold(
                onSuccess = { user ->
                    _uiState.update { it.copy(isAuthenticating = false) }
                    onAuthenticated(user)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAuthenticating = false,
                            authError = err.message ?: "Sign-in failed."
                        )
                    }
                }
            )
        }
    }

    private fun onAuthenticated(user: CurrentUserDto) {
        _uiState.update {
            it.copy(
                authState = AuthState.Authenticated(user),
                authError = null
            )
        }
        loadWorkspaceData()
        startSyncLoop()
    }

    fun signOut() {
        syncJob?.cancel()
        lastRevs = emptyMap()
        viewModelScope.launch {
            repository.signOut()
            _uiState.update {
                it.copy(
                    authState = AuthState.Unauthenticated,
                    teams = emptyList(),
                    projects = emptyList(),
                    selectedProject = null,
                    projectBoard = null,
                    boardIssues = emptyList(),
                    archivedIssues = emptyList(),
                    projectCycles = emptyList(),
                    unreadNotifications = 0,
                    probeResult = null,
                    authError = null
                )
            }
        }
    }

    fun loadWorkspaceData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProjects = true, boardError = null) }
            val teamsDeferred = async { repository.listTeams() }
            val projectsDeferred = async { repository.listProjects() }
            val unreadDeferred = async { repository.getUnreadCount() }

            val teamsRes = teamsDeferred.await()
            val projectsRes = projectsDeferred.await()
            val unreadRes = unreadDeferred.await()

            val teams = teamsRes.getOrElse { emptyList() }
            val projects = projectsRes.getOrElse { err ->
                handlePossibleAuthExpiry(err)
                _uiState.update {
                    it.copy(
                        isLoadingProjects = false,
                        boardError = err.message ?: "Failed to load projects."
                    )
                }
                return@launch
            }
            val unread = unreadRes.getOrElse { _uiState.value.unreadNotifications }

            val savedRef = _uiState.value.connection.lastProjectRef
            val currentSelected = _uiState.value.selectedProject
            val chosen = projects.firstOrNull { it.routeKey == currentSelected?.routeKey }
                ?: projects.firstOrNull { it.routeKey == savedRef }
                ?: projects.firstOrNull { it.isFavorite && !it.isHidden }
                ?: projects.firstOrNull { !it.isHidden }
                ?: projects.firstOrNull()

            _uiState.update {
                it.copy(
                    teams = teams,
                    projects = projects,
                    selectedProject = chosen,
                    unreadNotifications = unread,
                    isLoadingProjects = false
                )
            }

            if (chosen != null) {
                loadProjectBoard(chosen, silent = false)
            }
        }
    }

    fun selectProject(project: ProjectListItem) {
        _uiState.update {
            it.copy(
                selectedProject = project,
                boardFilters = BoardFilters(),
                archivedIssues = emptyList()
            )
        }
        viewModelScope.launch {
            repository.saveLastProjectRef(project.routeKey)
        }
        viewModelScope.launch { loadProjectBoard(project, silent = false) }
    }

    fun refreshBoard(silent: Boolean = false) {
        val project = _uiState.value.selectedProject ?: return
        viewModelScope.launch { loadProjectBoard(project, silent = silent) }
    }

    /**
     * @return true when the board scaffold was refreshed, so the sync loop knows whether
     * it may advance the revision marker for this project.
     */
    private suspend fun loadProjectBoard(project: ProjectListItem, silent: Boolean): Boolean = coroutineScope {
            if (!silent) {
                _uiState.update { it.copy(isLoadingBoard = true, boardError = null) }
            }
            val boardDeferred = async { repository.getProjectBoard(project.routeKey) }
            val issuesDeferred = async { repository.getBoardIssues(project.routeKey) }
            val cyclesDeferred = async {
                if (project.cyclesEnabled) repository.listPlannedCycles(project.routeKey)
                else Result.success(emptyList())
            }

            val expectedRef = project.routeKey
            val boardRes = boardDeferred.await()
            val issuesRes = issuesDeferred.await()
            val cyclesRes = cyclesDeferred.await()

            boardRes.fold(
                onSuccess = { scaffold ->
                    // Keep whatever we already have for the parts that failed: a partial
                    // refresh must not look like an empty board.
                    // Ignore a load that landed after the user switched projects.
                    if (_uiState.value.selectedProject?.routeKey != expectedRef) return@coroutineScope false
                    _uiState.update { current ->
                        current.copy(
                            projectBoard = scaffold,
                            boardIssues = issuesRes.getOrNull() ?: current.boardIssues,
                            projectCycles = cyclesRes.getOrNull() ?: current.projectCycles,
                            isLoadingBoard = false,
                            boardError = null,
                            lastSyncedAtMs = System.currentTimeMillis()
                        )
                    }
                    if (_uiState.value.boardFilters.showArchived) {
                        loadArchivedIssues(project.routeKey)
                    }
                    true
                },
                onFailure = { err ->
                    handlePossibleAuthExpiry(err)
                    if (!silent) {
                        _uiState.update {
                            it.copy(
                                isLoadingBoard = false,
                                boardError = err.message ?: "Could not load project board."
                            )
                        }
                    }
                    false
                }
            )
    }

    private fun loadArchivedIssues(projectRef: String) {
        viewModelScope.launch {
            repository.listArchivedIssues(projectRef).onSuccess { archived ->
                _uiState.update { it.copy(archivedIssues = archived) }
            }
        }
    }

    fun setBoardViewMode(mode: BoardViewMode) {
        _uiState.update { it.copy(boardViewMode = mode) }
    }

    fun updateBoardFilters(transform: (BoardFilters) -> BoardFilters) {
        val newFilters = transform(_uiState.value.boardFilters)
        val wasArchived = _uiState.value.boardFilters.showArchived
        _uiState.update { it.copy(boardFilters = newFilters) }
        if (newFilters.showArchived && !wasArchived) {
            _uiState.value.selectedProject?.let { loadArchivedIssues(it.routeKey) }
        }
    }

    fun toggleProjectFavorite(project: ProjectListItem) {
        val target = !project.isFavorite
        viewModelScope.launch {
            repository.updateProjectPreferences(project.routeKey, isFavorite = target).fold(
                onSuccess = {
                    _uiState.update { state ->
                        val updated = state.projects.map {
                            if (it.id == project.id) it.copy(isFavorite = target) else it
                        }
                        state.copy(
                            projects = updated,
                            selectedProject = state.selectedProject?.let { sel ->
                                if (sel.id == project.id) sel.copy(isFavorite = target) else sel
                            }
                        )
                    }
                },
                onFailure = { err -> showSnackbar(err.message ?: "Could not update favorite.") }
            )
        }
    }

    fun createProject(
        teamId: Int,
        key: String,
        name: String,
        description: String?,
        preset: String,
        onCreated: () -> Unit
    ) {
        viewModelScope.launch {
            repository.createProject(teamId, key, name, description, preset).fold(
                onSuccess = { created ->
                    showSnackbar("Project ${created.key} created")
                    onCreated()
                    loadWorkspaceData()
                },
                onFailure = { err ->
                    showSnackbar(err.message ?: "Failed to create project.")
                }
            )
        }
    }

    fun createIssue(
        columnId: Int,
        title: String,
        description: String?,
        typeId: Int?,
        cycleId: Int?,
        assigneeUserId: String?,
        delegateUserId: String?,
        priority: String,
        estimatePoints: Double?,
        estimateMinutes: Int?,
        dueDate: String?,
        labelIds: List<Int>,
        onSuccess: (Int) -> Unit
    ) {
        val project = _uiState.value.selectedProject ?: return
        if (createIssueJob?.isActive == true) return
        createIssueJob = viewModelScope.launch {
            repository.createIssue(
                projectRef = project.routeKey,
                columnId = columnId,
                title = title,
                description = description,
                typeId = typeId,
                cycleId = cycleId,
                assigneeUserId = assigneeUserId,
                delegateUserId = delegateUserId,
                priority = priority,
                estimatePoints = estimatePoints,
                estimateMinutes = estimateMinutes,
                dueDate = dueDate,
                labelIds = labelIds
            ).fold(
                onSuccess = { created ->
                    showSnackbar("Created ${created.identifier}")
                    refreshBoard(silent = true)
                    onSuccess(created.id)
                },
                onFailure = { err ->
                    showSnackbar(err.message ?: "Failed to create issue.")
                }
            )
        }
    }

    fun moveIssueToColumn(issue: BoardIssueDto, targetColumnId: Int) {
        if (issue.columnId == targetColumnId) return
        val previous = _uiState.value.boardIssues
        // Optimistic update
        _uiState.update { state ->
            state.copy(
                boardIssues = state.boardIssues.map {
                    if (it.id == issue.id) it.copy(columnId = targetColumnId) else it
                }
            )
        }
        viewModelScope.launch {
            repository.updateIssueFields(issue.id, mapOf("columnId" to targetColumnId)).fold(
                onSuccess = {
                    val colName = _uiState.value.projectBoard?.columns
                        ?.firstOrNull { it.id == targetColumnId }?.name ?: "column"
                    showSnackbar("Moved ${issue.identifier} to $colName")
                    refreshBoard(silent = true)
                },
                onFailure = { err ->
                    // Revert optimistic move on failure (e.g. 409 wip_limit_exceeded)
                    _uiState.update { it.copy(boardIssues = previous) }
                    showSnackbar(err.message ?: "Could not move issue.")
                }
            )
        }
    }

    fun refreshUnreadCount() {
        viewModelScope.launch {
            repository.getUnreadCount().onSuccess { count ->
                _uiState.update { it.copy(unreadNotifications = count) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.saveThemeMode(mode) }
    }

    fun setLiveSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.saveLiveSyncEnabled(enabled)
            if (enabled) startSyncLoop() else syncJob?.cancel()
        }
    }

    fun searchGlobally(query: String) {
        globalSearchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(globalSearchResults = emptyList(), isGlobalSearching = false) }
            return
        }
        globalSearchJob = viewModelScope.launch {
            _uiState.update { it.copy(isGlobalSearching = true) }
            delay(300)
            repository.searchCrossProjectIssues(query.trim()).fold(
                onSuccess = { results ->
                    _uiState.update { it.copy(globalSearchResults = results, isGlobalSearching = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isGlobalSearching = false, snackbarMessage = error.message ?: "Search failed") }
                }
            )
        }
    }

    fun clearGlobalSearch() {
        globalSearchJob?.cancel()
        _uiState.update { it.copy(globalSearchResults = emptyList(), isGlobalSearching = false) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun startSyncLoop() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            while (isActive) {
                delay(8_000L)
                val state = _uiState.value
                if (state.authState !is AuthState.Authenticated || !state.connection.liveSyncEnabled) {
                    continue
                }
                val scopes = mutableListOf("inbox:all")
                val projectId = state.selectedProject?.id
                if (projectId != null) {
                    scopes.add("board:$projectId")
                }
                repository.getSyncRevs(scopes).onSuccess { newRevs ->
                    if (lastRevs.isEmpty()) {
                        // First pass only establishes the baseline.
                        lastRevs = newRevs
                        return@onSuccess
                    }

                    val boardScope = projectId?.let { "board:$it" }
                    val next = lastRevs.toMutableMap()

                    if (boardScope != null &&
                        newRevs[boardScope] != null &&
                        newRevs[boardScope] != lastRevs[boardScope]
                    ) {
                        val project = _uiState.value.selectedProject
                        val refreshed = project != null && loadProjectBoard(project, silent = true)
                        // Advance only on success, so a transient failure is retried on
                        // the next tick instead of being skipped for good.
                        if (refreshed) next[boardScope] = newRevs.getValue(boardScope)
                    }

                    val inboxRev = newRevs["inbox:all"]
                    if (inboxRev != null && inboxRev != lastRevs["inbox:all"]) {
                        val counted = repository.getUnreadCount().fold(
                            onSuccess = { count ->
                                _uiState.update { it.copy(unreadNotifications = count) }
                                true
                            },
                            onFailure = { false }
                        )
                        if (counted) next["inbox:all"] = inboxRev
                    }

                    // Scopes we did not need to refresh are adopted as-is.
                    newRevs.forEach { (scope, rev) ->
                        if (!next.containsKey(scope)) next[scope] = rev
                    }
                    lastRevs = next
                }
            }
        }
    }

    private fun handlePossibleAuthExpiry(err: Throwable) {
        if (err is ApiException && err.statusCode == 401) {
            showSnackbar("Session expired. Please sign in again.")
            signOut()
        }
    }

    override fun onCleared() {
        syncJob?.cancel()
        super.onCleared()
    }
}
