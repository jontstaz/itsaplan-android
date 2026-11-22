package com.example.ui.components

import android.text.method.LinkMovementMethod
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.Markwon
import io.noties.markwon.core.MarkwonTheme
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin

/**
 * Theme-aware Markdown renderer built directly on Markwon so we can override
 * `compose-markdown`'s default light-on-light code highlight colors with values
 * from the active Material theme.
 */
@Composable
fun ThemedMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    isTextSelectable: Boolean = true
) {
    val context = LocalContext.current
    val textColor = style.color.takeIf { it != Color.Unspecified }
        ?: MaterialTheme.colorScheme.onSurface
    val fontSizeSp = style.fontSize.value.takeIf { it > 0f && style.fontSize != androidx.compose.ui.unit.TextUnit.Unspecified } ?: 14f

    val codeBlockBg = MaterialTheme.colorScheme.surfaceContainerHigh
    val codeBlockTextColor = MaterialTheme.colorScheme.onSurface
    val inlineCodeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    val inlineCodeTextColor = MaterialTheme.colorScheme.primary

    val markwon = remember(
        linkColor, textColor, fontSizeSp,
        codeBlockBg, codeBlockTextColor, inlineCodeBg, inlineCodeTextColor
    ) {
        Markwon.builder(context)
            .usePlugin(CoilImagesPlugin.create(context))
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(HtmlPlugin.create())
            .usePlugin(LinkifyPlugin.create())
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureTheme(builder: MarkwonTheme.Builder) {
                    builder
                        .codeBackgroundColor(inlineCodeBg.toArgb())
                        .codeTextColor(inlineCodeTextColor.toArgb())
                        .codeBlockBackgroundColor(codeBlockBg.toArgb())
                        .codeBlockTextColor(codeBlockTextColor.toArgb())
                        .codeTypeface(android.graphics.Typeface.MONOSPACE)
                        .codeBlockTypeface(android.graphics.Typeface.MONOSPACE)
                }
            })
            .build()
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
                setTextColor(textColor.toArgb())
                setLinkTextColor(linkColor.toArgb())
                movementMethod = LinkMovementMethod.getInstance()
                setTextIsSelectable(isTextSelectable)
            }
        },
        update = { tv ->
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSizeSp)
            tv.setTextColor(textColor.toArgb())
            tv.setLinkTextColor(linkColor.toArgb())
            markwon.setMarkdown(tv, markdown)
        }
    )
}
