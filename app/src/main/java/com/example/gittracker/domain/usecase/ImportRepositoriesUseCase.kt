package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.data.util.RepositorySerializer
import com.example.gittracker.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

data class ImportProgress(
    val current: Int,
    val total: Int,
    val importedCount: Int,
    val pendingCount: Int,
    val skippedCount: Int,
    val isCompleted: Boolean = false
) {
    val addedCount: Int get() = importedCount + pendingCount
}

@Singleton
class ImportRepositoriesUseCase @Inject constructor(
    private val repository: AppRepository,
    private val scheduler: WorkManagerScheduler,
    private val serializer: RepositorySerializer
) {
    private val mutex = Mutex()
    private val _progress = MutableStateFlow<ImportProgress?>(null)
    val progress: StateFlow<ImportProgress?> = _progress.asStateFlow()

    suspend operator fun invoke(json: String): ImportProgress {
        return mutex.withLock {
            val repoExports = serializer.deserialize(json)
            val total = repoExports.size
            var importedCount = 0
            var pendingCount = 0
            var skippedCount = 0
            var isRateLimited = repository.getRateLimitStatus() != null

            _progress.value = ImportProgress(
                current = 0,
                total = total,
                importedCount = 0,
                pendingCount = 0,
                skippedCount = 0
            )

            repoExports.forEachIndexed { index, repoExport ->
                try {
                    val existing = repository.getRepositoryByOwnerAndName(repoExport.owner, repoExport.repoName)
                    if (existing != null) {
                        skippedCount++
                    } else if (isRateLimited) {
                        repository.addRepositoryOffline(
                            owner = repoExport.owner,
                            repoName = repoExport.repoName,
                            name = repoExport.customName,
                            isPinned = repoExport.isPinned
                        )
                        pendingCount++
                    } else {
                        try {
                            repository.addRepository(
                                owner = repoExport.owner,
                                repoName = repoExport.repoName,
                                name = repoExport.customName,
                                isPinned = repoExport.isPinned
                            )
                            importedCount++
                        } catch (e: Exception) {
                            if (e.message?.contains("rate limit", ignoreCase = true) == true || repository.getRateLimitStatus() != null) {
                                isRateLimited = true
                            }
                            repository.addRepositoryOffline(
                                owner = repoExport.owner,
                                repoName = repoExport.repoName,
                                name = repoExport.customName,
                                isPinned = repoExport.isPinned
                            )
                            pendingCount++
                        }
                    }
                } catch (e: Exception) {
                    skippedCount++
                    e.printStackTrace()
                }
                _progress.value = ImportProgress(
                    current = index + 1,
                    total = total,
                    importedCount = importedCount,
                    pendingCount = pendingCount,
                    skippedCount = skippedCount
                )
            }

            if (pendingCount > 0) {
                scheduler.scheduleDetourSync(System.currentTimeMillis())
            }

            val finalResult = ImportProgress(
                current = total,
                total = total,
                importedCount = importedCount,
                pendingCount = pendingCount,
                skippedCount = skippedCount,
                isCompleted = true
            )
            _progress.value = finalResult
            finalResult
        }
    }

    fun clearProgress() {
        _progress.value = null
    }
}
