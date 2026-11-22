package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.KeyboardDoubleArrowUp
import androidx.compose.material.icons.rounded.PriorityHigh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Timelapse
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Indigo500
import com.example.ui.theme.MonoCodeStyle
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.PriorityNoneColor
import com.example.ui.theme.PriorityUrgentColor
import com.example.ui.theme.StateBacklogColor
import com.example.ui.theme.StateCanceledColor
import com.example.ui.theme.StateCompletedColor
import com.example.ui.theme.StateStartedColor
import com.example.ui.theme.StateUnstartedColor
import com.example.ui.theme.Violet500
import com.example.ui.theme.parseHexColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

val ALL_PRIORITIES = listOf("urgent", "high", "medium", "low", "none")

fun priorityLabel(priority: String?): String = when (priority?.lowercase()) {
    "urgent" -> "Urgent"
    "high" -> "High"
    "medium" -> "Medium"
    "low" -> "Low"
    else -> "No Priority"
}

fun priorityColor(priority: String?): Color = when (priority?.lowercase()) {
    "urgent" -> PriorityUrgentColor
    "high" -> PriorityHighColor
    "medium" -> PriorityMediumColor
    "low" -> PriorityLowColor
    else -> PriorityNoneColor
}

fun priorityIcon(priority: String?): ImageVector = when (priority?.lowercase()) {
    "urgent" -> Icons.Rounded.PriorityHigh
    "high" -> Icons.Rounded.KeyboardDoubleArrowUp
    "medium" -> Icons.Rounded.KeyboardArrowUp
    "low" -> Icons.Rounded.KeyboardArrowDown
    else -> Icons.Rounded.Remove
}

fun stateTypeColor(stateType: String?, hexOverride: String? = null): Color {
    if (!hexOverride.isNullOrBlank()) return parseHexColor(hexOverride)
    return when (stateType?.lowercase()) {
        "backlog" -> StateBacklogColor
        "unstarted" -> StateUnstartedColor
        "started" -> StateStartedColor
        "completed" -> StateCompletedColor
        "canceled" -> StateCanceledColor
        else -> Indigo500
    }
}

fun stateTypeIcon(stateType: String?): ImageVector = when (stateType?.lowercase()) {
    "backlog" -> Icons.Outlined.RadioButtonUnchecked
    "unstarted" -> Icons.Outlined.Circle
    "started" -> Icons.Rounded.Timelapse
    "completed" -> Icons.Outlined.CheckCircle
    "canceled" -> Icons.Outlined.Cancel
    else -> Icons.Outlined.Circle
}

@Composable
fun PriorityBadge(
    priority: String?,
    showLabel: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = priorityColor(priority)
    val icon = priorityIcon(priority)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(0.75.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (showLabel) 6.dp else 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Priority ${priorityLabel(priority)}",
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            if (showLabel) {
                Text(
                    text = priorityLabel(priority),
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun IdentifierChip(
    identifier: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = identifier,
            style = MonoCodeStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1
        )
    }
}

@Composable
fun LabelPill(
    name: String,
    hexColor: String?,
    modifier: Modifier = Modifier
) {
    val color = parseHexColor(hexColor, Indigo500)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(0.75.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UserAvatar(
    name: String,
    imageUrl: String? = null,
    isAgent: Boolean = false,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    val initials = name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    val palette = listOf(Indigo500, Emerald500, Violet500, Color(0xFF0EA5E9), Color(0xFFF59E0B), Color(0xFFEC4899))
    val bgColor = if (isAgent) Violet500 else palette[abs(name.hashCode()) % palette.size]

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = if (isAgent) Violet500 else bgColor.copy(alpha = 0.45f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
            )
        } else if (isAgent) {
            Icon(
                imageVector = Icons.Rounded.SmartToy,
                contentDescription = "AI Agent $name",
                tint = Violet500,
                modifier = Modifier.size(size * 0.6f)
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = bgColor,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ErrorBanner(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                Spacer(Modifier.width(8.dp))
                androidx.compose.material3.TextButton(onClick = onRetry) {
                    Text("Retry", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0m"
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

fun todayIsoDate(): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return fmt.format(Date())
}

fun formatShortDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return iso.take(10)
}

fun formatRelativeTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    val patterns = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd HH:mm:ss"
    )
    var parsed: Date? = null
    for (p in patterns) {
        try {
            val sdf = SimpleDateFormat(p, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            parsed = sdf.parse(iso)
            if (parsed != null) break
        } catch (_: Exception) {
        }
    }
    if (parsed == null) return iso.take(10)
    val diffSec = (System.currentTimeMillis() - parsed.time) / 1000L
    return when {
        diffSec < 60 -> "just now"
        diffSec < 3600 -> "${diffSec / 60}m ago"
        diffSec < 86_400 -> "${diffSec / 3600}h ago"
        diffSec < 604_800 -> "${diffSec / 86_400}d ago"
        else -> SimpleDateFormat("MMM d", Locale.US).format(parsed)
    }
}
