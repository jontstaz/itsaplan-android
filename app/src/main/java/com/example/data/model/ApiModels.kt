package com.example.data.model

import org.json.JSONObject

// --- Health & Auth ---

data class HealthResponse(
    val name: String? = null,
    val status: String? = null
)

data class AuthConfigResponse(
    val registration: String = "closed",
    val magicLink: Boolean = false,
    val requireEmailVerification: Boolean = false,
    val emailEnabled: Boolean = false,
    val emailPassword: Boolean = true,
    val google: Boolean = false,
    val oidc: Boolean = false,
    val oidcLabel: String? = null,
    val hasUsers: Boolean = true
)

data class MeResponse(
    val authenticated: Boolean = false,
    val user: CurrentUserDto? = null
)

data class CurrentUserDto(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val emailVerified: Boolean = false,
    val image: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val username: String? = null,
    val displayUsername: String? = null,
    val role: String? = null,
    val active: Boolean? = true,
    val timezone: String? = null,
    val isAgent: Boolean = false
)

data class CursorDto(
    val ts: String = "",
    val id: Int = 0
) {
    fun toQueryParam(): String = JSONObject()
        .put("ts", ts)
        .put("id", id)
        .toString()
}

// --- Teams ---

data class TeamDto(
    val id: Int = 0,
    val workspaceId: Int? = null,
    val name: String = "",
    val slug: String? = null,
    val ref: String = "",
    val mcpEnabled: Boolean = false,
    val role: String? = null,
    val via: String? = null,
    val source: String? = null,
    val joinedAt: String? = null,
    val projectCount: Int = 0,
    val memberCount: Int = 0,
    val ownerCount: Int = 0,
    val roleCount: Int = 0,
    val integrationCount: Int = 0,
    val agentCount: Int = 0,
    val skillCount: Int = 0,
    val toolCount: Int = 0,
    val createdAt: String? = null
)

data class TeamDetailDto(
    val id: Int = 0,
    val name: String = "",
    val slug: String? = null,
    val ref: String = "",
    val projectCount: Int = 0,
    val memberCount: Int = 0,
    val agentCount: Int = 0,
    val permissions: Map<String, Any?>? = null
)

data class MemberPageResponse(
    val items: List<MemberRowDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 50,
    val totalPages: Int = 1
)

data class MemberRowDto(
    val userId: String = "",
    val name: String = "",
    val email: String? = null,
    val username: String? = null,
    val image: String? = null,
    val isAgent: Boolean = false,
    val role: String? = null,
    val source: String? = null
)

// --- Projects ---

data class ProjectListItem(
    val id: Int = 0,
    val teamId: Int = 0,
    val teamName: String = "",
    val teamRef: String = "",
    val key: String = "",
    val ref: String = "",
    val name: String = "",
    val description: String? = null,
    val mcpEnabled: Boolean = false,
    val teamMcpEnabled: Boolean = false,
    val initiativesEnabled: Boolean = false,
    val dashboardsEnabled: Boolean = false,
    val documentsEnabled: Boolean = false,
    val notesEnabled: Boolean = false,
    val cyclesEnabled: Boolean = false,
    val subtasksEnabled: Boolean = false,
    val checklistsEnabled: Boolean = false,
    val issueStatsEnabled: Boolean = false,
    val pointsEstimateEnabled: Boolean = false,
    val timeEstimateEnabled: Boolean = false,
    val timeLoggingEnabled: Boolean = false,
    val availableFeatures: List<String> = emptyList(),
    val archivedAt: String? = null,
    val createdAt: String? = null,
    val role: String? = null,
    val via: String? = null,
    val lastActivityAt: String? = null,
    val isFavorite: Boolean = false,
    val isHidden: Boolean = false,
    // PermissionMatrixSchema is a nested JSON object ({ work_items: { read: true, ... }, ... })
    val permissions: Map<String, Any?>? = null
) {
    val routeKey: String get() = ref.ifBlank { key }
}

data class ProjectBoardResponse(
    val project: ProjectDetailDto = ProjectDetailDto(),
    val columns: List<ColumnDto> = emptyList(),
    val issueTypes: List<IssueTypeDto> = emptyList(),
    val labels: List<LabelDto> = emptyList(),
    val labelGroups: List<LabelGroupDto> = emptyList(),
    val assignees: List<AssigneeCandidateDto> = emptyList(),
    val customFields: List<CustomFieldDto> = emptyList(),
    val viewer: ViewerDto? = null,
    val permissions: Map<String, Any?>? = null,
    val sharingEnabled: Boolean = false
)

data class ProjectDetailDto(
    val id: Int = 0,
    val teamId: Int = 0,
    val teamName: String = "",
    val teamRef: String = "",
    val key: String = "",
    val ref: String = "",
    val name: String = "",
    val description: String? = null,
    val initiativesEnabled: Boolean = false,
    val cyclesEnabled: Boolean = false,
    val subtasksEnabled: Boolean = false,
    val checklistsEnabled: Boolean = false,
    val pointsEstimateEnabled: Boolean = false,
    val timeEstimateEnabled: Boolean = false,
    val timeLoggingEnabled: Boolean = false,
    val archivedAt: String? = null
) {
    val routeKey: String get() = ref.ifBlank { key }
}

data class ViewerDto(
    val role: String? = null,
    val teamRole: String? = null,
    val via: String? = null
)

data class ColumnDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val name: String = "",
    val stateType: String = "unstarted",
    val color: String? = null,
    val position: Int = 0,
    val wipLimit: Int? = null,
    val wipMode: String? = null
)

data class IssueTypeDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val name: String = "",
    val icon: String? = null,
    val color: String? = null,
    val isDefault: Boolean = false,
    val position: Int = 0
)

data class LabelGroupDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val name: String = "",
    val color: String? = null
)

data class LabelDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val groupId: Int? = null,
    val name: String = "",
    val color: String? = null
)

data class AssigneeCandidateDto(
    val userId: String = "",
    val name: String = "",
    val email: String? = null,
    val username: String? = null,
    val image: String? = null,
    val kind: String = "member",
    val agentKind: String? = null,
    val restrictedToUserId: String? = null,
    val respondsToMention: Boolean? = null,
    val canReadWorkItems: Boolean = true
) {
    val isAgent: Boolean get() = kind == "agent"
}

data class CustomFieldDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val name: String = "",
    val fieldType: String = "text",
    val position: Int = 0,
    val showOnBoard: Boolean = false,
    val options: List<CustomFieldOptionDto> = emptyList()
)

data class CustomFieldOptionDto(
    val id: Int = 0,
    val fieldId: Int = 0,
    val label: String = "",
    val color: String? = null,
    val position: Int = 0
)

// --- Issues ---

data class BoardIssuesResponse(
    val issues: List<BoardIssueDto> = emptyList()
)

data class IssueInitiativeRef(
    val id: Int = 0,
    val title: String = "",
    val status: String? = null
)

data class IssueCycleRef(
    val id: Int = 0,
    val name: String = "",
    val status: String = "upcoming"
)

data class DevelopmentCheckDto(
    val id: Int = 0,
    val name: String = "",
    val status: String = "pending",
    val url: String? = null,
    val updatedAt: String? = null
)

data class DevelopmentLinkDto(
    val id: Int = 0,
    val provider: String = "github",
    val repository: String = "",
    val kind: String = "pull_request",
    val number: Int? = null,
    val title: String = "",
    val url: String? = null,
    val state: String = "open",
    val draft: Boolean = false,
    val sourceBranch: String? = null,
    val targetBranch: String = "",
    val headSha: String? = null,
    val pipelineStatus: String? = null,
    val pipelineUrl: String? = null,
    val checkStatus: String? = null,
    val checks: List<DevelopmentCheckDto> = emptyList(),
    val updatedAt: String? = null
) {
    val isBranch: Boolean get() = kind == "branch"
    val displayTitle: String get() = if (number != null) "#$number $title" else title
}

data class IssueFieldValueDto(
    val fieldId: Int = 0,
    val name: String? = null,
    val fieldType: String? = null,
    val value: Any? = null,
    val valueEnd: String? = null,
    val optionIds: List<Int> = emptyList()
) {
    val valueText: String? get() = value?.toString()
}

data class BoardIssueLinkSummary(
    val id: Int = 0,
    val relation: String = "",
    val issueId: Int = 0
)

data class BoardIssueDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val columnId: Int = 0,
    val typeId: Int? = null,
    val parentId: Int? = null,
    val sequenceNumber: Int = 0,
    val identifier: String = "",
    val title: String = "",
    val description: String? = null,
    val priority: String? = null,
    val position: Double = 0.0,
    val assigneeUserId: String? = null,
    val delegateUserId: String? = null,
    val estimatePoints: Double? = null,
    val estimateMinutes: Int? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
    val archivedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val labelIds: List<Int> = emptyList(),
    val fieldValues: List<IssueFieldValueDto> = emptyList(),
    val links: List<BoardIssueLinkSummary> = emptyList(),
    val subtaskCount: Int = 0,
    val initiative: IssueInitiativeRef? = null,
    val cycle: IssueCycleRef? = null,
    val loggedMinutes: Int = 0,
    val statusSince: String? = null
) {
    val cycleId: Int? get() = cycle?.id
    val effectivePriority: String get() = priority ?: "none"
}

data class IssueResponseDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val columnId: Int = 0,
    val typeId: Int? = null,
    val parentId: Int? = null,
    val sequenceNumber: Int = 0,
    val identifier: String = "",
    val title: String = "",
    val description: String? = null,
    val priority: String? = null,
    val position: Double = 0.0,
    val assigneeUserId: String? = null,
    val delegateUserId: String? = null,
    val estimatePoints: Double? = null,
    val estimateMinutes: Int? = null,
    val loggedMinutes: Int = 0,
    val startDate: String? = null,
    val dueDate: String? = null,
    val archivedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val labelIds: List<Int> = emptyList(),
    val initiative: IssueInitiativeRef? = null,
    val cycle: IssueCycleRef? = null
)

data class IssueDetailDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val columnId: Int = 0,
    val typeId: Int? = null,
    val parentId: Int? = null,
    val sequenceNumber: Int = 0,
    val identifier: String = "",
    val title: String = "",
    val description: String? = null,
    val priority: String? = null,
    val position: Double = 0.0,
    val assigneeUserId: String? = null,
    val delegateUserId: String? = null,
    val estimatePoints: Double? = null,
    val estimateMinutes: Int? = null,
    val startDate: String? = null,
    val dueDate: String? = null,
    val archivedAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val labelIds: List<Int> = emptyList(),
    val loggedMinutes: Int = 0,
    val initiative: IssueInitiativeRef? = null,
    val cycle: IssueCycleRef? = null,
    val fields: List<IssueFieldValueDto> = emptyList(),
    val links: List<IssueLinkDto> = emptyList(),
    val watchers: List<IssueWatcherDto> = emptyList(),
    val parent: RelatedIssueSummary? = null,
    val subtasks: List<RelatedIssueSummary> = emptyList(),
    val ancestors: List<RelatedIssueSummary> = emptyList(),
    val checklists: List<ChecklistDto> = emptyList(),
    val development: List<DevelopmentLinkDto> = emptyList()
) {
    val effectivePriority: String get() = priority ?: "none"
}

data class RelatedIssueSummary(
    val id: Int = 0,
    val sequenceNumber: Int = 0,
    val identifier: String = "",
    val title: String = "",
    val columnId: Int? = null,
    val typeId: Int? = null,
    val archived: Boolean = false
)

data class IssueLinkDto(
    val id: Int = 0,
    val kind: String = "relates",
    val direction: String? = null,
    val issue: RelatedIssueSummary? = null
) {
    val displayRelation: String
        get() = when {
            kind == "blocks" && direction == "inward" -> "blocked by"
            kind == "duplicates" && direction == "inward" -> "duplicated by"
            else -> kind.replace('_', ' ')
        }
}

data class IssueWatcherDto(
    val userId: String = "",
    val name: String = "",
    val image: String? = null
)

data class WatchStateResponse(
    val ok: Boolean = true
)

data class ChecklistDto(
    val id: Int = 0,
    val title: String = "",
    val position: Double = 0.0,
    val items: List<ChecklistItemDto> = emptyList()
)

data class ChecklistItemDto(
    val id: Int = 0,
    val content: String = "",
    val done: Boolean = false,
    val position: Double = 0.0
)

data class IssueSearchHitDto(
    val id: Int = 0,
    val sequenceNumber: Int = 0,
    val identifier: String = "",
    val title: String = "",
    val columnId: Int = 0,
    val priority: String? = null,
    val assigneeUserId: String? = null,
    val delegateUserId: String? = null,
    val dueDate: String? = null,
    val updatedAt: String? = null,
    val archived: Boolean = false
)

data class CrossProjectIssueHitDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val projectKey: String = "",
    val projectRef: String = "",
    val projectName: String = "",
    val teamName: String? = null,
    val sequenceNumber: Int = 0,
    val identifier: String = "",
    val title: String = "",
    val priority: String? = null,
    val columnId: Int = 0,
    val assigneeUserId: String? = null,
    val delegateUserId: String? = null,
    val dueDate: String? = null,
    val updatedAt: String? = null,
    val archived: Boolean = false
) {
    val effectivePriority: String get() = priority ?: "none"
}

// --- Feed, Comments, Reactions, Worklogs, Attachments, Agent Runs ---

data class FeedPageResponse(
    val items: List<FeedItemDto> = emptyList(),
    val nextCursor: CursorDto? = null
)

data class FeedCountsDto(
    val all: Int = 0,
    val comments: Int = 0,
    val history: Int = 0,
    val worklog: Int = 0
)

data class ReactionUserDto(
    val id: String = "",
    val name: String = ""
)

data class CommentReactionDto(
    val emoji: String = "",
    val users: List<ReactionUserDto> = emptyList()
)

data class FeedItemDto(
    val id: Int = 0,
    val issueId: Int = 0,
    val kind: String = "comment",
    val replyToId: Int? = null,
    val actorUserId: String? = null,
    val actorName: String? = null,
    val body: String? = null,
    val action: String? = null,
    val payload: Map<String, Any?>? = null,
    val createdAt: String? = null,
    val editedAt: String? = null,
    val reactions: List<CommentReactionDto> = emptyList()
)

data class WorklogDto(
    val id: Int = 0,
    val issueId: Int = 0,
    val userId: String = "",
    val userName: String? = null,
    val userImage: String? = null,
    val minutes: Int = 0,
    val spentOn: String = "",
    val note: String? = null,
    val createdAt: String? = null
)

data class AttachmentDto(
    val id: String = "",
    val filename: String = "",
    val contentType: String = "application/octet-stream",
    val sizeBytes: Long = 0,
    val createdAt: String? = null,
    val url: String = ""
) {
    val isImage: Boolean get() = contentType.startsWith("image/", ignoreCase = true)
}

data class IssueAgentRunPageDto(
    val items: List<AgentRunDto> = emptyList(),
    val nextCursor: Int? = null
)

data class AgentRunDto(
    val id: Int = 0,
    val agentId: Int = 0,
    val username: String = "",
    val kind: String = "",
    val status: String = "queued",
    val executionState: String? = null,
    val trigger: String? = null,
    val prompt: String? = null,
    val output: String? = null,
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAt: String? = null,
    val startedAt: String? = null,
    val finishedAt: String? = null
)

// --- Cycles ---

data class CycleProgressDto(
    val completed: Int = 0,
    val canceled: Int = 0,
    val total: Int = 0
)

data class CycleDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val name: String = "",
    val goal: String? = null,
    val startDate: String = "",
    val endDate: String = "",
    val status: String = "upcoming",
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val completedAt: String? = null,
    val progress: CycleProgressDto = CycleProgressDto()
)

data class StartNextCycleResponse(
    val cycle: CycleDto = CycleDto(),
    val moved: Int = 0
)

data class CyclePageResponse(
    val items: List<CycleDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 20,
    val totalPages: Int = 1
)

data class InitiativeProgressDto(
    val completed: Int = 0,
    val canceled: Int = 0,
    val total: Int = 0
)

data class InitiativeDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val title: String = "",
    val description: String = "",
    val status: String = "proposed",
    val ownerUserId: String? = null,
    val priority: String? = null,
    val startDate: String? = null,
    val targetDate: String? = null,
    val position: Double = 0.0,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val labelIds: List<Int> = emptyList(),
    val progress: InitiativeProgressDto = InitiativeProgressDto(),
    val health: String? = null,
    val favorite: Boolean = false
) {
    /** Share of linked issues that are done or dropped. */
    val progressFraction: Float
        get() = if (progress.total <= 0) 0f
        else (progress.completed + progress.canceled).toFloat() / progress.total
}

data class InitiativePageResponse(
    val items: List<InitiativeDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val pageSize: Int = 25
)

// --- Notifications ---

data class NotificationPageResponse(
    val items: List<NotificationDto> = emptyList(),
    val nextCursor: CursorDto? = null
)

data class UnreadCountResponse(
    val unread: Int = 0
)

data class NotificationDto(
    val id: Int = 0,
    val type: String = "",
    val actorUserId: String? = null,
    val actorName: String? = null,
    val readAt: String? = null,
    val snoozedUntil: String? = null,
    val createdAt: String? = null,
    val issueId: Int? = null,
    val issueSeq: Int? = null,
    val issueTitle: String? = null,
    val issueStateType: String? = null,
    val issueCommentId: Int? = null,
    val documentId: Int? = null,
    val documentTitle: String? = null,
    val projectId: Int = 0,
    val projectKey: String = "",
    val projectName: String = "",
    val fromState: String? = null,
    val toState: String? = null,
    val reactionEmoji: String? = null
) {
    val isRead: Boolean get() = !readAt.isNullOrBlank()
    val isSnoozed: Boolean get() = !snoozedUntil.isNullOrBlank()

    val issueIdentifier: String?
        get() = if (projectKey.isNotBlank() && issueSeq != null) "$projectKey-$issueSeq" else null

    val summaryText: String
        get() {
            val actor = actorName ?: "Someone"
            return when (type) {
                "assigned" -> "$actor assigned you"
                "mentioned" -> "$actor mentioned you"
                "commented" -> "$actor commented"
                "state_changed" -> {
                    val transition = listOfNotNull(fromState, toState).joinToString(" → ")
                    if (transition.isNotBlank()) "$actor moved status: $transition" else "$actor updated status"
                }
                "reacted" -> "$actor reacted ${reactionEmoji.orEmpty()}".trim()
                else -> "$actor · ${type.replace('_', ' ')}"
            }
        }
}

// --- Live Sync ---

data class SyncRevResponse(
    val revs: Map<String, String> = emptyMap()
)

// --- Docs ---

data class DocumentSummaryDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val parentId: Int? = null,
    val title: String = "",
    val icon: String? = null,
    val isPrivate: Boolean = false,
    val isLocked: Boolean = false,
    val isFavorite: Boolean = false,
    val archivedAt: String? = null,
    val position: Double = 0.0,
    val version: Int = 1,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

data class DocumentDto(
    val id: Int = 0,
    val projectId: Int = 0,
    val parentId: Int? = null,
    val title: String = "",
    val icon: String? = null,
    val isPrivate: Boolean = false,
    val isLocked: Boolean = false,
    val isFavorite: Boolean = false,
    val archivedAt: String? = null,
    val position: Double = 0.0,
    val version: Int = 1,
    val content: String = "",
    val createdAt: String? = null,
    val updatedAt: String? = null
)

// --- Error ---

data class ApiErrorBody(
    val error: String? = null,
    val message: String? = null,
    val code: String? = null
)
