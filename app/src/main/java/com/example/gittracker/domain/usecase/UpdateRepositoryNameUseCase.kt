package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.domain.model.TrackedRepo
import javax.inject.Inject

class UpdateRepositoryNameUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(repo: TrackedRepo, name: String) {
        repository.updateRepository(repo.copy(name = name))
    }
}
