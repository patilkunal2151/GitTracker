package com.example.gittracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.gittracker.data.model.ReleaseEntity
import com.example.gittracker.data.model.TrackedRepository

@Database(entities = [TrackedRepository::class, ReleaseEntity::class], version = 12, exportSchema = false)
@TypeConverters(Converters::class)
abstract class GitTrackerDatabase : RoomDatabase() {
    abstract fun repositoryDao(): RepositoryDao
}
