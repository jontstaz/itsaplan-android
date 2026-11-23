package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.InitiativeDto
import com.example.data.repository.ItsAPlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class InitiativeFilter(val label: String, val statuses: String?) {
    OPEN("Open", "proposed,planned,active"),
    ALL("All", null),
    COMPLETED("Completed", "completed"),
    CANCELED("Canceled", "canceled")
}

data class InitiativesUiState(
    val projectRef: String = "",
    val filter: InitiativeFilter = InitiativeFilter.OPEN,
    val initiatives: List<InitiativeDto> = emptyList(),
    val isLoading: Boolean = false,
    val page: Int = 1,
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class InitiativesViewModel(
    private val repository: ItsAPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InitiativesUiState())
    val uiState: StateFlow<InitiativesUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun load(projectRef: String, filter: InitiativeFilter = _uiState.value.filter) {
        if (projectRef.isBlank()) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(projectRef = projectRef, filter = filter, isLoading = true, error = null)
            }
            repository.listInitiatives(projectRef, status = filter.statuses, page = 1).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            initiatives = page.items,
                            page = 1,
                            hasMore = page.items.size < page.total,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isLoading = false, error = err.message ?: "Failed to load initiatives.")
                    }
                }
            )
        }
    }

    /** Appends the next page; ignored while another page is already loading. */
    fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.isLoadingMore || state.isLoading) return
        val nextPage = state.page + 1
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            repository.listInitiatives(
                projectRef = state.projectRef,
                status = state.filter.statuses,
                page = nextPage
            ).fold(
                onSuccess = { page ->
                    _uiState.update { current ->
                        // A page for a filter that is no longer selected is dropped.
                        if (current.filter != state.filter) current.copy(isLoadingMore = false)
                        else current.copy(
                            initiatives = (current.initiatives + page.items).distinctBy { it.id },
                            page = nextPage,
                            hasMore = (current.initiatives.size + page.items.size) < page.total,
                            isLoadingMore = false
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isLoadingMore = false, message = err.message ?: "Could not load more initiatives.")
                    }
                }
            )
        }
    }

    fun setFilter(filter: InitiativeFilter) {
        val projectRef = _uiState.value.projectRef
        _uiState.update { it.copy(filter = filter) }
        if (projectRef.isNotBlank()) load(projectRef, filter)
    }

    fun create(
        title: String,
        description: String?,
        targetDate: String?,
        onDone: () -> Unit
    ) {
        val projectRef = _uiState.value.projectRef
        if (projectRef.isBlank() || title.isBlank()) return
        // Ignore a second tap while the first create is in flight.
        if (_uiState.value.isSaving) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            // New initiatives start as planned: that is the state the roadmap uses for
            // work that is committed but not yet underway.
            repository.createInitiative(
                projectRef = projectRef,
                title = title,
                description = description,
                status = "planned",
                targetDate = targetDate
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, message = "Initiative created") }
                    onDone()
                    load(projectRef)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isSaving = false, message = err.message ?: "Could not create initiative.")
                    }
                }
            )
        }
    }

    fun updateStatus(initiative: InitiativeDto, status: String) {
        viewModelScope.launch {
            repository.updateInitiativeStatus(initiative.id, status).fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "Moved to $status") }
                    load(_uiState.value.projectRef)
                },
                onFailure = { err ->
                    _uiState.update { it.copy(message = err.message ?: "Could not update initiative.") }
                }
            )
        }
    }

    fun delete(initiative: InitiativeDto) {
        viewModelScope.launch {
            repository.deleteInitiative(initiative.id).fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "Initiative deleted") }
                    load(_uiState.value.projectRef)
                },
                onFailure = { err ->
                    _uiState.update { it.copy(message = err.message ?: "Could not delete initiative.") }
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}