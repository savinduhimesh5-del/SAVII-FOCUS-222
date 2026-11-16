package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.ui.navigation.AppDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Savii Focus", appName)
    }

    @Test
    fun `navigation destinations initialize without null entries and use Study label`() {
        val tabs = AppDestination.primaryTabs
        assertEquals(5, tabs.size)
        assertTrue(tabs.all { it.route.isNotBlank() && it.label.isNotBlank() })
        assertEquals("Study", AppDestination.Learning.label)
    }

    @Test
    fun `main activity launches and navigates across primary tabs`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_learning").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("study_home_list")
            .performScrollToNode(hasTestTag("study_category_al"))
        composeTestRule.onNodeWithTag("study_category_al").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nav_planner").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_focus").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_insights").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("top_bar_settings_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("settings_back_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_theme_dark").assertIsDisplayed()
    }

    @Test
    fun `study tab navigates AL, AAT levels, OTHER and creates independent subjects`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_learning").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Home categories visible via LazyColumn scroll
        composeTestRule.onNodeWithTag("study_home_list")
            .performScrollToNode(hasTestTag("study_category_al"))
        composeTestRule.onNodeWithTag("study_category_al").assertIsDisplayed()
        composeTestRule.onNodeWithTag("study_home_list")
            .performScrollToNode(hasTestTag("study_category_aat"))
        composeTestRule.onNodeWithTag("study_category_aat").assertIsDisplayed()
        composeTestRule.onNodeWithTag("study_home_list")
            .performScrollToNode(hasTestTag("study_category_other"))
        composeTestRule.onNodeWithTag("study_category_other").assertIsDisplayed()

        // 1) A/L -> Add subject -> Verify -> Back
        composeTestRule.onNodeWithTag("study_home_list")
            .performScrollToNode(hasTestTag("study_category_al"))
        composeTestRule.onNodeWithTag("study_category_al").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("learning_add_subject_fab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("subject_name_input").performTextInput("Combined Maths")
        composeTestRule.onNodeWithTag("save_subject_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Combined Maths").assertIsDisplayed()
        composeTestRule.onNodeWithTag("study_subscreen_back_button").performClick()
        composeTestRule.waitForIdle()

        // 2) AAT -> Level 1 -> Add "Financial Accounting" -> Back to Level 2 -> Add "Financial Accounting"
        composeTestRule.onNodeWithTag("study_home_list")
            .performScrollToNode(hasTestTag("study_category_aat"))
        composeTestRule.onNodeWithTag("study_category_aat").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("study_aat_folders_list")
            .performScrollToNode(hasTestTag("study_aat_folder_1"))
        composeTestRule.onNodeWithTag("study_aat_folder_1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("study_aat_folders_list")
            .performScrollToNode(hasTestTag("study_aat_folder_2"))
        composeTestRule.onNodeWithTag("study_aat_folder_2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("study_aat_folders_list")
            .performScrollToNode(hasTestTag("study_aat_folder_3"))
        composeTestRule.onNodeWithTag("study_aat_folder_3").assertIsDisplayed()

        composeTestRule.onNodeWithTag("study_aat_folders_list")
            .performScrollToNode(hasTestTag("study_aat_folder_1"))
        composeTestRule.onNodeWithTag("study_aat_folder_1").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("learning_add_subject_fab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("subject_name_input").performTextInput("Financial Accounting")
        composeTestRule.onNodeWithTag("save_subject_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Financial Accounting").assertIsDisplayed()
        composeTestRule.onNodeWithTag("study_subscreen_back_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("study_aat_folders_list")
            .performScrollToNode(hasTestTag("study_aat_folder_2"))
        composeTestRule.onNodeWithTag("study_aat_folder_2").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("learning_add_subject_fab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("subject_name_input").performTextInput("Financial Accounting")
        composeTestRule.onNodeWithTag("save_subject_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Financial Accounting").assertIsDisplayed()

        // Back to AAT folders -> Back to Study Home -> OTHER -> Add subject
        composeTestRule.onNodeWithTag("study_subscreen_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("study_aat_back_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("study_home_list")
            .performScrollToNode(hasTestTag("study_category_other"))
        composeTestRule.onNodeWithTag("study_category_other").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("learning_add_subject_fab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("subject_name_input").performTextInput("Spoken English")
        composeTestRule.onNodeWithTag("save_subject_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Spoken English").assertIsDisplayed()
    }

    @Test
    fun `planner search stays synchronized across tab navigation away and back`() {
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_planner").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()

        // Create two tasks via Planner FAB
        composeTestRule.onNodeWithTag("planner_add_task_fab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_title_input").performTextInput("Organic Synthesis")
        composeTestRule.onNodeWithTag("save_task_button").performScrollTo().performClick()
        val firstSaveDeadline = System.currentTimeMillis() + 5_000L
        while (
            (composeTestRule.onAllNodesWithTag("save_task_button").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Organic Synthesis").fetchSemanticsNodes().isEmpty()) &&
            System.currentTimeMillis() < firstSaveDeadline
        ) {
            composeTestRule.waitForIdle()
            Thread.sleep(20)
        }

        composeTestRule.onNodeWithTag("planner_add_task_fab").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("task_title_input").performTextInput("Financial Statements")
        composeTestRule.onNodeWithTag("save_task_button").performScrollTo().performClick()
        val secondSaveDeadline = System.currentTimeMillis() + 5_000L
        while (
            (composeTestRule.onAllNodesWithTag("save_task_button").fetchSemanticsNodes().isNotEmpty() ||
                composeTestRule.onAllNodesWithText("Financial Statements").fetchSemanticsNodes().isEmpty()) &&
            System.currentTimeMillis() < secondSaveDeadline
        ) {
            composeTestRule.waitForIdle()
            Thread.sleep(20)
        }

        // Scroll back to top so search input is visible, then type search query
        composeTestRule.onNodeWithTag("planner_list").performScrollToNode(hasTestTag("planner_search_input"))
        composeTestRule.onNodeWithTag("planner_search_input").performTextInput("Organic")
        val deadline = System.currentTimeMillis() + 5_000L
        while (
            composeTestRule.onAllNodesWithText("Financial Statements").fetchSemanticsNodes().isNotEmpty() &&
            System.currentTimeMillis() < deadline
        ) {
            composeTestRule.waitForIdle()
            Thread.sleep(20)
        }
        composeTestRule.onNodeWithTag("planner_search_input").assertTextContains("Organic")
        composeTestRule.onNodeWithTag("planner_list").performScrollToNode(hasText("Organic Synthesis"))
        composeTestRule.onNodeWithText("Organic Synthesis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Financial Statements").assertDoesNotExist()

        // Navigate away to Dashboard and back to Planner
        composeTestRule.onNodeWithTag("nav_dashboard").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("nav_planner").performClick()
        composeTestRule.waitForIdle()

        // Both search input and filtered results remain synchronized
        composeTestRule.onNodeWithTag("planner_search_input").assertTextContains("Organic")
        composeTestRule.onNodeWithTag("planner_list").performScrollToNode(hasText("Organic Synthesis"))
        composeTestRule.onNodeWithText("Organic Synthesis").assertIsDisplayed()
        composeTestRule.onNodeWithText("Financial Statements").assertDoesNotExist()

        // Clear search restores both tasks
        composeTestRule.onNodeWithTag("planner_list").performScrollToNode(hasTestTag("planner_search_input"))
        composeTestRule.onNodeWithTag("planner_search_clear").performClick()
        val clearDeadline = System.currentTimeMillis() + 5_000L
        while (
            composeTestRule.onAllNodesWithText("Financial Statements").fetchSemanticsNodes().isEmpty() &&
            System.currentTimeMillis() < clearDeadline
        ) {
            composeTestRule.waitForIdle()
            Thread.sleep(20)
        }
        composeTestRule.onNodeWithTag("planner_list").performScrollToNode(hasText("Organic Synthesis"))
        composeTestRule.onNodeWithText("Organic Synthesis").assertIsDisplayed()
        composeTestRule.onNodeWithTag("planner_list").performScrollToNode(hasText("Financial Statements"))
        composeTestRule.onNodeWithText("Financial Statements").assertIsDisplayed()
    }
}
