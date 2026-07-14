package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import javax.inject.Inject

class RestoreRepositoryUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(repo: TrackedRepo, releases: List<Release>) {
        repository.restoreRepository(repo, releases)
    }
}
