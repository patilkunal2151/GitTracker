package com.example.gittracker.data.model

import com.google.gson.annotations.SerializedName

data class ExportData(
    @SerializedName("version")
    val version: Int = 2,
    @SerializedName("repositories")
    val repositories: List<TrackedRepositoryExport>
)

data class TrackedRepositoryExport(
    @SerializedName("owner")
    val owner: String,
    @SerializedName("repoName")
    val repoName: String,
    @SerializedName("customName")
    val customName: String = "",
    @SerializedName("isPinned")
    val isPinned: Boolean = false
)
