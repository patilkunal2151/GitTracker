package com.example.gittracker.domain.usecase

import com.example.gittracker.data.mapper.*
import com.example.gittracker.data.remote.GitHubApiService
import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.util.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

sealed class SyncResult {
    object Success : SyncResult()
    data class RateLimited(val resetTimeMillis: Long) : SyncResult()
    data class Error(val message: String) : SyncResult()
}

class SyncRepositoriesUseCase @Inject constructor(
    private val repository: AppRepository,
    private val apiService: GitHubApiService,
    private val notificationHelper: NotificationHelper
) {
    suspend operator fun invoke(): SyncResult {
        // 1. Pre-check quota
        val resetTime = repository.getRateLimitStatus()
        if (resetTime != null) {
            return SyncResult.RateLimited(resetTime)
        }

        return withTimeoutOrNull(10.minutes) {
            val repos = repository.getAllTrackedRepositories().first()
            for (repo in repos) {
                try {
                    // Fetch repo details for metadata, renames, and 404 check
                    val repoDetailsResponse = try {
                        apiService.getRepoDetails(repo.owner, repo.repoName)
                    } catch (_: Exception) {
                        null
                    }

                    if (repoDetailsResponse?.code() == 403) {
                        val resetHeader = repoDetailsResponse.headers()["x-ratelimit-reset"]?.toLongOrNull()
                        if (resetHeader != null) {
                            return@withTimeoutOrNull SyncResult.RateLimited(resetHeader * 1000)
                        }
                    }

                    var currentOwner = repo.owner
                    var currentRepoName = repo.repoName
                    var updatedRepo = repo

                    if (repoDetailsResponse?.code() == 404) {
                        updatedRepo = updatedRepo.copy(description = "Repository not found or private on GitHub")
                        repository.updateRepository(updatedRepo)
                        continue
                    }

                    val details = repoDetailsResponse?.body()
                    if (details != null) {
                        val newOwner = details.owner.login
                        val newRepoName = details.name
                        updatedRepo = updatedRepo.copy(
                            owner = newOwner,
                            repoName = newRepoName,
                            description = details.description ?: updatedRepo.description,
                            stargazersCount = details.stargazersCount,
                            forksCount = details.forksCount,
                            language = details.language,
                            topics = details.topics?.joinToString(",")
                        )
                        currentOwner = newOwner
                        currentRepoName = newRepoName
                        repository.updateRepository(updatedRepo)
                    }

                    // Fetch releases
                    val releasesResponse = try { 
                        apiService.getReleases(currentOwner, currentRepoName) 
                    } catch (_: Exception) { 
                        null 
                    }

                    if (releasesResponse?.code() == 403) {
                        val resetHeader = releasesResponse.headers()["x-ratelimit-reset"]?.toLongOrNull()
                        if (resetHeader != null) {
                            return@withTimeoutOrNull SyncResult.RateLimited(resetHeader * 1000)
                        }
                    }

                    val releases = releasesResponse?.body() ?: emptyList()

                    if (releases.isEmpty()) {
                        if (updatedRepo.latestVersionTag == "Pending") {
                            updatedRepo = updatedRepo.copy(
                                latestVersionTag = "No Releases",
                                description = details?.description ?: "No official releases published"
                            )
                            repository.updateRepository(updatedRepo)
                        }
                        continue
                    }

                    val latestRelease = releases.first()
                    val latestVersion = latestRelease.tagName
                    val latestId = latestRelease.id
                    val isInitialSync = updatedRepo.latestVersionTag == "Pending" || updatedRepo.latestReleaseId == 0L

                    if (isInitialSync) {
                        val domainReleases = releases.map { it.toDomain(updatedRepo.id) }
                        repository.saveReleases(domainReleases)

                        updatedRepo = updatedRepo.copy(
                            latestVersionTag = latestVersion,
                            latestReleaseId = latestId,
                            hasNewUpdate = false
                        )
                        repository.updateRepository(updatedRepo)
                    } else if (latestId != updatedRepo.latestReleaseId) {
                        val existingReleases = repository.getReleasesSync(updatedRepo.id)
                        val fetchedRemoteIds = releases.map { it.id }.toSet()

                        val staleReleases = existingReleases.filter { it.remoteId !in fetchedRemoteIds }
                        if (staleReleases.isNotEmpty()) {
                            repository.deleteReleases(staleReleases)
                        }

                        val domainReleases = releases.map { it.toDomain(updatedRepo.id) }
                        repository.saveReleases(domainReleases)

                        updatedRepo = updatedRepo.copy(
                            latestVersionTag = latestVersion,
                            latestReleaseId = latestId,
                            hasNewUpdate = true
                        )
                        repository.updateRepository(updatedRepo)

                        val bestAsset = latestRelease.assets.find { it.name.endsWith(".apk", ignoreCase = true) }
                            ?: latestRelease.assets.firstOrNull()

                        notificationHelper.showUpdateNotification(
                            repo = updatedRepo,
                            assetUrl = bestAsset?.downloadUrl,
                            assetName = bestAsset?.name
                        )
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SyncUseCase", "Error updating ${repo.repoName}", e)
                }
            }
            SyncResult.Success
        } ?: SyncResult.Error("Sync timed out")
    }
}
