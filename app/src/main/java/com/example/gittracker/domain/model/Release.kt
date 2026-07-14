package com.example.gittracker.domain.model

import com.example.gittracker.data.model.ReleaseAsset

data class Release(
    val id: Long = 0,
    val repoId: Long,
    val remoteId: Long = 0,
    val tagName: String,
    val changelog: String,
    val htmlUrl: String,
    val createdAt: Long,
    val isPrerelease: Boolean = false,
    val assets: List<ReleaseAsset> = emptyList()
)
