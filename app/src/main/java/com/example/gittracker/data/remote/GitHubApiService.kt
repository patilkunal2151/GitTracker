package com.example.gittracker.data.remote

import com.example.gittracker.data.model.GitHubRelease
import com.example.gittracker.data.model.GitHubRateLimit
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

    @GET("rate_limit")
    suspend fun getRateLimit(): Response<GitHubRateLimit>
}
