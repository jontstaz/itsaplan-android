package io.github.jontstaz.itsaplan.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jontstaz.itsaplan.data.model.CycleDto
import io.github.jontstaz.itsaplan.data.repository.ItsAPlanRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CyclesUiState(
    val projectRef: String = "",
    val plannedCycles: List<CycleDto> = emptyList(),
    val completedCycles: List<CycleDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class CyclesViewModel(
    private val repository: ItsAPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CyclesUiState())
    val uiState: StateFlow<CyclesUiState> = _uiState.asStateFlow()

    fun loadCycles(projectRef: String) {
        if (projectRef.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(projectRef = projectRef, isLoading = true, error = null) }
            val plannedDef = async { repository.listPlannedCycles(projectRef) }
            val completedDef = async { repository.listCompletedCycles(projectRef, page = 1) }

            val plannedRes = plannedDef.await()
            val completedRes = completedDef.await()

            plannedRes.fold(
                onSuccess = { planned ->
                    val completed = completedRes.getOrNull()?.items ?: emptyList()
                    _uiState.update {
                        it.copy(
                            plannedCycles = planned,
                            completedCycles = completed,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to load cycles."
                        )
                    }
                }
            )
        }
    }

    fun createCycle(
        name: String,
        goal: String?,
        startDate: String,
        endDate: String,
        onDone: () -> Unit
    ) {
        val ref = _uiState.value.projectRef
        if (ref.isBlank()) return
        viewModelScope.launch {
            repository.createCycle(ref, name, goal, startDate, endDate).fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "Cycle created") }
                    onDone()
                    loadCycles(ref)
                },
                onFailure = { err -> _uiState.update { it.copy(message = err.message ?: "Could not create cycle.") } }
            )
        }
    }

    fun finishCycle(cycleId: Int, onBoardRefresh: () -> Unit) {
        val ref = _uiState.value.projectRef
        viewModelScope.launch {
            repository.finishCycle(cycleId).fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "Cycle finished") }
                    loadCycles(ref)
                    onBoardRefresh()
                },
                onFailure = { err -> _uiState.update { it.copy(message = err.message ?: "Could not finish cycle.") } }
            )
        }
    }

    fun startNextCycle(cycleId: Int, onBoardRefresh: () -> Unit) {
        val ref = _uiState.value.projectRef
        viewModelScope.launch {
            repository.startNextCycle(cycleId).fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "Started next cycle") }
                    loadCycles(ref)
                    onBoardRefresh()
                },
                onFailure = { err -> _uiState.update { it.copy(message = err.message ?: "Could not start cycle.") } }
            )
        }
    }

    fun transferIssues(cycleId: Int, targetCycleId: Int?, onBoardRefresh: () -> Unit) {
        val ref = _uiState.value.projectRef
        viewModelScope.launch {
            repository.transferCycleIssues(cycleId, targetCycleId).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(message = if (targetCycleId == null) "Moved unfinished issues to backlog" else "Transferred unfinished issues")
                    }
                    loadCycles(ref)
                    onBoardRefresh()
                },
                onFailure = { err -> _uiState.update { it.copy(message = err.message ?: "Transfer failed.") } }
            )
        }
    }

    fun deleteCycle(cycleId: Int, onBoardRefresh: () -> Unit) {
        val ref = _uiState.value.projectRef
        viewModelScope.launch {
            repository.deleteCycle(cycleId).fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "Cycle deleted") }
                    loadCycles(ref)
                    onBoardRefresh()
                },
                onFailure = { err -> _uiState.update { it.copy(message = err.message ?: "Delete failed.") } }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
