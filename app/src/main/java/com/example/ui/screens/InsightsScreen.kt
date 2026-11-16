package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.model.SubjectProgressRow
import com.example.domain.DateTimeUtils
import com.example.domain.StudyCategory
import com.example.ui.components.SaviiEmptyStateCard
import com.example.ui.components.subjectIconVector
import com.example.ui.theme.StatDigitsStyle
import com.example.ui.viewmodel.DayBarPoint
import com.example.ui.viewmodel.InsightsUiState

private enum class InsightsRange(val label: String) {
    TODAY("Today"),
    WEEK("Last 7 Days"),
    ALL_TIME("All Time")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsScreen(
    uiState: InsightsUiState,
    modifier: Modifier = Modifier
) {
    var selectedRange by rememberSaveable { mutableStateOf(InsightsRange.WEEK) }

    val focusSeconds = when (selectedRange) {
        InsightsRange.TODAY -> uiState.todayFocusSeconds
        InsightsRange.WEEK -> uiState.weekFocusSeconds
        InsightsRange.ALL_TIME -> uiState.allTimeFocusSeconds
    }
    val sessionCount = when (selectedRange) {
        InsightsRange.TODAY -> uiState.todaySessionCount
        InsightsRange.WEEK -> uiState.weekSessionCount
        InsightsRange.ALL_TIME -> uiState.allTimeSessionCount
    }
    val completedTasks = when (selectedRange) {
        InsightsRange.TODAY -> uiState.todayCompletedTasks
        InsightsRange.WEEK -> uiState.weekCompletedTasks
        InsightsRange.ALL_TIME -> uiState.allTimeCompletedTasks
    }

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
            item(key = "insights_header") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Productivity Insights",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Real focus duration, sessions, and learning progress stored on-device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item(key = "insights_range_selector") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    InsightsRange.entries.forEach { range ->
                        FilterChip(
                            selected = selectedRange == range,
                            onClick = { selectedRange = range },
                            label = { Text(range.label) },
                            modifier = Modifier.testTag("insights_range_${range.name.lowercase()}")
                        )
                    }
                }
            }

            item(key = "insights_summary_cards") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InsightMetricCard(
                        title = "Focus Time",
                        value = DateTimeUtils.formatDurationFromSeconds(focusSeconds),
                        caption = selectedRange.label,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    InsightMetricCard(
                        title = "Sessions",
                        value = "$sessionCount",
                        caption = "Completed",
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f)
                    )
                    InsightMetricCard(
                        title = "Tasks Done",
                        value = "$completedTasks",
                        caption = selectedRange.label,
                        accent = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "insights_7day_chart") {
                WeeklyFocusChartCard(points = uiState.last7DaysChart)
            }

            item(key = "insights_all_periods_comparison") {
                AllPeriodComparisonCard(uiState = uiState)
            }

            item(key = "insights_category_breakdown") {
                CategoryBreakdownCard(
                    subjectProgress = uiState.subjectProgress,
                    useAllTimeFocus = selectedRange == InsightsRange.ALL_TIME
                )
            }

            item(key = "insights_subjects_header") {
                Text(
                    text = "Study Progress",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (uiState.subjectProgress.isEmpty()) {
                item(key = "insights_subjects_empty") {
                    SaviiEmptyStateCard(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = "No subject data yet",
                        subtitle = "Add subjects in Study and tag tasks or focus sessions to track subject progress."
                    )
                }
            } else {
                items(
                    items = uiState.subjectProgress,
                    key = { "insight_subj_${it.subject.id}" }
                ) { row ->
                    SubjectInsightRowCard(row = row)
                }
            }

            item(key = "insights_bottom_spacer") {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun InsightMetricCard(
    title: String,
    value: String,
    caption: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                style = StatDigitsStyle,
                color = accent,
                maxLines = 1
            )
            Text(
                text = caption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeeklyFocusChartCard(points: List<DayBarPoint>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val maxMinutes = remember(points) {
        (points.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(30)
    }
    val hasAnyFocus = remember(points) { points.any { it.minutes > 0 } }
    val barBrush = remember(tertiaryColor, primaryColor) {
        Brush.verticalGradient(colors = listOf(tertiaryColor, primaryColor))
    }
    val gridFractions = remember { listOf(0.25f, 0.5f, 0.75f, 1f) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Last 7 Days Focus",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Minutes / day",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!hasAnyFocus) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Complete a focus session to populate your 7-day trend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    if (points.isEmpty()) return@Canvas
                    val slotWidth = size.width / points.size.toFloat()
                    val barWidth = (slotWidth * 0.48f).coerceAtMost(36.dp.toPx())
                    val chartHeight = size.height

                    // Subtle horizontal guidelines
                    gridFractions.forEach { fraction ->
                        val y = chartHeight * (1f - fraction)
                        drawLine(
                            color = gridColor.copy(alpha = 0.6f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    points.forEachIndexed { index, point ->
                        val left = index * slotWidth + (slotWidth - barWidth) / 2f
                        drawRoundRect(
                            color = trackColor.copy(alpha = 0.45f),
                            topLeft = Offset(left, 0f),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                        )

                        val barFraction = (point.minutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                        if (barFraction > 0f) {
                            val barHeight = (chartHeight * barFraction).coerceAtLeast(8.dp.toPx())
                            drawRoundRect(
                                brush = barBrush,
                                topLeft = Offset(left, chartHeight - barHeight),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    points.forEach { point ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${point.minutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (point.minutes > 0) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = point.dayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllPeriodComparisonCard(uiState: InsightsUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Summary Breakdown",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            PeriodRow(
                label = "Today",
                focusText = DateTimeUtils.formatDurationFromSeconds(uiState.todayFocusSeconds),
                details = "${uiState.todaySessionCount} sessions • ${uiState.todayCompletedTasks} tasks"
            )
            PeriodRow(
                label = "Last 7 Days",
                focusText = DateTimeUtils.formatDurationFromSeconds(uiState.weekFocusSeconds),
                details = "${uiState.weekSessionCount} sessions • ${uiState.weekCompletedTasks} tasks"
            )
            PeriodRow(
                label = "All Time",
                focusText = DateTimeUtils.formatDurationFromSeconds(uiState.allTimeFocusSeconds),
                details = "${uiState.allTimeSessionCount} sessions • ${uiState.allTimeCompletedTasks} tasks"
            )
        }
    }
}

@Composable
private fun PeriodRow(label: String, focusText: String, details: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = focusText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun CategoryBreakdownCard(
    subjectProgress: List<SubjectProgressRow>,
    useAllTimeFocus: Boolean
) {
    val totalFocusAcrossCategories = remember(subjectProgress, useAllTimeFocus) {
        subjectProgress.sumOf {
            if (useAllTimeFocus) it.totalFocusSeconds else it.weekFocusSeconds
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_category_breakdown"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Study Categories",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (useAllTimeFocus) "All-time focus" else "7-day focus",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            StudyCategory.entries.forEach { category ->
                val rows = subjectProgress.filter { it.subject.studyCategory == category }
                val subjectCount = rows.size
                val completedTasks = rows.sumOf { it.completedTaskCount }
                val totalTasks = rows.sumOf { it.completedTaskCount + it.pendingTaskCount }
                val focusSeconds = rows.sumOf {
                    if (useAllTimeFocus) it.totalFocusSeconds else it.weekFocusSeconds
                }
                val accent = when (category) {
                    StudyCategory.AL -> MaterialTheme.colorScheme.primary
                    StudyCategory.AAT -> MaterialTheme.colorScheme.tertiary
                    StudyCategory.OTHER -> MaterialTheme.colorScheme.secondary
                }
                val progress = when {
                    totalFocusAcrossCategories > 0 ->
                        focusSeconds.toFloat() / totalFocusAcrossCategories.toFloat()
                    totalTasks > 0 ->
                        completedTasks.toFloat() / totalTasks.toFloat()
                    else -> 0f
                }.coerceIn(0f, 1f)

                val subjectLabel = if (subjectCount == 1) "1 subject" else "$subjectCount subjects"
                val taskLabel = if (totalTasks > 0) "$completedTasks/$totalTasks tasks" else "0 tasks"

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("insights_category_${category.storageValue.lowercase()}"),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$subjectLabel • $taskLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatDurationFromSeconds(focusSeconds),
                            style = MaterialTheme.typography.titleSmall,
                            color = accent
                        )
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        strokeCap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectInsightRowCard(row: SubjectProgressRow) {
    val accent = Color(row.subject.colorHex)
    val weekMinutes = (row.weekFocusSeconds + 30) / 60
    val totalMinutes = (row.totalFocusSeconds + 30) / 60
    val target = row.subject.targetMinutesPerWeek.coerceAtLeast(1)
    val progress = (weekMinutes.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    val totalTasks = row.completedTaskCount + row.pendingTaskCount

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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
                    Column {
                        Text(
                            text = row.subject.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = row.subject.categoryBadgeLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
                Text(
                    text = "${DateTimeUtils.formatDurationMinutes(totalMinutes)} total",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }

            LinearProgressIndicator(
                progress = { progress },
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
                    text = "Week: ${DateTimeUtils.formatDurationMinutes(weekMinutes)} / ${DateTimeUtils.formatDurationMinutes(target)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (totalTasks > 0) {
                        "${row.completedTaskCount}/$totalTasks tasks done"
                    } else {
                        "No tasks linked"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
