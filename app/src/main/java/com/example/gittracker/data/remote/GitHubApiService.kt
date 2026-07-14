package com.example.gittracker.data.remote

import com.example.gittracker.data.model.GitHubRelease
import com.example.gittracker.data.model.GitHubTag
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
    ): List<GitHubRelease>

    @GET("repos/{owner}/{repo}/tags")
    suspend fun getTags(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30,
        @Query("page") page: Int = 1
    ): List<GitHubTag>
}
