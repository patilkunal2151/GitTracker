package com.example.gittracker.domain.model

data class TrackedRepo(
    val id: Long = 0,
    val owner: String,
    val repoName: String,
    val latestVersionTag: String,
    val latestReleaseId: Long = 0,
    val hasNewUpdate: Boolean = false,
    val name: String = "",
    val isPinned: Boolean = false,
    val reachedEndOfReleases: Boolean = false,
    val description: String? = null,
    val stargazersCount: Int = 0,
    val forksCount: Int = 0,
    val language: String? = null,
    val readme: String? = null
)
