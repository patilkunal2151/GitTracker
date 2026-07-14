package com.example.gittracker.domain.usecase

import com.example.gittracker.data.mapper.*
import com.example.gittracker.data.remote.GitHubApiService
import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.domain.model.Release
import com.example.gittracker.util.NotificationHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class SyncRepositoriesUseCase @Inject constructor(
    private val repository: AppRepository,
    private val apiService: GitHubApiService,
    private val notificationHelper: NotificationHelper
) {
    suspend operator fun invoke() {
        withTimeoutOrNull(10.minutes) {
            val repos = repository.getAllTrackedRepositories().first()
            for (repo in repos) {
                try {
                    val releases = try { 
                        apiService.getReleases(repo.owner, repo.repoName) 
                    } catch (_: Exception) { 
                        emptyList() 
                    }

                    if (releases.isEmpty()) {
                        val tags = try { 
                            apiService.getTags(repo.owner, repo.repoName) 
                        } catch (_: Exception) { 
                            emptyList() 
                        }

                        if (tags.isNotEmpty()) {
                            val latestTag = tags.first()
                            val latestId = latestTag.name.hashCode().toLong()
                            
                            if (latestId != repo.latestReleaseId) {
                                val existingReleases = repository.getReleasesSync(repo.id)
                                val existingRemoteIds = existingReleases.map { it.remoteId }.toSet()
                                
                                val newReleases = tags.filter { it.name.hashCode().toLong() !in existingRemoteIds }
                                    .map { tag ->
                                        Release(
                                            repoId = repo.id,
                                            remoteId = tag.name.hashCode().toLong(),
                                            tagName = tag.name,
                                            changelog = "Tag release: ${tag.name}",
                                            htmlUrl = "https://github.com/${repo.owner}/${repo.repoName}/releases/tag/${tag.name}",
                                            createdAt = System.currentTimeMillis(),
                                            isPrerelease = tag.name.contains("beta", true) || tag.name.contains("rc", true) || tag.name.contains("alpha", true),
                                            assets = emptyList()
                                        )
                                    }
                                
                                if (newReleases.isNotEmpty()) {
                                    repository.saveReleases(newReleases)
                                    val updatedRepo = repo.copy(
                                        latestVersionTag = latestTag.name,
                                        latestReleaseId = latestId,
                                        hasNewUpdate = true
                                    )
                                    repository.updateRepository(updatedRepo)
                                    notificationHelper.showUpdateNotification(updatedRepo)
                                }
                            }
                        }
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
                } catch (e: Exception) {
                    android.util.Log.e("SyncUseCase", "Error updating ${repo.repoName}", e)
                }
            }
        }
    }
}
