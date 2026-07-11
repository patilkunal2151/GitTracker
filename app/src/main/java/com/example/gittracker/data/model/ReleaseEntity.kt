package com.example.gittracker.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "releases",
    foreignKeys = [
        ForeignKey(
            entity = TrackedRepository::class,
            parentColumns = ["id"],
            childColumns = ["repoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["repoId"])]
)
data class ReleaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val repoId: Long,
    val remoteId: Long = 0,
    val tagName: String,
    val changelog: String,
    val htmlUrl: String,
    val createdAt: String,
    val isPrerelease: Boolean = false,
    val assetsJson: String = "[]"
)
