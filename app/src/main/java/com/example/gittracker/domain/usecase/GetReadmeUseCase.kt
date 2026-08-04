package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import javax.inject.Inject

class GetReadmeUseCase @Inject constructor(
    private val repository: AppRepository
) {
    suspend operator fun invoke(owner: String, repoName: String): String? {
        return repository.getReadme(owner, repoName)
    }
}
