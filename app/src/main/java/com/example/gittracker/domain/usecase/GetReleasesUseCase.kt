package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.domain.model.Release
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetReleasesUseCase @Inject constructor(
    private val repository: AppRepository
) {
    operator fun invoke(repoId: Long): Flow<List<Release>> {
        return repository.getReleasesForRepository(repoId)
    }
}
