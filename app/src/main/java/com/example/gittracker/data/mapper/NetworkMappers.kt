package com.example.gittracker.data.mapper

import com.example.gittracker.data.model.GitHubRelease
import com.example.gittracker.domain.model.Release
import com.example.gittracker.util.DateUtils

fun GitHubRelease.toDomain(repoId: Long): Release = Release(
    repoId = repoId,
    remoteId = id,
    tagName = tagName,
    changelog = body ?: "No changelog provided.",
    htmlUrl = htmlUrl,
    createdAt = DateUtils.parseGithubDate(publishedAt),
    isPrerelease = isPrerelease,
    assets = assets
)
