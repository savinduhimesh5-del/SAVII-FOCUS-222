package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.TaskEntity
import com.example.data.local.model.TaskWithSubject
import com.example.domain.PlannerFilter
import com.example.domain.TaskPriority
import com.example.ui.components.SaviiEmptyStateCard
import com.example.ui.components.TaskItemCard
import com.example.ui.viewmodel.PlannerUiState
import kotlinx.coroutines.launch

@Composable
fun PlannerScreen(
    uiState: PlannerUiState,
    onFilterSelected: (PlannerFilter) -> Unit,
    onPriorityFilterSelected: (TaskPriority?) -> Unit,
    onSubjectFilterSelected: (Long?) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleTask: (Long, Boolean) -> Unit,
    onOpenNewTask: () -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onUndoDelete: () -> Unit,
    onStartFocusForTask: (TaskWithSubject) -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = uiState.searchQuery
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var searchInput by rememberSaveable { mutableStateOf(searchQuery) }

    // Reconcile saved input vs live ViewModel state on entry or process recreation
    // without overwriting in-flight keystrokes during rapid typing.
    LaunchedEffect(Unit) {
        if (searchInput.isNotEmpty() && searchQuery.isEmpty()) {
            onSearchQueryChange(searchInput)
        } else if (searchInput != searchQuery) {
            searchInput = searchQuery
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onOpenNewTask,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("planner_add_task_fab")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create task"
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .testTag("planner_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(key = "planner_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Study Planner",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "${uiState.pendingCount} pending • ${uiState.completedCount} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item(key = "planner_search") {
                    OutlinedTextField(
                        value = searchInput,
                        onValueChange = { next ->
                            searchInput = next
                            onSearchQueryChange(next)
                        },
                        placeholder = { Text("Search tasks or subjects") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchInput.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        searchInput = ""
                                        onSearchQueryChange("")
                                    },
                                    modifier = Modifier.testTag("planner_search_clear")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Clear search"
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("planner_search_input")
                    )
                }

                item(key = "planner_status_filters") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(PlannerFilter.entries) { filter ->
                            FilterChip(
                                selected = uiState.selectedFilter == filter,
                                onClick = { onFilterSelected(filter) },
                                label = { Text(filter.label) },
                                modifier = Modifier.testTag("planner_filter_${filter.name.lowercase()}")
                            )
                        }
                    }
                }

                item(key = "planner_priority_and_subject_filters") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TaskPriority.entries) { priority ->
                            FilterChip(
                                selected = uiState.selectedPriority == priority,
                                onClick = { onPriorityFilterSelected(priority) },
                                label = { Text("${priority.label} Priority") }
                            )
                        }
                        if (uiState.subjects.isNotEmpty()) {
                            item {
                                FilterChip(
                                    selected = uiState.selectedSubjectId == null,
                                    onClick = { onSubjectFilterSelected(null) },
                                    label = { Text("All Subjects") }
                                )
                            }
                            items(
                                items = uiState.subjects,
                                key = { "filter_subj_${it.id}" }
                            ) { subject ->
                                FilterChip(
                                    selected = uiState.selectedSubjectId == subject.id,
                                    onClick = {
                                        onSubjectFilterSelected(
                                            if (uiState.selectedSubjectId == subject.id) null else subject.id
                                        )
                                    },
                                    label = { Text(subject.displayWithCategory) }
                                )
                            }
                        }
                    }
                }

                if (uiState.tasks.isEmpty()) {
                    item(key = "planner_empty") {
                        val hasActiveFilter = uiState.selectedFilter != PlannerFilter.ALL ||
                            uiState.selectedPriority != null ||
                            uiState.selectedSubjectId != null ||
                            uiState.searchQuery.isNotBlank()

                        SaviiEmptyStateCard(
                            icon = Icons.Outlined.Checklist,
                            title = if (hasActiveFilter) "No matching tasks" else "Your planner is clear",
                            subtitle = if (hasActiveFilter) {
                                "Try clearing filters or create a new task."
                            } else {
                                "Add tasks with priorities, due dates, and subjects to stay on track."
                            },
                            actionLabel = if (hasActiveFilter) "Reset Filters" else "Create Task",
                            actionTestTag = "planner_empty_action",
                            onAction = {
                                if (hasActiveFilter) {
                                    onFilterSelected(PlannerFilter.ALL)
                                    onPriorityFilterSelected(null)
                                    onSubjectFilterSelected(null)
                                    searchInput = ""
                                    onSearchQueryChange("")
                                } else {
                                    onOpenNewTask()
                                }
                            }
                        )
                    }
                } else {
                    items(
                        items = uiState.tasks,
                        key = { "planner_task_${it.task.id}" }
                    ) { item ->
                        TaskItemCard(
                            item = item,
                            onToggleComplete = {
                                onToggleTask(item.task.id, item.task.isCompleted)
                            },
                            onEdit = { onEditTask(item.task) },
                            onDelete = {
                                val deleted = item.task
                                onDeleteTask(deleted)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Deleted \"${deleted.title}\"",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        onUndoDelete()
                                    }
                                }
                            },
                            onStartFocus = {
                                onStartFocusForTask(item)
                            }
                        )
                    }
                }
            }
        }
    }
}
