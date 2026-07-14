package com.example.gittracker.domain.usecase

import com.example.gittracker.data.util.RepositorySerializer
import javax.inject.Inject

class ImportRepositoriesUseCase @Inject constructor(
    private val addRepositoryUseCase: AddRepositoryUseCase,
    private val serializer: RepositorySerializer
) {
    suspend operator fun invoke(json: String) {
        val repoExports = serializer.deserialize(json)
        repoExports.forEach { repoExport ->
            val url = "https://github.com/${repoExport.owner}/${repoExport.repoName}"
            try {
                addRepositoryUseCase(url, repoExport.customName, repoExport.isPinned)
            } catch (e: Exception) {
                // Log error or handle individual failures
                e.printStackTrace()
            }
        }
    }
}
