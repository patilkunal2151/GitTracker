package com.example.gittracker.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.gittracker.data.model.ReleaseEntity
import com.example.gittracker.data.model.TrackedRepository
import kotlinx.coroutines.flow.Flow

@Dao
interface RepositoryDao {
    @Query("SELECT * FROM tracked_repositories ORDER BY id DESC")
    fun getAllRepositories(): Flow<List<TrackedRepository>>

    @Query("SELECT * FROM tracked_repositories WHERE id = :id")
    suspend fun getRepositoryById(id: Long): TrackedRepository?

    @Query("SELECT * FROM tracked_repositories WHERE owner = :owner AND repoName = :repoName")
    suspend fun getRepositoryByOwnerAndName(owner: String, repoName: String): TrackedRepository?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRepository(repo: TrackedRepository): Long

    @Update
    suspend fun updateRepository(repo: TrackedRepository)

    @Delete
    suspend fun deleteRepository(repo: TrackedRepository)

    @Query("SELECT * FROM releases WHERE repoId = :repoId ORDER BY createdAt DESC")
    fun getReleasesForRepository(repoId: Long): Flow<List<ReleaseEntity>>

    @Query("SELECT * FROM releases WHERE repoId = :repoId")
    suspend fun getReleasesSync(repoId: Long): List<ReleaseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReleases(releases: List<ReleaseEntity>)
}
