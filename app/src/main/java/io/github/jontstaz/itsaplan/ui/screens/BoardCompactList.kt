package io.github.jontstaz.itsaplan.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.jontstaz.itsaplan.data.model.BoardIssueDto
import io.github.jontstaz.itsaplan.data.model.ColumnDto
import io.github.jontstaz.itsaplan.ui.components.priorityIcon
import io.github.jontstaz.itsaplan.ui.components.stateTypeColor
import io.github.jontstaz.itsaplan.ui.components.stateTypeIcon
import io.github.jontstaz.itsaplan.ui.theme.MonoCodeStyle

/**
 * The densest board view: one line per issue, grouped under collapsible status headers.
 * Built for scanning a long backlog rather than reading cards.
 */
@Composable
internal fun CompactIssueList(
    columns: List<ColumnDto>,
    issuesByColumn: Map<Int, List<BoardIssueDto>>,
    onOpenIssue: (Int) -> Unit,
    onRequestMove: (BoardIssueDto) -> Unit,
    onQuickAdd: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Collapsed groups survive rotation and fold/unfold through rememberSaveable.
    var collapsed by rememberSaveable { mutableStateOf(setOf<Int>()) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 88.dp)
    ) {
        columns.forEach { column ->
            val issues = issuesByColumn[column.id].orEmpty()
            val isCollapsed = column.id in collapsed

            item(key = "hdr_${column.id}") {
                CompactGroupHeader(
                    column = column,
                    count = issues.size,
                    collapsed = isCollapsed,
                    onToggle = {
                        collapsed = if (isCollapsed) collapsed - column.id else collapsed + column.id
                    },
                    onQuickAdd = { onQuickAdd(column.id) }
                )
            }

            if (!isCollapsed) {
                if (issues.isEmpty()) {
                    item(key = "empty_${column.id}") {
                        Text(
                            text = "No issues in ${column.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 34.dp, top = 6.dp, bottom = 10.dp)
                        )
                    }
                } else {
                    items(issues.size, key = { "i_${column.id}_${issues[it].id}" }) { index ->
                        val issue = issues[index]
                        CompactIssueRow(
                            issue = issue,
                            onClick = { onOpenIssue(issue.id) },
                            onMove = { onRequestMove(issue) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactGroupHeader(
    column: ColumnDto,
    count: Int,
    collapsed: Boolean,
    onToggle: () -> Unit,
    onQuickAdd: () -> Unit
) {
    val color = stateTypeColor(column.stateType, column.color)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(start = 4.dp, end = 2.dp, top = 2.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = if (collapsed) Icons.Rounded.KeyboardArrowDown else Icons.Rounded.KeyboardArrowUp,
                contentDescription = if (collapsed) "Expand ${column.name}" else "Collapse ${column.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = stateTypeIcon(column.stateType),
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = column.name,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (column.wipLimit != null) "$count/${column.wipLimit}" else "$count",
                style = MonoCodeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(onClick = onQuickAdd, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add issue to ${column.name}",
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactIssueRow(
    issue: BoardIssueDto,
    onClick: () -> Unit,
    onMove: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 8.dp, end = 2.dp, top = 9.dp, bottom = 9.dp)
                .testTag("compact_issue_${issue.id}"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = priorityIcon(issue.effectivePriority),
                contentDescription = "Priority ${issue.effectivePriority}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = issue.identifier,
                style = MonoCodeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = issue.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (issue.subtaskCount > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountTree,
                        contentDescription = "Subtasks",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "${issue.subtaskCount}",
                        style = MonoCodeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clickable(onClick = onMove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.SwapHoriz,
                    contentDescription = "Move ${issue.identifier} to another column",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}