package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ErrorBanner
import com.example.ui.viewmodel.DocsViewModel
import com.example.ui.components.ThemedMarkdownText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    viewModel: DocsViewModel,
    onBack: () -> Unit,
    serverBaseUrl: String = "",
    isFullscreen: Boolean = false,
    onToggleFullscreen: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val doc = state.activeDocument
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler(onBack = onBack)

    var isEditing by remember(doc?.id) { mutableStateOf(false) }
    var showEditorPreview by remember(doc?.id) { mutableStateOf(false) }
    var editedTitle by remember(doc?.id) { mutableStateOf(doc?.title.orEmpty()) }
    var editedContent by remember(doc?.id) { mutableStateOf(doc?.content.orEmpty()) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    // Captured when the sheet opens, so switching documents behind it cannot redirect
    // the confirmation to a different doc.
    var archiveTarget by remember { mutableStateOf<com.example.data.model.DocumentDto?>(null) }
    var docDraftRestored by remember(doc?.id) { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val draftStore = remember { com.example.data.local.DraftStore(context.applicationContext) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    // Restore draft content when entering edit mode on a doc that has a saved draft.
    LaunchedEffect(doc?.id, isEditing) {
        val id = doc?.id ?: return@LaunchedEffect
        if (isEditing) {
            val draft = draftStore.loadDocDraft(id)
            if (draft.isNotBlank()) editedContent = draft
            docDraftRestored = true
        } else {
            docDraftRestored = false
        }
    }

    // Persist draft while editing, but only once the restore above has run, so the
    // first autosave cannot blank the stored draft.
    LaunchedEffect(editedContent, isEditing, doc?.id, docDraftRestored) {
        val id = doc?.id ?: return@LaunchedEffect
        if (isEditing && docDraftRestored) {
            draftStore.saveDocDraft(id, editedContent)
        }
    }

    // Keep edit buffers in sync whenever a fresh document loads (e.g. after save).
    LaunchedEffect(doc?.version) {
        if (doc != null && !isEditing) {
            editedTitle = doc.title
            editedContent = doc.content
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Text(
                        text = doc?.title?.ifBlank { "Untitled" } ?: "Document",
                        maxLines = 1
                    )
                },
                actions = {
                    if (onToggleFullscreen != null) {
                        IconButton(
                            onClick = onToggleFullscreen,
                            modifier = Modifier.testTag("doc_fullscreen_toggle")
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = if (isFullscreen) "Exit fullscreen" else "Open fullscreen"
                            )
                        }
                    }
                    if (doc != null) {
                        IconButton(
                            onClick = {
                                val webUrl = serverBaseUrl.trimEnd('/') + "/docs/" + doc.id
                                val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(android.content.Intent.EXTRA_TEXT, webUrl)
                                }
                                context.startActivity(android.content.Intent.createChooser(send, "Share document"))
                            },
                            modifier = Modifier.testTag("doc_share_button")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share document")
                        }
                        IconButton(
                            onClick = {
                                val webUrl = serverBaseUrl.trimEnd('/') + "/docs/" + doc.id
                                val open = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(webUrl))
                                context.startActivity(open)
                            },
                            modifier = Modifier.testTag("doc_open_browser_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open in browser")
                        }
                    }
                    if (doc != null && !doc.isLocked) {
                        if (isEditing) {
                            IconButton(
                                onClick = {
                                    // Stay in the editor, draft included, until the server
                                    // accepts the save; otherwise a failure loses the text.
                                    viewModel.saveDocument(editedTitle, editedContent) {
                                        doc.id.let { id ->
                                            scope.launch { draftStore.saveDocDraft(id, "") }
                                        }
                                        isEditing = false
                                    }
                                },
                                enabled = !state.isSaving
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                                } else {
                                    Icon(Icons.Rounded.Check, contentDescription = "Save")
                                }
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit document")
                            }
                            IconButton(onClick = {
                                archiveTarget = doc
                                showArchiveConfirm = true
                            }) {
                                Icon(Icons.Rounded.Archive, contentDescription = "Archive document")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
            contentAlignment = androidx.compose.ui.Alignment.TopCenter
        ) {
            when {
                state.isLoadingDocument && doc == null -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(modifier = Modifier.padding(32.dp))
                    }
                }

                !state.documentError.isNullOrBlank() && doc == null -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ErrorBanner(message = state.documentError ?: "Failed to load document.")
                    }
                }

                doc != null -> {
                    if (isEditing) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 720.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = editedTitle,
                                onValueChange = { editedTitle = it },
                                label = { Text("Title") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { showEditorPreview = !showEditorPreview }) {
                                    Text(if (showEditorPreview) "Edit" else "Preview")
                                }
                            }
                            if (showEditorPreview) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(modifier = Modifier.padding(12.dp)) {
                                        if (editedContent.isBlank()) {
                                            Text(
                                                text = "Nothing to preview.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        } else {
                                            ThemedMarkdownText(
                                                markdown = editedContent,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            )
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = editedContent,
                                    onValueChange = { editedContent = it },
                                    label = { Text("Content (Markdown)") },
                                    minLines = 12,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .widthIn(max = 720.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text(
                                text = doc.title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            if (doc.content.isNotBlank()) {
                                ThemedMarkdownText(
                                    markdown = doc.content,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    linkColor = MaterialTheme.colorScheme.primary,
                                    isTextSelectable = true,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            } else {
                                Text(
                                    text = "This document is empty. Tap edit to add content.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    val pendingArchive = archiveTarget
    if (showArchiveConfirm && pendingArchive != null) {
        AlertDialog(
            onDismissRequest = {
                showArchiveConfirm = false
                archiveTarget = null
            },
            title = { Text("Archive document?") },
            text = { Text("This document and its subpages will be archived. You can restore it later from an archived view.") },
            confirmButton = {
                Button(onClick = {
                    showArchiveConfirm = false
                    archiveTarget = null
                    viewModel.archiveDocument(pendingArchive, onBack)
                }) { Text("Archive") }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
