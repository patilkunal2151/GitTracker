package com.example.gittracker.ui

import app.cash.turbine.test
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.domain.usecase.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val getTrackedRepositoriesUseCase: GetTrackedRepositoriesUseCase = mockk()
    private val repository: com.example.gittracker.data.repository.AppRepository = mockk(relaxed = true)
    private val addRepositoryUseCase: AddRepositoryUseCase = mockk(relaxed = true)
    private val deleteRepositoryUseCase: DeleteRepositoryUseCase = mockk(relaxed = true)
    private val togglePinUseCase: TogglePinUseCase = mockk(relaxed = true)
    private val markAsReadUseCase: MarkAsReadUseCase = mockk(relaxed = true)
    private val updateRepositoryNameUseCase: UpdateRepositoryNameUseCase = mockk(relaxed = true)
    private val restoreRepositoryUseCase: RestoreRepositoryUseCase = mockk(relaxed = true)
    private val fetchMoreReleasesUseCase: FetchMoreReleasesUseCase = mockk(relaxed = true)
    private val getReleasesUseCase: GetReleasesUseCase = mockk(relaxed = true)
    private val getReadmeUseCase: GetReadmeUseCase = mockk(relaxed = true)

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { getTrackedRepositoriesUseCase() } returns flowOf(emptyList())
        
        viewModel = MainViewModel(
            getTrackedRepositoriesUseCase,
            repository,
            addRepositoryUseCase,
            deleteRepositoryUseCase,
            togglePinUseCase,
            markAsReadUseCase,
            updateRepositoryNameUseCase,
            restoreRepositoryUseCase,
            fetchMoreReleasesUseCase,
            getReleasesUseCase,
            getReadmeUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial uiState is empty list`() = runTest {
        viewModel.uiState.test {
            assertEquals(emptyList<TrackedRepo>(), awaitItem())
        }
    }

    @Test
    fun `addRepo success sends success event`() = runTest {
        val url = "https://github.com/owner/repo"
        
        viewModel.successEvent.test {
            viewModel.addRepo(url)
            assertEquals("Repository added successfully", awaitItem())
        }
        
        coVerify { addRepositoryUseCase(url) }
    }

    @Test
    fun `deleteRepo calls use case and sends undo event`() = runTest {
        val repo = TrackedRepo(owner = "owner", repoName = "repo", latestVersionTag = "v1.0")
        val releases = listOf<Release>()
        coEvery { deleteRepositoryUseCase(repo) } returns (repo to releases)

        viewModel.undoDeleteEvent.test {
            viewModel.deleteRepo(repo)
            val result = awaitItem()
            assertEquals(repo, result.first().first)
            assertEquals(releases, result.first().second)
        }
    }

    @Test
    fun `togglePin calls use case`() = runTest {
        val repo = TrackedRepo(owner = "owner", repoName = "repo", latestVersionTag = "v1.0")
        
        viewModel.togglePin(repo)
        
        coVerify { togglePinUseCase(repo) }
    }
}
