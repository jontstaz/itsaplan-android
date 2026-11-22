package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BoardIssueDto
import com.example.data.model.ColumnDto
import com.example.data.model.CycleDto
import com.example.data.model.ProjectBoardResponse
import com.example.data.model.ProjectListItem
import com.example.data.model.TeamDto
import com.example.ui.components.ALL_PRIORITIES
import com.example.ui.components.UserAvatar
import com.example.ui.components.priorityLabel
import com.example.ui.components.stateTypeColor
import com.example.ui.components.stateTypeIcon
import com.example.ui.theme.Amber500
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.viewmodel.BoardFilters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectSwitcherSheet(
    projects: List<ProjectListItem>,
    teams: List<TeamDto>,
    selectedProject: ProjectListItem?,
    onDismiss: () -> Unit,
    onSelectProject: (ProjectListItem) -> Unit,
    onToggleFavorite: (ProjectListItem) -> Unit,
    onCreateProject: (teamId: Int, key: String, name: String, desc: String?, preset: String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    val filtered = remember(projects, query) {
        val q = query.trim().lowercase()
        projects.filter { p ->
            !p.isHidden && (
                q.isEmpty() ||
                    p.name.lowercase().contains(q) ||
                    p.key.lowercase().contains(q) ||
                    p.teamName.lowercase().contains(q)
                )
        }
    }
    val favorites = filtered.filter { it.isFavorite }
    val others = filtered.filterNot { it.isFavorite }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Switch Project", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "The active project controls Board, Docs, and Cycles.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (teams.isNotEmpty()) {
                    TextButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier.testTag("new_project_button")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New Project")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search projects or teams…") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (favorites.isNotEmpty()) {
                    item {
                        Text(
                            text = "Favorites",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    items(favorites, key = { "fav_${it.id}" }) { proj ->
                        ProjectRowItem(
                            project = proj,
                            selected = proj.id == selectedProject?.id,
                            onClick = { onSelectProject(proj); onDismiss() },
                            onToggleFavorite = { onToggleFavorite(proj) }
                        )
                    }
                }
                if (others.isNotEmpty()) {
                    item {
                        Text(
                            text = if (favorites.isNotEmpty()) "All Projects" else "Projects",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(others, key = { it.id }) { proj ->
                        ProjectRowItem(
                            project = proj,
                            selected = proj.id == selectedProject?.id,
                            onClick = { onSelectProject(proj); onDismiss() },
                            onToggleFavorite = { onToggleFavorite(proj) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog && teams.isNotEmpty()) {
        CreateProjectDialog(
            teams = teams,
            onDismiss = { showCreateDialog = false },
            onCreate = { teamId, key, name, desc, preset ->
                onCreateProject(teamId, key, name, desc, preset)
                showCreateDialog = false
                onDismiss()
            }
        )
    }
}

@Composable
private fun ProjectRowItem(
    project: ProjectListItem,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = project.key,
                    style = MonoCodeStyle.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    project.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${project.teamName} • ${project.routeKey}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Active project",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "Active",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (project.isFavorite) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                    contentDescription = "Toggle favorite",
                    tint = if (project.isFavorite) Amber500 else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreateProjectDialog(
    teams: List<TeamDto>,
    onDismiss: () -> Unit,
    onCreate: (teamId: Int, key: String, name: String, desc: String?, preset: String) -> Unit
) {
    var selectedTeamId by remember { mutableStateOf(teams.first().id) }
    var name by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf("software") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Project") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Team", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    teams.forEach { team ->
                        FilterChip(
                            selected = team.id == selectedTeamId,
                            onClick = { selectedTeamId = team.id },
                            label = { Text(team.name) }
                        )
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (key.isBlank() && it.length >= 2) {
                            key = it.filter { ch -> ch.isLetter() }.take(4).uppercase()
                        }
                    },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it.uppercase().filter { ch -> ch.isLetterOrDigit() }.take(10) },
                    label = { Text("Project Key (e.g. APP)") },
                    singleLine = true,
                    textStyle = MonoCodeStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Issue Type Preset", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "general" to "General",
                        "software" to "Software",
                        "product" to "Product",
                        "marketing" to "Marketing",
                        "design" to "Design",
                        "support" to "Support"
                    ).forEach { (id, label) ->
                        FilterChip(
                            selected = preset == id,
                            onClick = { preset = id },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(selectedTeamId, key, name, description.ifBlank { null }, preset) },
                enabled = name.isNotBlank() && key.length >= 2
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveColumnSheet(
    issue: BoardIssueDto,
    columns: List<ColumnDto>,
    issuesByColumn: Map<Int, List<BoardIssueDto>>,
    onDismiss: () -> Unit,
    onSelectColumn: (Int) -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Move ${issue.identifier}",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = issue.title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            columns.forEach { col ->
                val count = issuesByColumn[col.id]?.size ?: 0
                val isCurrent = col.id == issue.columnId
                val color = stateTypeColor(col.stateType, col.color)
                ListItem(
                    headlineContent = { Text(col.name, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal) },
                    supportingContent = col.wipLimit?.let { { Text("WIP $count / $it") } },
                    leadingContent = {
                        Icon(
                            imageVector = stateTypeIcon(col.stateType),
                            contentDescription = null,
                            tint = color
                        )
                    },
                    trailingContent = {
                        if (isCurrent) {
                            Icon(Icons.Rounded.Check, contentDescription = "Current column", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectColumn(col.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BoardFilterSheet(
    filters: BoardFilters,
    scaffold: ProjectBoardResponse,
    cycles: List<CycleDto>,
    onUpdateFilters: ((BoardFilters) -> BoardFilters) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter Board", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { onUpdateFilters { BoardFilters() } }) {
                    Text("Reset All")
                }
            }

            Text("Priority", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ALL_PRIORITIES.forEach { p ->
                    FilterChip(
                        selected = filters.priority == p,
                        onClick = {
                            onUpdateFilters { it.copy(priority = if (it.priority == p) null else p) }
                        },
                        label = { Text(priorityLabel(p)) }
                    )
                }
            }

            if (scaffold.assignees.isNotEmpty()) {
                Text("Assignee or AI Delegate", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    scaffold.assignees.take(15).forEach { candidate ->
                        FilterChip(
                            selected = filters.assigneeUserId == candidate.userId,
                            onClick = {
                                onUpdateFilters {
                                    it.copy(assigneeUserId = if (it.assigneeUserId == candidate.userId) null else candidate.userId)
                                }
                            },
                            label = { Text(candidate.name) }
                        )
                    }
                }
            }

            if (scaffold.labels.isNotEmpty()) {
                Text("Label", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    scaffold.labels.forEach { label ->
                        FilterChip(
                            selected = filters.labelId == label.id,
                            onClick = {
                                onUpdateFilters {
                                    it.copy(labelId = if (it.labelId == label.id) null else label.id)
                                }
                            },
                            label = { Text(label.name) }
                        )
                    }
                }
            }

            if (cycles.isNotEmpty()) {
                Text("Cycle", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    cycles.forEach { cycle ->
                        FilterChip(
                            selected = filters.cycleId == cycle.id,
                            onClick = {
                                onUpdateFilters {
                                    it.copy(cycleId = if (it.cycleId == cycle.id) null else cycle.id)
                                }
                            },
                            label = { Text(cycle.name) }
                        )
                    }
                }
            }

            HorizontalDivider()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("View Archived Issues", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Switch board to archived items list",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = filters.showArchived,
                    onCheckedChange = { checked -> onUpdateFilters { it.copy(showArchived = checked) } }
                )
            }

            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CreateIssueSheet(
    scaffold: ProjectBoardResponse,
    cycles: List<CycleDto>,
    initialColumnId: Int?,
    onDismiss: () -> Unit,
    onCreate: (
        columnId: Int,
        title: String,
        description: String?,
        typeId: Int?,
        cycleId: Int?,
        assigneeUserId: String?,
        delegateUserId: String?,
        priority: String,
        estimatePoints: Double?,
        estimateMinutes: Int?,
        dueDate: String?,
        labelIds: List<Int>
    ) -> Unit
) {
    val columns = remember(scaffold) { scaffold.columns.sortedBy { it.position } }
    val members = remember(scaffold) { scaffold.assignees.filter { !it.isAgent } }
    val agents = remember(scaffold) { scaffold.assignees.filter { it.isAgent } }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var columnId by remember { mutableStateOf(initialColumnId ?: columns.firstOrNull()?.id ?: 0) }
    var priority by remember { mutableStateOf("none") }
    var typeId by remember { mutableStateOf(scaffold.issueTypes.firstOrNull { it.isDefault }?.id ?: scaffold.issueTypes.firstOrNull()?.id) }
    var assigneeUserId by remember { mutableStateOf<String?>(null) }
    var delegateUserId by remember { mutableStateOf<String?>(null) }
    var cycleId by remember { mutableStateOf<Int?>(cycles.firstOrNull { it.status == "active" }?.id) }
    var dueDate by remember { mutableStateOf("") }
    var estimatePoints by remember { mutableStateOf("") }
    var estimateMinutes by remember { mutableStateOf("") }
    val selectedLabels = remember { mutableStateListOf<Int>() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "New Issue in ${scaffold.project.name}",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title *") },
                placeholder = { Text("Concise summary of the work item") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_issue_title_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Markdown)") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_issue_desc_input")
            )

            // Column picker
            Text("Status Column", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                columns.forEach { col ->
                    FilterChip(
                        selected = columnId == col.id,
                        onClick = { columnId = col.id },
                        label = { Text(col.name) }
                    )
                }
            }

            // Priority picker
            Text("Priority", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ALL_PRIORITIES.forEach { p ->
                    FilterChip(
                        selected = priority == p,
                        onClick = { priority = p },
                        label = { Text(priorityLabel(p)) }
                    )
                }
            }

            // Issue Type
            if (scaffold.issueTypes.isNotEmpty()) {
                Text("Type", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    scaffold.issueTypes.forEach { t ->
                        FilterChip(
                            selected = typeId == t.id,
                            onClick = { typeId = t.id },
                            label = { Text(t.name) }
                        )
                    }
                }
            }

            // Human Assignee
            if (members.isNotEmpty()) {
                Text("Assignee", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = assigneeUserId == null,
                        onClick = { assigneeUserId = null },
                        label = { Text("Unassigned") }
                    )
                    members.take(10).forEach { m ->
                        FilterChip(
                            selected = assigneeUserId == m.userId,
                            onClick = { assigneeUserId = m.userId },
                            leadingIcon = { UserAvatar(name = m.name, size = 18.dp) },
                            label = { Text(m.name) }
                        )
                    }
                }
            }

            // AI Agent Delegate
            if (agents.isNotEmpty()) {
                Text("AI Agent Delegate", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = delegateUserId == null,
                        onClick = { delegateUserId = null },
                        label = { Text("None") }
                    )
                    agents.forEach { a ->
                        FilterChip(
                            selected = delegateUserId == a.userId,
                            onClick = { delegateUserId = a.userId },
                            leadingIcon = { UserAvatar(name = a.name, isAgent = true, size = 18.dp) },
                            label = { Text(a.name) }
                        )
                    }
                }
            }

            // Cycle
            if (cycles.isNotEmpty()) {
                Text("Cycle", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = cycleId == null,
                        onClick = { cycleId = null },
                        label = { Text("No Cycle") }
                    )
                    cycles.forEach { c ->
                        FilterChip(
                            selected = cycleId == c.id,
                            onClick = { cycleId = c.id },
                            label = { Text(c.name) }
                        )
                    }
                }
            }

            // Labels
            if (scaffold.labels.isNotEmpty()) {
                Text("Labels", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    scaffold.labels.forEach { label ->
                        val checked = selectedLabels.contains(label.id)
                        FilterChip(
                            selected = checked,
                            onClick = {
                                if (checked) selectedLabels.remove(label.id) else selectedLabels.add(label.id)
                            },
                            label = { Text(label.name) }
                        )
                    }
                }
            }

            // Dates & Estimates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { dueDate = it },
                    label = { Text("Due Date") },
                    placeholder = { Text("YYYY-MM-DD") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (scaffold.project.pointsEstimateEnabled) {
                    OutlinedTextField(
                        value = estimatePoints,
                        onValueChange = { estimatePoints = it },
                        label = { Text("Points") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(0.7f)
                    )
                }
                if (scaffold.project.timeEstimateEnabled) {
                    OutlinedTextField(
                        value = estimateMinutes,
                        onValueChange = { estimateMinutes = it },
                        label = { Text("Est (min)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.7f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onCreate(
                            columnId,
                            title,
                            description.ifBlank { null },
                            typeId,
                            cycleId,
                            assigneeUserId,
                            delegateUserId,
                            priority,
                            estimatePoints.toDoubleOrNull(),
                            estimateMinutes.toIntOrNull(),
                            dueDate.ifBlank { null },
                            selectedLabels.toList()
                        )
                    },
                    enabled = title.isNotBlank() && columnId > 0,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("create_issue_submit_button")
                ) {
                    Text("Create Issue")
                }
            }
        }
    }
}
