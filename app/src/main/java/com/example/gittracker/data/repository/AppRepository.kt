package com.example.gittracker.data.repository

import com.example.gittracker.data.local.RepositoryDao
import com.example.gittracker.data.mapper.toDomain
import com.example.gittracker.data.mapper.toEntity
import com.example.gittracker.data.model.ReleaseEntity
import com.example.gittracker.data.model.TrackedRepository
import com.example.gittracker.data.remote.GitHubApiService
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import com.example.gittracker.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val dao: RepositoryDao,
    private val apiService: GitHubApiService
) {
    fun getAllTrackedRepositories(): Flow<List<TrackedRepo>> = 
        dao.getAllRepositories().map { list -> list.map { it.toDomain() } }

    suspend fun getRepositoryById(id: Long): TrackedRepo? = 
        dao.getRepositoryById(id)?.toDomain()

    suspend fun getRepositoryByOwnerAndName(owner: String, name: String): TrackedRepo? =
        dao.getRepositoryByOwnerAndName(owner, name)?.toDomain()

    fun getReleasesForRepository(repoId: Long): Flow<List<Release>> = 
        dao.getReleasesForRepository(repoId).map { list -> list.map { it.toDomain() } }

    suspend fun addRepository(owner: String, repoName: String, name: String = "", isPinned: Boolean = false) {
        val releasesResponse = try { 
            apiService.getReleases(owner, repoName, perPage = 10, page = 1) 
        } catch (_: Exception) { 
            null 
        }
        val releases = releasesResponse?.body() ?: emptyList()

        if (releases.isEmpty()) {
            throw IllegalStateException("No official releases found for this repository")
        }

        val latestRelease = releases.first()
        
        val newRepo = TrackedRepository(
            owner = owner,
            repoName = repoName,
            latestVersionTag = latestRelease.tagName,
            latestReleaseId = latestRelease.id,
            hasNewUpdate = false,
            name = name,
            isPinned = isPinned,
            reachedEndOfReleases = releases.size < 10
        )
        val repoId = dao.insertRepository(newRepo)
        
        val releaseEntities = releases.take(5).map { rel ->
            ReleaseEntity(
                repoId = repoId,
                remoteId = rel.id,
                tagName = rel.tagName,
                changelog = rel.body ?: "No changelog provided.",
                htmlUrl = rel.htmlUrl,
                createdAt = DateUtils.parseGithubDate(rel.publishedAt),
                isPrerelease = rel.isPrerelease,
                assets = rel.assets
            )
        }
        
        dao.insertReleases(releaseEntities)
    }

    suspend fun deleteRepository(repo: TrackedRepo) {
        dao.deleteRepository(repo.toEntity())
    }

    suspend fun getReleasesSync(repoId: Long): List<Release> = 
        dao.getReleasesSync(repoId).map { it.toDomain() }

    suspend fun restoreRepository(repo: TrackedRepo, releases: List<Release>) {
        dao.insertRepository(repo.toEntity())
        val updatedReleases = releases.map { it.toEntity().copy(id = 0) }
        dao.insertReleases(updatedReleases)
    }

    suspend fun fetchMoreReleases(repoId: Long) {
        val repo = dao.getRepositoryById(repoId) ?: return
        if (repo.reachedEndOfReleases) return
        
        val currentReleasesCount = dao.getReleasesSync(repoId).size
        val nextPage = (currentReleasesCount / 10) + 1
        
        val newReleasesResponse = try { 
            apiService.getReleases(repo.owner, repo.repoName, perPage = 10, page = nextPage) 
        } catch (_: Exception) { 
            null 
        }
        val newReleases = newReleasesResponse?.body() ?: emptyList()
        
        if (newReleases.isEmpty()) {
            dao.updateRepository(repo.copy(reachedEndOfReleases = true))
            return
        }
        
        if (newReleases.size < 10) {
            dao.updateRepository(repo.copy(reachedEndOfReleases = true))
        }

        if (newReleases.isNotEmpty()) {
            val existingEntities = dao.getReleasesSync(repoId)
            val existingRemoteIds = existingEntities.map { it.remoteId }.toSet()
            
            val entitiesToAdd = newReleases
                .filter { it.id !in existingRemoteIds }
                .map { rel ->
                    ReleaseEntity(
                        repoId = repoId,
                        remoteId = rel.id,
                        tagName = rel.tagName,
                        changelog = rel.body ?: "No changelog provided.",
                        htmlUrl = rel.htmlUrl,
                        createdAt = DateUtils.parseGithubDate(rel.publishedAt),
                        isPrerelease = rel.isPrerelease,
                        assets = rel.assets
                    )
                }
            if (entitiesToAdd.isNotEmpty()) {
                dao.insertReleases(entitiesToAdd)
            }
        }
    }

    suspend fun updateRepository(repo: TrackedRepo) {
        dao.updateRepository(repo.toEntity())
    }

    suspend fun getRateLimitStatus(): Long? {
        return try {
            val response = apiService.getRateLimit()
            if (response.isSuccessful) {
                val status = response.body()?.resources?.core
                if (status != null && status.remaining == 0) {
                    status.reset * 1000 // Convert to millis
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveReleases(releases: List<Release>) {
        dao.insertReleases(releases.map { it.toEntity() })
    }
}
