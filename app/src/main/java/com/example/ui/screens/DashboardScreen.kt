package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.model.SubjectProgressRow
import com.example.domain.DateTimeUtils
import com.example.ui.components.SaviiBrandLogoBadge
import com.example.ui.components.SaviiEmptyStateCard
import com.example.ui.components.SaviiSectionHeader
import com.example.ui.components.TaskItemCard
import com.example.ui.components.subjectIconVector
import com.example.ui.theme.StatDigitsStyle
import com.example.ui.viewmodel.DashboardUiState

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onToggleTask: (Long, Boolean) -> Unit,
    onStartFocus: () -> Unit,
    onQuickAddTask: () -> Unit,
    onQuickAddSubject: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenLearning: () -> Unit,
    onOpenSubjectInPlanner: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Welcome Header
            item(key = "welcome_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SaviiBrandLogoBadge(size = 50.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.greetingText,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "${uiState.headerDateText} • ${stringResource(R.string.brand_tagline)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Daily Focus & Task Progress Summary Card
            item(key = "daily_progress_card") {
                val goalMinutes = uiState.dailyGoalMinutes.coerceAtLeast(1)
                val focusRatio = (uiState.todayFocusMinutes.toFloat() / goalMinutes.toFloat())
                    .coerceIn(0f, 1f)
                val todayTotalTasks = uiState.todayCompletedCount + uiState.todayPendingCount
                val taskRatio = if (todayTotalTasks > 0) {
                    uiState.todayCompletedCount.toFloat() / todayTotalTasks.toFloat()
                } else {
                    0f
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.surfaceContainer
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Today's Focus Overview",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "${uiState.todaySessionCount} sessions",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MetricTile(
                                    label = "Focus Time",
                                    value = DateTimeUtils.formatDurationMinutes(uiState.todayFocusMinutes),
                                    subtitle = "Goal ${DateTimeUtils.formatDurationMinutes(goalMinutes)}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "Completed",
                                    value = "${uiState.todayCompletedCount}",
                                    subtitle = "${uiState.totalCompletedTasksCount} all-time",
                                    modifier = Modifier.weight(1f)
                                )
                                MetricTile(
                                    label = "Pending",
                                    value = "${uiState.todayPendingCount}",
                                    subtitle = "${uiState.totalPendingTasksCount} total open",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Daily Focus Goal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${(focusRatio * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { focusRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    strokeCap = StrokeCap.Round
                                )
                            }

                            if (todayTotalTasks > 0) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Today's Tasks Progress",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${uiState.todayCompletedCount} of $todayTotalTasks",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { taskRatio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        strokeCap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Actions
            item(key = "quick_actions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SaviiSectionHeader(title = "Quick Actions")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val actionButtonPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        Button(
                            onClick = onStartFocus,
                            contentPadding = actionButtonPadding,
                            modifier = Modifier
                                .weight(1.1f)
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_start_focus_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Focus",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = onQuickAddTask,
                            contentPadding = actionButtonPadding,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_add_task_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddTask,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Task",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        FilledTonalButton(
                            onClick = onQuickAddSubject,
                            contentPadding = actionButtonPadding,
                            modifier = Modifier
                                .weight(1.1f)
                                .minimumInteractiveComponentSize()
                                .testTag("dashboard_add_subject_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LibraryAdd,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Subject",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Today's Tasks Section
            item(key = "today_tasks_header") {
                SaviiSectionHeader(
                    title = "Today's Tasks",
                    actionLabel = "View Planner",
                    actionTestTag = "dashboard_view_planner",
                    onActionClick = onOpenPlanner
                )
            }

            if (uiState.todayTasks.isEmpty()) {
                item(key = "today_tasks_empty") {
                    SaviiEmptyStateCard(
                        icon = Icons.Outlined.TaskAlt,
                        title = "No tasks scheduled for today",
                        subtitle = "Plan your study priorities in the Planner or create a quick task.",
                        actionLabel = "Add Today's Task",
                        actionTestTag = "dashboard_empty_add_task",
                        onAction = onQuickAddTask
                    )
                }
            } else {
                items(
                    items = uiState.todayTasks,
                    key = { "dash_task_${it.task.id}" }
                ) { item ->
                    TaskItemCard(
                        item = item,
                        onToggleComplete = {
                            onToggleTask(item.task.id, item.task.isCompleted)
                        },
                        onStartFocus = { onStartFocus() }
                    )
                }
            }

            // Study Overview
            item(key = "learning_overview_header") {
                SaviiSectionHeader(
                    title = "Study Overview",
                    actionLabel = "Open Study",
                    actionTestTag = "dashboard_view_learning",
                    onActionClick = onOpenLearning
                )
            }

            if (uiState.subjectsOverview.isEmpty()) {
                item(key = "learning_overview_empty") {
                    SaviiEmptyStateCard(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "Organize your subjects",
                        subtitle = "Create A/L, AAT, or other study subjects to track weekly targets and planner tasks.",
                        actionLabel = "Add First Subject",
                        actionTestTag = "dashboard_empty_add_subject",
                        onAction = onQuickAddSubject
                    )
                }
            } else {
                item(key = "learning_overview_row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.subjectsOverview,
                            key = { "dash_subject_${it.subject.id}" }
                        ) { row ->
                            SubjectOverviewCard(
                                row = row,
                                onClick = { onOpenSubjectInPlanner(row.subject.id) }
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = StatDigitsStyle.copy(fontSize = MaterialTheme.typography.titleLarge.fontSize),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SubjectOverviewCard(
    row: SubjectProgressRow,
    onClick: () -> Unit
) {
    val accent = Color(row.subject.colorHex)
    val weekMinutes = (row.weekFocusSeconds + 30) / 60
    val targetMinutes = row.subject.targetMinutesPerWeek.coerceAtLeast(1)
    val ratio = (weekMinutes.toFloat() / targetMinutes.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .width(190.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("dashboard_subject_card_${row.subject.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = subjectIconVector(row.subject.iconOption),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = row.subject.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = row.subject.categoryBadgeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Text(
                text = "${row.pendingTaskCount} open • ${row.completedTaskCount} done",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${DateTimeUtils.formatDurationMinutes(weekMinutes)} this week",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Filled.CheckCircleOutline,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
