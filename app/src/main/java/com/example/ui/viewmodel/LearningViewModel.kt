package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.model.SubjectProgressRow
import com.example.data.local.model.TaskWithSubject
import com.example.data.repository.SaviiFocusRepository
import com.example.data.repository.SubjectMutationResult
import com.example.domain.AatLevel
import com.example.domain.DateTimeUtils
import com.example.domain.StudyCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface StudyStep {
    data object Home : StudyStep
    data object AL : StudyStep
    data object AatFolders : StudyStep
    data class AatLevelFolder(val level: AatLevel) : StudyStep
    data object Other : StudyStep
}

data class LearningUiState(
    val currentStep: StudyStep = StudyStep.Home,
    val subjects: List<SubjectProgressRow> = emptyList(),
    val alSubjects: List<SubjectProgressRow> = emptyList(),
    val aatSubjectsByLevel: Map<AatLevel, List<SubjectProgressRow>> = emptyMap(),
    val otherSubjects: List<SubjectProgressRow> = emptyList(),
    val tasksBySubjectId: Map<Long, List<TaskWithSubject>> = emptyMap(),
    val formError: String? = null,
    val isLoaded: Boolean = false
) {
    val totalSubjectsCount: Int get() = subjects.size
    val aatTotalCount: Int get() = aatSubjectsByLevel.values.sumOf { it.size }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LearningViewModel(
    private val repository: SaviiFocusRepository
) : ViewModel() {

    private val currentStepState = MutableStateFlow<StudyStep>(StudyStep.Home)
    private val formErrorState = MutableStateFlow<String?>(null)
    private val currentEpochDay = MutableStateFlow(DateTimeUtils.todayEpochDay())

    private val subjectsFlow = currentEpochDay.flatMapLatest { epochDay ->
        val weekStart = epochDay - 6L
        repository.observeSubjectsWithProgress(weekStart)
    }

    val uiState: StateFlow<LearningUiState> = combine(
        currentStepState,
        subjectsFlow,
        repository.observeAllTasksWithSubject(),
        formErrorState
    ) { step, subjects, allTasks, formError ->
        val groupedTasks = allTasks
            .filter { it.task.subjectId != null }
            .groupBy { it.task.subjectId!! }

        val alList = subjects.filter { it.subject.studyCategory == StudyCategory.AL }
        val otherList = subjects.filter { it.subject.studyCategory == StudyCategory.OTHER }
        val aatByLevel = AatLevel.entries.associateWith { level ->
            subjects.filter {
                it.subject.studyCategory == StudyCategory.AAT &&
                    it.subject.aatLevel == level.levelNumber
            }
        }

        LearningUiState(
            currentStep = step,
            subjects = subjects,
            alSubjects = alList,
            aatSubjectsByLevel = aatByLevel,
            otherSubjects = otherList,
            tasksBySubjectId = groupedTasks,
            formError = formError,
            isLoaded = true
        )
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LearningUiState()
        )

    fun refreshDate() {
        val latest = DateTimeUtils.todayEpochDay()
        if (currentEpochDay.value != latest) {
            currentEpochDay.value = latest
        }
    }

    fun openStep(step: StudyStep) {
        currentStepState.value = step
    }

    fun navigateBack(): Boolean {
        return when (val current = currentStepState.value) {
            StudyStep.Home -> false
            StudyStep.AL,
            StudyStep.AatFolders,
            StudyStep.Other -> {
                currentStepState.value = StudyStep.Home
                true
            }
            is StudyStep.AatLevelFolder -> {
                val _level = current.level
                currentStepState.value = StudyStep.AatFolders
                true
            }
        }
    }

    fun clearFormError() {
        formErrorState.value = null
    }

    fun saveSubject(
        editingSubjectId: Long?,
        rawName: String,
        category: StudyCategory,
        aatLevel: AatLevel?,
        iconKey: String,
        colorHex: Long,
        targetMinutesPerWeek: Int,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val result = if (editingSubjectId == null) {
                repository.addSubject(
                    rawName = rawName,
                    colorHex = colorHex,
                    targetMinutesPerWeek = targetMinutesPerWeek,
                    category = category,
                    aatLevel = aatLevel,
                    iconKey = iconKey
                )
            } else {
                repository.updateSubject(
                    subjectId = editingSubjectId,
                    rawName = rawName,
                    colorHex = colorHex,
                    targetMinutesPerWeek = targetMinutesPerWeek,
                    category = category,
                    aatLevel = aatLevel,
                    iconKey = iconKey
                )
            }

            when (result) {
                is SubjectMutationResult.Success -> {
                    formErrorState.value = null
                    onSaved()
                }
                SubjectMutationResult.EmptyName -> {
                    formErrorState.value = "Please enter a subject name."
                }
                SubjectMutationResult.MissingAatLevel -> {
                    formErrorState.value = "Please select Level 1, Level 2, or Level 3 for AAT."
                }
                SubjectMutationResult.DuplicateName -> {
                    val scopeLabel = when (category) {
                        StudyCategory.AL -> StudyCategory.AL.displayName
                        StudyCategory.AAT -> "AAT ${aatLevel?.displayName.orEmpty()}".trim()
                        StudyCategory.OTHER -> StudyCategory.OTHER.displayName
                    }
                    formErrorState.value = "A subject with this name already exists in $scopeLabel."
                }
                SubjectMutationResult.NotFound -> {
                    formErrorState.value = "This subject is no longer available."
                }
            }
        }
    }

    fun deleteSubject(subjectId: Long) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
        }
    }

    fun toggleTaskCompletion(taskId: Long, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            repository.setTaskCompleted(taskId, !currentlyCompleted)
        }
    }
}
