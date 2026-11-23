package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ViewKanban
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CrossProjectIssueHitDto
import com.example.data.model.CurrentUserDto
import com.example.data.model.NotificationDto
import com.example.data.model.ProjectListItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.IdentifierChip
import com.example.ui.components.PriorityBadge
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.formatShortDate
import com.example.ui.components.priorityLabel
import com.example.ui.theme.Amber500
import com.example.ui.theme.Coral500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.viewmodel.InboxViewModel
import com.example.ui.viewmodel.MyIssuesScope
import com.example.ui.viewmodel.MyIssuesViewModel
import java.util.Calendar

private enum class HomeTab(val label: String) {
    OVERVIEW("Overview"),
    INBOX("Inbox"),
    TASKS("Tasks")
}

/**
 * Personal home: a greeting, a summary of what needs attention, recent inbox activity
 * and the user's tasks across every project they can reach.
 *
 * Overview is a curated digest; Inbox and Tasks are the full lists, reusing the existing
 * screens so filtering and deep-links behave identically to the dedicated tabs.
 */
@Composable
fun HomeScreen(
    user: CurrentUserDto,
    projects: List<ProjectListItem>,
    myIssuesViewModel: MyIssuesViewModel,
    inboxViewModel: InboxViewModel,
    unreadCount: Int,
    onOpenIssue: (issueId: Int, projectRef: String?) -> Unit,
    onOpenComment: (issueId: Int, projectRef: String?, commentId: Int) -> Unit,
    onShowMessage: (String) -> Unit,
    onRefresh: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenProjectBoard: (ProjectListItem) -> Unit,
    windowSize: com.example.ui.layout.AppWindowSize
) {
    var tab by rememberSaveable { mutableStateOf(HomeTab.OVERVIEW) }
    val issuesState by myIssuesViewModel.uiState.collectAsStateWithLifecycle()
    val inboxState by inboxViewModel.uiState.collectAsStateWithLifecycle()

    // The Overview reads counts from the other segments' view models, so prime them on
    // entry; otherwise the header and Tasks tab read zero until you open those tabs.
    LaunchedEffect(user.id) {
        inboxViewModel.loadNotifications(reset = true)
        onRefresh()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(
            userName = user.name.ifBlank { user.displayUsername ?: user.username ?: "there" },
            projectCount = projects.size,
            openCount = issuesState.issues.size,
            unreadCount = unreadCount,
            onSearch = onOpenSearch,
            onRefresh = onRefresh
        )

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            HomeTab.entries.forEachIndexed { index, entry ->
                SegmentedButton(
                    selected = tab == entry,
                    onClick = { tab = entry },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = HomeTab.entries.size),
                    icon = {
                        if (tab == entry) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    },
                    modifier = Modifier.testTag("home_tab_${entry.name.lowercase()}")
                ) {
                    Text(
                        when (entry) {
                            HomeTab.TASKS -> "Tasks (${issuesState.issues.size})"
                            HomeTab.INBOX -> if (unreadCount > 0) "Inbox ($unreadCount)" else entry.label
                            else -> entry.label
                        },
                        maxLines = 1
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (tab) {
                HomeTab.OVERVIEW -> HomeOverview(
                    issues = issuesState.issues,
                    isLoading = issuesState.isLoading,
                    notifications = inboxState.items,
                    projects = projects,
                    unreadCount = unreadCount,
                    onOpenIssue = onOpenIssue,
                    onOpenProjectBoard = onOpenProjectBoard,
                    onShowAllInbox = { tab = HomeTab.INBOX },
                    onShowAllTasks = { tab = HomeTab.TASKS },
                    onOpenComment = onOpenComment
                )

                HomeTab.INBOX -> InboxScreen(
                    viewModel = inboxViewModel,
                    onUnreadChanged = {},
                    onOpenIssue = onOpenIssue,
                    onOpenComment = onOpenComment,
                    onShowMessage = onShowMessage,
                    showHeader = false
                )

                HomeTab.TASKS -> MyIssuesScreen(
                    currentUserId = user.id,
                    viewModel = myIssuesViewModel,
                    onOpenIssue = onOpenIssue,
                    showHeader = false
                )
            }
        }
    }
}

@Composable
private fun HomeHeader(
    userName: String,
    projectCount: Int,
    openCount: Int,
    unreadCount: Int,
    onSearch: () -> Unit,
    onRefresh: () -> Unit
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UserAvatar(name = userName, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${greeting()}, $userName",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append("$projectCount ")
                        append(if (projectCount == 1) "project" else "projects")
                        append(" · $openCount open")
                        if (unreadCount > 0) append(" · $unreadCount unread")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onSearch, modifier = Modifier.testTag("home_search")) {
                Icon(Icons.Rounded.Search, contentDescription = "Search all projects")
            }
            IconButton(onClick = onRefresh, modifier = Modifier.testTag("home_refresh")) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
            }
        }
    }
}

/** Time-of-day greeting, so the header reads like a person wrote it. */
private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}

@Composable
private fun HomeOverview(
    issues: List<CrossProjectIssueHitDto>,
    isLoading: Boolean,
    notifications: List<NotificationDto>,
    projects: List<ProjectListItem>,
    unreadCount: Int,
    onOpenIssue: (Int, String?) -> Unit,
    onOpenProjectBoard: (ProjectListItem) -> Unit,
    onShowAllInbox: () -> Unit,
    onShowAllTasks: () -> Unit,
    onOpenComment: (Int, String?, Int) -> Unit
) {
    if (isLoading && issues.isEmpty() && notifications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val recent = notifications.take(2)
    val urgent = issues.filter {
        it.effectivePriority == "urgent" || it.effectivePriority == "high"
    }.take(2)
    val rest = issues.take(4)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (recent.isNotEmpty()) {
            item(key = "inbox_header") {
                SectionHeader(
                    title = "Recent activity",
                    trailingLabel = if (unreadCount > 0) "View all $unreadCount" else "View all",
                    onTrailingClick = onShowAllInbox
                )
            }
            items(recent, key = { "n_${it.id}" }) { notification ->
                NotificationRow(
                    notification = notification,
                    onClick = {
                        val issueId = notification.issueId
                        if (issueId != null) {
                            val commentId = notification.issueCommentId
                            if (commentId != null) {
                                onOpenComment(issueId, notification.projectKey.ifBlank { null }, commentId)
                            } else {
                                onOpenIssue(issueId, notification.projectKey.ifBlank { null })
                            }
                        }
                    }
                )
            }
        }

        if (urgent.isNotEmpty()) {
            item(key = "urgent_header") {
                SectionHeader(
                    title = "Needs attention",
                    trailingLabel = "All tasks",
                    onTrailingClick = onShowAllTasks
                )
            }
            items(urgent, key = { "u_${it.id}" }) { issue ->
                TaskRow(issue = issue, onOpen = { onOpenIssue(issue.id, issue.projectRef.ifBlank { null }) })
            }
        }

        if (rest.isNotEmpty()) {
            item(key = "tasks_header") {
                SectionHeader(
                    title = "Your tasks",
                    trailingLabel = "All tasks",
                    onTrailingClick = onShowAllTasks
                )
            }
            items(rest, key = { "t_${it.id}" }) { issue ->
                TaskRow(issue = issue, onOpen = { onOpenIssue(issue.id, issue.projectRef.ifBlank { null }) })
            }
        }

        if (projects.isNotEmpty()) {
            item(key = "projects_header") {
                SectionHeader(
                    title = "Jump to a project",
                    trailingLabel = null,
                    onTrailingClick = {}
                )
            }
            item(key = "projects_row") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(projects, key = { it.id }) { project ->
                        ProjectTile(project = project, onClick = { onOpenProjectBoard(project) })
                    }
                }
            }
        }

        if (recent.isEmpty() && rest.isEmpty() && projects.isEmpty()) {
            item(key = "empty") {
                EmptyStateView(
                    icon = Icons.Rounded.NotificationsNone,
                    title = "Nothing needs you right now",
                    subtitle = "Issues assigned to you and notifications from your projects will appear here."
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailingLabel: String?,
    onTrailingClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.weight(1f)
        )
        if (trailingLabel != null) {
            TextButton(onClick = onTrailingClick) {
                Text(trailingLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: NotificationDto, onClick: () -> Unit) {
    val unread = !notification.isRead
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (unread) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UserAvatar(name = notification.actorName ?: "User", size = 22.dp)
                notification.issueIdentifier?.let { IdentifierChip(it) }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatRelativeTime(notification.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = notification.issueTitle.orEmpty().ifBlank { notification.documentTitle.orEmpty() },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = notification.summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TaskRow(issue: CrossProjectIssueHitDto, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_task_${issue.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IdentifierChip(issue.identifier)
                if (issue.effectivePriority != "none") {
                    PriorityBadge(issue.effectivePriority)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatRelativeTime(issue.updatedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = issue.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = issue.projectName.ifBlank { issue.projectKey },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (!issue.dueDate.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Due",
                            tint = if (isOverdue(issue.dueDate)) Coral500 else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formatShortDate(issue.dueDate),
                            style = MonoCodeStyle,
                            color = if (isOverdue(issue.dueDate)) Coral500 else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun isOverdue(iso: String?): Boolean {
    val date = iso?.take(10) ?: return false
    val today = com.example.ui.components.todayIsoDate()
    return date < today
}

@Composable
private fun ProjectTile(project: ProjectListItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.width(168.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = project.key,
                        style = MonoCodeStyle.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ViewKanban,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = project.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (project.teamName.isNotBlank()) {
                Text(
                    text = project.teamName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
