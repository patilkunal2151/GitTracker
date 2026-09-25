package com.example.gittracker.domain.usecase

import android.util.Log
import com.example.gittracker.data.model.GitHubRelease
import com.example.gittracker.data.remote.GitHubApiService
import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.util.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class SyncRepositoriesUseCaseTest {

    private lateinit var syncUseCase: SyncRepositoriesUseCase
    private val repository: AppRepository = mockk(relaxed = true)
    private val apiService: GitHubApiService = mockk()
    private val notificationHelper: NotificationHelper = mockk(relaxed = true)

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any()) } returns 0
        
        syncUseCase = SyncRepositoriesUseCase(repository, apiService, notificationHelper)
    }

    @Test
    fun `invoke returns RateLimited when API quota is exceeded`() = runTest {
        // Given
        val resetTime = 123456789L
        coEvery { repository.getRateLimitStatus() } returns resetTime

        // When
        val result = syncUseCase()

        // Then
        assertEquals(SyncResult.RateLimited(resetTime), result)
    }

    @Test
    fun `invoke syncs repository, purges stale releases and shows notification when new release is found`() = runTest {
        // Given
        val repo = TrackedRepo(id = 1, owner = "owner", repoName = "repo", latestVersionTag = "v1.0", latestReleaseId = 100)
        coEvery { repository.getRateLimitStatus() } returns null
        coEvery { repository.getAllTrackedRepositories() } returns flowOf(listOf(repo))
        
        val staleRelease = Release(id = 10, repoId = 1, remoteId = 99, tagName = "v0.9-deleted", changelog = "", htmlUrl = "", createdAt = 0, isPrerelease = false, assets = emptyList())
        val existingRelease = Release(id = 11, repoId = 1, remoteId = 100, tagName = "v1.0", changelog = "", htmlUrl = "", createdAt = 0, isPrerelease = false, assets = emptyList())
        coEvery { repository.getReleasesSync(1) } returns listOf(staleRelease, existingRelease)

        val newGithubRelease = GitHubRelease(
            id = 101, 
            tagName = "v1.1", 
            htmlUrl = "url", 
            body = "changelog", 
            publishedAt = "2024-01-01T00:00:00Z", 
            isPrerelease = false, 
            assets = emptyList()
        )
        val currentGithubRelease = GitHubRelease(
            id = 100, 
            tagName = "v1.0", 
            htmlUrl = "url", 
            body = "changelog", 
            publishedAt = "2024-01-01T00:00:00Z", 
            isPrerelease = false, 
            assets = emptyList()
        )
        // Remote only returns 101 and 100; 99 is removed on GitHub
        coEvery { apiService.getReleases("owner", "repo") } returns Response.success(listOf(newGithubRelease, currentGithubRelease))

        // When
        val result = syncUseCase()

        // Then
        assertEquals(SyncResult.Success, result)
        coVerify { repository.deleteReleases(listOf(staleRelease)) }
        coVerify { repository.saveReleases(any()) }
        coVerify { repository.updateRepository(match { it.hasNewUpdate && it.latestReleaseId == 101L }) }
        coVerify { notificationHelper.showUpdateNotification(any()) }
    }

    @Test
    fun `invoke returns Success and does nothing if no new releases`() = runTest {
        // Given
        val repo = TrackedRepo(id = 1, owner = "owner", repoName = "repo", latestVersionTag = "v1.0", latestReleaseId = 100)
        coEvery { repository.getRateLimitStatus() } returns null
        coEvery { repository.getAllTrackedRepositories() } returns flowOf(listOf(repo))
        
        val githubRelease = GitHubRelease(
            id = 100, 
            tagName = "v1.0", 
            htmlUrl = "url", 
            body = "changelog", 
            publishedAt = "2024-01-01T00:00:00Z", 
            isPrerelease = false, 
            assets = emptyList()
        )
        coEvery { apiService.getReleases("owner", "repo") } returns Response.success(listOf(githubRelease))

        // When
        val result = syncUseCase()

        // Then
        assertEquals(SyncResult.Success, result)
        coVerify(exactly = 0) { repository.saveReleases(any()) }
        coVerify(exactly = 0) { notificationHelper.showUpdateNotification(any()) }
    }
}
