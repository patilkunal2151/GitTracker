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
                    val releasesResponse = try { 
                        apiService.getReleases(repo.owner, repo.repoName) 
                    } catch (_: Exception) { 
                        null 
                    }

                    if (releasesResponse != null && releasesResponse.code() == 403) {
                        val resetHeader = releasesResponse.headers()["x-ratelimit-reset"]?.toLongOrNull()
                        if (resetHeader != null) {
                            return@withTimeoutOrNull SyncResult.RateLimited(resetHeader * 1000)
                        }
                    }

                    val releases = releasesResponse?.body() ?: emptyList()

                    if (releases.isEmpty()) {
                        continue
                    }

                    val latestRelease = releases.firstOrNull()
                    val latestVersion = latestRelease?.tagName ?: ""
                    val latestId = latestRelease?.id ?: 0L
                    
                    if (latestId != repo.latestReleaseId && latestId != 0L) {
                        val existingReleases = repository.getReleasesSync(repo.id)
                        val existingRemoteIds = existingReleases.map { it.remoteId }.toSet()

                        val newReleases = releases.filter { it.id !in existingRemoteIds }
                            .map { it.toDomain(repo.id) }
                        
                        if (newReleases.isNotEmpty()) {
                            repository.saveReleases(newReleases)
                            
                            val updatedRepo = repo.copy(
                                latestVersionTag = latestVersion,
                                latestReleaseId = latestId,
                                hasNewUpdate = true
                            )
                            repository.updateRepository(updatedRepo)
                            notificationHelper.showUpdateNotification(updatedRepo)
                        } else {
                            repository.updateRepository(repo.copy(latestVersionTag = latestVersion, latestReleaseId = latestId))
                        }
                    }
                } catch (_: Exception) {
                    android.util.Log.e("SyncUseCase", "Error updating ${repo.repoName}")
                }
            }
            SyncResult.Success
        } ?: SyncResult.Error("Sync timed out")
    }
}
