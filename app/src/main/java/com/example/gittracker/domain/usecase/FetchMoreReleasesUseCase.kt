package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import javax.inject.Inject

class FetchMoreReleasesUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(repoId: Long) {
        repository.fetchMoreReleases(repoId)
    }
}
