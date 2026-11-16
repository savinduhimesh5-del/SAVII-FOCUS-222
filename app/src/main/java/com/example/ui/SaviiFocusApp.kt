package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.entity.TaskEntity
import com.example.domain.StudyCategory
import com.example.domain.TimerStatus
import com.example.ui.components.SaviiBrandLogoBadge
import com.example.ui.components.SubjectEditorDialog
import com.example.ui.components.TaskEditorBottomSheet
import com.example.ui.navigation.AppDestination
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.FocusTimerScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.LearningScreen
import com.example.ui.screens.PlannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.FocusTimerUiState
import com.example.ui.viewmodel.FocusTimerViewModel
import com.example.ui.viewmodel.InsightsViewModel
import com.example.ui.viewmodel.LearningViewModel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.PlannerViewModel
import com.example.ui.viewmodel.SaviiViewModelFactory
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaviiFocusApp(
    factory: SaviiViewModelFactory,
    mainViewModel: MainViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppDestination.Dashboard.route

    val dashboardViewModel: DashboardViewModel = viewModel(factory = factory)
    val learningViewModel: LearningViewModel = viewModel(factory = factory)
    val plannerViewModel: PlannerViewModel = viewModel(factory = factory)
    val focusTimerViewModel: FocusTimerViewModel = viewModel(factory = factory)
    val insightsViewModel: InsightsViewModel = viewModel(factory = factory)

    // Global quick-create modals usable from Dashboard, Learning, and Planner
    var showTaskSheet by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var preselectedTaskSubjectId by remember { mutableStateOf<Long?>(null) }
    var showQuickSubjectDialog by remember { mutableStateOf(false) }

    // Reconcile wall-clock timer & local calendar dates whenever app returns to foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    focusTimerViewModel.onAppResumed()
                    dashboardViewModel.refreshDate()
                    learningViewModel.refreshDate()
                    insightsViewModel.refreshDate()
                }
                Lifecycle.Event.ON_STOP -> {
                    focusTimerViewModel.onAppBackgrounded()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showTaskSheet) {
        val plannerState by plannerViewModel.uiState.collectAsStateWithLifecycle()
        TaskEditorBottomSheet(
            initialTask = editingTask,
            subjects = plannerState.subjects,
            preselectedSubjectId = preselectedTaskSubjectId,
            onDismiss = {
                showTaskSheet = false
                editingTask = null
                preselectedTaskSubjectId = null
            },
            onSave = { title, notes, subjectId, priority, dueEpochDay, estimatedMinutes ->
                plannerViewModel.saveTask(
                    editingTaskId = editingTask?.id,
                    title = title,
                    notes = notes,
                    subjectId = subjectId,
                    priority = priority,
                    dueDateEpochDay = dueEpochDay,
                    estimatedMinutes = estimatedMinutes
                ) {
                    showTaskSheet = false
                    editingTask = null
                    preselectedTaskSubjectId = null
                }
            }
        )
    }

    if (showQuickSubjectDialog) {
        val learningState by learningViewModel.uiState.collectAsStateWithLifecycle()
        SubjectEditorDialog(
            initialSubject = null,
            initialCategory = StudyCategory.AL,
            initialAatLevel = null,
            lockCategoryAndLevel = false,
            externalError = learningState.formError,
            onClearError = learningViewModel::clearFormError,
            onDismiss = {
                learningViewModel.clearFormError()
                showQuickSubjectDialog = false
            },
            onSave = { name, category, aatLevel, iconKey, colorHex, targetMins ->
                learningViewModel.saveSubject(
                    editingSubjectId = null,
                    rawName = name,
                    category = category,
                    aatLevel = aatLevel,
                    iconKey = iconKey,
                    colorHex = colorHex,
                    targetMinutesPerWeek = targetMins
                ) {
                    showQuickSubjectDialog = false
                }
            }
        )
    }

    fun navigateTopLevel(destination: AppDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp
        val isSettingsScreen = currentRoute == AppDestination.Settings.route

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    navigationIcon = {
                        if (isSettingsScreen) {
                            IconButton(
                                onClick = { navController.popBackStack() },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("settings_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.padding(start = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SaviiBrandLogoBadge(size = 30.dp)
                                TopBarRunningTimerClock(timerStateFlow = focusTimerViewModel.uiState)
                            }
                        }
                    },
                    title = {
                        Text(
                            text = if (isSettingsScreen) {
                                stringResource(R.string.nav_settings)
                            } else {
                                stringResource(R.string.brand_title)
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    actions = {
                        if (!isSettingsScreen) {
                            IconButton(
                                onClick = {
                                    navController.navigate(AppDestination.Settings.route) {
                                        launchSingleTop = true
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("top_bar_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Open Settings"
                                )
                            }
                        }
                    }
                )
            },
            bottomBar = {
                if (!useNavigationRail && !isSettingsScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        AppDestination.primaryTabs.forEach { destination ->
                            val selected = currentRoute == destination.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                        contentDescription = destination.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = destination.label,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail && !isSettingsScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        AppDestination.primaryTabs.forEach { destination ->
                            val selected = currentRoute == destination.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) {
                                            destination.selectedIcon
                                        } else {
                                            destination.unselectedIcon
                                        },
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) },
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = AppDestination.Dashboard.route,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(AppDestination.Dashboard.route) {
                        val dashboardState by dashboardViewModel.uiState.collectAsStateWithLifecycle()
                        DashboardScreen(
                            uiState = dashboardState,
                            onToggleTask = dashboardViewModel::toggleTaskCompleted,
                            onStartFocus = { navigateTopLevel(AppDestination.Focus) },
                            onQuickAddTask = {
                                editingTask = null
                                preselectedTaskSubjectId = null
                                showTaskSheet = true
                            },
                            onQuickAddSubject = {
                                learningViewModel.clearFormError()
                                showQuickSubjectDialog = true
                            },
                            onOpenPlanner = { navigateTopLevel(AppDestination.Planner) },
                            onOpenLearning = { navigateTopLevel(AppDestination.Learning) },
                            onOpenSubjectInPlanner = { subjectId ->
                                plannerViewModel.setSubjectFilter(subjectId)
                                navigateTopLevel(AppDestination.Planner)
                            }
                        )
                    }

                    composable(AppDestination.Learning.route) {
                        val learningState by learningViewModel.uiState.collectAsStateWithLifecycle()
                        LearningScreen(
                            uiState = learningState,
                            onOpenStep = learningViewModel::openStep,
                            onNavigateBack = learningViewModel::navigateBack,
                            onSaveSubject = learningViewModel::saveSubject,
                            onClearFormError = learningViewModel::clearFormError,
                            onDeleteSubject = learningViewModel::deleteSubject,
                            onToggleTask = learningViewModel::toggleTaskCompletion,
                            onAddTaskForSubject = { subjectId ->
                                editingTask = null
                                preselectedTaskSubjectId = subjectId
                                showTaskSheet = true
                            },
                            onOpenSubjectInPlanner = { subjectId ->
                                plannerViewModel.setSubjectFilter(subjectId)
                                navigateTopLevel(AppDestination.Planner)
                            },
                            onStartFocusForSubject = { subjectId ->
                                focusTimerViewModel.selectSubject(subjectId)
                                navigateTopLevel(AppDestination.Focus)
                            }
                        )
                    }

                    composable(AppDestination.Planner.route) {
                        val plannerState by plannerViewModel.uiState.collectAsStateWithLifecycle()
                        val plannerSearchQuery by plannerViewModel.searchQuery.collectAsStateWithLifecycle()
                        PlannerScreen(
                            uiState = plannerState,
                            searchQuery = plannerSearchQuery,
                            onFilterSelected = plannerViewModel::setFilter,
                            onPriorityFilterSelected = plannerViewModel::setPriorityFilter,
                            onSubjectFilterSelected = plannerViewModel::setSubjectFilter,
                            onSearchQueryChange = plannerViewModel::setSearchQuery,
                            onToggleTask = plannerViewModel::toggleTaskCompleted,
                            onOpenNewTask = {
                                editingTask = null
                                preselectedTaskSubjectId = plannerState.selectedSubjectId
                                showTaskSheet = true
                            },
                            onEditTask = { task ->
                                editingTask = task
                                preselectedTaskSubjectId = task.subjectId
                                showTaskSheet = true
                            },
                            onDeleteTask = plannerViewModel::deleteTask,
                            onUndoDelete = plannerViewModel::undoLastDelete,
                            onStartFocusForTask = { taskItem ->
                                focusTimerViewModel.selectTask(taskItem)
                                navigateTopLevel(AppDestination.Focus)
                            }
                        )
                    }

                    composable(AppDestination.Focus.route) {
                        val timerState by focusTimerViewModel.uiState.collectAsStateWithLifecycle()
                        val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()
                        FocusTimerScreen(
                            uiState = timerState,
                            hapticsEnabled = preferences.hapticsEnabled,
                            onSelectPreset = focusTimerViewModel::selectPreset,
                            onSelectSubject = focusTimerViewModel::selectSubject,
                            onSelectTask = focusTimerViewModel::selectTask,
                            onStart = focusTimerViewModel::startTimer,
                            onPause = focusTimerViewModel::pauseTimer,
                            onResume = focusTimerViewModel::resumeTimer,
                            onReset = focusTimerViewModel::resetTimer,
                            onMarkLinkedTaskCompleted = focusTimerViewModel::markLinkedTaskCompleted
                        )
                    }

                    composable(AppDestination.Insights.route) {
                        val insightsState by insightsViewModel.uiState.collectAsStateWithLifecycle()
                        InsightsScreen(uiState = insightsState)
                    }

                    composable(AppDestination.Settings.route) {
                        val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()
                        SettingsScreen(
                            preferences = preferences,
                            hasNotificationPermission = mainViewModel::hasNotificationPermission,
                            onThemeModeChange = mainViewModel::setThemeMode,
                            onDynamicColorsChange = mainViewModel::setDynamicColors,
                            onDefaultFocusMinutesChange = mainViewModel::setDefaultFocusMinutes,
                            onDailyGoalMinutesChange = mainViewModel::setDailyFocusGoalMinutes,
                            onNotificationsEnabledChange = mainViewModel::setNotificationsEnabled,
                            onHapticsEnabledChange = mainViewModel::setHapticsEnabled
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBarRunningTimerClock(timerStateFlow: StateFlow<FocusTimerUiState>) {
    val timerState by timerStateFlow.collectAsStateWithLifecycle()
    if (timerState.status == TimerStatus.RUNNING) {
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = com.example.domain.DateTimeUtils.formatTimerClock(timerState.remainingMillis),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
