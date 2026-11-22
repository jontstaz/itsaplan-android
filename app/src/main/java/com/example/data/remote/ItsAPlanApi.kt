package com.example.data.remote

import com.example.data.model.AttachmentDto
import com.example.data.model.AuthConfigResponse
import com.example.data.model.BoardIssuesResponse
import com.example.data.model.ChecklistDto
import com.example.data.model.ChecklistItemDto
import com.example.data.model.CommentReactionDto
import com.example.data.model.CrossProjectIssueHitDto
import com.example.data.model.CycleDto
import com.example.data.model.CyclePageResponse
import com.example.data.model.FeedCountsDto
import com.example.data.model.FeedItemDto
import com.example.data.model.FeedPageResponse
import com.example.data.model.HealthResponse
import com.example.data.model.IssueAgentRunPageDto
import com.example.data.model.IssueDetailDto
import com.example.data.model.IssueLinkDto
import com.example.data.model.IssueResponseDto
import com.example.data.model.IssueSearchHitDto
import com.example.data.model.IssueWatcherDto
import com.example.data.model.LabelDto
import com.example.data.model.MeResponse
import com.example.data.model.MemberPageResponse
import com.example.data.model.NotificationPageResponse
import com.example.data.model.ProjectBoardResponse
import com.example.data.model.ProjectDetailDto
import com.example.data.model.ProjectListItem
import com.example.data.model.StartNextCycleResponse
import com.example.data.model.SyncRevResponse
import com.example.data.model.TeamDetailDto
import com.example.data.model.TeamDto
import com.example.data.model.UnreadCountResponse
import com.example.data.model.WatchStateResponse
import com.example.data.model.WorklogDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ItsAPlanApi {

    // --- Health & Auth ---

    @GET("/")
    suspend fun getHealth(): HealthResponse

    @GET("auth-config")
    suspend fun getAuthConfig(): AuthConfigResponse

    @GET("me")
    suspend fun getMe(): MeResponse

    @POST("api/auth/sign-in/email")
    suspend fun signInEmail(@Body body: RequestBody): Response<ResponseBody>

    @POST("api/auth/sign-in/username")
    suspend fun signInUsername(@Body body: RequestBody): Response<ResponseBody>

    @POST("api/auth/sign-out")
    suspend fun signOut(@Body body: RequestBody): Response<ResponseBody>

    // --- Teams ---

    @GET("teams")
    suspend fun listTeams(): List<TeamDto>

    @GET("teams/{teamId}")
    suspend fun getTeam(@Path("teamId") teamId: Int): TeamDetailDto

    @GET("teams/{teamId}/members")
    suspend fun listTeamMembers(
        @Path("teamId") teamId: Int,
        @Query("search") search: String? = null,
        @Query("kind") kind: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): MemberPageResponse

    @POST("teams/{teamId}/projects")
    suspend fun createProject(
        @Path("teamId") teamId: Int,
        @Body body: RequestBody
    ): ProjectDetailDto

    // --- Projects ---

    @GET("projects")
    suspend fun listProjects(
        @Query("q") query: String? = null,
        @Query("sort") sort: String? = null,
        @Query("teamId") teamId: Int? = null,
        @Query("permissions") permissions: Boolean = true
    ): List<ProjectListItem>

    @GET("projects/{projectKey}")
    suspend fun getProjectBoard(
        @Path("projectKey", encoded = false) projectKey: String
    ): ProjectBoardResponse

    @PATCH("projects/{projectKey}/preferences")
    suspend fun updateProjectPreferences(
        @Path("projectKey", encoded = false) projectKey: String,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @GET("projects/{projectKey}/members")
    suspend fun listProjectMembers(
        @Path("projectKey", encoded = false) projectKey: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50
    ): MemberPageResponse

    // --- Issues ---

    @GET("projects/{projectKey}/issues/board")
    suspend fun getBoardIssues(
        @Path("projectKey", encoded = false) projectKey: String
    ): BoardIssuesResponse

    @GET("projects/{projectKey}/issues/archived")
    suspend fun listArchivedIssues(
        @Path("projectKey", encoded = false) projectKey: String
    ): List<IssueResponseDto>

    @GET("projects/{projectKey}/issues/search")
    suspend fun searchProjectIssues(
        @Path("projectKey", encoded = false) projectKey: String,
        @Query("q") query: String,
        @Query("limit") limit: Int = 25
    ): List<IssueSearchHitDto>

    @GET("issues")
    suspend fun listCrossProjectIssues(
        @Query("assigneeUserId") assigneeUserId: String? = null,
        @Query("delegateUserId") delegateUserId: String? = null,
        @Query("priority") priority: String? = null,
        @Query("stateTypes") stateTypes: String? = null,
        @Query("sort") sort: String = "updated",
        @Query("dir") dir: String = "desc",
        @Query("limit") limit: Int = 100
    ): List<CrossProjectIssueHitDto>

    @GET("issues/search")
    suspend fun searchCrossProjectIssues(
        @Query("q") query: String,
        @Query("limit") limit: Int = 40
    ): List<CrossProjectIssueHitDto>

    @POST("projects/{projectKey}/issues")
    suspend fun createIssue(
        @Path("projectKey", encoded = false) projectKey: String,
        @Body body: RequestBody
    ): IssueResponseDto

    @GET("issues/{issueId}")
    suspend fun getIssueDetail(@Path("issueId") issueId: Int): IssueDetailDto

    @PATCH("issues/{issueId}")
    suspend fun updateIssue(
        @Path("issueId") issueId: Int,
        @Body body: RequestBody
    ): IssueResponseDto

    @POST("issues/{issueId}/archive")
    suspend fun archiveIssue(
        @Path("issueId") issueId: Int,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("issues/{issueId}/restore")
    suspend fun restoreIssue(@Path("issueId") issueId: Int): Response<ResponseBody>

    @DELETE("issues/{issueId}")
    suspend fun deleteIssue(
        @Path("issueId") issueId: Int,
        @Query("subtasks") subtasks: String? = null
    ): Response<ResponseBody>

    @PUT("issues/{issueId}/fields/{fieldId}")
    suspend fun setCustomFieldValue(
        @Path("issueId") issueId: Int,
        @Path("fieldId") fieldId: Int,
        @Body body: RequestBody
    ): Response<ResponseBody>

    // --- Feed, Comments, Reactions ---

    @GET("issues/{issueId}/feed")
    suspend fun getIssueFeed(
        @Path("issueId") issueId: Int,
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null,
        @Query("filter") filter: String? = null,
        @Query("order") order: String = "desc"
    ): FeedPageResponse

    @GET("issues/{issueId}/feed/counts")
    suspend fun getIssueFeedCounts(@Path("issueId") issueId: Int): FeedCountsDto

    @POST("issues/{issueId}/comments")
    suspend fun createComment(
        @Path("issueId") issueId: Int,
        @Body body: RequestBody
    ): FeedItemDto

    @PATCH("comments/{commentId}")
    suspend fun updateComment(
        @Path("commentId") commentId: Int,
        @Body body: RequestBody
    ): FeedItemDto

    @DELETE("comments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: Int): Response<ResponseBody>

    @POST("comments/{commentId}/reactions")
    suspend fun addReaction(
        @Path("commentId") commentId: Int,
        @Body body: RequestBody
    ): List<CommentReactionDto>

    @DELETE("comments/{commentId}/reactions/{emoji}")
    suspend fun removeReaction(
        @Path("commentId") commentId: Int,
        @Path("emoji") emoji: String
    ): List<CommentReactionDto>

    // --- Watchers ---

    @POST("issues/{issueId}/watch")
    suspend fun watchIssue(@Path("issueId") issueId: Int): WatchStateResponse

    @DELETE("issues/{issueId}/watch")
    suspend fun unwatchIssue(@Path("issueId") issueId: Int): WatchStateResponse

    // --- Checklists ---

    @POST("issues/{issueId}/checklists")
    suspend fun createChecklist(
        @Path("issueId") issueId: Int,
        @Body body: RequestBody
    ): ChecklistDto

    @DELETE("checklists/{checklistId}")
    suspend fun deleteChecklist(@Path("checklistId") checklistId: Int): Response<ResponseBody>

    @POST("checklists/{checklistId}/items")
    suspend fun createChecklistItem(
        @Path("checklistId") checklistId: Int,
        @Body body: RequestBody
    ): ChecklistItemDto

    @PATCH("checklists/items/{itemId}")
    suspend fun updateChecklistItem(
        @Path("itemId") itemId: Int,
        @Body body: RequestBody
    ): ChecklistItemDto

    @DELETE("checklists/items/{itemId}")
    suspend fun deleteChecklistItem(@Path("itemId") itemId: Int): Response<ResponseBody>

    // --- Worklogs ---

    @GET("issues/{issueId}/worklogs")
    suspend fun listWorklogs(@Path("issueId") issueId: Int): List<WorklogDto>

    @POST("issues/{issueId}/worklogs")
    suspend fun createWorklog(
        @Path("issueId") issueId: Int,
        @Body body: RequestBody
    ): WorklogDto

    @DELETE("worklogs/{worklogId}")
    suspend fun deleteWorklog(@Path("worklogId") worklogId: Int): Response<ResponseBody>

    // --- Links ---

    @POST("issues/{issueId}/links")
    suspend fun createIssueLink(
        @Path("issueId") issueId: Int,
        @Body body: RequestBody
    ): IssueLinkDto

    @DELETE("issues/{issueId}/links/{linkId}")
    suspend fun deleteIssueLink(
        @Path("issueId") issueId: Int,
        @Path("linkId") linkId: Int
    ): Response<ResponseBody>

    // --- Attachments ---

    @GET("issues/{issueId}/attachments")
    suspend fun listAttachments(@Path("issueId") issueId: Int): List<AttachmentDto>

    @Multipart
    @POST("issues/{issueId}/attachments")
    suspend fun uploadAttachment(
        @Path("issueId") issueId: Int,
        @Part file: MultipartBody.Part
    ): AttachmentDto

    @DELETE("attachments/{publicId}")
    suspend fun deleteAttachment(@Path("publicId") publicId: String): Response<ResponseBody>

    // --- Agent Runs ---

    @GET("issues/{issueId}/agent-runs")
    suspend fun listAgentRuns(
        @Path("issueId") issueId: Int,
        @Query("before") before: String? = null,
        @Query("limit") limit: Int = 20
    ): IssueAgentRunPageDto

    // --- Labels ---

    @POST("projects/{projectKey}/labels")
    suspend fun createLabel(
        @Path("projectKey", encoded = false) projectKey: String,
        @Body body: RequestBody
    ): LabelDto

    @DELETE("projects/{projectKey}/labels/{labelId}")
    suspend fun deleteLabel(
        @Path("projectKey", encoded = false) projectKey: String,
        @Path("labelId") labelId: Int
    ): Response<ResponseBody>

    // --- Cycles ---

    @GET("projects/{projectKey}/cycles")
    suspend fun listCycles(
        @Path("projectKey", encoded = false) projectKey: String,
        @Query("status") status: String? = null
    ): List<CycleDto>

    @GET("projects/{projectKey}/cycles/completed")
    suspend fun listCompletedCycles(
        @Path("projectKey", encoded = false) projectKey: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): CyclePageResponse

    @POST("projects/{projectKey}/cycles")
    suspend fun createCycle(
        @Path("projectKey", encoded = false) projectKey: String,
        @Body body: RequestBody
    ): CycleDto

    @PATCH("cycles/{cycleId}")
    suspend fun updateCycle(
        @Path("cycleId") cycleId: Int,
        @Body body: RequestBody
    ): CycleDto

    @DELETE("cycles/{cycleId}")
    suspend fun deleteCycle(@Path("cycleId") cycleId: Int): Response<ResponseBody>

    @POST("cycles/{cycleId}/finish")
    suspend fun finishCycle(@Path("cycleId") cycleId: Int): CycleDto

    @POST("cycles/{cycleId}/start-next")
    suspend fun startNextCycle(@Path("cycleId") cycleId: Int): StartNextCycleResponse

    @POST("cycles/{cycleId}/transfer")
    suspend fun transferCycleIssues(
        @Path("cycleId") cycleId: Int,
        @Body body: RequestBody
    ): Response<ResponseBody>

    // --- Notifications ---

    @GET("notifications")
    suspend fun listNotifications(
        @Query("limit") limit: Int = 40,
        @Query("cursor") cursor: String? = null,
        @Query("types") types: String? = null,
        @Query("projectId") projectId: Int? = null,
        @Query("includeRead") includeRead: Boolean? = null,
        @Query("includeSnoozed") includeSnoozed: Boolean? = null
    ): NotificationPageResponse

    @GET("notifications/unread")
    suspend fun getUnreadNotificationCount(
        @Query("projectId") projectId: Int? = null
    ): UnreadCountResponse

    @POST("notifications/read-all")
    suspend fun markAllNotificationsRead(@Body body: RequestBody): Response<ResponseBody>

    @POST("notifications/{id}/read")
    suspend fun setNotificationRead(
        @Path("id") id: Int,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @POST("notifications/{id}/snooze")
    suspend fun snoozeNotification(
        @Path("id") id: Int,
        @Body body: RequestBody
    ): Response<ResponseBody>

    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): Response<ResponseBody>

    // --- Live Sync ---

    @GET("sync/rev")
    suspend fun getSyncRevs(@Query("scopes") scopes: String): SyncRevResponse
}
