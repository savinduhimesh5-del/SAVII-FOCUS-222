package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.model.FocusSessionWithDetails
import com.example.data.local.model.TaskWithSubject
import com.example.domain.DateTimeUtils
import com.example.domain.FocusPresets
import com.example.domain.TimerStatus
import com.example.ui.components.SaviiEmptyStateCard
import com.example.ui.components.SubjectPill
import com.example.ui.theme.TimerDigitsStyle
import com.example.ui.viewmodel.FocusTimerUiState

@Composable
fun FocusTimerScreen(
    uiState: FocusTimerUiState,
    hapticsEnabled: Boolean,
    onSelectPreset: (Int) -> Unit,
    onSelectSubject: (Long?) -> Unit,
    onSelectTask: (TaskWithSubject?) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onMarkLinkedTaskCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val triggerHaptic = {
        if (hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 840.dp

        if (isWide) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 1120.dp)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1.1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "timer_header_wide") {
                        TimerHeaderSection(uiState.status)
                    }
                    item(key = "timer_dial_wide") {
                        TimerDialCard(
                            uiState = uiState,
                            onStart = {
                                triggerHaptic()
                                onStart()
                            },
                            onPause = {
                                triggerHaptic()
                                onPause()
                            },
                            onResume = {
                                triggerHaptic()
                                onResume()
                            },
                            onReset = {
                                triggerHaptic()
                                onReset()
                            },
                            onSelectPreset = onSelectPreset,
                            onMarkLinkedTaskCompleted = onMarkLinkedTaskCompleted
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.weight(0.9f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item(key = "context_selectors_wide") {
                        ContextLinkingSection(
                            uiState = uiState,
                            onSelectSubject = onSelectSubject,
                            onSelectTask = onSelectTask
                        )
                    }
                    item(key = "recent_sessions_header_wide") {
                        Text(
                            text = "Recent Sessions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    if (uiState.recentSessions.isEmpty()) {
                        item(key = "recent_sessions_empty_wide") {
                            SaviiEmptyStateCard(
                                icon = Icons.Outlined.History,
                                title = "No focus sessions yet",
                                subtitle = "Complete your first focus timer to log study minutes locally."
                            )
                        }
                    } else {
                        items(
                            items = uiState.recentSessions,
                            key = { "session_${it.session.id}" }
                        ) { session ->
                            SessionHistoryCard(session = session)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item(key = "timer_header") {
                    TimerHeaderSection(uiState.status)
                }

                item(key = "timer_dial") {
                    TimerDialCard(
                        uiState = uiState,
                        onStart = {
                            triggerHaptic()
                            onStart()
                        },
                        onPause = {
                            triggerHaptic()
                            onPause()
                        },
                        onResume = {
                            triggerHaptic()
                            onResume()
                        },
                        onReset = {
                            triggerHaptic()
                            onReset()
                        },
                        onSelectPreset = onSelectPreset,
                        onMarkLinkedTaskCompleted = onMarkLinkedTaskCompleted
                    )
                }

                item(key = "context_selectors") {
                    ContextLinkingSection(
                        uiState = uiState,
                        onSelectSubject = onSelectSubject,
                        onSelectTask = onSelectTask
                    )
                }

                item(key = "recent_sessions_header") {
                    Text(
                        text = "Recent Focus Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (uiState.recentSessions.isEmpty()) {
                    item(key = "recent_sessions_empty") {
                        SaviiEmptyStateCard(
                            icon = Icons.Outlined.History,
                            title = "No focus sessions yet",
                            subtitle = "Completed 15, 25, 45, or 60-minute sessions are saved automatically."
                        )
                    }
                } else {
                    items(
                        items = uiState.recentSessions,
                        key = { "session_${it.session.id}" }
                    ) { session ->
                        SessionHistoryCard(session = session)
                    }
                }

                item(key = "timer_bottom_spacer") {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun TimerHeaderSection(status: TimerStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = "Focus Timer",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = when (status) {
                TimerStatus.IDLE -> "Choose a preset and lock in distraction-free study."
                TimerStatus.RUNNING -> "Session active — time continues accurately even if you leave the app."
                TimerStatus.PAUSED -> "Session paused — resume when ready."
                TimerStatus.COMPLETED -> "Session saved to your Insights."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerDialCard(
    uiState: FocusTimerUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onReset: () -> Unit,
    onSelectPreset: (Int) -> Unit,
    onMarkLinkedTaskCompleted: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val progressBrush = remember(primaryColor, tertiaryColor) {
        Brush.sweepGradient(colors = listOf(primaryColor, tertiaryColor, primaryColor))
    }
    val canChangePreset = uiState.status == TimerStatus.IDLE || uiState.status == TimerStatus.COMPLETED
    val linkedTask = uiState.pendingTasks.firstOrNull { it.task.id == uiState.selectedTaskId }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Presets Row (15, 25, 45, 60 min)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FocusPresets.minutesOptions.forEach { minutes ->
                    FilterChip(
                        selected = uiState.presetMinutes == minutes,
                        onClick = { onSelectPreset(minutes) },
                        enabled = canChangePreset,
                        label = { Text("${minutes}m") },
                        modifier = Modifier.testTag("timer_preset_$minutes")
                    )
                }
            }

            // Responsive Custom Canvas Countdown Ring
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = size.minDimension * 0.065f
                    val diameter = size.minDimension - strokeWidth
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = (size.height - diameter) / 2f
                    )
                    val arcSize = Size(diameter, diameter)

                    drawArc(
                        color = trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    val sweep = if (uiState.status == TimerStatus.COMPLETED) {
                        360f
                    } else {
                        (uiState.progress * 360f).coerceIn(0f, 360f)
                    }
                    if (sweep > 0f) {
                        drawArc(
                            brush = progressBrush,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when (uiState.status) {
                            TimerStatus.IDLE -> "READY"
                            TimerStatus.RUNNING -> "FOCUSING"
                            TimerStatus.PAUSED -> "PAUSED"
                            TimerStatus.COMPLETED -> "COMPLETE"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = if (uiState.status == TimerStatus.COMPLETED) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = DateTimeUtils.formatTimerClock(uiState.remainingMillis),
                        style = TimerDigitsStyle,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("timer_clock_display")
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${uiState.presetMinutes} min session",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (uiState.status == TimerStatus.COMPLETED) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Text(
                                text = "Logged ${uiState.lastCompletedMinutes ?: uiState.presetMinutes} minutes of focus!",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        if (linkedTask != null) {
                            FilledTonalButton(
                                onClick = onMarkLinkedTaskCompleted,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Text("Mark \"${linkedTask.task.title}\" Done")
                            }
                        }
                    }
                }
            }

            // Primary Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
            ) {
                when (uiState.status) {
                    TimerStatus.IDLE, TimerStatus.COMPLETED -> {
                        Button(
                            onClick = onStart,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("timer_start_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.status == TimerStatus.COMPLETED) {
                                    "Start Another"
                                } else {
                                    "Start Focus"
                                }
                            )
                        }
                    }

                    TimerStatus.RUNNING -> {
                        Button(
                            onClick = onPause,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("timer_pause_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pause")
                        }
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("timer_reset_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset timer")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }

                    TimerStatus.PAUSED -> {
                        Button(
                            onClick = onResume,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("timer_resume_button")
                        ) {
                            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resume")
                        }
                        OutlinedButton(
                            onClick = onReset,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("timer_reset_button")
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset timer")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reset")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContextLinkingSection(
    uiState: FocusTimerUiState,
    onSelectSubject: (Long?) -> Unit,
    onSelectTask: (TaskWithSubject?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Focus Context (Optional)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (uiState.subjects.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Subject",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.selectedSubjectId == null,
                                onClick = { onSelectSubject(null) },
                                label = { Text("None") }
                            )
                        }
                        items(
                            items = uiState.subjects,
                            key = { "timer_subj_${it.id}" }
                        ) { subject ->
                            FilterChip(
                                selected = uiState.selectedSubjectId == subject.id,
                                onClick = { onSelectSubject(subject.id) },
                                label = { Text(subject.displayWithCategory) }
                            )
                        }
                    }
                }
            }

            if (uiState.pendingTasks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Linked Task",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = uiState.selectedTaskId == null,
                                onClick = { onSelectTask(null) },
                                label = { Text("No task") }
                            )
                        }
                        items(
                            items = uiState.pendingTasks.take(10),
                            key = { "timer_task_${it.task.id}" }
                        ) { taskItem ->
                            FilterChip(
                                selected = uiState.selectedTaskId == taskItem.task.id,
                                onClick = { onSelectTask(taskItem) },
                                label = {
                                    Text(
                                        text = taskItem.task.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Add tasks in Planner or subjects in Study to tag your focus sessions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SessionHistoryCard(session: FocusSessionWithDetails) {
    val entity = session.session
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = session.taskTitle ?: session.subjectName ?: "Focused Study Session",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = DateTimeUtils.formatSessionTimestamp(entity.completedAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (session.subjectName != null && session.subjectColorHex != null) {
                    SubjectPill(
                        name = session.subjectDisplayName ?: session.subjectName,
                        colorHex = session.subjectColorHex
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = DateTimeUtils.formatDurationFromSeconds(entity.actualSeconds),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
