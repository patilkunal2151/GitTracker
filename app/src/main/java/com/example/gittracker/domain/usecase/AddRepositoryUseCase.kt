package com.example.gittracker.domain.usecase

import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.util.GitHubUrlParser
import javax.inject.Inject

class AddRepositoryUseCase @Inject constructor(
    private val repository: AppRepository,
    private val urlParser: GitHubUrlParser
) {
    suspend operator fun invoke(url: String, name: String = "", isPinned: Boolean = false) {
        val repoInfo = urlParser.parse(url) ?: throw IllegalArgumentException("Invalid GitHub URL")
        
        if (repository.getRepositoryByOwnerAndName(repoInfo.owner, repoInfo.name) != null) {
            throw IllegalStateException("Repository already tracked")
        }

        repository.addRepository(repoInfo.owner, repoInfo.name, name, isPinned)
    }
}
