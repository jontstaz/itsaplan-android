package io.github.jontstaz.itsaplan.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.jontstaz.itsaplan.MainActivity
import io.github.jontstaz.itsaplan.data.local.WidgetCache
import android.content.ComponentName

class MyIssuesWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val cache = WidgetCache(context)
        val items = cache.loadMyIssues()
        val loaded = cache.hasLoaded()
        val openCount = items.size

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(GlanceTheme.colors.surface)
                        .padding(12.dp)
                        .clickable(actionStartActivity(ComponentName(context, MainActivity::class.java)))
                ) {
                    Row(
                        modifier = GlanceModifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Vertical.CenterVertically
                    ) {
                        Text(
                            text = "My Issues",
                            style = TextStyle(
                                color = GlanceTheme.colors.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(GlanceModifier.defaultWeight())
                        Text(
                            text = "$openCount open",
                            style = TextStyle(color = GlanceTheme.colors.primary)
                        )
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    if (items.isEmpty()) {
                        Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                // Distinguish "not synced yet" from a genuine empty result,
                                // so a freshly added widget does not claim you have no work.
                                text = if (loaded) "No open issues assigned to you."
                                else "Loading your issues… open the app to sync.",
                                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant)
                            )
                        }
                    } else {
                        LazyColumn {
                            items(items.take(5)) { issue ->
                                Row(
                                    modifier = GlanceModifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable(actionStartActivity(
                                            ComponentName(context, MainActivity::class.java)
                                        ))
                                ) {
                                    Text(
                                        text = issue.identifier,
                                        style = TextStyle(
                                            color = GlanceTheme.colors.primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                    Spacer(GlanceModifier.width(8.dp))
                                    Text(
                                        text = issue.title,
                                        maxLines = 1,
                                        style = TextStyle(color = GlanceTheme.colors.onSurface)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class MyIssuesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyIssuesWidget()
}
