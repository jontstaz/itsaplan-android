package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.ViewKanban
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.ProjectListItem
import com.example.ui.theme.Amber500
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.viewmodel.AppUiState
import com.example.ui.viewmodel.BoardViewMode

@Composable
internal fun BoardHeaderContent(
    state: AppUiState,
    selectedProject: ProjectListItem?,
    showSearchBar: Boolean,
    onOpenProjectSwitcher: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenGlobalSearch: () -> Unit,
    onToggleSearch: () -> Unit,
    onSetViewMode: (BoardViewMode) -> Unit,
    onOpenFilterSheet: () -> Unit,
    onRefresh: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 600.dp
        if (compact) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 4.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProjectSwitcherButton(selectedProject, onOpenProjectSwitcher, Modifier.weight(1f))
                    FavoriteProjectButton(selectedProject, onToggleFavorite)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoardHeaderActions(
                        state, showSearchBar, onOpenGlobalSearch, onToggleSearch,
                        onSetViewMode, onOpenFilterSheet, onRefresh,
                        canRefresh = selectedProject != null && !state.isLoadingBoard
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProjectSwitcherButton(selectedProject, onOpenProjectSwitcher, Modifier.weight(1f))
                FavoriteProjectButton(selectedProject, onToggleFavorite)
                BoardHeaderActions(
                    state, showSearchBar, onOpenGlobalSearch, onToggleSearch,
                    onSetViewMode, onOpenFilterSheet, onRefresh,
                    canRefresh = selectedProject != null && !state.isLoadingBoard
                )
            }
        }
    }
}

@Composable
private fun ProjectSwitcherButton(
    project: ProjectListItem?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
        modifier = modifier.testTag("project_switcher_button")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (project != null) {
                Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        text = project.key,
                        style = MonoCodeStyle.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = project.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (project.teamName.isNotBlank()) "Active · ${project.teamName}" else "Active project",
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
}

@Composable
private fun FavoriteProjectButton(project: ProjectListItem?, onClick: () -> Unit) {
    if (project == null) return
    IconButton(onClick = onClick, modifier = Modifier.testTag("favorite_project_button")) {
        Icon(
            imageVector = if (project.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
            contentDescription = if (project.isFavorite) "Remove favorite" else "Favorite project",
            tint = if (project.isFavorite) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowScope.BoardHeaderActions(
    state: AppUiState,
    showSearchBar: Boolean,
    onOpenGlobalSearch: () -> Unit,
    onToggleSearch: () -> Unit,
    onSetViewMode: (BoardViewMode) -> Unit,
    onOpenFilterSheet: () -> Unit,
    onRefresh: () -> Unit,
    canRefresh: Boolean
) {
    IconButton(onClick = onOpenGlobalSearch, modifier = Modifier.testTag("global_search_toggle")) {
        Icon(Icons.Rounded.TravelExplore, contentDescription = "Search all projects")
    }
    IconButton(onClick = onToggleSearch, modifier = Modifier.testTag("board_search_toggle")) {
        Icon(
            imageVector = if (showSearchBar) Icons.Rounded.Close else Icons.Rounded.Search,
            contentDescription = "Search board"
        )
    }
    IconButton(
        onClick = { onSetViewMode(state.boardViewMode.next()) },
        modifier = Modifier.testTag("board_view_mode_toggle")
    ) {
        Icon(
            imageVector = when (state.boardViewMode) {
                BoardViewMode.KANBAN -> Icons.AutoMirrored.Rounded.FormatListBulleted
                BoardViewMode.LIST -> Icons.Rounded.ViewStream
                BoardViewMode.COMPACT -> Icons.Rounded.ViewKanban
            },
            contentDescription = when (state.boardViewMode) {
                BoardViewMode.KANBAN -> "Kanban view. Switch to list."
                BoardViewMode.LIST -> "List view. Switch to compact list."
                BoardViewMode.COMPACT -> "Compact list view. Switch to Kanban."
            }
        )
    }
    IconButton(onClick = onOpenFilterSheet, modifier = Modifier.testTag("board_filter_button")) {
        BadgedBox(badge = {
            if (state.boardFilters.activeCount > 0) Badge { Text("${state.boardFilters.activeCount}") }
        }) {
            Icon(Icons.Rounded.FilterList, contentDescription = "Filter issues")
        }
    }
    IconButton(
        onClick = onRefresh,
        enabled = canRefresh,
        modifier = Modifier.testTag("board_refresh_button")
    ) {
        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh board")
    }
}
