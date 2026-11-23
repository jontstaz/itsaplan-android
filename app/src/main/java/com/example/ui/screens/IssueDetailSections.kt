package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddReaction
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.CallMerge
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AssigneeCandidateDto
import com.example.data.model.ChecklistDto
import com.example.data.model.CustomFieldDto
import com.example.data.model.CycleDto
import com.example.data.model.DevelopmentLinkDto
import com.example.data.model.FeedCountsDto
import com.example.data.model.FeedItemDto
import com.example.data.model.IssueDetailDto
import com.example.data.model.IssueFieldValueDto
import com.example.data.model.IssueSearchHitDto
import com.example.data.model.ProjectBoardResponse
import com.example.ui.components.ALL_PRIORITIES
import com.example.ui.components.IdentifierChip
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatMinutes
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.priorityLabel
import com.example.ui.components.todayIsoDate
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.Amber500
import com.example.ui.theme.Coral500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Violet500
import com.example.ui.viewmodel.FeedFilter

enum class MetadataPicker {
    STATUS, PRIORITY, ASSIGNEE, DELEGATE, TYPE, CYCLE, LABELS, DATES_ESTIMATES
}

private val QUICK_EMOJIS = listOf("👍", "🎉", "👀", "❤️", "🚀", "🔥")

@Composable
fun ChecklistsCard(
    checklists: List<ChecklistDto>,
    onAddChecklist: (String) -> Unit,
    onDeleteChecklist: (Int) -> Unit,
    onAddItem: (Int, String) -> Unit,
    onToggleItem: (Int, Boolean) -> Unit,
    onDeleteItem: (Int) -> Unit
) {
    var showNewChecklist by remember { mutableStateOf(false) }
    var newChecklistTitle by remember { mutableStateOf("") }
    var addItemTargetChecklistId by remember { mutableStateOf<Int?>(null) }
    var newItemContent by remember { mutableStateOf("") }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.CheckBox, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Checklists (${checklists.size})", style = MaterialTheme.typography.titleSmall)
                }
                TextButton(onClick = { showNewChecklist = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New List")
                }
            }

            if (checklists.isEmpty()) {
                Text("No checklists yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                checklists.forEach { cl ->
                    val doneCount = cl.items.count { it.done }
                    val progress = if (cl.items.isEmpty()) 0f else doneCount.toFloat() / cl.items.size
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cl.title, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                                Text("$doneCount/${cl.items.size}", style = MonoCodeStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                IconButton(
                                    onClick = { addItemTargetChecklistId = cl.id; newItemContent = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = "Add checklist item", modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteChecklist(cl.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "Delete checklist", modifier = Modifier.size(16.dp))
                                }
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(5.dp)
                            )
                            cl.items.sortedBy { it.position }.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = item.done,
                                        onCheckedChange = { onToggleItem(item.id, it) }
                                    )
                                    Text(
                                        text = item.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (item.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { onDeleteItem(item.id) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Remove item", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewChecklist) {
        AlertDialog(
            onDismissRequest = { showNewChecklist = false },
            title = { Text("New Checklist") },
            text = {
                OutlinedTextField(
                    value = newChecklistTitle,
                    onValueChange = { newChecklistTitle = it },
                    label = { Text("Checklist Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddChecklist(newChecklistTitle)
                        newChecklistTitle = ""
                        showNewChecklist = false
                    },
                    enabled = newChecklistTitle.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewChecklist = false }) { Text("Cancel") }
            }
        )
    }

    addItemTargetChecklistId?.let { cid ->
        AlertDialog(
            onDismissRequest = { addItemTargetChecklistId = null },
            title = { Text("Add Checklist Item") },
            text = {
                OutlinedTextField(
                    value = newItemContent,
                    onValueChange = { newItemContent = it },
                    label = { Text("Task description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAddItem(cid, newItemContent)
                        addItemTargetChecklistId = null
                    },
                    enabled = newItemContent.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { addItemTargetChecklistId = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun IssueFeedSection(
    items: List<FeedItemDto>,
    counts: FeedCountsDto,
    selectedFilter: FeedFilter,
    currentUserId: String,
    highlightedCommentId: Int? = null,
    targetCommentId: Int? = null,
    hasMore: Boolean,
    onSelectFilter: (FeedFilter) -> Unit,
    onLoadMore: () -> Unit,
    onReply: (commentId: Int, preview: String) -> Unit,
    onEditComment: (commentId: Int, body: String) -> Unit,
    onDeleteComment: (commentId: Int) -> Unit,
    onToggleReaction: (commentId: Int, emoji: String, reacted: Boolean) -> Unit
) {
    var editingComment by remember { mutableStateOf<Pair<Int, String>?>(null) }
    var reactionPickerCommentId by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Activity & Discussion", style = MaterialTheme.typography.titleMedium)

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(FeedFilter.entries) { filter ->
                val count = when (filter) {
                    FeedFilter.ALL -> counts.all
                    FeedFilter.COMMENTS -> counts.comments
                    FeedFilter.HISTORY -> counts.history
                    FeedFilter.WORKLOG -> counts.worklog
                }
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onSelectFilter(filter) },
                    label = { Text("${filter.label} ($count)") }
                )
            }
        }

        if (items.isEmpty()) {
            Text(
                text = "No activity yet. Start a discussion below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val commentById = remember(items) { items.associateBy { it.id } }
            items.forEach { item ->
                when (item.kind.lowercase()) {
                    "comment" -> {
                        val cid = item.id
                        val bringIntoViewRequester = remember(cid) { androidx.compose.foundation.relocation.BringIntoViewRequester() }
                        LaunchedEffect(targetCommentId, cid) {
                            if (targetCommentId == cid) bringIntoViewRequester.bringIntoView()
                        }
                        val isHighlighted = highlightedCommentId == cid
                        val parentComment = item.replyToId?.let { commentById[it] }
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                if (isHighlighted) 2.dp else 1.dp,
                                if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.bringIntoViewRequester(bringIntoViewRequester)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    UserAvatar(
                                        name = item.actorName ?: "User",
                                        size = 26.dp
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.actorName ?: "Member", style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            formatRelativeTime(item.createdAt),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onReply(cid, item.body.orEmpty().take(60)) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = "Reply", modifier = Modifier.size(17.dp))
                                    }
                                    IconButton(
                                        onClick = { reactionPickerCommentId = cid },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Rounded.AddReaction, contentDescription = "Add reaction", modifier = Modifier.size(17.dp))
                                    }
                                    if (item.actorUserId == currentUserId) {
                                        IconButton(
                                            onClick = { editingComment = cid to item.body.orEmpty() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Rounded.Edit, contentDescription = "Edit comment", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = { onDeleteComment(cid) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Rounded.Delete, contentDescription = "Delete comment", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                if (parentComment != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                                    ) {
                                        Text(
                                            text = "Replying to ${parentComment.actorName ?: "comment"}: ${parentComment.body.orEmpty()}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }

                                com.example.ui.components.ThemedMarkdownText(
                                    markdown = item.body.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    linkColor = MaterialTheme.colorScheme.primary,
                                    isTextSelectable = true
                                )

                                if (item.reactions.isNotEmpty()) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        item.reactions.forEach { reaction ->
                                            val reacted = reaction.users.any { it.id == currentUserId }
                                            Surface(
                                                onClick = { onToggleReaction(cid, reaction.emoji, reacted) },
                                                shape = RoundedCornerShape(999.dp),
                                                color = if (reacted) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (reacted) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant
                                                )
                                            ) {
                                                Text(
                                                    text = "${reaction.emoji} ${reaction.users.size}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        val isWorklog = item.action?.contains("worklog", ignoreCase = true) == true
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                if (isWorklog) Icons.Rounded.Schedule else Icons.Rounded.History,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isWorklog) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${item.actorName ?: "System"} ${item.action?.replace('_', ' ') ?: "updated issue"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(formatRelativeTime(item.createdAt), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (hasMore) {
                OutlinedButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) {
                    Text("Load Earlier Activity")
                }
            }
        }
    }

    editingComment?.let { (cid, initialBody) ->
        var editedText by remember(cid) { mutableStateOf(initialBody) }
        AlertDialog(
            onDismissRequest = { editingComment = null },
            title = { Text("Edit Comment") },
            text = {
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { onEditComment(cid, editedText); editingComment = null },
                    enabled = editedText.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingComment = null }) { Text("Cancel") }
            }
        )
    }

    reactionPickerCommentId?.let { cid ->
        AlertDialog(
            onDismissRequest = { reactionPickerCommentId = null },
            title = { Text("React to Comment") },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QUICK_EMOJIS.forEach { emoji ->
                        Surface(
                            onClick = {
                                onToggleReaction(cid, emoji, false)
                                reactionPickerCommentId = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(emoji, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(10.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { reactionPickerCommentId = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun CommentComposerBar(
    replyPreview: String?,
    onCancelReply: () -> Unit,
    assignees: List<AssigneeCandidateDto>,
    isSubmitting: Boolean,
    issueId: Int,
    draftStore: com.example.data.local.DraftStore,
    modifier: Modifier = Modifier,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var showMentions by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    LaunchedEffect(issueId) {
        text = draftStore.loadCommentDraft(issueId)
    }

    LaunchedEffect(text, issueId) {
        draftStore.saveCommentDraft(issueId, text)
    }

    Surface(
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (replyPreview != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Replying to: $replyPreview",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Cancel reply", modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (showMentions && assignees.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(assignees.filter { !it.username.isNullOrBlank() }.take(10), key = { it.userId }) { user ->
                        SuggestionChip(
                            onClick = {
                                val handle = "@${user.username} "
                                text = if (text.isEmpty() || text.endsWith(" ")) text + handle else "$text $handle"
                                showMentions = false
                            },
                            label = { Text("@${user.username}") }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TextButton(
                    onClick = { showMentions = !showMentions },
                    modifier = Modifier.width(42.dp)
                ) {
                    Text("@", fontWeight = FontWeight.Bold)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("Write a comment…") },
                    maxLines = 4,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_composer_input")
                )
                IconButton(
                    onClick = { showPreview = !showPreview },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.testTag("comment_preview_button")
                ) {
                    Icon(
                        imageVector = if (showPreview) Icons.Rounded.Edit else Icons.Rounded.Visibility,
                        contentDescription = if (showPreview) "Edit comment" else "Preview comment",
                        tint = if (showPreview) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = {
                        val trimmed = text.trim()
                        if (trimmed.isNotEmpty()) {
                            onSend(trimmed)
                            text = ""
                            showPreview = false
                        }
                    },
                    enabled = text.isNotBlank() && !isSubmitting,
                    modifier = Modifier.testTag("comment_send_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.AutoMirrored.Rounded.Send,
                            contentDescription = "Send comment",
                            tint = if (text.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showPreview && text.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Box(modifier = Modifier.padding(12.dp)) {
                        com.example.ui.components.ThemedMarkdownText(
                            markdown = text,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditTitleDescriptionDialog(
    initialTitle: String,
    initialDescription: String,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var desc by remember { mutableStateOf(initialDescription) }
    var showPreview by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Issue")
                TextButton(onClick = { showPreview = !showPreview }) {
                    Text(if (showPreview) "Edit" else "Preview")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showPreview) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            if (desc.isBlank()) {
                                Text(
                                    text = "Nothing to preview.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                com.example.ui.components.ThemedMarkdownText(
                                    markdown = desc,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text("Description (Markdown)") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, desc) }, enabled = title.isNotBlank()) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MetadataPickerDialog(
    picker: MetadataPicker,
    issue: IssueDetailDto,
    scaffold: ProjectBoardResponse,
    cycles: List<CycleDto>,
    onDismiss: () -> Unit,
    onApplyPatch: (Map<String, Any?>) -> Unit
) {
    when (picker) {
        MetadataPicker.STATUS -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Select Status") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        scaffold.columns.sortedBy { it.position }.forEach { col ->
                            TextButton(
                                onClick = { onApplyPatch(mapOf("columnId" to col.id)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(col.name, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
            )
        }

        MetadataPicker.PRIORITY -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Select Priority") },
                text = {
                    Column {
                        ALL_PRIORITIES.forEach { p ->
                            TextButton(
                                onClick = { onApplyPatch(mapOf("priority" to if (p == "none") null else p)) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(priorityLabel(p), modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
            )
        }

        MetadataPicker.ASSIGNEE -> {
            val humans = scaffold.assignees.filter { !it.isAgent }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Select Assignee") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        TextButton(
                            onClick = { onApplyPatch(mapOf("assigneeUserId" to null)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Unassigned", modifier = Modifier.fillMaxWidth()) }
                        humans.forEach { u ->
                            TextButton(
                                onClick = { onApplyPatch(mapOf("assigneeUserId" to u.userId)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(u.name, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
            )
        }

        MetadataPicker.DELEGATE -> {
            val agents = scaffold.assignees.filter { it.isAgent }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Select AI Agent Delegate") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        TextButton(
                            onClick = { onApplyPatch(mapOf("delegateUserId" to null)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("None", modifier = Modifier.fillMaxWidth()) }
                        if (agents.isEmpty()) {
                            Text("No agents configured in this project.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            agents.forEach { a ->
                                TextButton(
                                    onClick = { onApplyPatch(mapOf("delegateUserId" to a.userId)) },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text(a.name, modifier = Modifier.fillMaxWidth()) }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
            )
        }

        MetadataPicker.TYPE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Select Issue Type") },
                text = {
                    Column {
                        scaffold.issueTypes.forEach { t ->
                            TextButton(
                                onClick = { onApplyPatch(mapOf("typeId" to t.id)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(t.name, modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
            )
        }

        MetadataPicker.CYCLE -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Assign to Cycle") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        TextButton(
                            onClick = { onApplyPatch(mapOf("cycleId" to null)) },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("No Cycle (Backlog)", modifier = Modifier.fillMaxWidth()) }
                        cycles.forEach { c ->
                            TextButton(
                                onClick = { onApplyPatch(mapOf("cycleId" to c.id)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("${c.name} (${c.status})", modifier = Modifier.fillMaxWidth()) }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
            )
        }

        MetadataPicker.LABELS -> {
            val selected = remember { mutableStateListOf<Int>().apply { addAll(issue.labelIds) } }
            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Edit Labels") },
                text = {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        scaffold.labels.forEach { label ->
                            val isSelected = selected.contains(label.id)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    if (isSelected) selected.remove(label.id) else selected.add(label.id)
                                },
                                label = { Text(label.name) }
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { onApplyPatch(mapOf("labelIds" to selected.toList())) }) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }

        MetadataPicker.DATES_ESTIMATES -> {
            var startDate by remember { mutableStateOf(issue.startDate?.take(10).orEmpty()) }
            var dueDate by remember { mutableStateOf(issue.dueDate?.take(10).orEmpty()) }
            var points by remember { mutableStateOf(issue.estimatePoints?.toString().orEmpty()) }
            var estMinutes by remember { mutableStateOf(issue.estimateMinutes?.toString().orEmpty()) }

            AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Schedule & Estimates") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = startDate,
                            onValueChange = { startDate = it },
                            label = { Text("Start Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = { Text("Due Date (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = points,
                            onValueChange = { points = it },
                            label = { Text("Story Points") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = estMinutes,
                            onValueChange = { estMinutes = it },
                            label = { Text("Time Estimate (minutes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onApplyPatch(
                                mapOf(
                                    "startDate" to startDate.trim().ifBlank { null },
                                    "dueDate" to dueDate.trim().ifBlank { null },
                                    "estimatePoints" to points.trim().toDoubleOrNull(),
                                    "estimateMinutes" to estMinutes.trim().toIntOrNull()
                                )
                            )
                        }
                    ) { Text("Save") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomFieldEditDialog(
    field: CustomFieldDto,
    currentValue: IssueFieldValueDto?,
    onDismiss: () -> Unit,
    onSave: (value: String?, optionIds: List<Int>?) -> Unit
) {
    val isSelect = field.fieldType in listOf("select", "multiselect", "multi_select")
    val isBool = field.fieldType in listOf("boolean", "checkbox")

    var textVal by remember { mutableStateOf(currentValue?.valueText.orEmpty()) }
    var boolVal by remember { mutableStateOf(currentValue?.valueText.equals("true", ignoreCase = true)) }
    val selectedOpts = remember {
        mutableStateListOf<Int>().apply {
            addAll(currentValue?.optionIds ?: emptyList())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(field.name) },
        text = {
            when {
                isSelect -> {
                    val isMulti = field.fieldType.contains("multi", ignoreCase = true)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        field.options.forEach { opt ->
                            val checked = selectedOpts.contains(opt.id)
                            FilterChip(
                                selected = checked,
                                onClick = {
                                    if (isMulti) {
                                        if (checked) selectedOpts.remove(opt.id) else selectedOpts.add(opt.id)
                                    } else {
                                        selectedOpts.clear()
                                        if (!checked) selectedOpts.add(opt.id)
                                    }
                                },
                                label = { Text(opt.label) }
                            )
                        }
                    }
                }

                isBool -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(field.name)
                        Switch(checked = boolVal, onCheckedChange = { boolVal = it })
                    }
                }

                else -> {
                    OutlinedTextField(
                        value = textVal,
                        onValueChange = { textVal = it },
                        label = { Text(field.fieldType.replaceFirstChar { it.uppercase() }) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        isSelect -> onSave(null, selectedOpts.toList())
                        isBool -> onSave(boolVal.toString(), null)
                        else -> onSave(textVal.trim().ifBlank { null }, null)
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddIssueLinkDialog(
    results: List<IssueSearchHitDto>,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    onLink: (targetId: Int, kind: String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedKind by remember { mutableStateOf("relates") }
    val kinds = listOf("relates", "blocks", "blocked_by", "duplicates")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Link Issue") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    kinds.forEach { k ->
                        FilterChip(
                            selected = selectedKind == k,
                            onClick = { selectedKind = k },
                            label = { Text(k.replace('_', ' ')) }
                        )
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it; onSearch(it) },
                    label = { Text("Search project issues…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    results.take(6).forEach { hit ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onLink(hit.id, selectedKind) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IdentifierChip(hit.identifier)
                            Text(hit.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun AddWorklogDialog(
    onDismiss: () -> Unit,
    onSave: (minutes: Int, spentOn: String, note: String?) -> Unit
) {
    var minutes by remember { mutableStateOf("30") }
    var spentOn by remember { mutableStateOf(todayIsoDate()) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Time") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Duration (minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = spentOn,
                    onValueChange = { spentOn = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val mins = minutes.toIntOrNull() ?: 0
            Button(
                onClick = { onSave(mins, spentOn.trim(), note.ifBlank { null }) },
                enabled = mins > 0 && spentOn.length >= 10
            ) { Text("Log") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun pipelineColor(status: String?): androidx.compose.ui.graphics.Color = when (status) {
    "success" -> com.example.ui.theme.Emerald500
    "failed" -> com.example.ui.theme.Coral500
    "running", "pending" -> com.example.ui.theme.Amber500
    else -> androidx.compose.ui.graphics.Color.Gray
}

@Composable
fun DevelopmentCard(
    links: List<DevelopmentLinkDto>,
    onOpenLink: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.CallMerge, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("Development (${links.size})", style = MaterialTheme.typography.titleSmall)
            }
            if (links.isEmpty()) {
                Text(
                    "No linked pull requests or branches",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                links.forEach { link ->
                    val stateColor = when (link.state) {
                        "merged" -> Violet500
                        "closed" -> Coral500
                        else -> Emerald500
                    }
                    Surface(
                        onClick = { link.url?.let(onOpenLink) },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    if (link.isBranch) Icons.Rounded.AccountTree else Icons.Rounded.CallMerge,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = stateColor
                                )
                                Text(
                                    text = link.displayTitle,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                if (link.draft) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                                        Text("Draft", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = stateColor.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        link.state.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = stateColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = link.repository,
                                style = MonoCodeStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!link.isBranch) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${link.sourceBranch ?: "?"} → ${link.targetBranch}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val checkStatus = link.checkStatus ?: link.pipelineStatus
                                    if (checkStatus != null) {
                                        Icon(
                                            if (checkStatus == "success") Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = "Checks: $checkStatus",
                                            modifier = Modifier.size(14.dp),
                                            tint = pipelineColor(checkStatus)
                                        )
                                        Text(
                                            checkStatus.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = pipelineColor(checkStatus)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

