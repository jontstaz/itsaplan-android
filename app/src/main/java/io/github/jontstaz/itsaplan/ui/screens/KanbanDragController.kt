package io.github.jontstaz.itsaplan.ui.screens

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.jontstaz.itsaplan.data.model.AssigneeCandidateDto
import io.github.jontstaz.itsaplan.data.model.BoardIssueDto
import io.github.jontstaz.itsaplan.data.model.LabelDto
import kotlin.math.roundToInt

/** Shared gesture state for moving cards between visible Kanban lanes. */
internal class KanbanDragController(
    private val haptic: HapticFeedback,
    private val onDrop: (BoardIssueDto, Int) -> Unit
) {
    var draggedIssue by mutableStateOf<BoardIssueDto?>(null)
        private set
    var targetColumnId by mutableStateOf<Int?>(null)
        private set
    var pointerPosition by mutableStateOf(Offset.Zero)
        private set
    var boardBounds by mutableStateOf(Rect.Zero)

    private val columnBounds = mutableStateMapOf<Int, Rect>()
    private var cardSize = IntSize.Zero
    private var grabOffset = Offset.Zero

    val isDragging: Boolean get() = draggedIssue != null

    fun cardDragModifier(issue: BoardIssueDto): Modifier = Modifier.composed {
        var coordinates by rememberCoordinates()
        this
            .onGloballyPositioned { coordinates = it }
            .graphicsLayer { alpha = if (draggedIssue?.id == issue.id) 0.3f else 1f }
            .pointerInput(issue.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        val currentCoordinates = coordinates ?: return@detectDragGesturesAfterLongPress
                        draggedIssue = issue
                        cardSize = currentCoordinates.size
                        grabOffset = startOffset
                        pointerPosition = currentCoordinates.localToRoot(startOffset)
                        updateTarget()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        coordinates?.let { pointerPosition = it.localToRoot(change.position) }
                        updateTarget()
                    },
                    onDragEnd = { finishDrag() },
                    onDragCancel = { cancelDrag() }
                )
            }
    }

    fun updateColumnBounds(columnId: Int, coordinates: LayoutCoordinates) {
        val bounds = coordinates.boundsInRoot()
        if (columnBounds[columnId] != bounds) {
            columnBounds[columnId] = bounds
            updateTarget()
        }
    }

    fun autoScrollDelta(): Float {
        if (!isDragging || boardBounds.isEmpty || !boardBounds.contains(pointerPosition)) return 0f
        val edgeWidth = 64f
        return when {
            pointerPosition.x < boardBounds.left + edgeWidth -> -18f
            pointerPosition.x > boardBounds.right - edgeWidth -> 18f
            else -> 0f
        }
    }

    private fun updateTarget() {
        targetColumnId = columnBounds.entries.firstOrNull { it.value.contains(pointerPosition) }?.key
    }

    private fun finishDrag() {
        val issue = draggedIssue
        val target = targetColumnId
        if (issue != null && target != null && issue.columnId != target) {
            onDrop(issue, target)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        cancelDrag()
    }

    private fun cancelDrag() {
        draggedIssue = null
        targetColumnId = null
        cardSize = IntSize.Zero
        grabOffset = Offset.Zero
    }

    fun overlayPosition(): IntOffset = IntOffset(
        (pointerPosition.x - grabOffset.x).roundToInt(),
        (pointerPosition.y - grabOffset.y).roundToInt()
    )

    fun cardWidthPx(): Int = cardSize.width
}

@Composable
private fun rememberCoordinates() = androidx.compose.runtime.remember {
    mutableStateOf<LayoutCoordinates?>(null)
}

@Composable
internal fun KanbanDragOverlay(
    controller: KanbanDragController,
    labelMap: Map<Int, LabelDto>,
    assigneeMap: Map<String, AssigneeCandidateDto>
) {
    val issue = controller.draggedIssue ?: return
    val density = LocalDensity.current
    Box(modifier = Modifier.fillMaxSize().zIndex(10f)) {
        BoardIssueCard(
            issue = issue,
            labelMap = labelMap,
            assigneeMap = assigneeMap,
            onClick = {},
            onMoveClick = {},
            dragModifier = Modifier
                .offset { controller.overlayPosition() }
                .width(with(density) { controller.cardWidthPx().toDp() })
                .graphicsLayer { rotationZ = 1.5f }
        )
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 96.dp)
        ) {
            Text(
                text = if (controller.targetColumnId != null && controller.targetColumnId != issue.columnId) {
                    "Release to move"
                } else {
                    "Drag over a status column"
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
