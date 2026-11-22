package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CrossProjectIssueHitDto
import com.example.data.repository.ItsAPlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MyIssuesScope(val label: String) {
    OPEN("Open"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    ALL("All")
}

data class MyIssuesUiState(
    val searchQuery: String = "",
    val scope: MyIssuesScope = MyIssuesScope.OPEN,
    val assignedToMeOnly: Boolean = true,
    val priorityFilter: String? = null,
    val issues: List<CrossProjectIssueHitDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class MyIssuesViewModel(
    private val repository: ItsAPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyIssuesUiState())
    val uiState: StateFlow<MyIssuesUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var searchJob: Job? = null

    fun load(userId: String?) {
        currentUserId = userId
        val state = _uiState.value
        if (state.searchQuery.trim().length >= 2) {
            runSearch(state.searchQuery.trim())
            return
        }

        val stateTypes = when (state.scope) {
            MyIssuesScope.OPEN -> "backlog,unstarted,started"
            MyIssuesScope.IN_PROGRESS -> "started"
            MyIssuesScope.COMPLETED -> "completed"
            MyIssuesScope.ALL -> null
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listCrossProjectIssues(
                assigneeUserId = if (state.assignedToMeOnly) userId else null,
                priority = state.priorityFilter,
                stateTypes = stateTypes,
                limit = 100
            ).fold(
                onSuccess = { list ->
                    _uiState.update { it.copy(issues = list, isLoading = false, error = null) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Could not load issues."
                        )
                    }
                }
            )
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            load(currentUserId)
            return
        }
        if (trimmed.length < 2) return
        searchJob = viewModelScope.launch {
            delay(320L)
            runSearch(trimmed)
        }
    }

    private fun runSearch(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.searchCrossProjectIssues(query).fold(
                onSuccess = { hits ->
                    _uiState.update { it.copy(issues = hits, isLoading = false, error = null) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isLoading = false, error = err.message ?: "Search failed.")
                    }
                }
            )
        }
    }

    fun setScope(scope: MyIssuesScope) {
        _uiState.update { it.copy(scope = scope) }
        load(currentUserId)
    }

    fun setAssignedToMeOnly(onlyMe: Boolean) {
        _uiState.update { it.copy(assignedToMeOnly = onlyMe) }
        load(currentUserId)
    }

    fun setPriorityFilter(priority: String?) {
        _uiState.update { it.copy(priorityFilter = priority) }
        load(currentUserId)
    }
}
