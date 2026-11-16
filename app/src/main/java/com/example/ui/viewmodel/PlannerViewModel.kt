package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.SubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.model.TaskWithSubject
import com.example.data.repository.SaviiFocusRepository
import com.example.domain.DateTimeUtils
import com.example.domain.PlannerFilter
import com.example.domain.TaskPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlannerUiState(
    val tasks: List<TaskWithSubject> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList(),
    val selectedFilter: PlannerFilter = PlannerFilter.ALL,
    val selectedPriority: TaskPriority? = null,
    val selectedSubjectId: Long? = null,
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val isLoaded: Boolean = false
)

class PlannerViewModel(
    private val repository: SaviiFocusRepository
) : ViewModel() {

    private val filterState = MutableStateFlow(PlannerFilter.ALL)
    private val priorityFilterState = MutableStateFlow<TaskPriority?>(null)
    private val subjectFilterState = MutableStateFlow<Long?>(null)
    private val searchQueryState = MutableStateFlow("")
    val searchQuery: StateFlow<String> = searchQueryState.asStateFlow()
    private var lastDeletedTask: TaskEntity? = null

    private data class FilterCriteria(
        val filter: PlannerFilter,
        val priority: TaskPriority?,
        val subjectId: Long?,
        val query: String
    )

    private val criteriaFlow = combine(
        filterState,
        priorityFilterState,
        subjectFilterState,
        searchQueryState
    ) { filter, priority, subjectId, query ->
        FilterCriteria(filter, priority, subjectId, query)
    }

    val uiState: StateFlow<PlannerUiState> = combine(
        repository.observeAllTasksWithSubject(),
        repository.observeAllSubjects(),
        criteriaFlow
    ) { allTasks, subjects, criteria ->
        val today = DateTimeUtils.todayEpochDay()
        val normalizedQuery = criteria.query.trim().lowercase()

        val filtered = allTasks.filter { item ->
            val task = item.task
            val matchesFilter = when (criteria.filter) {
                PlannerFilter.ALL -> true
                PlannerFilter.TODAY -> !task.isCompleted &&
                    task.dueDateEpochDay != null &&
                    task.dueDateEpochDay <= today
                PlannerFilter.UPCOMING -> !task.isCompleted &&
                    (task.dueDateEpochDay == null || task.dueDateEpochDay > today)
                PlannerFilter.COMPLETED -> task.isCompleted
            }
            val matchesPriority = criteria.priority == null || task.priority == criteria.priority.level
            val matchesSubject = criteria.subjectId == null || task.subjectId == criteria.subjectId
            val matchesSearch = normalizedQuery.isEmpty() ||
                task.title.lowercase().contains(normalizedQuery) ||
                task.notes.lowercase().contains(normalizedQuery) ||
                (item.subjectName?.lowercase()?.contains(normalizedQuery) == true) ||
                (item.subjectDisplayName?.lowercase()?.contains(normalizedQuery) == true)

            matchesFilter && matchesPriority && matchesSubject && matchesSearch
        }

        PlannerUiState(
            tasks = filtered,
            subjects = subjects,
            selectedFilter = criteria.filter,
            selectedPriority = criteria.priority,
            selectedSubjectId = criteria.subjectId,
            searchQuery = criteria.query,
            totalCount = allTasks.size,
            pendingCount = allTasks.count { !it.task.isCompleted },
            completedCount = allTasks.count { it.task.isCompleted },
            isLoaded = true
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlannerUiState()
        )

    fun setFilter(filter: PlannerFilter) {
        filterState.value = filter
    }

    fun setPriorityFilter(priority: TaskPriority?) {
        priorityFilterState.value = if (priorityFilterState.value == priority) null else priority
    }

    fun setSubjectFilter(subjectId: Long?) {
        subjectFilterState.value = subjectId
    }

    fun setSearchQuery(query: String) {
        searchQueryState.value = query
    }

    fun saveTask(
        editingTaskId: Long?,
        title: String,
        notes: String,
        subjectId: Long?,
        priority: TaskPriority,
        dueDateEpochDay: Long?,
        estimatedMinutes: Int,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val savedId = repository.saveTask(
                taskId = editingTaskId,
                title = title,
                notes = notes,
                subjectId = subjectId,
                priority = priority,
                dueDateEpochDay = dueDateEpochDay,
                estimatedMinutes = estimatedMinutes
            )
            if (savedId != null) {
                onSaved()
            }
        }
    }

    fun toggleTaskCompleted(taskId: Long, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompleted(taskId, !currentlyCompleted)
        }
    }

    fun deleteTask(task: TaskEntity) {
        lastDeletedTask = task
        viewModelScope.launch {
            repository.deleteTask(task.id)
        }
    }

    fun undoLastDelete() {
        val taskToRestore = lastDeletedTask ?: return
        lastDeletedTask = null
        viewModelScope.launch {
            repository.restoreTask(taskToRestore)
        }
    }
}
