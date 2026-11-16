package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.model.SubjectProgressRow
import com.example.data.local.model.TaskWithSubject
import com.example.domain.AatLevel
import com.example.domain.DateTimeUtils
import com.example.domain.StudyCategory
import com.example.ui.components.SaviiEmptyStateCard
import com.example.ui.components.SubjectEditorDialog
import com.example.ui.components.TaskItemCard
import com.example.ui.components.subjectIconVector
import com.example.ui.viewmodel.LearningUiState
import com.example.ui.viewmodel.StudyStep

@Composable
fun LearningScreen(
    uiState: LearningUiState,
    onOpenStep: (StudyStep) -> Unit,
    onNavigateBack: () -> Boolean,
    onSaveSubject: (
        editingSubjectId: Long?,
        name: String,
        category: StudyCategory,
        aatLevel: AatLevel?,
        iconKey: String,
        colorHex: Long,
        targetMinutesPerWeek: Int,
        onSaved: () -> Unit
    ) -> Unit,
    onClearFormError: () -> Unit,
    onDeleteSubject: (Long) -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onAddTaskForSubject: (Long) -> Unit,
    onOpenSubjectInPlanner: (Long) -> Unit,
    onStartFocusForSubject: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddOrEditDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }
    var expandedSubjectId by rememberSaveable { mutableStateOf<Long?>(null) }

    BackHandler(enabled = uiState.currentStep != StudyStep.Home) {
        onNavigateBack()
    }

    if (showAddOrEditDialog) {
        val (defaultCategory, defaultLevel, lockScope) = when (val step = uiState.currentStep) {
            StudyStep.Home -> Triple(StudyCategory.AL, null, false)
            StudyStep.AL -> Triple(StudyCategory.AL, null, true)
            StudyStep.AatFolders -> Triple(StudyCategory.AAT, AatLevel.LEVEL_1, false)
            is StudyStep.AatLevelFolder -> Triple(StudyCategory.AAT, step.level, true)
            StudyStep.Other -> Triple(StudyCategory.OTHER, null, true)
        }

        SubjectEditorDialog(
            initialSubject = editingSubject,
            initialCategory = defaultCategory,
            initialAatLevel = defaultLevel,
            lockCategoryAndLevel = lockScope,
            externalError = uiState.formError,
            onClearError = onClearFormError,
            onDismiss = {
                onClearFormError()
                showAddOrEditDialog = false
                editingSubject = null
            },
            onSave = { name, category, aatLevel, iconKey, colorHex, targetMins ->
                onSaveSubject(
                    editingSubject?.id,
                    name,
                    category,
                    aatLevel,
                    iconKey,
                    colorHex,
                    targetMins
                ) {
                    showAddOrEditDialog = false
                    editingSubject = null
                }
            }
        )
    }

    subjectToDelete?.let { pendingDelete ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Remove Subject?") },
            text = {
                Text(
                    "\"${pendingDelete.name}\" will be removed from ${pendingDelete.categoryBadgeLabel}. Existing tasks and focus history will be kept without a subject tag."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSubject(pendingDelete.id)
                        subjectToDelete = null
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { subjectToDelete = null },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    val openAddSubjectDialog = {
        onClearFormError()
        editingSubject = null
        showAddOrEditDialog = true
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = openAddSubjectDialog,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("learning_add_subject_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add subject"
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedContent(
                targetState = uiState.currentStep,
                transitionSpec = {
                    fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(140))
                },
                label = "study_step_transition"
            ) { step ->
                when (step) {
                    StudyStep.Home -> {
                        StudyHomeContent(
                            uiState = uiState,
                            onSelectAl = { onOpenStep(StudyStep.AL) },
                            onSelectAat = { onOpenStep(StudyStep.AatFolders) },
                            onSelectOther = { onOpenStep(StudyStep.Other) },
                            onAddSubject = openAddSubjectDialog
                        )
                    }

                    StudyStep.AL -> {
                        SubjectCollectionScreen(
                            title = "A/L",
                            subtitle = "Advanced Level subjects",
                            emptyTitle = "No A/L subjects yet",
                            emptySubtitle = "Add your A/L subjects to track weekly goals, planner tasks, and focus time.",
                            emptyActionLabel = "Add A/L Subject",
                            subjects = uiState.alSubjects,
                            tasksBySubjectId = uiState.tasksBySubjectId,
                            expandedSubjectId = expandedSubjectId,
                            onBack = { onNavigateBack() },
                            onAddSubject = openAddSubjectDialog,
                            onToggleExpand = { id ->
                                expandedSubjectId = if (expandedSubjectId == id) null else id
                            },
                            onEdit = { subject ->
                                onClearFormError()
                                editingSubject = subject
                                showAddOrEditDialog = true
                            },
                            onDelete = { subject -> subjectToDelete = subject },
                            onToggleTask = onToggleTask,
                            onAddTask = onAddTaskForSubject,
                            onOpenPlanner = onOpenSubjectInPlanner,
                            onStartFocus = onStartFocusForSubject
                        )
                    }

                    StudyStep.AatFolders -> {
                        AatFoldersContent(
                            uiState = uiState,
                            onBack = { onNavigateBack() },
                            onSelectLevel = { level -> onOpenStep(StudyStep.AatLevelFolder(level)) }
                        )
                    }

                    is StudyStep.AatLevelFolder -> {
                        val levelSubjects = uiState.aatSubjectsByLevel[step.level].orEmpty()
                        SubjectCollectionScreen(
                            title = "AAT • ${step.level.displayName}",
                            subtitle = "Independent ${step.level.displayName} subject collection",
                            emptyTitle = "No ${step.level.displayName} subjects yet",
                            emptySubtitle = "Add subjects for AAT ${step.level.displayName} without mixing them with other levels.",
                            emptyActionLabel = "Add ${step.level.displayName} Subject",
                            subjects = levelSubjects,
                            tasksBySubjectId = uiState.tasksBySubjectId,
                            expandedSubjectId = expandedSubjectId,
                            onBack = { onNavigateBack() },
                            onAddSubject = openAddSubjectDialog,
                            onToggleExpand = { id ->
                                expandedSubjectId = if (expandedSubjectId == id) null else id
                            },
                            onEdit = { subject ->
                                onClearFormError()
                                editingSubject = subject
                                showAddOrEditDialog = true
                            },
                            onDelete = { subject -> subjectToDelete = subject },
                            onToggleTask = onToggleTask,
                            onAddTask = onAddTaskForSubject,
                            onOpenPlanner = onOpenSubjectInPlanner,
                            onStartFocus = onStartFocusForSubject
                        )
                    }

                    StudyStep.Other -> {
                        SubjectCollectionScreen(
                            title = "OTHER",
                            subtitle = "Additional courses & self-study topics",
                            emptyTitle = "No OTHER subjects yet",
                            emptySubtitle = "Keep extra skills, certifications, or personal study areas separate from A/L and AAT.",
                            emptyActionLabel = "Add OTHER Subject",
                            subjects = uiState.otherSubjects,
                            tasksBySubjectId = uiState.tasksBySubjectId,
                            expandedSubjectId = expandedSubjectId,
                            onBack = { onNavigateBack() },
                            onAddSubject = openAddSubjectDialog,
                            onToggleExpand = { id ->
                                expandedSubjectId = if (expandedSubjectId == id) null else id
                            },
                            onEdit = { subject ->
                                onClearFormError()
                                editingSubject = subject
                                showAddOrEditDialog = true
                            },
                            onDelete = { subject -> subjectToDelete = subject },
                            onToggleTask = onToggleTask,
                            onAddTask = onAddTaskForSubject,
                            onOpenPlanner = onOpenSubjectInPlanner,
                            onStartFocus = onStartFocusForSubject
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyHomeContent(
    uiState: LearningUiState,
    onSelectAl: () -> Unit,
    onSelectAat: () -> Unit,
    onSelectOther: () -> Unit,
    onAddSubject: () -> Unit
) {
    val totalWeekMinutes = remember(uiState.subjects) {
        (uiState.subjects.sumOf { it.weekFocusSeconds } + 30) / 60
    }
    val totalOpenTasks = remember(uiState.subjects) {
        uiState.subjects.sumOf { it.pendingTaskCount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("study_home_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "study_header") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Study",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Organize A/L, AAT levels, and independent study subjects.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "study_overview_banner") {
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
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${uiState.totalSubjectsCount} active subjects",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "$totalOpenTasks open tasks • ${DateTimeUtils.formatDurationMinutes(totalWeekMinutes)} studied this week",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        FilledTonalButton(
                            onClick = onAddSubject,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("study_home_add_subject_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            }
        }

        item(key = "study_cat_al") {
            StudyCategoryCard(
                title = "A/L",
                subtitle = "Advanced Level subjects",
                subjectCount = uiState.alSubjects.size,
                icon = Icons.Filled.School,
                accentColor = MaterialTheme.colorScheme.primary,
                testTag = "study_category_al",
                onClick = onSelectAl
            )
        }

        item(key = "study_cat_aat") {
            StudyCategoryCard(
                title = "AAT",
                subtitle = "Level 1 • Level 2 • Level 3 folders",
                subjectCount = uiState.aatTotalCount,
                icon = Icons.Filled.AccountBalance,
                accentColor = MaterialTheme.colorScheme.tertiary,
                testTag = "study_category_aat",
                onClick = onSelectAat
            )
        }

        item(key = "study_cat_other") {
            StudyCategoryCard(
                title = "OTHER",
                subtitle = "Independent courses & extra skills",
                subjectCount = uiState.otherSubjects.size,
                icon = Icons.Filled.AutoStories,
                accentColor = MaterialTheme.colorScheme.secondary,
                testTag = "study_category_other",
                onClick = onSelectOther
            )
        }
    }
}

@Composable
private fun StudyCategoryCard(
    title: String,
    subtitle: String,
    subjectCount: Int,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    val countLabel = if (subjectCount == 1) "1 subject" else "$subjectCount subjects"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open $title",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AatFoldersContent(
    uiState: LearningUiState,
    onBack: () -> Unit,
    onSelectLevel: (AatLevel) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("study_aat_folders_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "aat_header") {
            StudySubscreenHeader(
                title = "AAT",
                subtitle = "Select a level folder • ${uiState.aatTotalCount} total subjects",
                onBack = onBack,
                backTestTag = "study_aat_back_button"
            )
        }

        items(
            items = AatLevel.entries,
            key = { "aat_folder_${it.levelNumber}" }
        ) { level ->
            val levelSubjects = uiState.aatSubjectsByLevel[level].orEmpty()
            val countText = if (levelSubjects.size == 1) "1 subject" else "${levelSubjects.size} subjects"
            val openTasks = levelSubjects.sumOf { it.pendingTaskCount }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .clickable { onSelectLevel(level) }
                    .testTag("study_aat_folder_${level.levelNumber}"),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = level.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (openTasks > 0) {
                                "$countText • $openTasks open tasks"
                            } else {
                                countText
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "${levelSubjects.size}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Open ${level.displayName}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectCollectionScreen(
    title: String,
    subtitle: String,
    emptyTitle: String,
    emptySubtitle: String,
    emptyActionLabel: String,
    subjects: List<SubjectProgressRow>,
    tasksBySubjectId: Map<Long, List<TaskWithSubject>>,
    expandedSubjectId: Long?,
    onBack: () -> Unit,
    onAddSubject: () -> Unit,
    onToggleExpand: (Long) -> Unit,
    onEdit: (SubjectEntity) -> Unit,
    onDelete: (SubjectEntity) -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onAddTask: (Long) -> Unit,
    onOpenPlanner: (Long) -> Unit,
    onStartFocus: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 840.dp)
            .testTag("study_subject_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "collection_header") {
            StudySubscreenHeader(
                title = title,
                subtitle = "$subtitle • ${subjects.size} ${if (subjects.size == 1) "subject" else "subjects"}",
                onBack = onBack,
                backTestTag = "study_subscreen_back_button"
            )
        }

        if (subjects.isEmpty()) {
            item(key = "collection_empty") {
                SaviiEmptyStateCard(
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    title = emptyTitle,
                    subtitle = emptySubtitle,
                    actionLabel = emptyActionLabel,
                    actionTestTag = "learning_empty_add_button",
                    onAction = onAddSubject
                )
            }
        } else {
            items(
                items = subjects,
                key = { "subject_${it.subject.id}" }
            ) { row ->
                val isExpanded = expandedSubjectId == row.subject.id
                val subjectTasks = tasksBySubjectId[row.subject.id].orEmpty()

                SubjectDetailCard(
                    row = row,
                    tasks = subjectTasks,
                    expanded = isExpanded,
                    onToggleExpand = { onToggleExpand(row.subject.id) },
                    onEdit = { onEdit(row.subject) },
                    onDelete = { onDelete(row.subject) },
                    onToggleTask = onToggleTask,
                    onAddTask = { onAddTask(row.subject.id) },
                    onOpenPlanner = { onOpenPlanner(row.subject.id) },
                    onStartFocus = { onStartFocus(row.subject.id) }
                )
            }
        }
    }
}

@Composable
private fun StudySubscreenHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    backTestTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(backTestTag)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back"
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SubjectDetailCard(
    row: SubjectProgressRow,
    tasks: List<TaskWithSubject>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onAddTask: () -> Unit,
    onOpenPlanner: () -> Unit,
    onStartFocus: () -> Unit
) {
    val subject = row.subject
    val accent = Color(subject.colorHex)
    val weekMins = (row.weekFocusSeconds + 30) / 60
    val totalMins = (row.totalFocusSeconds + 30) / 60
    val targetMins = subject.targetMinutesPerWeek.coerceAtLeast(1)
    val ratio = (weekMins.toFloat() / targetMins.toFloat()).coerceIn(0f, 1f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subject_card_${subject.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = subjectIconVector(subject.iconOption),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${row.pendingTaskCount} pending • ${row.completedTaskCount} completed • ${DateTimeUtils.formatDurationMinutes(totalMins)} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("subject_edit_${subject.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit ${subject.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("subject_delete_${subject.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete ${subject.name}",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse tasks" else "Expand tasks"
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Weekly goal: ${DateTimeUtils.formatDurationMinutes(weekMins)} / ${DateTimeUtils.formatDurationMinutes(targetMins)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${(ratio * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
                LinearProgressIndicator(
                    progress = { ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp),
                    color = accent,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    strokeCap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onStartFocus,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Focus", maxLines = 1)
                }
                OutlinedButton(
                    onClick = onAddTask,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddTask,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Task", maxLines = 1)
                }
                TextButton(
                    onClick = onOpenPlanner,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text("Planner", maxLines = 1)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tasks.isEmpty()) {
                        Text(
                            text = "No tasks linked to ${subject.name} yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        tasks.take(5).forEach { taskItem ->
                            TaskItemCard(
                                item = taskItem,
                                onToggleComplete = {
                                    onToggleTask(taskItem.task.id, taskItem.task.isCompleted)
                                }
                            )
                        }
                        if (tasks.size > 5) {
                            TextButton(onClick = onOpenPlanner) {
                                Text("View all ${tasks.size} tasks in Planner")
                            }
                        }
                    }
                }
            }
        }
    }
}
