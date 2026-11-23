package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.example.data.model.AgentRunDto
import com.example.data.model.AttachmentDto
import com.example.data.model.CurrentUserDto
import com.example.data.model.CustomFieldDto
import com.example.data.model.IssueDetailDto
import com.example.data.model.ProjectBoardResponse
import com.example.data.model.WorklogDto
import com.example.ui.components.ErrorBanner
import com.example.ui.components.IdentifierChip
import com.example.ui.components.LabelPill
import com.example.ui.components.PriorityBadge
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatMinutes
import com.example.ui.components.formatRelativeTime
import com.example.ui.components.formatShortDate
import com.example.ui.components.priorityLabel
import com.example.ui.components.stateTypeColor
import com.example.ui.components.stateTypeIcon
import com.example.ui.components.todayIsoDate
import com.example.ui.theme.Amber500
import com.example.ui.theme.Coral500
import com.example.ui.theme.Emerald500
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.Violet500
import com.example.ui.viewmodel.IssueDetailViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IssueDetailScreen(
    issueId: Int,
    currentUser: CurrentUserDto,
    knownBoard: ProjectBoardResponse?,
    knownProjectRef: String?,
    viewModel: IssueDetailViewModel,
    onBack: () -> Unit,
    onNavigateToIssue: (Int) -> Unit,
    onBoardDirty: () -> Unit,
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null,
    serverBaseUrl: String = "",
    targetCommentId: Int? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var highlightedCommentId by remember(targetCommentId) { mutableStateOf(targetCommentId) }

    LaunchedEffect(targetCommentId, state.feedItems) {
        if (targetCommentId != null && state.feedItems.any { it.id == targetCommentId }) {
            kotlinx.coroutines.delay(3500)
            highlightedCommentId = null
        }
    }

    BackHandler(onBack = onBack)

    LaunchedEffect(issueId) {
        viewModel.loadIssue(issueId, knownBoard, knownProjectRef)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopSync() }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(state.deleted) {
        if (state.deleted) onBack()
    }

    // Zero-permission Photo Picker & Document Picker (Play Policy compliant)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAttachment(it) }
    }

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadAttachment(it) }
    }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteTargetId by remember { mutableStateOf<Int?>(null) }
    var showEditContentDialog by remember { mutableStateOf(false) }
    var activePicker by remember { mutableStateOf<MetadataPicker?>(null) }
    var activeCustomField by remember { mutableStateOf<CustomFieldDto?>(null) }
    var showAddSubtaskDialog by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showAddWorklogDialog by remember { mutableStateOf(false) }
    var replyToCommentId by remember { mutableStateOf<Int?>(null) }
    var replyToPreview by remember { mutableStateOf<String?>(null) }

    val issue = state.issue
    val scaffold = state.boardScaffold
    val isWatching = remember(issue, currentUser.id) {
        issue?.watchers?.any { it.userId == currentUser.id } == true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("issue_back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IdentifierChip(issue?.identifier ?: "#$issueId")
                        if (!issue?.archivedAt.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Amber500.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "Archived",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Amber500,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (onToggleFullscreen != null) {
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.testTag("issue_fullscreen_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit fullscreen" else "Open fullscreen"
                            )
                        }
                    }
                    if (issue != null) {
                        IconButton(
                            onClick = {
                                val webUrl = serverBaseUrl.trimEnd('/') + "/" + (knownProjectRef ?: "") + "/issues/" + issue.identifier
                                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, webUrl)
                                }
                                context.startActivity(android.content.Intent.createChooser(send, "Share issue"))
                            },
                            modifier = Modifier.testTag("issue_share_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share issue")
                        }
                        IconButton(
                            onClick = {
                                val webUrl = serverBaseUrl.trimEnd('/') + "/" + (knownProjectRef ?: "") + "/issues/" + issue.identifier
                                val open = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(webUrl))
                                context.startActivity(open)
                            },
                            modifier = Modifier.testTag("issue_open_browser_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open in browser")
                        }
                        IconButton(
                            onClick = { viewModel.toggleWatch(isWatching) },
                            modifier = Modifier.testTag("issue_watch_button")
                        ) {
                            Icon(
                                imageVector = if (isWatching) Icons.Rounded.NotificationsActive else Icons.Rounded.NotificationsNone,
                                contentDescription = if (isWatching) "Unwatch issue" else "Watch issue",
                                tint = if (isWatching) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.loadIssue(issueId, scaffold, silent = false) }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh issue")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Rounded.MoreVert, contentDescription = "More actions")
                            }
                            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("Edit Title & Description") },
                                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                    onClick = { showMenu = false; showEditContentDialog = true }
                                )
                                val archived = !issue.archivedAt.isNullOrBlank()
                                DropdownMenuItem(
                                    text = { Text(if (archived) "Restore Issue" else "Archive Issue") },
                                    leadingIcon = {
                                        Icon(
                                            if (archived) Icons.Rounded.Restore else Icons.Rounded.Archive,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        viewModel.archiveOrRestore(archived, onBoardDirty)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete Issue", color = Coral500) },
                                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = Coral500) },
                                    onClick = {
                                        showMenu = false
                                        deleteTargetId = issue?.id
                                        showDeleteConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (issue != null) {
                CommentComposerBar(
                    replyPreview = replyToPreview,
                    onCancelReply = { replyToCommentId = null; replyToPreview = null },
                    assignees = scaffold?.assignees.orEmpty(),
                    isSubmitting = state.isMutating,
                    issueId = issueId,
                    draftStore = remember { com.example.data.local.DraftStore(context.applicationContext) },
                    modifier = Modifier.imePadding(),
                    onSend = { body, onSent ->
                        viewModel.postComment(body, replyToCommentId) {
                            replyToCommentId = null
                            replyToPreview = null
                            onSent()
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            when {
                state.isLoading && issue == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                !state.error.isNullOrBlank() && issue == null -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ErrorBanner(
                            message = state.error ?: "Failed to load issue.",
                            onRetry = { viewModel.loadIssue(issueId, scaffold) }
                        )
                    }
                }

                issue != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().widthIn(max = 720.dp),
                        state = rememberSaveable(saver = LazyListState.Saver) { LazyListState() },
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Parent Breadcrumb
                        if (issue.parent != null) {
                            item(key = "parent") {
                                Surface(
                                    onClick = { onNavigateToIssue(issue.parent.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.AccountTree,
                                            contentDescription = "Parent issue",
                                            modifier = Modifier.size(15.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Parent: ${issue.parent.identifier} · ${issue.parent.title}",
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Title & Description Card
                        item(key = "title_desc") {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text(
                                            text = issue.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { showEditContentDialog = true },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.Edit,
                                                contentDescription = "Edit title and description",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    if (!issue.description.isNullOrBlank()) {
                                        com.example.ui.components.ThemedMarkdownText(
                                            markdown = issue.description,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            linkColor = MaterialTheme.colorScheme.primary,
                                            isTextSelectable = true
                                        )
                                    } else {
                                        Text(
                                            text = "No description yet. Tap edit to add details.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.clickable { showEditContentDialog = true }
                                        )
                                    }
                                }
                            }
                        }

                        // Metadata Grid
                        item(key = "metadata") {
                            IssueMetadataCard(
                                issue = issue,
                                scaffold = scaffold,
                                onPick = { activePicker = it }
                            )
                        }

                        // Labels
                        if (scaffold != null) {
                            item(key = "labels") {
                                val labelMap = scaffold.labels.associateBy { it.id }
                                val assigned = issue.labelIds.mapNotNull { labelMap[it] }
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Labels", style = MaterialTheme.typography.titleSmall)
                                            TextButton(onClick = { activePicker = MetadataPicker.LABELS }) {
                                                Text("Edit")
                                            }
                                        }
                                        if (assigned.isEmpty()) {
                                            Text(
                                                "No labels assigned",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                assigned.forEach { LabelPill(it.name, it.color) }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Custom Fields
                        if (scaffold != null && scaffold.customFields.isNotEmpty()) {
                            item(key = "custom_fields") {
                                CustomFieldsCard(
                                    fields = scaffold.customFields,
                                    issue = issue,
                                    onEditField = { activeCustomField = it }
                                )
                            }
                        }

                        // Checklists
                        item(key = "checklists") {
                            ChecklistsCard(
                                checklists = issue.checklists,
                                onAddChecklist = { viewModel.addChecklist(it) },
                                onDeleteChecklist = { viewModel.deleteChecklist(it) },
                                onAddItem = { cid, content -> viewModel.addChecklistItem(cid, content) },
                                onToggleItem = { itemId, done -> viewModel.toggleChecklistItem(itemId, done) },
                                onDeleteItem = { viewModel.deleteChecklistItem(it) }
                            )
                        }

                        // Subtasks
                        item(key = "subtasks") {
                            SubtasksCard(
                                subtasks = issue.subtasks,
                                columns = scaffold?.columns.orEmpty(),
                                onAddSubtask = { showAddSubtaskDialog = true },
                                onOpenSubtask = onNavigateToIssue
                            )
                        }

                        // Linked Issues
                        item(key = "links") {
                            LinkedIssuesCard(
                                links = issue.links,
                                onAddLink = { showAddLinkDialog = true },
                                onDeleteLink = { viewModel.deleteIssueLink(it) },
                                onOpenIssue = onNavigateToIssue
                            )
                        }

                        // Attachments
                        item(key = "attachments") {
                            AttachmentsCard(
                                attachments = state.attachments,
                                isUploading = state.isUploading,
                                resolveUrl = { viewModel.resolveAttachmentUrl(it) },
                                onPickPhoto = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                onPickFile = { documentPickerLauncher.launch(arrayOf("*/*")) },
                                onOpenAttachment = { url ->
                                    if (url.startsWith("http://") || url.startsWith("https://")) {
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    }
                                },
                                onDeleteAttachment = { viewModel.deleteAttachment(it) }
                            )
                        }

                        // Worklogs / Time Tracking
                        item(key = "worklogs") {
                            WorklogsCard(
                                worklogs = state.worklogs,
                                totalLoggedMinutes = issue.loggedMinutes,
                                estimateMinutes = issue.estimateMinutes,
                                onLogTime = { showAddWorklogDialog = true },
                                onDeleteWorklog = { viewModel.deleteWorklog(it) }
                            )
                        }

                        // AI Agent Runs
                        if (state.agentRuns.isNotEmpty()) {
                            item(key = "agent_runs") {
                                AgentRunsCard(runs = state.agentRuns)
                            }
                        }

                        // Development (linked pull requests / branches)
                        if (issue.development.isNotEmpty()) {
                            item(key = "development") {
                                DevelopmentCard(
                                    links = issue.development,
                                    onOpenLink = { url ->
                                        runCatching {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        }
                                    }
                                )
                            }
                        }

                        // Activity & Comments Feed
                        item(key = "feed") {
                            IssueFeedSection(
                                items = state.feedItems,
                                counts = state.feedCounts,
                                selectedFilter = state.feedFilter,
                                currentUserId = currentUser.id,
                                highlightedCommentId = highlightedCommentId,
                                targetCommentId = targetCommentId,
                                hasMore = state.feedNextCursor != null,
                                onSelectFilter = { viewModel.selectFeedFilter(it) },
                                onLoadMore = { viewModel.loadMoreFeed() },
                                onReply = { commentId, preview ->
                                    replyToCommentId = commentId
                                    replyToPreview = preview
                                },
                                onEditComment = { cid, body -> viewModel.editComment(cid, body) },
                                onDeleteComment = { cid -> viewModel.deleteComment(cid) },
                                onToggleReaction = { cid, emoji, reacted ->
                                    viewModel.toggleReaction(cid, emoji, reacted)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs & Sheets
    if (showEditContentDialog && issue != null) {
        EditTitleDescriptionDialog(
            initialTitle = issue.title,
            initialDescription = issue.description.orEmpty(),
            onDismiss = { showEditContentDialog = false },
            onSave = { newTitle, newDesc ->
                viewModel.updateIssue(
                    mapOf("title" to newTitle.trim(), "description" to newDesc.trim().ifBlank { null }),
                    successMsg = "Issue updated",
                    onBoardDirty = onBoardDirty
                )
                showEditContentDialog = false
            }
        )
    }

    if (activePicker != null && issue != null && scaffold != null) {
        MetadataPickerDialog(
            picker = activePicker!!,
            issue = issue,
            scaffold = scaffold,
            cycles = state.projectCycles,
            onDismiss = { activePicker = null },
            onApplyPatch = { patch ->
                viewModel.updateIssue(patch, successMsg = "Updated", onBoardDirty = onBoardDirty)
                activePicker = null
            }
        )
    }

    if (activeCustomField != null && issue != null) {
        val currentVal = issue.fields.firstOrNull { it.fieldId == activeCustomField!!.id }
        CustomFieldEditDialog(
            field = activeCustomField!!,
            currentValue = currentVal,
            onDismiss = { activeCustomField = null },
            onSave = { value, optionIds ->
                viewModel.updateCustomField(activeCustomField!!.id, value = value, optionIds = optionIds)
                activeCustomField = null
            }
        )
    }

    if (showAddSubtaskDialog) {
        var subTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubtaskDialog = false },
            title = { Text("Add Subtask") },
            text = {
                OutlinedTextField(
                    value = subTitle,
                    onValueChange = { subTitle = it },
                    label = { Text("Subtask title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createSubtask(subTitle, onBoardDirty)
                        showAddSubtaskDialog = false
                    },
                    enabled = subTitle.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubtaskDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddLinkDialog) {
        AddIssueLinkDialog(
            results = state.linkSearchResults,
            onSearch = { viewModel.searchLinkCandidates(it) },
            onDismiss = { showAddLinkDialog = false },
            onLink = { targetId, kind ->
                viewModel.addIssueLink(targetId, kind)
                showAddLinkDialog = false
            }
        )
    }

    if (showAddWorklogDialog) {
        AddWorklogDialog(
            onDismiss = { showAddWorklogDialog = false },
            onSave = { minutes, spentOn, note ->
                viewModel.addWorklog(minutes, spentOn, note)
                showAddWorklogDialog = false
            }
        )
    }

    val pendingDeleteId = deleteTargetId
    if (showDeleteConfirm && pendingDeleteId != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete ${issue?.identifier ?: "#$pendingDeleteId"}?") },
            text = { Text("This permanently deletes the issue and its comments. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        deleteTargetId = null
                        viewModel.deleteIssue(pendingDeleteId, onBoardDirty)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Coral500)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    deleteTargetId = null
                }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun IssueMetadataCard(
    issue: IssueDetailDto,
    scaffold: ProjectBoardResponse?,
    onPick: (MetadataPicker) -> Unit
) {
    val column = scaffold?.columns?.firstOrNull { it.id == issue.columnId }
    val issueType = scaffold?.issueTypes?.firstOrNull { it.id == issue.typeId }
    val assignee = scaffold?.assignees?.firstOrNull { it.userId == issue.assigneeUserId }
    val delegate = scaffold?.assignees?.firstOrNull { it.userId == issue.delegateUserId }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MetadataRow(
                label = "Status",
                onClick = { onPick(MetadataPicker.STATUS) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = stateTypeIcon(column?.stateType),
                        contentDescription = null,
                        tint = stateTypeColor(column?.stateType, column?.color),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(column?.name ?: "Unknown", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MetadataRow(
                label = "Priority",
                onClick = { onPick(MetadataPicker.PRIORITY) }
            ) {
                PriorityBadge(issue.priority, showLabel = true)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MetadataRow(
                label = "Assignee",
                onClick = { onPick(MetadataPicker.ASSIGNEE) }
            ) {
                if (assignee != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        UserAvatar(assignee.name, assignee.image, assignee.isAgent, size = 22.dp)
                        Text(assignee.name, style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    Text("Unassigned", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MetadataRow(
                label = "AI Delegate",
                onClick = { onPick(MetadataPicker.DELEGATE) }
            ) {
                if (delegate != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = Violet500, modifier = Modifier.size(16.dp))
                        Text(delegate.name, style = MaterialTheme.typography.bodyMedium, color = Violet500)
                    }
                } else {
                    Text("None", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (scaffold?.issueTypes?.isNotEmpty() == true) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MetadataRow(label = "Type", onClick = { onPick(MetadataPicker.TYPE) }) {
                    Text(issueType?.name ?: "None", style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (scaffold?.project?.cyclesEnabled == true) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                MetadataRow(label = "Cycle", onClick = { onPick(MetadataPicker.CYCLE) }) {
                    Text(issue.cycle?.name ?: "Backlog", style = MaterialTheme.typography.bodyMedium)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            MetadataRow(label = "Due Date", onClick = { onPick(MetadataPicker.DATES_ESTIMATES) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = issue.dueDate?.let { formatShortDate(it) } ?: "Set date / estimate",
                        style = if (issue.dueDate != null) MonoCodeStyle else MaterialTheme.typography.bodyMedium,
                        color = if (issue.dueDate != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    issue.estimatePoints?.let { pts ->
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                            Text("${pts}pt", style = MonoCodeStyle, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    onClick: () -> Unit,
    valueContent: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        valueContent()
    }
}

@Composable
private fun CustomFieldsCard(
    fields: List<CustomFieldDto>,
    issue: IssueDetailDto,
    onEditField: (CustomFieldDto) -> Unit
) {
    val valueMap = issue.fields.associateBy { it.fieldId }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Custom Fields", style = MaterialTheme.typography.titleSmall)
            fields.sortedBy { it.position }.forEach { field ->
                val fv = valueMap[field.id]
                val display = when {
                    fv == null -> "Not set"
                    fv.optionIds.isNotEmpty() -> field.options.filter { fv.optionIds.contains(it.id) }.joinToString(", ") { it.label }.ifBlank { "Set" }
                    !fv.valueText.isNullOrBlank() -> if (!fv.valueEnd.isNullOrBlank()) "${fv.valueText} → ${fv.valueEnd}" else fv.valueText!!
                    else -> "Not set"
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditField(field) }
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(field.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(display, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SubtasksCard(
    subtasks: List<com.example.data.model.RelatedIssueSummary>,
    columns: List<com.example.data.model.ColumnDto>,
    onAddSubtask: () -> Unit,
    onOpenSubtask: (Int) -> Unit
) {
    val colMap = remember(columns) { columns.associateBy { it.id } }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Subtasks (${subtasks.size})", style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onAddSubtask) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
            if (subtasks.isEmpty()) {
                Text("No subtasks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                subtasks.forEach { sub ->
                    val col = sub.columnId?.let { colMap[it] }
                    Surface(
                        onClick = { onOpenSubtask(sub.id) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                stateTypeIcon(col?.stateType),
                                contentDescription = null,
                                tint = stateTypeColor(col?.stateType, col?.color),
                                modifier = Modifier.size(16.dp)
                            )
                            IdentifierChip(sub.identifier)
                            Text(
                                sub.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkedIssuesCard(
    links: List<com.example.data.model.IssueLinkDto>,
    onAddLink: () -> Unit,
    onDeleteLink: (Int) -> Unit,
    onOpenIssue: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Linked Issues (${links.size})", style = MaterialTheme.typography.titleSmall)
                }
                TextButton(onClick = onAddLink) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Link")
                }
            }
            if (links.isEmpty()) {
                Text("No linked issues", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                links.forEach { link ->
                    val target = link.issue
                    if (target != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenIssue(target.id) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = link.displayRelation,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            IdentifierChip(target.identifier)
                            Text(
                                target.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { onDeleteLink(link.id) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Unlink", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentsCard(
    attachments: List<AttachmentDto>,
    isUploading: Boolean,
    resolveUrl: (String?) -> String?,
    onPickPhoto: () -> Unit,
    onPickFile: () -> Unit,
    onOpenAttachment: (String) -> Unit,
    onDeleteAttachment: (String) -> Unit
) {
    var previewImageUrl by remember { mutableStateOf<String?>(null) }
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
                    Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("Attachments (${attachments.size})", style = MaterialTheme.typography.titleSmall)
                }
                if (isUploading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onPickPhoto) {
                            Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Photo")
                        }
                        TextButton(onClick = onPickFile) {
                            Icon(Icons.Rounded.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("File")
                        }
                    }
                }
            }
            if (attachments.isEmpty()) {
                Text("No files attached", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                attachments.forEach { att ->
                    val url = resolveUrl(att.url.ifBlank { "/attachments/${att.id}/raw" })
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (att.isImage && url != null) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = att.filename,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { previewImageUrl = url }
                                )
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(att.filename, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val kb = (att.sizeBytes / 1024L).coerceAtLeast(1)
                                Text("${kb} KB · ${att.contentType}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (url != null) {
                                IconButton(onClick = { onOpenAttachment(url) }) {
                                    Icon(Icons.Rounded.OpenInNew, contentDescription = "Open attachment", modifier = Modifier.size(18.dp))
                                }
                            }
                            IconButton(onClick = { onDeleteAttachment(att.id) }) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete attachment", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    if (previewImageUrl != null) {
        ImageAttachmentViewer(
            url = previewImageUrl!!,
            onDismiss = { previewImageUrl = null }
        )
    }
}

@Composable
private fun ImageAttachmentViewer(url: String, onDismiss: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = "Attachment preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale > 1f) offset + pan else androidx.compose.ui.geometry.Offset.Zero
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .testTag("image_viewer_close")
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close preview", tint = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

@Composable
private fun WorklogsCard(
    worklogs: List<WorklogDto>,
    totalLoggedMinutes: Int,
    estimateMinutes: Int?,
    onLogTime: () -> Unit,
    onDeleteWorklog: (Int) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    val summary = if (estimateMinutes != null && estimateMinutes > 0) {
                        "${formatMinutes(totalLoggedMinutes)} / ${formatMinutes(estimateMinutes)}"
                    } else {
                        formatMinutes(totalLoggedMinutes)
                    }
                    Text("Time Tracking ($summary)", style = MaterialTheme.typography.titleSmall)
                }
                TextButton(onClick = onLogTime) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Log Time")
                }
            }
            if (worklogs.isEmpty()) {
                Text("No time logged yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                worklogs.forEach { wl ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                text = formatMinutes(wl.minutes),
                                style = MonoCodeStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(wl.note?.ifBlank { null } ?: "Work logged", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${wl.userName ?: "Member"} · ${formatShortDate(wl.spentOn)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onDeleteWorklog(wl.id) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Delete worklog", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentRunsCard(runs: List<AgentRunDto>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Violet500.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.SmartToy, contentDescription = null, tint = Violet500, modifier = Modifier.size(18.dp))
                Text("AI Agent Runs (${runs.size})", style = MaterialTheme.typography.titleSmall)
            }
            runs.take(5).forEach { run ->
                val statusColor = when (run.status.lowercase()) {
                    "completed", "succeeded" -> Emerald500
                    "failed", "error", "canceled" -> Coral500
                    else -> Amber500
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(run.username.ifBlank { "Agent #${run.agentId}" }, style = MaterialTheme.typography.labelLarge)
                            Surface(shape = RoundedCornerShape(999.dp), color = statusColor.copy(alpha = 0.16f)) {
                                Text(
                                    run.status,
                                    style = MonoCodeStyle,
                                    color = statusColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Trigger: ${run.trigger ?: "manual"} · ${formatRelativeTime(run.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!run.lastError.isNullOrBlank()) {
                            Text(run.lastError, style = MaterialTheme.typography.bodySmall, color = Coral500, maxLines = 3)
                        } else if (!run.output.isNullOrBlank()) {
                            Text(run.output, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
