package com.example

import android.content.Context
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SaviiFocusDatabase
import com.example.data.repository.SaviiFocusRepository
import com.example.domain.DateTimeUtils
import com.example.domain.StudyCategory
import com.example.domain.TaskPriority
import com.example.domain.ThemeMode
import com.example.ui.screens.PlannerScreen
import com.example.ui.theme.SaviiFocusTheme
import com.example.ui.viewmodel.PlannerViewModel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlannerSearchSyncRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var database: SaviiFocusDatabase
    private lateinit var repository: SaviiFocusRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SaviiFocusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SaviiFocusRepository(
            subjectDao = database.subjectDao(),
            taskDao = database.taskDao(),
            focusSessionDao = database.focusSessionDao()
        )
        runBlocking {
            val mathId = (repository.addSubject(
                rawName = "Combined Maths",
                colorHex = 0xFF9D6BFF,
                targetMinutesPerWeek = 240,
                category = StudyCategory.AL
            ) as com.example.data.repository.SubjectMutationResult.Success).subjectId
            val bioId = (repository.addSubject(
                rawName = "Biology",
                colorHex = 0xFF5EEAD4,
                targetMinutesPerWeek = 180,
                category = StudyCategory.AL
            ) as com.example.data.repository.SubjectMutationResult.Success).subjectId

            val today = DateTimeUtils.todayEpochDay()
            repository.saveTask(
                taskId = null,
                title = "Calculus Past Paper",
                notes = "Differentiation",
                subjectId = mathId,
                priority = TaskPriority.HIGH,
                dueDateEpochDay = today,
                estimatedMinutes = 45
            )
            repository.saveTask(
                taskId = null,
                title = "Cell Respiration Notes",
                notes = "Chapter 4",
                subjectId = bioId,
                priority = TaskPriority.MEDIUM,
                dueDateEpochDay = today,
                estimatedMinutes = 30
            )
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun waitUntil(timeoutMs: Long = 5_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition() && System.currentTimeMillis() < deadline) {
            Shadows.shadowOf(Looper.getMainLooper()).idle()
            composeTestRule.waitForIdle()
            Thread.sleep(15)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `planner search rapid typing, clear, and process recreation stay synchronized`() {
        val restorationTester = StateRestorationTester(composeTestRule)
        var currentVm = PlannerViewModel(repository)

        restorationTester.setContent {
            SaviiFocusTheme(themeMode = ThemeMode.DARK, dynamicColor = false) {
                val uiState by currentVm.uiState.collectAsStateWithLifecycle()
                val searchQuery by currentVm.searchQuery.collectAsStateWithLifecycle()
                PlannerScreen(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    onFilterSelected = currentVm::setFilter,
                    onPriorityFilterSelected = currentVm::setPriorityFilter,
                    onSubjectFilterSelected = currentVm::setSubjectFilter,
                    onSearchQueryChange = currentVm::setSearchQuery,
                    onToggleTask = currentVm::toggleTaskCompleted,
                    onOpenNewTask = {},
                    onEditTask = {},
                    onDeleteTask = currentVm::deleteTask,
                    onUndoDelete = currentVm::undoLastDelete,
                    onStartFocusForTask = {}
                )
            }
        }

        waitUntil {
            composeTestRule.onAllNodesWithText("Calculus Past Paper").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("Cell Respiration Notes").fetchSemanticsNodes().isNotEmpty()
        }

        // Rapid character-by-character typing
        val searchNode = composeTestRule.onNodeWithTag("planner_search_input")
        "Calculus".forEach { ch ->
            searchNode.performTextInput(ch.toString())
        }
        composeTestRule.waitForIdle()
        searchNode.assertTextContains("Calculus")

        waitUntil {
            composeTestRule.onAllNodesWithText("Cell Respiration Notes").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Calculus Past Paper").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cell Respiration Notes").assertDoesNotExist()

        // Simulate process death: brand new PlannerViewModel with empty searchQueryState,
        // while rememberSaveable restores "Calculus" into searchInput.
        currentVm = PlannerViewModel(repository)
        restorationTester.emulateSavedInstanceStateRestore()
        composeTestRule.waitForIdle()

        // Restored search input must stay visible AND push filter to fresh ViewModel
        composeTestRule.onNodeWithTag("planner_search_input").assertTextContains("Calculus")
        waitUntil {
            composeTestRule.onAllNodesWithText("Calculus Past Paper").fetchSemanticsNodes().isNotEmpty() &&
                composeTestRule.onAllNodesWithText("Cell Respiration Notes").fetchSemanticsNodes().isEmpty()
        }
        composeTestRule.onNodeWithText("Calculus Past Paper").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cell Respiration Notes").assertDoesNotExist()

        // Clear button clears both UI text box and ViewModel filter
        composeTestRule.onNodeWithTag("planner_search_clear").performClick()
        waitUntil {
            composeTestRule.onAllNodesWithText("Cell Respiration Notes").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithTag("planner_list").performScrollToNode(hasText("Calculus Past Paper"))
        composeTestRule.onNodeWithText("Calculus Past Paper").assertIsDisplayed()
        composeTestRule.onNodeWithTag("planner_list").performScrollToNode(hasText("Cell Respiration Notes"))
        composeTestRule.onNodeWithText("Cell Respiration Notes").assertIsDisplayed()
    }
}
