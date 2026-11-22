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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.MoveUp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CycleDto
import com.example.data.model.ProjectListItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorBanner
import com.example.ui.components.formatShortDate
import com.example.ui.components.todayIsoDate
import com.example.ui.theme.Amber500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.viewmodel.CyclesViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CyclesScreen(
    selectedProject: ProjectListItem?,
    viewModel: CyclesViewModel,
    onBoardRefresh: () -> Unit,
    onFilterBoardByCycle: (Int) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var transferSourceCycle by remember { mutableStateOf<CycleDto?>(null) }

    LaunchedEffect(selectedProject?.routeKey, selectedProject?.cyclesEnabled) {
        if (selectedProject != null && selectedProject.cyclesEnabled) {
            viewModel.loadCycles(selectedProject.routeKey)
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            onShowMessage(it)
            viewModel.clearMessage()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cycles", style = MaterialTheme.typography.titleLarge)
                    if (selectedProject != null) {
                        Text(
                            text = "${selectedProject.key} · ${selectedProject.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (selectedProject?.cyclesEnabled == true) {
                    IconButton(onClick = { viewModel.loadCycles(selectedProject.routeKey) }) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Refresh cycles")
                    }
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("create_cycle_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Cycle")
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        when {
            selectedProject == null -> {
                EmptyStateView(
                    icon = Icons.Rounded.Autorenew,
                    title = "No project selected",
                    subtitle = "Select a project on the Board tab to manage its sprint cycles.",
                    modifier = Modifier.fillMaxSize()
                )
            }

            !selectedProject.cyclesEnabled -> {
                EmptyStateView(
                    icon = Icons.Rounded.Autorenew,
                    title = "Cycles disabled for ${selectedProject.key}",
                    subtitle = "Enable Cycles in project settings on your itsaplan server to track time-boxed sprints here.",
                    modifier = Modifier.fillMaxSize()
                )
            }

            state.isLoading && state.plannedCycles.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            !state.error.isNullOrBlank() -> {
                Box(modifier = Modifier.padding(16.dp)) {
                    ErrorBanner(
                        message = state.error!!,
                        onRetry = { viewModel.loadCycles(selectedProject.routeKey) }
                    )
                }
            }

            state.plannedCycles.isEmpty() && state.completedCycles.isEmpty() -> {
                EmptyStateView(
                    icon = Icons.Rounded.Flag,
                    title = "No cycles yet",
                    subtitle = "Create a cycle to group work items into time-boxed iterations and track completion.",
                    actionLabel = "Create First Cycle",
                    onAction = { showCreateDialog = true },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                val activeCycle = state.plannedCycles.firstOrNull { it.status == "active" }
                val upcoming = state.plannedCycles.filter { it.status != "active" }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeCycle != null) {
                        item(key = "active_header") {
                            Text("Active Cycle", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        item(key = "active_${activeCycle.id}") {
                            CycleCard(
                                cycle = activeCycle,
                                isActive = true,
                                onViewIssues = { onFilterBoardByCycle(activeCycle.id) },
                                onFinish = { viewModel.finishCycle(activeCycle.id, onBoardRefresh) },
                                onStartNext = { viewModel.startNextCycle(activeCycle.id, onBoardRefresh) },
                                onTransfer = { transferSourceCycle = activeCycle },
                                onDelete = { viewModel.deleteCycle(activeCycle.id, onBoardRefresh) }
                            )
                        }
                    }

                    if (upcoming.isNotEmpty()) {
                        item(key = "upcoming_header") {
                            Text("Upcoming", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(upcoming, key = { it.id }) { cycle ->
                            CycleCard(
                                cycle = cycle,
                                isActive = false,
                                onViewIssues = { onFilterBoardByCycle(cycle.id) },
                                onFinish = null,
                                onStartNext = null,
                                onTransfer = { transferSourceCycle = cycle },
                                onDelete = { viewModel.deleteCycle(cycle.id, onBoardRefresh) }
                            )
                        }
                    }

                    if (state.completedCycles.isNotEmpty()) {
                        item(key = "completed_header") {
                            Text("Completed", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        items(state.completedCycles, key = { "done_${it.id}" }) { cycle ->
                            CycleCard(
                                cycle = cycle,
                                isActive = false,
                                onViewIssues = { onFilterBoardByCycle(cycle.id) },
                                onFinish = null,
                                onStartNext = null,
                                onTransfer = null,
                                onDelete = null
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateCycleDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, goal, start, end ->
                viewModel.createCycle(name, goal, start, end) {
                    showCreateDialog = false
                    onBoardRefresh()
                }
            }
        )
    }

    transferSourceCycle?.let { source ->
        val candidates = state.plannedCycles.filter { it.id != source.id }
        AlertDialog(
            onDismissRequest = { transferSourceCycle = null },
            title = { Text("Transfer Unfinished Issues") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Move open issues from ${source.name} to:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            viewModel.transferIssues(source.id, null, onBoardRefresh)
                            transferSourceCycle = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Backlog (No Cycle)", modifier = Modifier.fillMaxWidth()) }
                    candidates.forEach { target ->
                        TextButton(
                            onClick = {
                                viewModel.transferIssues(source.id, target.id, onBoardRefresh)
                                transferSourceCycle = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(target.name, modifier = Modifier.fillMaxWidth()) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { transferSourceCycle = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CycleCard(
    cycle: CycleDto,
    isActive: Boolean,
    onViewIssues: () -> Unit,
    onFinish: (() -> Unit)?,
    onStartNext: (() -> Unit)?,
    onTransfer: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    val denominator = (cycle.progress.total - cycle.progress.canceled).coerceAtLeast(1)
    val progress = if (cycle.progress.total == 0) 0f else (cycle.progress.completed.toFloat() / denominator).coerceIn(0f, 1f)
    val pct = (progress * 100).toInt()
    val badgeColor = when (cycle.status) {
        "active" -> Emerald500
        "completed" -> MaterialTheme.colorScheme.primary
        else -> Amber500
    }

    Card(
        onClick = onViewIssues,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isActive) 1.5.dp else 1.dp,
            color = if (isActive) Emerald500.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(cycle.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "${formatShortDate(cycle.startDate)} → ${formatShortDate(cycle.endDate)}",
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(shape = RoundedCornerShape(999.dp), color = badgeColor.copy(alpha = 0.16f)) {
                    Text(
                        text = cycle.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (!cycle.goal.isNullOrBlank()) {
                Text(cycle.goal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${cycle.progress.completed} of ${cycle.progress.total} completed",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("$pct%", style = MonoCodeStyle, fontWeight = FontWeight.Bold)
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(7.dp)
                )
            }

            if (onFinish != null || onTransfer != null || onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onFinish != null) {
                        OutlinedButton(onClick = onFinish, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Finish", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (onStartNext != null) {
                        FilledTonalButton(onClick = onStartNext, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)) {
                            Icon(Icons.Rounded.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Start Next", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (onTransfer != null) {
                        IconButton(onClick = onTransfer) {
                            Icon(Icons.Rounded.MoveUp, contentDescription = "Transfer issues")
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete cycle")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCycleDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, goal: String?, startDate: String, endDate: String) -> Unit
) {
    val defaultStart = remember { todayIsoDate() }
    val defaultEnd = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 14)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(defaultStart) }
    var endDate by remember { mutableStateOf(defaultEnd) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Cycle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Cycle Name (e.g. Sprint 12)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it },
                    label = { Text("Goal (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = startDate,
                    onValueChange = { startDate = it },
                    label = { Text("Start Date (YYYY-MM-DD)") },
                    singleLine = true,
                    textStyle = MonoCodeStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endDate,
                    onValueChange = { endDate = it },
                    label = { Text("End Date (YYYY-MM-DD)") },
                    singleLine = true,
                    textStyle = MonoCodeStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name, goal.ifBlank { null }, startDate, endDate) },
                enabled = name.isNotBlank() && startDate.length >= 10 && endDate.length >= 10
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
