package com.example.gittracker.data.util

import com.example.gittracker.data.model.ExportData
import com.example.gittracker.data.model.TrackedRepositoryExport
import com.example.gittracker.domain.model.TrackedRepo
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RepositorySerializer @Inject constructor(
    private val gson: Gson
) {
    fun serialize(repos: List<TrackedRepo>): String {
        val exportList = repos.map { 
            TrackedRepositoryExport(
                owner = it.owner,
                repoName = it.repoName,
                customName = it.name,
                isPinned = it.isPinned
            )
        }
        val exportData = ExportData(repositories = exportList)
        return gson.toJson(exportData)
    }

    fun deserialize(json: String): List<TrackedRepositoryExport> {
        val exportData = try {
            gson.fromJson(json, ExportData::class.java)
        } catch (e: Exception) {
            try {
                // Fallback for older versions or different formats
                val oldRepos = gson.fromJson(json, Array<TrackedRepo>::class.java).toList()
                ExportData(repositories = oldRepos.map { 
                    TrackedRepositoryExport(it.owner, it.repoName, it.name, it.isPinned)
                })
            } catch (e: Exception) {
                null
            }
        } ?: return emptyList()

        return exportData.repositories
    }
}
