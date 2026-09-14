package com.example.gittracker.data.mapper

import com.example.gittracker.data.model.GitHubRelease
import com.example.gittracker.domain.model.Release
import com.example.gittracker.util.DateUtils

fun GitHubRelease.toDomain(repoId: Long): Release = Release(
    repoId = repoId,
    remoteId = id,
    tagName = tagName,
    changelog = if (body.isNullOrBlank()) "No changelog provided." else body,
    htmlUrl = htmlUrl,
    createdAt = DateUtils.parseGithubDate(publishedAt),
    isPrerelease = isPrerelease,
    assets = assets
)
