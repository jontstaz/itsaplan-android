package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DocumentSummaryDto
import com.example.data.model.ProjectListItem
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorBanner
import com.example.ui.viewmodel.DocsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsScreen(
    selectedProject: ProjectListItem?,
    viewModel: DocsViewModel,
    onShowMessage: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(selectedProject?.routeKey) {
        selectedProject?.let { viewModel.loadDocuments(it.routeKey) }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            onShowMessage(it)
            viewModel.clearMessage()
        }
    }

    if (state.activeDocument != null) {
        DocumentDetailScreen(
            viewModel = viewModel,
            onBack = { viewModel.closeDocument() }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Docs") })
        },
        floatingActionButton = {
            if (selectedProject != null) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "New document")
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                selectedProject == null -> {
                    EmptyStateView(
                        icon = Icons.Rounded.Description,
                        title = "No project selected",
                        subtitle = "Select a project to view its docs."
                    )
                }

                state.isLoadingList && state.documents.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                !state.listError.isNullOrBlank() -> {
                    Box(modifier = Modifier.padding(16.dp)) {
                        ErrorBanner(
                            message = state.listError ?: "Failed to load documents.",
                            onRetry = { viewModel.loadDocuments(selectedProject.routeKey) }
                        )
                    }
                }

                state.documents.isEmpty() -> {
                    EmptyStateView(
                        icon = Icons.Rounded.Description,
                        title = "No documents yet",
                        subtitle = "Create the first doc for this project.",
                        actionLabel = "New Document",
                        onAction = { showCreateDialog = true }
                    )
                }

                else -> {
                    val topLevel = state.documents.filter { it.parentId == null }
                    val childrenOf = state.documents.groupBy { it.parentId }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topLevel.forEach { parent ->
                            item(key = parent.id) {
                                DocumentRow(
                                    doc = parent,
                                    depth = 0,
                                    onClick = { viewModel.openDocument(parent.id) }
                                )
                            }
                            val kids = childrenOf[parent.id].orEmpty()
                            items(kids, key = { it.id }) { child ->
                                DocumentRow(
                                    doc = child,
                                    depth = 1,
                                    onClick = { viewModel.openDocument(child.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog && selectedProject != null) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Document") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createDocument(title) { viewModel.openDocument(it) }
                        showCreateDialog = false
                    },
                    enabled = title.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DocumentRow(
    doc: DocumentSummaryDto,
    depth: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(start = (depth * 20).dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = doc.title.ifBlank { "Untitled" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (doc.isLocked) {
                Icon(Icons.Rounded.Lock, contentDescription = "Locked", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (doc.isPrivate) {
                Icon(Icons.Rounded.Visibility, contentDescription = "Private", modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
