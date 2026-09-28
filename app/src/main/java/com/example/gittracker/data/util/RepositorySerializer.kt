package com.example.gittracker.data.util

import com.example.gittracker.data.model.ExportData
import com.example.gittracker.data.model.TrackedRepositoryExport
import com.example.gittracker.domain.model.TrackedRepo
import com.google.gson.Gson
import com.google.gson.JsonParser
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
        val list = mutableListOf<TrackedRepositoryExport>()
        try {
            val jsonElement = JsonParser.parseString(json)
            val jsonArray = when {
                jsonElement.isJsonObject && jsonElement.asJsonObject.has("repositories") -> {
                    jsonElement.asJsonObject.getAsJsonArray("repositories")
                }
                jsonElement.isJsonArray -> jsonElement.asJsonArray
                else -> null
            }

            jsonArray?.forEach { element ->
                if (element.isJsonObject) {
                    val obj = element.asJsonObject
                    val owner = obj.get("owner")?.asString
                        ?: obj.get("author")?.asString
                        ?: ""
                    val repoName = obj.get("repoName")?.asString
                        ?: obj.get("repo")?.asString
                        ?: obj.get("name")?.asString
                        ?: ""
                    val customName = obj.get("customName")?.asString
                        ?: obj.get("name")?.asString
                        ?: repoName
                    val isPinned = obj.get("isPinned")?.asBoolean
                        ?: obj.get("pinned")?.asBoolean
                        ?: false

                    if (owner.isNotBlank() && repoName.isNotBlank()) {
                        list.add(TrackedRepositoryExport(owner = owner, repoName = repoName, customName = customName, isPinned = isPinned))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
