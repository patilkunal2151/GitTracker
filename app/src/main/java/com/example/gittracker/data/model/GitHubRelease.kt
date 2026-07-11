package com.example.gittracker.data.model

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    val id: Long,
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    val body: String?,
    @SerializedName("published_at")
    val publishedAt: String,
    @SerializedName("prerelease")
    val isPrerelease: Boolean,
    val assets: List<ReleaseAsset>
)
