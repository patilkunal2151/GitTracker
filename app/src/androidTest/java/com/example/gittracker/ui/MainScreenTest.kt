package com.example.gittracker.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.ui.screens.MainScreen
import com.example.gittracker.ui.theme.GitTrackerTheme
import org.junit.Rule
import org.junit.Test

class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mainScreen_displaysRepositories() {
        val repos = listOf(
            TrackedRepo(id = 1, owner = "owner1", repoName = "repo1", latestVersionTag = "v1.0"),
            TrackedRepo(id = 2, owner = "owner2", repoName = "repo2", latestVersionTag = "v2.0")
        )

        composeTestRule.setContent {
            GitTrackerTheme {
                MainScreen(
                    repositories = repos,
                    isAdding = false,
                    snackbarHost = {},
                    onRepoClick = {},
                    onAddRepo = {},
                    onDeleteRepo = {},
                    onTogglePin = {},
                    onUpdateName = { _, _ -> },
                    onSettingsClick = {}
                )
            }
        }

        // Check if repo names are displayed
        composeTestRule.onNodeWithText("repo1").assertIsDisplayed()
        composeTestRule.onNodeWithText("repo2").assertIsDisplayed()
        
        // Check if owners are displayed
        composeTestRule.onNodeWithText("owner1/repo1").assertIsDisplayed()
        composeTestRule.onNodeWithText("owner2/repo2").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsEmptyState_whenNoRepositories() {
        composeTestRule.setContent {
            GitTrackerTheme {
                MainScreen(
                    repositories = emptyList(),
                    isAdding = false,
                    snackbarHost = {},
                    onRepoClick = {},
                    onAddRepo = {},
                    onDeleteRepo = {},
                    onTogglePin = {},
                    onUpdateName = { _, _ -> },
                    onSettingsClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No repositories tracked yet.").assertIsDisplayed()
    }

    @Test
    fun mainScreen_opensAddRepoDialog_whenAddButtonClicked() {
        composeTestRule.setContent {
            GitTrackerTheme {
                MainScreen(
                    repositories = emptyList(),
                    isAdding = false,
                    snackbarHost = {},
                    onRepoClick = {},
                    onAddRepo = {},
                    onDeleteRepo = {},
                    onTogglePin = {},
                    onUpdateName = { _, _ -> },
                    onSettingsClick = {}
                )
            }
        }

        // Click the Add button (using content description from strings.xml)
        composeTestRule.onNodeWithContentDescription("Add GitHub Repository").performClick()

        // Check if Dialog title is displayed
        composeTestRule.onNodeWithText("Add GitHub Repository").assertIsDisplayed()
        composeTestRule.onNodeWithText("Enter the full GitHub repository URL:").assertIsDisplayed()
    }
}
