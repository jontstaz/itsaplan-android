package io.github.jontstaz.itsaplan.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Side-by-side list/detail layout for Medium and Expanded window sizes (e.g. an
 * unfolded book-style foldable). The list pane keeps a fixed, readable width; the
 * detail pane takes the rest. Call sites fall back to showing only one pane
 * full-screen on Compact widths.
 */
@Composable
fun TwoPaneListDetail(
    listPaneWidth: Dp = 320.dp,
    listPane: @Composable () -> Unit,
    detailPane: @Composable () -> Unit
) {
    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(listPaneWidth).fillMaxHeight()) {
            listPane()
        }
        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            detailPane()
        }
    }
}

/**
 * Centers [content] and caps its width at [maxWidthDp] so a single reading column
 * (an issue's description, a document, a settings page, a dialog's form) doesn't
 * stretch edge-to-edge on a near-square unfolded foldable or a tablet. On a narrow
 * (Compact) width it is a no-op: content still fills the available width.
 */
@Composable
fun ConstrainedWidthContainer(
    maxWidthDp: Int = CONTENT_MAX_WIDTH_DP,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = maxWidthDp.dp)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}
