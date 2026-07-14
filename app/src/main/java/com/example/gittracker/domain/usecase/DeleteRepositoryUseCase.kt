package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import javax.inject.Inject

class DeleteRepositoryUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(repo: TrackedRepo): Pair<TrackedRepo, List<Release>> {
        val releases = repository.getReleasesSync(repo.id)
        repository.deleteRepository(repo)
        return repo to releases
    }
}
