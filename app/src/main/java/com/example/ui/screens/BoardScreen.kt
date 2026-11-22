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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.ViewKanban
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AssigneeCandidateDto
import com.example.data.model.BoardIssueDto
import com.example.data.model.ColumnDto
import com.example.data.model.LabelDto
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorBanner
import com.example.ui.components.IdentifierChip
import com.example.ui.components.LabelPill
import com.example.ui.components.PriorityBadge
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatMinutes
import com.example.ui.components.formatShortDate
import com.example.ui.components.priorityLabel
import com.example.ui.components.stateTypeColor
import com.example.ui.components.stateTypeIcon
import com.example.ui.theme.Amber500
import com.example.ui.theme.Coral500
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.Violet500
import com.example.ui.viewmodel.AppUiState
import com.example.ui.viewmodel.BoardFilters
import com.example.ui.viewmodel.BoardViewMode

@Composable
fun BoardScreen(
    state: AppUiState,
    onOpenProjectSwitcher: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRefresh: () -> Unit,
    onSetViewMode: (BoardViewMode) -> Unit,
    onUpdateFilters: ((BoardFilters) -> BoardFilters) -> Unit,
    onOpenFilterSheet: () -> Unit,
    onOpenIssue: (Int) -> Unit,
    onQuickCreateInColumn: (Int) -> Unit,
    onMoveIssue: (BoardIssueDto, Int) -> Unit,
    forceListView: Boolean = false,
    recentIssues: List<com.example.data.local.RecentIssue> = emptyList(),
    onOpenGlobalSearch: () -> Unit = {}
) {
    var moveTargetIssue by remember { mutableStateOf<BoardIssueDto?>(null) }
    var showSearchBar by remember { mutableStateOf(false) }

    val scaffold = state.projectBoard
    val selectedProject = state.selectedProject
    val columns = remember(scaffold) { scaffold?.columns?.sortedBy { it.position } ?: emptyList() }
    val labelMap = remember(scaffold) { scaffold?.labels?.associateBy { it.id } ?: emptyMap() }
    val assigneeMap = remember(scaffold) { scaffold?.assignees?.associateBy { it.userId } ?: emptyMap() }

    val filteredIssues = remember(state.boardIssues, state.boardFilters) {
        val f = state.boardFilters
        val q = f.searchQuery.trim().lowercase()
        state.boardIssues.filter { issue ->
            val matchQuery = q.isEmpty() ||
                issue.title.lowercase().contains(q) ||
                issue.identifier.lowercase().contains(q)
            val matchPriority = f.priority == null || issue.effectivePriority.equals(f.priority, ignoreCase = true)
            val matchAssignee = f.assigneeUserId == null ||
                issue.assigneeUserId == f.assigneeUserId ||
                issue.delegateUserId == f.assigneeUserId
            val matchLabel = f.labelId == null || issue.labelIds.contains(f.labelId)
            val matchCycle = f.cycleId == null || issue.cycleId == f.cycleId
            matchQuery && matchPriority && matchAssignee && matchLabel && matchCycle
        }
    }

    val issuesByColumn = remember(filteredIssues) {
        filteredIssues.groupBy { it.columnId }.mapValues { (_, list) -> list.sortedBy { it.position } }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Project Header Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Project Switcher Pill
                    Surface(
                        onClick = onOpenProjectSwitcher,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("project_switcher_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (selectedProject != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = selectedProject.key,
                                        style = MonoCodeStyle.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = selectedProject.name,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (selectedProject.teamName.isNotBlank()) "Active · ${selectedProject.teamName}" else "Active project",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            } else {
                                Text(
                                    text = "Choose Active Project",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Switch project",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (selectedProject != null) {
                        IconButton(
                            onClick = onToggleFavorite,
                            modifier = Modifier.testTag("favorite_project_button")
                        ) {
                            Icon(
                                imageVector = if (selectedProject.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                                contentDescription = if (selectedProject.isFavorite) "Remove favorite" else "Favorite project",
                                tint = if (selectedProject.isFavorite) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenGlobalSearch,
                        modifier = Modifier.testTag("global_search_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.TravelExplore,
                            contentDescription = "Search all projects"
                        )
                    }

                    IconButton(
                        onClick = { showSearchBar = !showSearchBar },
                        modifier = Modifier.testTag("board_search_toggle")
                    ) {
                        Icon(
                            imageVector = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Search board"
                        )
                    }

                    IconButton(
                        onClick = {
                            onSetViewMode(
                                if (state.boardViewMode == BoardViewMode.KANBAN) BoardViewMode.LIST else BoardViewMode.KANBAN
                            )
                        },
                        modifier = Modifier.testTag("board_view_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (state.boardViewMode == BoardViewMode.KANBAN) {
                                Icons.AutoMirrored.Rounded.FormatListBulleted
                            } else {
                                Icons.Rounded.ViewKanban
                            },
                            contentDescription = "Toggle Kanban or List view"
                        )
                    }

                    IconButton(
                        onClick = onOpenFilterSheet,
                        modifier = Modifier.testTag("board_filter_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (state.boardFilters.activeCount > 0) {
                                    Badge { Text("${state.boardFilters.activeCount}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FilterList,
                                contentDescription = "Filter issues"
                            )
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("board_refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh board"
                        )
                    }
                }

                // Inline Search Bar
                if (showSearchBar) {
                    OutlinedTextField(
                        value = state.boardFilters.searchQuery,
                        onValueChange = { q -> onUpdateFilters { it.copy(searchQuery = q) } },
                        placeholder = { Text("Filter issues by title or ID…") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (state.boardFilters.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onUpdateFilters { it.copy(searchQuery = "") } }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("board_search_input")
                    )
                }

                // Recent Issues Strip
                if (recentIssues.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(recentIssues.take(5), key = { it.id }) { recent ->
                            Surface(
                                onClick = { onOpenIssue(recent.id) },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.testTag("recent_issue_${recent.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = recent.identifier,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    if (recent.title.isNotBlank()) {
                                        Text(
                                            text = recent.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 140.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Filter Chips
                if (state.boardFilters.activeCount > 0) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.boardFilters.priority?.let { p ->
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = { onUpdateFilters { it.copy(priority = null) } },
                                    label = { Text("Priority: ${priorityLabel(p)}") },
                                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                )
                            }
                        }
                        state.boardFilters.assigneeUserId?.let { uid ->
                            val name = assigneeMap[uid]?.name ?: "Assignee"
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = { onUpdateFilters { it.copy(assigneeUserId = null) } },
                                    label = { Text(name) },
                                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                )
                            }
                        }
                        state.boardFilters.labelId?.let { lid ->
                            val name = labelMap[lid]?.name ?: "Label"
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = { onUpdateFilters { it.copy(labelId = null) } },
                                    label = { Text(name) },
                                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                )
                            }
                        }
                        state.boardFilters.cycleId?.let { cid ->
                            val name = state.projectCycles.firstOrNull { it.id == cid }?.name ?: "Cycle"
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = { onUpdateFilters { it.copy(cycleId = null) } },
                                    label = { Text(name) },
                                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                )
                            }
                        }
                        if (state.boardFilters.showArchived) {
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick = { onUpdateFilters { it.copy(showArchived = false) } },
                                    label = { Text("Archived") },
                                    trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(15.dp)) }
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }

        // Body Content
        when {
            state.isLoadingProjects || (state.isLoadingBoard && scaffold == null) -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            !state.boardError.isNullOrBlank() && scaffold == null -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    ErrorBanner(message = state.boardError, onRetry = onRefresh)
                }
            }

            selectedProject == null -> {
                EmptyStateView(
                    icon = Icons.Rounded.FolderOpen,
                    title = "No projects yet",
                    subtitle = "Create a project in one of your teams or ask a team admin to invite you.",
                    actionLabel = "Open Projects",
                    onAction = onOpenProjectSwitcher,
                    modifier = Modifier.fillMaxSize()
                )
            }

            state.boardFilters.showArchived -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = "Archived Issues (${state.archivedIssues.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    if (state.archivedIssues.isEmpty()) {
                        item {
                            EmptyStateView(
                                icon = Icons.Rounded.Archive,
                                title = "No archived issues",
                                subtitle = "Archived issues in ${selectedProject.name} will appear here."
                            )
                        }
                    } else {
                        items(state.archivedIssues, key = { it.id }) { archived ->
                            Card(
                                onClick = { onOpenIssue(archived.id) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IdentifierChip(archived.identifier)
                                    Text(
                                        text = archived.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    PriorityBadge(archived.priority)
                                }
                            }
                        }
                    }
                }
            }

            !forceListView && state.boardViewMode == BoardViewMode.KANBAN -> {
                LazyRow(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(columns, key = { it.id }) { column ->
                        val colIssues = issuesByColumn[column.id].orEmpty()
                        KanbanColumnLane(
                            column = column,
                            issues = colIssues,
                            labelMap = labelMap,
                            assigneeMap = assigneeMap,
                            onQuickAdd = { onQuickCreateInColumn(column.id) },
                            onIssueClick = onOpenIssue,
                            onRequestMove = { moveTargetIssue = it }
                        )
                    }
                }
            }

            else -> {
                // Grouped List View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    columns.forEach { column ->
                        val colIssues = issuesByColumn[column.id].orEmpty()
                        item(key = "header_${column.id}") {
                            ColumnHeaderRow(
                                column = column,
                                issueCount = colIssues.size,
                                onQuickAdd = { onQuickCreateInColumn(column.id) }
                            )
                        }
                        if (colIssues.isEmpty()) {
                            item(key = "empty_${column.id}") {
                                Text(
                                    text = "No issues in ${column.name}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 12.dp, bottom = 6.dp)
                                )
                            }
                        } else {
                            items(colIssues, key = { it.id }) { issue ->
                                BoardIssueCard(
                                    issue = issue,
                                    labelMap = labelMap,
                                    assigneeMap = assigneeMap,
                                    onClick = { onOpenIssue(issue.id) },
                                    onMoveClick = { moveTargetIssue = issue }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    moveTargetIssue?.let { issue ->
        MoveColumnSheet(
            issue = issue,
            columns = columns,
            issuesByColumn = issuesByColumn,
            onDismiss = { moveTargetIssue = null },
            onSelectColumn = { colId ->
                onMoveIssue(issue, colId)
                moveTargetIssue = null
            }
        )
    }
}

@Composable
private fun KanbanColumnLane(
    column: ColumnDto,
    issues: List<BoardIssueDto>,
    labelMap: Map<Int, LabelDto>,
    assigneeMap: Map<String, AssigneeCandidateDto>,
    onQuickAdd: () -> Unit,
    onIssueClick: (Int) -> Unit,
    onRequestMove: (BoardIssueDto) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(304.dp)
            .fillMaxHeight(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            ColumnHeaderRow(
                column = column,
                issueCount = issues.size,
                onQuickAdd = onQuickAdd,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 84.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (issues.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Drop or add issues here",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(issues, key = { it.id }) { issue ->
                        BoardIssueCard(
                            issue = issue,
                            labelMap = labelMap,
                            assigneeMap = assigneeMap,
                            onClick = { onIssueClick(issue.id) },
                            onMoveClick = { onRequestMove(issue) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnHeaderRow(
    column: ColumnDto,
    issueCount: Int,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colColor = stateTypeColor(column.stateType, column.color)
    val overWip = column.wipLimit != null && issueCount >= column.wipLimit
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = stateTypeIcon(column.stateType),
            contentDescription = column.stateType,
            tint = colColor,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = column.name,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = if (overWip) Coral500.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Text(
                text = if (column.wipLimit != null) "$issueCount/${column.wipLimit}" else "$issueCount",
                style = MonoCodeStyle,
                color = if (overWip) Coral500 else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
        IconButton(
            onClick = onQuickAdd,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add issue to ${column.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun BoardIssueCard(
    issue: BoardIssueDto,
    labelMap: Map<Int, LabelDto>,
    assigneeMap: Map<String, AssigneeCandidateDto>,
    onClick: () -> Unit,
    onMoveClick: () -> Unit
) {
    val assignee = issue.assigneeUserId?.let { assigneeMap[it] }
    val delegate = issue.delegateUserId?.let { assigneeMap[it] }
    val labels = issue.labelIds.mapNotNull { labelMap[it] }.take(3)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("issue_card_${issue.id}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Identifier, Priority, Quick Move
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IdentifierChip(issue.identifier)
                if (issue.effectivePriority != "none") {
                    PriorityBadge(issue.effectivePriority)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onMoveClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Move ${issue.identifier} to another column",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Title
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Labels
            if (labels.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    labels.forEach { label ->
                        LabelPill(name = label.name, hexColor = label.color)
                    }
                }
            }

            // Footer Metadata Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!issue.dueDate.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Due date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = formatShortDate(issue.dueDate),
                            style = MonoCodeStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (issue.subtaskCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountTree,
                            contentDescription = "Subtasks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${issue.subtaskCount}",
                            style = MonoCodeStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (issue.loggedMinutes > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = "Logged time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatMinutes(issue.loggedMinutes),
                            style = MonoCodeStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Delegate AI Agent Badge
                if (delegate != null) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Violet500.copy(alpha = 0.14f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SmartToy,
                                contentDescription = "Delegated to ${delegate.name}",
                                tint = Violet500,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = delegate.name.substringBefore(' '),
                                style = MaterialTheme.typography.labelMedium,
                                color = Violet500,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (assignee != null) {
                    UserAvatar(
                        name = assignee.name,
                        imageUrl = assignee.image,
                        isAgent = assignee.isAgent,
                        size = 24.dp
                    )
                }
            }
        }
    }
}
