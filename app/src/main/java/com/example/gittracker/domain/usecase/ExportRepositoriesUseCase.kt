package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.data.util.RepositorySerializer
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ExportRepositoriesUseCase @Inject constructor(
    private val repository: AppRepository,
    private val serializer: RepositorySerializer
) {
    suspend operator fun invoke(): String {
        val repos = repository.getAllTrackedRepositories().first()
        return serializer.serialize(repos)
    }
}
