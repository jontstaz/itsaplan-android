package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.DocumentDto
import com.example.data.model.DocumentSummaryDto
import com.example.data.repository.ItsAPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocsUiState(
    val projectRef: String = "",
    val documents: List<DocumentSummaryDto> = emptyList(),
    val isLoadingList: Boolean = false,
    val listError: String? = null,
    val activeDocument: DocumentDto? = null,
    val isLoadingDocument: Boolean = false,
    val documentError: String? = null,
    val isSaving: Boolean = false,
    val message: String? = null
)

class DocsViewModel(
    private val repository: ItsAPlanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DocsUiState())
    val uiState: StateFlow<DocsUiState> = _uiState.asStateFlow()

    fun loadDocuments(projectRef: String) {
        if (projectRef.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(projectRef = projectRef, isLoadingList = true, listError = null) }
            repository.listDocuments(projectRef).fold(
                onSuccess = { docs ->
                    _uiState.update { it.copy(documents = docs, isLoadingList = false) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(isLoadingList = false, listError = err.message ?: "Failed to load docs.")
                    }
                }
            )
        }
    }

    fun openDocument(documentId: Int) {
        val projectRef = _uiState.value.projectRef
        if (projectRef.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDocument = true, documentError = null) }
            repository.getDocument(projectRef, documentId).fold(
                onSuccess = { doc ->
                    _uiState.update { it.copy(activeDocument = doc, isLoadingDocument = false) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingDocument = false,
                            documentError = err.message ?: "Failed to load document."
                        )
                    }
                }
            )
        }
    }

    fun closeDocument() {
        _uiState.update { it.copy(activeDocument = null, documentError = null) }
    }

    fun createDocument(title: String, parentId: Int? = null, onCreated: (Int) -> Unit = {}) {
        val projectRef = _uiState.value.projectRef
        if (projectRef.isBlank() || title.isBlank()) return
        viewModelScope.launch {
            repository.createDocument(projectRef, title, parentId = parentId).fold(
                onSuccess = { doc ->
                    postMessage("Document created")
                    loadDocuments(projectRef)
                    onCreated(doc.id)
                },
                onFailure = { err -> postMessage(err.message ?: "Could not create document.") }
            )
        }
    }

    fun saveDocument(title: String, content: String) {
        val projectRef = _uiState.value.projectRef
        val doc = _uiState.value.activeDocument ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.updateDocument(
                projectRef = projectRef,
                documentId = doc.id,
                version = doc.version,
                title = title.takeIf { it != doc.title },
                content = content.takeIf { it != doc.content }
            ).fold(
                onSuccess = { updated ->
                    _uiState.update { it.copy(activeDocument = updated, isSaving = false) }
                    postMessage("Document saved")
                    loadDocuments(projectRef)
                },
                onFailure = { err ->
                    _uiState.update { it.copy(isSaving = false) }
                    postMessage(err.message ?: "Could not save document.")
                }
            )
        }
    }

    fun archiveDocument(onDone: () -> Unit = {}) {
        val projectRef = _uiState.value.projectRef
        val doc = _uiState.value.activeDocument ?: return
        viewModelScope.launch {
            repository.archiveDocument(projectRef, doc.id, doc.version).fold(
                onSuccess = {
                    postMessage("Document archived")
                    closeDocument()
                    loadDocuments(projectRef)
                    onDone()
                },
                onFailure = { err -> postMessage(err.message ?: "Could not archive document.") }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    private fun postMessage(msg: String) {
        _uiState.update { it.copy(message = msg) }
    }
}
