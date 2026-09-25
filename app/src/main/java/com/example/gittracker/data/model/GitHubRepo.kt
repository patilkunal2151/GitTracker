package com.example.gittracker.data.model

import com.google.gson.annotations.SerializedName

data class GitHubRepo(
    val id: Long,
    val name: String,
    @SerializedName("full_name")
    val fullName: String,
    val description: String?,
    @SerializedName("stargazers_count")
    val stargazersCount: Int,
    @SerializedName("forks_count")
    val forksCount: Int,
    val language: String?,
    @SerializedName("html_url")
    val htmlUrl: String,
    val topics: List<String>? = emptyList(),
    val owner: GitHubOwner
)

data class GitHubOwner(
    val login: String,
    @SerializedName("avatar_url")
    val avatarUrl: String
)
