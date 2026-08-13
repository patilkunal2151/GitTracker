package com.example.gittracker.domain.usecase

import com.example.gittracker.data.local.SettingsManager
import com.example.gittracker.data.repository.AppRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ToggleTrackSelfUseCase @Inject constructor(
    private val settingsManager: SettingsManager,
    private val repository: AppRepository,
    private val addRepositoryUseCase: AddRepositoryUseCase,
    private val deleteRepositoryUseCase: DeleteRepositoryUseCase
) {
    private val repoOwner = "patilkunal2151"
    private val repoName = "GitTracker"

    suspend operator fun invoke(enabled: Boolean) {
        settingsManager.setTrackingSelf(enabled)
        
        val existingRepo = repository.getRepositoryByOwnerAndName(repoOwner, repoName)
        
        if (enabled) {
            if (existingRepo == null) {
                try {
                    addRepositoryUseCase("https://github.com/$repoOwner/$repoName")
                } catch (_: Exception) {
                    // Fail silently or handle error in ViewModel
                }
            }
        } else {
            if (existingRepo != null) {
                deleteRepositoryUseCase(existingRepo)
            }
        }
    }
}
