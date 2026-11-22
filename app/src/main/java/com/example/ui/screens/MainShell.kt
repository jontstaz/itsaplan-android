package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentInd
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewKanban
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AssignmentInd
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewKanban
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import kotlinx.coroutines.launch
import com.example.data.model.CurrentUserDto
import com.example.ui.layout.AppWindowSize
import com.example.ui.layout.TwoPaneListDetail
import com.example.ui.layout.isCompact
import com.example.ui.layout.supportsListDetail
import com.example.ui.layout.usesNavRail
import com.example.ui.viewmodel.AppUiState
import com.example.ui.viewmodel.AppViewModel
import com.example.ui.viewmodel.CyclesViewModel
import com.example.ui.viewmodel.DocsViewModel
import com.example.ui.viewmodel.InboxViewModel
import com.example.ui.viewmodel.IssueDetailViewModel
import com.example.ui.viewmodel.MyIssuesViewModel

enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    BOARD("Board", Icons.Rounded.ViewKanban, Icons.Outlined.ViewKanban, "nav_tab_board"),
    MY_ISSUES("My Issues", Icons.Rounded.AssignmentInd, Icons.Outlined.AssignmentInd, "nav_tab_my_issues"),
    INBOX("Inbox", Icons.Rounded.Inbox, Icons.Outlined.Inbox, "nav_tab_inbox"),
    CYCLES("Cycles", Icons.Rounded.Autorenew, Icons.Outlined.Autorenew, "nav_tab_cycles"),
    DOCS("Docs", Icons.Rounded.Description, Icons.Outlined.Description, "nav_tab_docs"),
    SETTINGS("Settings", Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_tab_settings")
}

private data class OpenIssueEntry(
    val issueId: Int,
    val projectRef: String?,
    val targetCommentId: Int? = null
)

private val OpenIssueStackSaver = androidx.compose.runtime.saveable.Saver<
    androidx.compose.runtime.snapshots.SnapshotStateList<OpenIssueEntry>,
    ArrayList<Any?>
>(
    save = { list ->
        ArrayList(list.flatMap { listOf<Any?>(it.issueId, it.projectRef, it.targetCommentId) })
    },
    restore = { flat ->
        val restored = androidx.compose.runtime.mutableStateListOf<OpenIssueEntry>()
        var i = 0
        while (i + 2 < flat.size) {
            restored.add(OpenIssueEntry(flat[i] as Int, flat[i + 1] as String?, flat[i + 2] as Int?))
            i += 3
        }
        restored
    }
)

@Composable
fun MainShell(
    user: CurrentUserDto,
    appState: AppUiState,
    appViewModel: AppViewModel,
    issueDetailViewModel: IssueDetailViewModel,
    inboxViewModel: InboxViewModel,
    cyclesViewModel: CyclesViewModel,
    myIssuesViewModel: MyIssuesViewModel,
    docsViewModel: DocsViewModel,
    windowSize: AppWindowSize = AppWindowSize.COMPACT
) {
    var currentTab by rememberSaveable { mutableStateOf(MainTab.BOARD) }
    val issueStack = rememberSaveable(saver = OpenIssueStackSaver) { mutableStateListOf<OpenIssueEntry>() }
    var issueFullscreen by rememberSaveable { mutableStateOf(false) }

    var showProjectSwitcher by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var createIssueInitialColumn by remember { mutableStateOf<Int?>(null) }
    var showCreateIssueSheet by remember { mutableStateOf(false) }
    var showGlobalSearch by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current
    val recentStore = remember { com.example.data.local.RecentIssuesStore(context.applicationContext) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var recentIssues by remember { mutableStateOf<List<com.example.data.local.RecentIssue>>(emptyList()) }

    LaunchedEffect(Unit) {
        recentIssues = recentStore.load()
    }

    fun openIssueAndRecord(id: Int, projRef: String?, targetCommentId: Int? = null) {
        issueStack.add(OpenIssueEntry(id, projRef, targetCommentId))
        val boardIssue = appState.boardIssues.firstOrNull { it.id == id }
        val identifier = boardIssue?.identifier ?: "#$id"
        val title = boardIssue?.title ?: ""
        scope.launch {
            recentStore.record(com.example.data.local.RecentIssue(id, identifier, title, projRef))
            recentIssues = recentStore.load()
        }
    }

    LaunchedEffect(appState.snackbarMessage) {
        appState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            appViewModel.clearSnackbar()
        }
    }

    // On a Compact width (folded cover screen) an open issue replaces the whole
    // screen, with its own back handling. On Medium/Expanded (unfolded) it sits
    // beside the originating list in a two-pane layout instead, so opening an
    // issue never loses the list's scroll position or filters.
    val activeIssue = issueStack.lastOrNull()
    val showsIssueList = currentTab == MainTab.BOARD || currentTab == MainTab.MY_ISSUES || currentTab == MainTab.INBOX

    if (activeIssue != null && (windowSize.isCompact || !showsIssueList || issueFullscreen)) {
        val knownBoard = if (activeIssue.projectRef == null || activeIssue.projectRef == appState.selectedProject?.routeKey) {
            appState.projectBoard
        } else null

        IssueDetailScreen(
            issueId = activeIssue.issueId,
            currentUser = user,
            knownBoard = knownBoard,
            knownProjectRef = activeIssue.projectRef ?: appState.selectedProject?.routeKey,
            targetCommentId = activeIssue.targetCommentId,
            viewModel = issueDetailViewModel,
            onBack = {
                if (issueFullscreen) issueFullscreen = false
                else if (issueStack.isNotEmpty()) issueStack.removeAt(issueStack.lastIndex)
            },
            onNavigateToIssue = { nextId ->
                issueStack.add(OpenIssueEntry(nextId, activeIssue.projectRef))
            },
            onBoardDirty = {
                appViewModel.refreshBoard(silent = true)
            },
            isFullscreen = true,
            onToggleFullscreen = if (windowSize.supportsListDetail && showsIssueList) ({ issueFullscreen = false }) else null,
            serverBaseUrl = appState.connection.serverUrl
        )
        return
    }

    val useNavRail = windowSize.usesNavRail

    Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = appState.unreadNotifications
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentTab == MainTab.BOARD && appState.projectBoard != null && !appState.boardFilters.showArchived) {
                    FloatingActionButton(
                        onClick = {
                            createIssueInitialColumn = appState.projectBoard.columns.minByOrNull { it.position }?.id
                            showCreateIssueSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("fab_create_issue")
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Create new issue")
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        unreadCount = appState.unreadNotifications
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    val showDetailPane = windowSize.supportsListDetail && activeIssue != null && showsIssueList

                    if (showDetailPane && activeIssue != null) {
                        TwoPaneListDetail(
                            listPane = {
                                MainTabContent(
                                    currentTab = currentTab,
                                    appState = appState,
                                    appViewModel = appViewModel,
                                    user = user,
                                    myIssuesViewModel = myIssuesViewModel,
                                    inboxViewModel = inboxViewModel,
                                    cyclesViewModel = cyclesViewModel,
                                    docsViewModel = docsViewModel,
                                    windowSize = windowSize,
                                    onOpenIssue = { id, projRef -> openIssueAndRecord(id, projRef) },
                                    onOpenProjectSwitcher = { showProjectSwitcher = true },
                                    onOpenFilterSheet = { showFilterSheet = true },
                                    onQuickCreateInColumn = { colId ->
                                        createIssueInitialColumn = colId
                                        showCreateIssueSheet = true
                                    },
                                    onSwitchTab = { currentTab = it },
                                    recentIssues = recentIssues,
                                    // Inside a 320dp list pane the Kanban columns
                                    // would clip. Force the vertical list view.
                                    forceBoardListView = true,
                                    onOpenGlobalSearch = { showGlobalSearch = true },
                                    onOpenComment = { id, ref, commentId -> openIssueAndRecord(id, ref, commentId) }
                                )
                            },
                            detailPane = {
                                IssueDetailScreen(
                                    issueId = activeIssue.issueId,
                                    currentUser = user,
                                    knownBoard = if (activeIssue.projectRef == null || activeIssue.projectRef == appState.selectedProject?.routeKey) appState.projectBoard else null,
                                    knownProjectRef = activeIssue.projectRef ?: appState.selectedProject?.routeKey,
                                    targetCommentId = activeIssue.targetCommentId,
                                    viewModel = issueDetailViewModel,
                                    onBack = { if (issueStack.isNotEmpty()) issueStack.removeAt(issueStack.lastIndex) },
                                    onNavigateToIssue = { nextId -> issueStack.add(OpenIssueEntry(nextId, activeIssue.projectRef)) },
                                    onBoardDirty = { appViewModel.refreshBoard(silent = true) },
                                    isFullscreen = false,
                                    onToggleFullscreen = { issueFullscreen = true },
                                    serverBaseUrl = appState.connection.serverUrl
                                )
                            }
                        )
                    } else {
                        MainTabContent(
                            currentTab = currentTab,
                            appState = appState,
                            appViewModel = appViewModel,
                            user = user,
                            myIssuesViewModel = myIssuesViewModel,
                            inboxViewModel = inboxViewModel,
                            cyclesViewModel = cyclesViewModel,
                            docsViewModel = docsViewModel,
                            windowSize = windowSize,
                            onOpenIssue = { id, projRef -> openIssueAndRecord(id, projRef) },
                            onOpenProjectSwitcher = { showProjectSwitcher = true },
                            onOpenFilterSheet = { showFilterSheet = true },
                            onQuickCreateInColumn = { colId ->
                                createIssueInitialColumn = colId
                                showCreateIssueSheet = true
                            },
                            onSwitchTab = { currentTab = it },
                            recentIssues = recentIssues,
                            onOpenGlobalSearch = { showGlobalSearch = true },
                            onOpenComment = { id, ref, commentId -> openIssueAndRecord(id, ref, commentId) }
                        )
                    }
                }
            }
        }

    // Sheets
    if (showGlobalSearch) {
        GlobalSearchDialog(
            results = appState.globalSearchResults,
            isSearching = appState.isGlobalSearching,
            onQueryChanged = { appViewModel.searchGlobally(it) },
            onOpenIssue = { hit ->
                openIssueAndRecord(hit.id, hit.projectRef.ifBlank { null })
                showGlobalSearch = false
                appViewModel.clearGlobalSearch()
            },
            onDismiss = {
                showGlobalSearch = false
                appViewModel.clearGlobalSearch()
            }
        )
    }

    if (showProjectSwitcher) {
        ProjectSwitcherSheet(
            projects = appState.projects,
            teams = appState.teams,
            selectedProject = appState.selectedProject,
            onDismiss = { showProjectSwitcher = false },
            onSelectProject = { appViewModel.selectProject(it) },
            onToggleFavorite = { appViewModel.toggleProjectFavorite(it) },
            onCreateProject = { teamId, key, name, desc, preset ->
                appViewModel.createProject(teamId, key, name, desc, preset) {
                    showProjectSwitcher = false
                }
            }
        )
    }

    if (showFilterSheet && appState.projectBoard != null) {
        BoardFilterSheet(
            filters = appState.boardFilters,
            scaffold = appState.projectBoard,
            cycles = appState.projectCycles,
            onUpdateFilters = { appViewModel.updateBoardFilters(it) },
            onDismiss = { showFilterSheet = false }
        )
    }

    if (showCreateIssueSheet && appState.projectBoard != null) {
        CreateIssueSheet(
            scaffold = appState.projectBoard,
            cycles = appState.projectCycles,
            initialColumnId = createIssueInitialColumn,
            onDismiss = { showCreateIssueSheet = false },
            onCreate = { colId, title, desc, typeId, cycleId, assignee, delegate, priority, pts, mins, due, labels ->
                appViewModel.createIssue(
                    columnId = colId,
                    title = title,
                    description = desc,
                    typeId = typeId,
                    cycleId = cycleId,
                    assigneeUserId = assignee,
                    delegateUserId = delegate,
                    priority = priority,
                    estimatePoints = pts,
                    estimateMinutes = mins,
                    dueDate = due,
                    labelIds = labels
                ) {
                    showCreateIssueSheet = false
                }
            }
        )
    }
}

@Composable
private fun MainTabContent(
    currentTab: MainTab,
    appState: AppUiState,
    appViewModel: AppViewModel,
    user: CurrentUserDto,
    myIssuesViewModel: MyIssuesViewModel,
    inboxViewModel: InboxViewModel,
    cyclesViewModel: CyclesViewModel,
    docsViewModel: DocsViewModel,
    windowSize: AppWindowSize,
    onOpenIssue: (Int, String?) -> Unit,
    onOpenProjectSwitcher: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    onQuickCreateInColumn: (Int) -> Unit,
    onSwitchTab: (MainTab) -> Unit,
    forceBoardListView: Boolean = false,
    recentIssues: List<com.example.data.local.RecentIssue> = emptyList(),
    onOpenGlobalSearch: () -> Unit,
    onOpenComment: (Int, String?, Int) -> Unit
) {
    when (currentTab) {
        MainTab.BOARD -> {
            BoardScreen(
                state = appState,
                onOpenProjectSwitcher = onOpenProjectSwitcher,
                onToggleFavorite = {
                    appState.selectedProject?.let { appViewModel.toggleProjectFavorite(it) }
                },
                onRefresh = { appViewModel.refreshBoard(silent = false) },
                onSetViewMode = { appViewModel.setBoardViewMode(it) },
                onUpdateFilters = { appViewModel.updateBoardFilters(it) },
                onOpenFilterSheet = onOpenFilterSheet,
                onOpenIssue = { id -> onOpenIssue(id, appState.selectedProject?.routeKey) },
                onQuickCreateInColumn = onQuickCreateInColumn,
                onMoveIssue = { issue, targetColId ->
                    appViewModel.moveIssueToColumn(issue, targetColId)
                },
                forceListView = forceBoardListView,
                recentIssues = recentIssues,
                onOpenGlobalSearch = onOpenGlobalSearch
            )
        }

        MainTab.MY_ISSUES -> {
            MyIssuesScreen(
                currentUserId = user.id,
                viewModel = myIssuesViewModel,
                onOpenIssue = onOpenIssue
            )
        }

        MainTab.INBOX -> {
            InboxScreen(
                viewModel = inboxViewModel,
                onUnreadChanged = { appViewModel.refreshUnreadCount() },
                onOpenIssue = onOpenIssue,
                onOpenComment = onOpenComment,
                onShowMessage = { appViewModel.showSnackbar(it) }
            )
        }

        MainTab.CYCLES -> {
            CyclesScreen(
                selectedProject = appState.selectedProject,
                viewModel = cyclesViewModel,
                onBoardRefresh = { appViewModel.refreshBoard(silent = true) },
                onFilterBoardByCycle = { cycleId ->
                    appViewModel.updateBoardFilters { it.copy(cycleId = cycleId, showArchived = false) }
                    onSwitchTab(MainTab.BOARD)
                },
                onShowMessage = { appViewModel.showSnackbar(it) }
            )
        }

        MainTab.DOCS -> {
            DocsScreen(
                selectedProject = appState.selectedProject,
                viewModel = docsViewModel,
                onShowMessage = { appViewModel.showSnackbar(it) },
                twoPane = windowSize.supportsListDetail,
                serverBaseUrl = appState.connection.serverUrl
            )
        }

        MainTab.SETTINGS -> {
            SettingsScreen(
                user = user,
                connection = appState.connection,
                teams = appState.teams,
                projectCount = appState.projects.size,
                onThemeModeChange = { appViewModel.setThemeMode(it) },
                onLiveSyncToggle = { appViewModel.setLiveSyncEnabled(it) },
                onSignOut = { appViewModel.signOut() }
            )
        }
    }
}

@Composable
private fun TabIconWithBadge(
    tab: MainTab,
    selected: Boolean,
    unreadCount: Int
) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    if (tab == MainTab.INBOX && unreadCount > 0) {
        BadgedBox(
            badge = {
                Badge {
                    Text(if (unreadCount > 99) "99+" else "$unreadCount")
                }
            }
        ) {
            Icon(icon, contentDescription = tab.label)
        }
    } else {
        Icon(icon, contentDescription = tab.label)
    }
}
