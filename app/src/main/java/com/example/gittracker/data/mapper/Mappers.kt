package com.example.gittracker.data.mapper

import com.example.gittracker.data.model.ReleaseEntity
import com.example.gittracker.data.model.TrackedRepository
import com.example.gittracker.domain.model.Release
import com.example.gittracker.domain.model.TrackedRepo

fun TrackedRepository.toDomain(): TrackedRepo = TrackedRepo(
    id = id,
    owner = owner,
    repoName = repoName,
    latestVersionTag = latestVersionTag,
    latestReleaseId = latestReleaseId,
    hasNewUpdate = hasNewUpdate,
    name = name,
    isPinned = isPinned,
    reachedEndOfReleases = reachedEndOfReleases
)

fun TrackedRepo.toEntity(): TrackedRepository = TrackedRepository(
    id = id,
    owner = owner,
    repoName = repoName,
    latestVersionTag = latestVersionTag,
    latestReleaseId = latestReleaseId,
    hasNewUpdate = hasNewUpdate,
    name = name,
    isPinned = isPinned,
    reachedEndOfReleases = reachedEndOfReleases
)

fun ReleaseEntity.toDomain(): Release = Release(
    id = id,
    repoId = repoId,
    remoteId = remoteId,
    tagName = tagName,
    changelog = changelog,
    htmlUrl = htmlUrl,
    createdAt = createdAt,
    isPrerelease = isPrerelease,
    assets = assets
)

fun Release.toEntity(): ReleaseEntity = ReleaseEntity(
    id = id,
    repoId = repoId,
    remoteId = remoteId,
    tagName = tagName,
    changelog = changelog,
    htmlUrl = htmlUrl,
    createdAt = createdAt,
    isPrerelease = isPrerelease,
    assets = assets
)
