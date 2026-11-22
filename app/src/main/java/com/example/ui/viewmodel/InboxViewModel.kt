package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CursorDto
import com.example.data.model.NotificationDto
import com.example.data.repository.ItsAPlanRepository
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

    fun loadNotifications(reset: Boolean = true) {
        val state = _uiState.value
        val cursor = if (reset) null else state.nextCursor
        if (!reset && cursor == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            repository.listNotifications(
                includeRead = !state.unreadOnly,
                includeSnoozed = state.includeSnoozed,
                types = state.selectedType,
                cursor = cursor?.toQueryParam()
            ).fold(
                onSuccess = { page ->
                    _uiState.update {
                        it.copy(
                            items = if (reset) page.items else it.items + page.items,
                            nextCursor = page.nextCursor,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { err ->
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
