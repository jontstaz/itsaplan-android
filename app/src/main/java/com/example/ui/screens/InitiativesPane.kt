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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.InitiativeDto
import com.example.data.model.ProjectListItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorBanner
import com.example.ui.components.formatShortDate
import com.example.ui.theme.Emerald500
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.viewmodel.InitiativeFilter
import com.example.ui.viewmodel.InitiativesViewModel

/**
 * The Initiatives half of the Cycles tab: a project's strategic milestones, with the
 * progress its linked issues add up to.
 */
@Composable
fun InitiativesPane(
    project: ProjectListItem?,
    viewModel: InitiativesViewModel,
    showCreateDialog: Boolean,
    onCreateRequested: () -> Unit,
    onDismissCreate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        project == null -> {
            EmptyStateView(
                icon = Icons.Rounded.RocketLaunch,
                title = "No project selected",
                subtitle = "Select a project on the Board tab to see its initiatives.",
                modifier = modifier.fillMaxSize()
            )
        }

        state.isLoading && state.initiatives.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        !state.error.isNullOrBlank() -> {
            Box(modifier = modifier.padding(16.dp)) {
                ErrorBanner(
                    message = state.error ?: "Failed to load initiatives.",
                    onRetry = { viewModel.load(project.routeKey) }
                )
            }
        }

        state.initiatives.isEmpty() -> {
            EmptyStateView(
                icon = Icons.Rounded.RocketLaunch,
                title = "No initiatives yet",
                subtitle = "Group related issues into strategic milestones and track roadmap progress across the project.",
                actionLabel = "Create First Initiative",
                onAction = onCreateRequested,
                modifier = modifier.fillMaxSize()
            )
        }

        else -> {
            Column(modifier = modifier.fillMaxSize()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(InitiativeFilter.entries.toList()) { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.label) }
                        )
                    }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
                    contentPadding = PaddingValues(start = 14.dp, end = 14.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.initiatives, key = { it.id }) { initiative ->
                        InitiativeCard(initiative = initiative)
                    }
                    if (state.hasMore) {
                        item(key = "load_more") {
                            TextButton(
                                onClick = { viewModel.loadMore() },
                                enabled = !state.isLoadingMore,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (state.isLoadingMore) "Loading…" else "Load more")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateInitiativeDialog(
            isSaving = state.isSaving,
            onDismiss = onDismissCreate,
            onCreate = { title, description, targetDate ->
                viewModel.create(title, description, targetDate, onDismissCreate)
            }
        )
    }
}

@Composable
private fun InitiativeCard(initiative: InitiativeDto) {
    val accent = initiativeStatusColor(initiative.status)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("initiative_card_${initiative.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.RocketLaunch,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = initiative.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                InitiativeStatusPill(status = initiative.status)
            }

            if (initiative.description.isNotBlank()) {
                Text(
                    text = initiative.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            LinearProgressIndicator(
                progress = { initiative.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${initiative.progress.completed}/${initiative.progress.total} issues",
                    style = MonoCodeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                initiative.health?.let { health ->
                    Text(
                        text = health.replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (health) {
                            "on_track" -> Emerald500
                            "at_risk" -> com.example.ui.theme.Amber500
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                }
                Spacer(Modifier.weight(1f))
                initiative.targetDate?.takeIf { it.isNotBlank() }?.let { date ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = "Target date",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatShortDate(date),
                            style = MonoCodeStyle,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InitiativeStatusPill(status: String) {
    val color = initiativeStatusColor(status)
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.16f))
    ) {
        Text(
            text = status.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun initiativeStatusColor(status: String) = when (status) {
    "active" -> Emerald500
    "planned" -> MaterialTheme.colorScheme.primary
    "completed" -> Emerald500
    "canceled" -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.secondary
}

@Composable
private fun CreateInitiativeDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, description: String?, targetDate: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Initiative") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("initiative_title_input")
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCreate(
                        title,
                        description.ifBlank { null },
                        targetDate.ifBlank { null }
                    )
                },
                enabled = title.isNotBlank() && !isSaving,
                modifier = Modifier.testTag("initiative_create_confirm")
            ) {
                Text(if (isSaving) "Creating…" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}