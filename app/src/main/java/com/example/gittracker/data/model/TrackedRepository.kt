package com.example.gittracker.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracked_repositories")
data class TrackedRepository(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val owner: String,
    val repoName: String,
    val latestVersionTag: String,
    val hasNewUpdate: Boolean = false,
    val name: String = "",
    val isPinned: Boolean = false
)
