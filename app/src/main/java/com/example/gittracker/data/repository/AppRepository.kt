package com.example.gittracker.data.repository

import com.example.gittracker.data.local.RepositoryDao
import com.example.gittracker.data.model.ExportData
import com.example.gittracker.data.model.ReleaseEntity
import com.example.gittracker.data.model.TrackedRepository
import com.example.gittracker.data.model.TrackedRepositoryExport
import com.example.gittracker.data.remote.GitHubApiService
import com.example.gittracker.util.NotificationHelper
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val dao: RepositoryDao,
    private val apiService: GitHubApiService,
    private val notificationHelper: NotificationHelper,
    private val gson: Gson
) {
    fun getAllTrackedRepositories(): Flow<List<TrackedRepository>> = dao.getAllRepositories()

    suspend fun getRepositoryById(id: Long) = dao.getRepositoryById(id)

    fun getReleasesForRepository(repoId: Long): Flow<List<ReleaseEntity>> = 
        dao.getReleasesForRepository(repoId)

    suspend fun addRepository(url: String, name: String = "", isPinned: Boolean = false) {
        val regex = Regex("https://github.com/([^/]+)/([^/\\s]+)")
        val matchResult = regex.find(url) ?: throw IllegalArgumentException("Invalid GitHub URL")
        
        val owner = matchResult.groupValues[1]
        val repoName = matchResult.groupValues[2].removeSuffix(".git")

        if (dao.getRepositoryByOwnerAndName(owner, repoName) != null) {
            throw IllegalStateException("Repository already tracked")
        }
        
        val releases = try { apiService.getReleases(owner, repoName) } catch (_: Exception) { emptyList() }

        if (releases.isEmpty()) {
            throw IllegalStateException("No official releases found for this repository")
        }

        val latestVersion = releases.first().tagName
        
        val newRepo = TrackedRepository(
            owner = owner,
            repoName = repoName,
            latestVersionTag = latestVersion,
            hasNewUpdate = false,
            name = name,
            isPinned = isPinned
        )
        val repoId = dao.insertRepository(newRepo)
        
        val releaseEntities = releases.map { rel ->
            ReleaseEntity(
                repoId = repoId,
                tagName = rel.tagName,
                changelog = rel.body ?: "No changelog provided.",
                htmlUrl = rel.htmlUrl,
                createdAt = rel.publishedAt,
                assetsJson = gson.toJson(rel.assets)
            )
        }
        
        dao.insertReleases(releaseEntities)
    }

    suspend fun checkForUpdates() {
        android.util.Log.d("AppRepository", "Checking for updates...")
        val repos = dao.getAllRepositories().first()
        for (repo in repos) {
            try {
                android.util.Log.d("AppRepository", "Checking repo: ${repo.owner}/${repo.repoName}")
                val releases = try { apiService.getReleases(repo.owner, repo.repoName) } catch (e: Exception) { 
                    android.util.Log.e("AppRepository", "Failed to fetch releases for ${repo.repoName}", e)
                    emptyList() 
                }
                val latestVersion = releases.firstOrNull()?.tagName ?: ""
                
                if (latestVersion != repo.latestVersionTag && latestVersion.isNotEmpty()) {
                    android.util.Log.d("AppRepository", "New version found for ${repo.repoName}: $latestVersion (old: ${repo.latestVersionTag})")
                    
                    val existingEntities = dao.getReleasesForRepository(repo.id).first()
                    val existingReleases = existingEntities.map { it.tagName }.toSet()

                    val newReleases = releases.filter { it.tagName !in existingReleases }
                        .map { rel ->
                            ReleaseEntity(
                                repoId = repo.id,
                                tagName = rel.tagName,
                                changelog = rel.body ?: "No changelog provided.",
                                htmlUrl = rel.htmlUrl,
                                createdAt = rel.publishedAt,
                                assetsJson = gson.toJson(rel.assets)
                            )
                        }
                    
                    if (newReleases.isNotEmpty()) {
                        dao.insertReleases(newReleases)
                        val updatedRepo = repo.copy(
                            latestVersionTag = latestVersion,
                            hasNewUpdate = true
                        )
                        dao.updateRepository(updatedRepo)
                        notificationHelper.showUpdateNotification(updatedRepo)
                    } else {
                        // Tag changed but no new releases found in the list? 
                        // Update tag anyway to avoid infinite loop of checks
                        dao.updateRepository(repo.copy(latestVersionTag = latestVersion))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("AppRepository", "Error during update check for ${repo.repoName}", e)
            }
        }
    }

    suspend fun deleteRepository(repo: TrackedRepository) {
        dao.deleteRepository(repo)
    }

    suspend fun getReleasesSync(repoId: Long): List<ReleaseEntity> = 
        dao.getReleasesSync(repoId)

    suspend fun restoreRepository(repo: TrackedRepository, releases: List<ReleaseEntity>) {
        dao.insertRepository(repo)
        val updatedReleases = releases.map { it.copy(id = 0) }
        dao.insertReleases(updatedReleases)
    }

    suspend fun markAsRead(repo: TrackedRepository) {
        dao.updateRepository(repo.copy(hasNewUpdate = false))
    }

    suspend fun updateRepository(repo: TrackedRepository) {
        dao.updateRepository(repo)
    }

    suspend fun exportRepositories(): String {
        val repos = dao.getAllRepositories().first()
        val exportList = repos.map { 
            TrackedRepositoryExport(
                owner = it.owner,
                repoName = it.repoName,
                customName = it.name,
                isPinned = it.isPinned
            )
        }
        val exportData = ExportData(repositories = exportList)
        return gson.toJson(exportData)
    }

    suspend fun importRepositories(json: String) {
        val exportData = try {
            gson.fromJson(json, ExportData::class.java)
        } catch (e: Exception) {
            try {
                val oldRepos = gson.fromJson(json, Array<TrackedRepository>::class.java).toList()
                ExportData(repositories = oldRepos.map { 
                    TrackedRepositoryExport(it.owner, it.repoName, it.name, it.isPinned)
                })
            } catch (e: Exception) {
                null
            }
        } ?: return

        exportData.repositories.forEach { repoExport ->
            val url = "https://github.com/${repoExport.owner}/${repoExport.repoName}"
            try {
                addRepository(url, repoExport.customName, repoExport.isPinned)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
