package com.example.gittracker.data.model

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    @SerializedName("tag_name")
    val tagName: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    val body: String?,
    @SerializedName("published_at")
    val publishedAt: String,
    val assets: List<ReleaseAsset>
)
