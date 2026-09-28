package com.example.gittracker.domain.usecase

import com.example.gittracker.data.local.SettingsManager
import com.example.gittracker.data.repository.AppRepository
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo
import javax.inject.Inject

class DeleteRepositoryUseCase @Inject constructor(
    private val repository: AppRepository,
    private val settingsManager: SettingsManager
) {
    suspend operator fun invoke(repo: TrackedRepo): Pair<TrackedRepo, List<Release>> {
        val releases = repository.getReleasesSync(repo.id)
        repository.deleteRepository(repo)
        
        if (repo.owner.equals("patilkunal2151", ignoreCase = true) && repo.repoName.equals("GitTracker", ignoreCase = true)) {
            settingsManager.setTrackingSelf(false)
        }
        
        return repo to releases
    }
}
