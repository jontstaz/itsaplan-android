package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CurrentUserDto
import com.example.ui.viewmodel.InboxViewModel
import com.example.ui.viewmodel.MyIssuesViewModel

private enum class HomeSection(val label: String) {
    ASSIGNED("My Items"),
    ACTIVITY("Notifications")
}

/**
 * The personal home screen: everything that concerns the signed-in user across every
 * project they can reach. "My Items" lists issues assigned to or delegated to them;
 * "Notifications" is the cross-project inbox.
 *
 * Both halves reuse the standalone screens, so the data, filtering and deep-link
 * behaviour stay identical to the dedicated tabs.
 */
@Composable
fun HomeScreen(
    user: CurrentUserDto,
    myIssuesViewModel: MyIssuesViewModel,
    inboxViewModel: InboxViewModel,
    unreadCount: Int,
    onOpenIssue: (issueId: Int, projectRef: String?) -> Unit,
    onOpenComment: (issueId: Int, projectRef: String?, commentId: Int) -> Unit,
    onShowMessage: (String) -> Unit,
    onUnreadChanged: () -> Unit,
    windowSize: com.example.ui.layout.AppWindowSize
) {
    var section by rememberSaveable { mutableStateOf(HomeSection.ASSIGNED) }
    val inboxState by inboxViewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Home", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Everything assigned to you, across every project",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    HomeSection.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = section == entry,
                            onClick = { section = entry },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = HomeSection.entries.size
                            ),
                            modifier = Modifier.testTag("home_section_${entry.name.lowercase()}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (entry == HomeSection.ACTIVITY && unreadCount > 0) {
                                    BadgedBox(badge = {
                                        Badge { Text(if (unreadCount > 99) "99+" else "$unreadCount") }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Rounded.Inbox,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = entry.label,
                                    fontWeight = if (section == entry) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (section) {
                HomeSection.ASSIGNED -> MyIssuesScreen(
                    currentUserId = user.id,
                    viewModel = myIssuesViewModel,
                    onOpenIssue = onOpenIssue,
                    showHeader = false
                )

                HomeSection.ACTIVITY -> InboxScreen(
                    viewModel = inboxViewModel,
                    onUnreadChanged = onUnreadChanged,
                    onOpenIssue = onOpenIssue,
                    onOpenComment = onOpenComment,
                    onShowMessage = onShowMessage,
                    showHeader = false
                )
            }
        }
    }
}
