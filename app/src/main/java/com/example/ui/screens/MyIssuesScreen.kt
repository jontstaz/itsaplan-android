package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CrossProjectIssueHitDto
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorBanner
import com.example.ui.components.IdentifierChip
import com.example.ui.components.PriorityBadge
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.stateTypeColor
import com.example.ui.components.stateTypeIcon
import com.example.ui.viewmodel.MyIssuesScope
import com.example.ui.viewmodel.MyIssuesViewModel

@Composable
fun MyIssuesScreen(
    currentUserId: String,
    viewModel: MyIssuesViewModel,
    onOpenIssue: (issueId: Int, projectRef: String?) -> Unit,
    // The Home dashboard supplies its own header and tab bar.
    showHeader: Boolean = true
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(currentUserId) {
        viewModel.load(currentUserId)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showHeader) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Issues & Search", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = { viewModel.load(currentUserId) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh issues")
                        }
                    }
                }

                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    placeholder = { Text(stringResource(R.string.search_issues_hint)) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_search_input")
                )

                if (state.searchQuery.trim().length < 2) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = state.assignedToMeOnly,
                                onClick = { viewModel.setAssignedToMeOnly(!state.assignedToMeOnly) },
                                label = { Text(if (state.assignedToMeOnly) "Assigned to Me" else "Everyone") }
                            )
                        }
                        items(MyIssuesScope.entries) { scope ->
                            FilterChip(
                                selected = state.scope == scope,
                                onClick = { viewModel.setScope(scope) },
                                label = { Text(scope.label) }
                            )
                        }
                        item {
                            val isHigh = state.priorityFilter == "urgent"
                            FilterChip(
                                selected = isHigh,
                                onClick = { viewModel.setPriorityFilter(if (isHigh) null else "urgent") },
                                label = { Text("Urgent") }
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when {
            state.isLoading && state.issues.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            !state.error.isNullOrBlank() -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    ErrorBanner(message = state.error!!, onRetry = { viewModel.load(currentUserId) })
                }
            }

            state.issues.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Rounded.AssignmentTurnedIn,
                    title = if (state.searchQuery.length >= 2) "No matching issues" else "All clear!",
                    subtitle = if (state.searchQuery.length >= 2) {
                        "Try a different keyword or issue identifier."
                    } else {
                        "No issues match your current filters across accessible projects."
                    },
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
                    items(state.issues, key = { it.id }) { hit ->
                        CrossProjectIssueCard(
                            hit = hit,
                            onClick = {
                                onOpenIssue(hit.id, hit.projectRef.ifBlank { hit.projectKey })
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CrossProjectIssueCard(
    hit: CrossProjectIssueHitDto,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IdentifierChip(hit.identifier)
                if (hit.effectivePriority != "none") {
                    PriorityBadge(hit.effectivePriority)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = formatRelativeTime(hit.updatedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = hit.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildString {
                        if (!hit.teamName.isNullOrBlank()) append("${hit.teamName} · ")
                        append(hit.projectName.ifBlank { hit.projectKey })
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (hit.archived) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "Archived",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
