package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NotificationDto
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorBanner
import com.example.ui.components.IdentifierChip
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.viewmodel.InboxViewModel

private val NOTIFICATION_TYPES = listOf(
    null to "All Types",
    "assigned" to "Assigned",
    "mentioned" to "Mentions",
    "commented" to "Comments",
    "state_changed" to "Status",
    "reacted" to "Reactions"
)

@Composable
fun InboxScreen(
    viewModel: InboxViewModel,
    onUnreadChanged: () -> Unit,
    onOpenIssue: (issueId: Int, projectRef: String?) -> Unit,
    onOpenComment: (issueId: Int, projectRef: String?, commentId: Int) -> Unit,
    onShowMessage: (String) -> Unit,
    showHeader: Boolean = true
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var snoozeTarget by remember { mutableStateOf<NotificationDto?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadNotifications(reset = true)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            onShowMessage(it)
            viewModel.clearMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showHeader) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Inbox", style = MaterialTheme.typography.titleLarge)
                        Row {
                            IconButton(
                                onClick = { viewModel.markAllRead(onUnreadChanged) },
                                modifier = Modifier.testTag("inbox_mark_all_read")
                            ) {
                                Icon(Icons.Rounded.DoneAll, contentDescription = "Mark all notifications read")
                            }
                            IconButton(onClick = { viewModel.loadNotifications(reset = true) }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh inbox")
                            }
                        }
                    }
                } else {
                    // Still expose the inbox actions without the duplicate title.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.markAllRead(onUnreadChanged) },
                            modifier = Modifier.testTag("inbox_mark_all_read")
                        ) {
                            Icon(Icons.Rounded.DoneAll, contentDescription = "Mark all notifications read")
                        }
                        IconButton(onClick = { viewModel.loadNotifications(reset = true) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh inbox")
                        }
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = state.unreadOnly,
                            onClick = { viewModel.setUnreadOnly(!state.unreadOnly) },
                            label = { Text("Unread Only") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = state.includeSnoozed,
                            onClick = { viewModel.setIncludeSnoozed(!state.includeSnoozed) },
                            label = { Text("Snoozed") }
                        )
                    }
                    items(NOTIFICATION_TYPES) { (typeKey, label) ->
                        FilterChip(
                            selected = state.selectedType == typeKey,
                            onClick = { viewModel.setSelectedType(typeKey) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when {
            state.isLoading && state.items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            !state.error.isNullOrBlank() -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    ErrorBanner(message = state.error!!, onRetry = { viewModel.loadNotifications(reset = true) })
                }
            }

            state.items.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Rounded.NotificationsOff,
                    title = "Inbox zero",
                    subtitle = "Mentions, assignments, comments, and status updates will show up here.",
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.items, key = { it.id }) { item ->
                        NotificationCard(
                            item = item,
                            onClick = {
                                if (!item.isRead) viewModel.toggleRead(item, onUnreadChanged)
                                item.issueId?.let { id ->
                                    val commentId = item.issueCommentId
                                    if (commentId != null) onOpenComment(id, item.projectKey.ifBlank { null }, commentId)
                                    else onOpenIssue(id, item.projectKey.ifBlank { null })
                                }
                            },
                            onToggleRead = { viewModel.toggleRead(item, onUnreadChanged) },
                            onSnooze = { snoozeTarget = item },
                            onDelete = { viewModel.delete(item.id, onUnreadChanged) }
                        )
                    }
                    if (state.nextCursor != null) {
                        item {
                            OutlinedButton(
                                onClick = { viewModel.loadNotifications(reset = false) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Load More") }
                        }
                    }
                }
            }
        }
    }

    snoozeTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { snoozeTarget = null },
            title = { Text("Snooze Notification") },
            text = {
                Column {
                    listOf(
                        1 to "In 1 hour",
                        4 to "In 4 hours",
                        24 to "Tomorrow",
                        168 to "Next week"
                    ).forEach { (hours, label) ->
                        TextButton(
                            onClick = {
                                viewModel.snooze(target.id, hours, onUnreadChanged)
                                snoozeTarget = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                    if (target.isSnoozed) {
                        TextButton(
                            onClick = {
                                viewModel.snooze(target.id, null, onUnreadChanged)
                                snoozeTarget = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear Snooze", modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { snoozeTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NotificationCard(
    item: NotificationDto,
    onClick: () -> Unit,
    onToggleRead: () -> Unit,
    onSnooze: () -> Unit,
    onDelete: () -> Unit
) {
    val unread = !item.isRead
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unread) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            width = if (unread) 1.25.dp else 1.dp,
            color = if (unread) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (unread) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
                UserAvatar(
                    name = item.actorName ?: "System",
                    size = 24.dp
                )
                if (!item.issueIdentifier.isNullOrBlank()) {
                    IdentifierChip(item.issueIdentifier!!)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatRelativeTime(item.createdAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = item.issueTitle?.ifBlank { null }
                    ?: item.documentTitle?.ifBlank { null }
                    ?: item.summaryText,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = item.summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.projectName.ifBlank { item.projectKey },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    IconButton(onClick = onToggleRead, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = if (unread) Icons.Rounded.MarkEmailRead else Icons.Rounded.MarkEmailUnread,
                            contentDescription = if (unread) "Mark read" else "Mark unread",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onSnooze, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Snooze, contentDescription = "Snooze", modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
