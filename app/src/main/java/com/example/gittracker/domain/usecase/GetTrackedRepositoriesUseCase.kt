package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.domain.model.TrackedRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetTrackedRepositoriesUseCase @Inject constructor(
    private val repository: AppRepository
) {
    operator fun invoke(): Flow<List<TrackedRepo>> {
        return repository.getAllTrackedRepositories().map { repos ->
            repos.sortedWith(
                compareByDescending<TrackedRepo> { it.isPinned }
                    .thenByDescending { it.hasNewUpdate }
                    .thenBy { it.name.ifBlank { it.repoName }.lowercase() }
            )
        }
    }
}
