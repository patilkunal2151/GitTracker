package com.example.gittracker.data.remote

import com.example.gittracker.data.model.GitHubRelease
import com.example.gittracker.data.model.GitHubRateLimit
import com.example.gittracker.data.model.GitHubRepo
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface GitHubApiService {
    @GET("repos/{owner}/{repo}/releases")
    suspend fun getReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): Response<List<GitHubRelease>>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepoDetails(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRepo>

    @GET("repos/{owner}/{repo}/readme")
    suspend fun getReadme(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubReadme>

    @GET("search/repositories")
    suspend fun searchRepositories(
        @Query("q") query: String,
        @Query("sort") sort: String = "stars",
        @Query("order") order: String = "desc",
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): Response<GitHubSearchResponse>

    @GET("rate_limit")
    suspend fun getRateLimit(): Response<GitHubRateLimit>

    @GET("repos/{owner}/{repo}/git/ref/tags/{tag}")
    suspend fun getTagRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("tag") tag: String
    ): Response<GitHubRefResponse>

    @GET("repos/{owner}/{repo}/git/commits/{sha}")
    suspend fun getCommitDetails(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("sha") sha: String
    ): Response<GitHubCommitResponse>
}

data class GitHubSearchResponse(
    @SerializedName("total_count")
    val totalCount: Int,
    @SerializedName("incomplete_results")
    val incompleteResults: Boolean,
    val items: List<GitHubRepo>
)

data class GitHubReadme(
    val content: String,
    val encoding: String
)

data class GitHubRefResponse(
    val ref: String,
    @SerializedName("object")
    val objectInfo: GitHubRefObject
)

data class GitHubRefObject(
    val sha: String,
    val type: String
)

data class GitHubCommitResponse(
    val message: String
)
