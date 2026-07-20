package com.example.gittracker.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GitHubRateLimit(
    val resources: RateLimitResources
)

@Serializable
data class RateLimitResources(
    val core: RateLimitStatus
)

@Serializable
data class RateLimitStatus(
    val limit: Int,
    val remaining: Int,
    val reset: Long,
    val used: Int
)
