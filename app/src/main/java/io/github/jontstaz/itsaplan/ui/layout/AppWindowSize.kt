package io.github.jontstaz.itsaplan.ui.layout

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * App-wide breakpoints, aligned with Android's official window size classes:
 * Compact < 600dp, Medium 600–839dp, Expanded >= 840dp.
 *
 * On a book-style foldable (e.g. Honor Magic V3) the cover screen (~353–404dp)
 * lands in Compact, and the inner screen (~719–821dp) lands in Medium. We treat
 * Medium and Expanded the same way here (nav rail + wider content), since both
 * have enough room for a list-detail split.
 */
enum class AppWindowSize {
    COMPACT,
    MEDIUM,
    EXPANDED
}

fun WindowWidthSizeClass.toAppWindowSize(): AppWindowSize = when (this) {
    WindowWidthSizeClass.Compact -> AppWindowSize.COMPACT
    WindowWidthSizeClass.Medium -> AppWindowSize.MEDIUM
    else -> AppWindowSize.EXPANDED
}

/**
 * Same Compact/Medium/Expanded breakpoints as [toAppWindowSize], but computed from a
 * live width (e.g. `BoxWithConstraints.maxWidth`). Useful inside a composable that is
 * already measuring its own width and wants per-pane decisions without re-deriving
 * the window size class from the Activity.
 */
fun Dp.toAppWindowSize(): AppWindowSize = when {
    this < 600.dp -> AppWindowSize.COMPACT
    this < 840.dp -> AppWindowSize.MEDIUM
    else -> AppWindowSize.EXPANDED
}

val AppWindowSize.isCompact: Boolean get() = this == AppWindowSize.COMPACT
val AppWindowSize.usesNavRail: Boolean get() = this != AppWindowSize.COMPACT
val AppWindowSize.supportsListDetail: Boolean get() = this != AppWindowSize.COMPACT

/** Max width for a single content column (dialogs, forms, detail panes) so text
 * doesn't stretch edge-to-edge on a near-square unfolded display. */
const val CONTENT_MAX_WIDTH_DP = 720
