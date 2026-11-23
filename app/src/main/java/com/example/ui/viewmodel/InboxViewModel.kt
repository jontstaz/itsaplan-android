package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CursorDto
import com.example.data.model.NotificationDto
import com.example.data.repository.ItsAPlanRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

data class InboxUiState(
    val items: List<NotificationDto> = emptyList(),
    val nextCursor: CursorDto? = null,
    val unreadOnly: Boolean = false,
    val includeSnoozed: Boolean = false,
    val selectedType: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

class InboxViewModel(
    private val repository: ItsAPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InboxUiState())
    val uiState: StateFlow<InboxUiState> = _uiState.asStateFlow()

    /** In-flight load, so a refresh or filter change cancels the one it supersedes. */
    private var loadJob: Job? = null

    fun loadNotifications(reset: Boolean = true) {
        val state = _uiState.value
        // The cursor belongs to the query that produced it: a different filter must start
        // from the first page rather than resume the previous query's position.
        val query = QueryIdentity(
            includeRead = !state.unreadOnly,
            includeSnoozed = state.includeSnoozed,
            types = state.selectedType
        )
        if (query != activeQuery) {
            activeQuery = query
            loadJob?.cancel()
            _uiState.update { it.copy(items = emptyList(), nextCursor = null, error = null) }
            return loadNotifications(reset = true)
        }

        val cursor = if (reset) null else state.nextCursor
        if (!reset && cursor == null) return

        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listNotifications(
                includeRead = query.includeRead,
                includeSnoozed = query.includeSnoozed,
                types = query.types,
                cursor = cursor?.takeIf { it.isValid }?.toQueryParam()
            ).fold(
                onSuccess = { page ->
                    // Ignore a page whose query has since been replaced.
                    if (query != activeQuery) return@launch
                    _uiState.update {
                        // Guard against a page being appended twice (double tap on Load More).
                        val merged = if (reset) page.items
                        else (it.items + page.items).distinctBy { item -> item.id }
                        it.copy(
                            items = merged,
                            nextCursor = page.nextCursor?.takeIf { c -> c.isValid },
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { err ->
                    if (query != activeQuery) return@launch
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = err.message ?: "Failed to load notifications."
                        )
                    }
                }
            )
        }
    }

    private data class QueryIdentity(
        val includeRead: Boolean,
        val includeSnoozed: Boolean,
        val types: String?
    )

    private var activeQuery = QueryIdentity(includeRead = true, includeSnoozed = false, types = null)

    fun setUnreadOnly(unreadOnly: Boolean) {
        _uiState.update { it.copy(unreadOnly = unreadOnly) }
        loadNotifications(reset = true)
    }

    fun setIncludeSnoozed(includeSnoozed: Boolean) {
        _uiState.update { it.copy(includeSnoozed = includeSnoozed) }
        loadNotifications(reset = true)
    }

    fun setSelectedType(type: String?) {
        _uiState.update { it.copy(selectedType = type) }
        loadNotifications(reset = true)
    }

    fun toggleRead(item: NotificationDto, onUnreadChanged: () -> Unit) {
        val targetRead = !item.isRead
        viewModelScope.launch {
            repository.markNotificationRead(item.id, targetRead).fold(
                onSuccess = {
                    loadNotifications(reset = true)
                    onUnreadChanged()
                },
                onFailure = { err -> _uiState.update { it.copy(message = err.message) } }
            )
        }
    }

    fun markAllRead(onUnreadChanged: () -> Unit) {
        viewModelScope.launch {
            repository.markAllNotificationsRead().fold(
                onSuccess = {
                    _uiState.update { it.copy(message = "All notifications marked as read") }
                    loadNotifications(reset = true)
                    onUnreadChanged()
                },
                onFailure = { err -> _uiState.update { it.copy(message = err.message) } }
            )
        }
    }

    fun snooze(itemId: Int, hoursFromNow: Int?, onUnreadChanged: () -> Unit) {
        val iso = hoursFromNow?.let { hours ->
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            cal.add(Calendar.HOUR_OF_DAY, hours)
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.format(cal.time)
        }
        viewModelScope.launch {
            repository.snoozeNotification(itemId, iso).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(message = if (iso == null) "Unsnoozed" else "Notification snoozed")
                    }
                    loadNotifications(reset = true)
                    onUnreadChanged()
                },
                onFailure = { err -> _uiState.update { it.copy(message = err.message) } }
            )
        }
    }

    fun delete(itemId: Int, onUnreadChanged: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNotification(itemId).fold(
                onSuccess = {
                    _uiState.update { state ->
                        state.copy(items = state.items.filterNot { it.id == itemId })
                    }
                    onUnreadChanged()
                },
                onFailure = { err -> _uiState.update { it.copy(message = err.message) } }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
}
