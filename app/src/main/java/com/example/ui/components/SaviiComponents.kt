package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.model.TaskWithSubject
import com.example.domain.AatLevel
import com.example.domain.DateTimeUtils
import com.example.domain.StudyCategory
import com.example.domain.SubjectIconOption
import com.example.domain.TaskPriority
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor
import com.example.ui.theme.SubjectColorPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun SaviiBrandLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    Surface(
        modifier = modifier.size(size),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
        color = Color(0xFF01010E)
    ) {
        Image(
            painter = painterResource(id = R.drawable.savii_focus_logo),
            contentDescription = stringResource(id = R.string.logo_content_description),
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
fun SaviiSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionTestTag: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(
                onClick = onActionClick,
                modifier = if (actionTestTag != null) Modifier.testTag(actionTestTag) else Modifier
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
fun SaviiEmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionTestTag: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    modifier = if (actionTestTag != null) {
                        Modifier
                            .minimumInteractiveComponentSize()
                            .testTag(actionTestTag)
                    } else {
                        Modifier.minimumInteractiveComponentSize()
                    }
                ) {
                    Text(text = actionLabel)
                }
            }
        }
    }
}

@Composable
fun PriorityPill(priority: TaskPriority, modifier: Modifier = Modifier) {
    val accent = when (priority) {
        TaskPriority.HIGH -> PriorityHighColor
        TaskPriority.MEDIUM -> PriorityMediumColor
        TaskPriority.LOW -> PriorityLowColor
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = accent.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Text(
            text = priority.label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun SubjectPill(
    name: String,
    colorHex: Long,
    modifier: Modifier = Modifier
) {
    val dotColor = Color(colorHex)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = dotColor.copy(alpha = 0.14f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskItemCard(
    item: TaskWithSubject,
    onToggleComplete: () -> Unit,
    modifier: Modifier = Modifier,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onStartFocus: (() -> Unit)? = null
) {
    val task = item.task
    val priority = TaskPriority.fromLevel(task.priority)
    val isOverdue = remember(task.dueDateEpochDay, task.isCompleted) {
        DateTimeUtils.isOverdue(task.dueDateEpochDay, task.isCompleted)
    }
    val dueLabel = remember(task.dueDateEpochDay) {
        DateTimeUtils.formatDueDateLabel(task.dueDateEpochDay)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = false) {},
        shape = MaterialTheme.shapes.medium,
        color = if (task.isCompleted) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isOverdue) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.tertiary,
                    checkmarkColor = MaterialTheme.colorScheme.onTertiary
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("task_checkbox_${task.id}")
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp, end = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                    ),
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PriorityPill(priority = priority)

                    if (item.subjectName != null && item.subjectColorHex != null) {
                        SubjectPill(
                            name = item.subjectDisplayName ?: item.subjectName,
                            colorHex = item.subjectColorHex
                        )
                    }

                    if (task.dueDateEpochDay != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOverdue) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DateRange,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isOverdue) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = if (isOverdue) "Overdue • $dueLabel" else dueLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOverdue) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${task.estimatedMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (onStartFocus != null && !task.isCompleted) {
                    IconButton(
                        onClick = onStartFocus,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("task_focus_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Start focus for ${task.title}",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (onEdit != null) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("task_edit_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit ${task.title}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("task_delete_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete ${task.title}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditorBottomSheet(
    initialTask: TaskEntity?,
    subjects: List<SubjectEntity>,
    preselectedSubjectId: Long? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        notes: String,
        subjectId: Long?,
        priority: TaskPriority,
        dueDateEpochDay: Long?,
        estimatedMinutes: Int
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(initialTask) { mutableStateOf(initialTask?.title ?: "") }
    var notes by remember(initialTask) { mutableStateOf(initialTask?.notes ?: "") }
    var selectedSubjectId by remember(initialTask, preselectedSubjectId) {
        mutableStateOf(initialTask?.subjectId ?: preselectedSubjectId)
    }
    var selectedPriority by remember(initialTask) {
        mutableStateOf(TaskPriority.fromLevel(initialTask?.priority ?: TaskPriority.MEDIUM.level))
    }
    var dueDateEpochDay by remember(initialTask) {
        mutableStateOf<Long?>(
            if (initialTask != null) initialTask.dueDateEpochDay else DateTimeUtils.todayEpochDay()
        )
    }
    var estimatedMinutes by remember(initialTask) {
        mutableIntStateOf(initialTask?.estimatedMinutes ?: 25)
    }
    var titleError by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val todayEpoch = remember { DateTimeUtils.todayEpochDay() }

    if (showDatePicker) {
        val initialSelectedMs = (dueDateEpochDay ?: todayEpoch).let { epoch ->
            LocalDate.ofEpochDay(epoch).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialSelectedMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDateEpochDay = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                                .toEpochDay()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (initialTask == null) "New Study Task" else "Edit Task",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text("Task title") },
                placeholder = { Text("e.g., Complete Calculus problem set") },
                singleLine = true,
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Task title is required.") }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input")
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                placeholder = { Text("Chapters, formulas, or key deliverables") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_notes_input")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Priority",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.entries.forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority.label) },
                            modifier = Modifier.testTag("task_priority_${priority.name.lowercase()}")
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Due Date",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dueDateEpochDay == todayEpoch,
                        onClick = { dueDateEpochDay = todayEpoch },
                        label = { Text("Today") }
                    )
                    FilterChip(
                        selected = dueDateEpochDay == todayEpoch + 1,
                        onClick = { dueDateEpochDay = todayEpoch + 1 },
                        label = { Text("Tomorrow") }
                    )
                    FilterChip(
                        selected = dueDateEpochDay == null,
                        onClick = { dueDateEpochDay = null },
                        label = { Text("No date") }
                    )
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Choose custom due date",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(DateTimeUtils.formatDueDateLabel(dueDateEpochDay))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Subject",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subjects.isEmpty()) {
                    Text(
                        text = "No subjects yet. Add subjects in the Study tab to organize tasks.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedSubjectId == null,
                            onClick = { selectedSubjectId = null },
                            label = { Text("General") }
                        )
                        subjects.forEach { subject ->
                            FilterChip(
                                selected = selectedSubjectId == subject.id,
                                onClick = { selectedSubjectId = subject.id },
                                label = { Text(subject.displayWithCategory) }
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Estimated Focus",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 25, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = estimatedMinutes == mins,
                            onClick = { estimatedMinutes = mins },
                            label = { Text("${mins}m") }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            titleError = true
                        } else {
                            onSave(
                                title,
                                notes,
                                selectedSubjectId,
                                selectedPriority,
                                dueDateEpochDay,
                                estimatedMinutes
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_task_button")
                ) {
                    Text(if (initialTask == null) "Create Task" else "Save Changes")
                }
            }
        }
    }
}

fun subjectIconVector(option: SubjectIconOption): ImageVector = when (option) {
    SubjectIconOption.BOOK -> Icons.Filled.Book
    SubjectIconOption.CALCULATE -> Icons.Filled.Calculate
    SubjectIconOption.SCIENCE -> Icons.Filled.Science
    SubjectIconOption.ACCOUNTING -> Icons.Filled.AccountBalance
    SubjectIconOption.COMPUTER -> Icons.Filled.Computer
    SubjectIconOption.LANGUAGE -> Icons.Filled.Language
    SubjectIconOption.PSYCHOLOGY -> Icons.Filled.Psychology
    SubjectIconOption.ARCHITECTURE -> Icons.Filled.Architecture
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectEditorDialog(
    initialSubject: SubjectEntity?,
    initialCategory: StudyCategory = StudyCategory.AL,
    initialAatLevel: AatLevel? = null,
    lockCategoryAndLevel: Boolean = false,
    externalError: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        category: StudyCategory,
        aatLevel: AatLevel?,
        iconKey: String,
        colorHex: Long,
        targetMinutesPerWeek: Int
    ) -> Unit
) {
    var name by remember(initialSubject) { mutableStateOf(initialSubject?.name ?: "") }
    var selectedCategory by remember(initialSubject, initialCategory) {
        mutableStateOf(initialSubject?.studyCategory ?: initialCategory)
    }
    var selectedAatLevel by remember(initialSubject, initialAatLevel) {
        mutableStateOf(initialSubject?.aatLevelEnum ?: initialAatLevel ?: AatLevel.LEVEL_1)
    }
    var selectedIconKey by remember(initialSubject) {
        mutableStateOf(initialSubject?.iconKey ?: SubjectIconOption.BOOK.key)
    }
    var selectedColorHex by remember(initialSubject) {
        mutableStateOf(initialSubject?.colorHex ?: SubjectColorPalette.first())
    }
    var weeklyTargetMinutes by remember(initialSubject) {
        mutableIntStateOf(initialSubject?.targetMinutesPerWeek ?: 180)
    }
    var localEmptyError by remember { mutableStateOf(false) }

    val errorMessage = when {
        localEmptyError -> "Please enter a subject name."
        externalError != null -> externalError
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialSubject == null) "Add Subject" else "Edit Subject",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (lockCategoryAndLevel && initialSubject == null) {
                    val scopeText = when (selectedCategory) {
                        StudyCategory.AL -> "Category: A/L"
                        StudyCategory.AAT -> "Category: AAT • ${selectedAatLevel.displayName}"
                        StudyCategory.OTHER -> "Category: OTHER"
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = scopeText,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelLarge
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StudyCategory.entries.forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = category
                                        if (externalError != null) onClearError()
                                    },
                                    label = { Text(category.displayName) },
                                    modifier = Modifier.testTag("subject_category_${category.storageValue.lowercase()}")
                                )
                            }
                        }
                    }

                    if (selectedCategory == StudyCategory.AAT) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "AAT Level",
                                style = MaterialTheme.typography.labelLarge
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                AatLevel.entries.forEach { level ->
                                    FilterChip(
                                        selected = selectedAatLevel == level,
                                        onClick = {
                                            selectedAatLevel = level
                                            if (externalError != null) onClearError()
                                        },
                                        label = { Text(level.displayName) },
                                        modifier = Modifier.testTag("subject_aat_level_${level.levelNumber}")
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        localEmptyError = false
                        if (externalError != null) onClearError()
                    },
                    label = { Text("Subject name") },
                    placeholder = { Text("Enter subject name") },
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = errorMessage?.let { msg -> { Text(msg) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("subject_name_input")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Subject Icon",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubjectIconOption.entries.forEach { iconOption ->
                            val selected = selectedIconKey == iconOption.key
                            Surface(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedIconKey = iconOption.key }
                                    .testTag("subject_icon_${iconOption.key}"),
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    Color(selectedColorHex).copy(alpha = 0.24f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                border = BorderStroke(
                                    width = if (selected) 1.5.dp else 1.dp,
                                    color = if (selected) Color(selectedColorHex) else MaterialTheme.colorScheme.outlineVariant
                                )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = subjectIconVector(iconOption),
                                        contentDescription = iconOption.label,
                                        tint = if (selected) Color(selectedColorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Accent Color",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SubjectColorPalette.forEach { hex ->
                            val selected = selectedColorHex == hex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(hex))
                                    .then(
                                        if (selected) {
                                            Modifier.border(
                                                2.dp,
                                                MaterialTheme.colorScheme.onSurface,
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected color",
                                        tint = Color(0xFF090914),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Weekly Focus Target",
                        style = MaterialTheme.typography.labelLarge
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(120, 180, 300, 420).forEach { mins ->
                            FilterChip(
                                selected = weeklyTargetMinutes == mins,
                                onClick = { weeklyTargetMinutes = mins },
                                label = { Text("${mins / 60}h") }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isEmpty()) {
                        localEmptyError = true
                    } else {
                        val effectiveCategory = selectedCategory
                        val effectiveLevel = if (effectiveCategory == StudyCategory.AAT) {
                            selectedAatLevel
                        } else {
                            null
                        }
                        onSave(
                            name,
                            effectiveCategory,
                            effectiveLevel,
                            selectedIconKey,
                            selectedColorHex,
                            weeklyTargetMinutes
                        )
                    }
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("save_subject_button")
            ) {
                Text(if (initialSubject == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text("Cancel")
            }
        }
    )
}
