package com.example.gittracker.data.model

import com.google.gson.annotations.SerializedName

data class GitHubTag(
    @SerializedName("name") val name: String,
    @SerializedName("zipball_url") val zipballUrl: String,
    @SerializedName("tarball_url") val tarballUrl: String,
    @SerializedName("commit") val commit: TagCommit
)

data class TagCommit(
    @SerializedName("sha") val sha: String,
    @SerializedName("url") val url: String
)
