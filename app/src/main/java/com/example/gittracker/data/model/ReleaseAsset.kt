package com.example.gittracker.data.model

import com.google.gson.annotations.SerializedName

data class ReleaseAsset(
    val name: String,
    val size: Long,
    @SerializedName("browser_download_url")
    val downloadUrl: String
)
