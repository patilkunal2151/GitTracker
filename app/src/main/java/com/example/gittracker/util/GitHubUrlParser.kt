package com.example.gittracker.util

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubUrlParser @Inject constructor() {
    private val regex = Regex("https://github.com/([^/]+)/([^/\\s]+)")

    data class GitHubRepoInfo(val owner: String, val name: String)

    fun parse(url: String): GitHubRepoInfo? {
        val matchResult = regex.find(url) ?: return null
        val owner = matchResult.groupValues[1]
        val repoName = matchResult.groupValues[2].removeSuffix(".git")
        return GitHubRepoInfo(owner, repoName)
    }
}
