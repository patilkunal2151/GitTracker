package com.example.gittracker.di

import android.content.Context
import androidx.room.Room
import com.example.gittracker.data.local.GitTrackerDatabase
import com.example.gittracker.data.local.RepositoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): GitTrackerDatabase {
        return Room.databaseBuilder(
            context,
            GitTrackerDatabase::class.java,
            "git_tracker_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideRepositoryDao(db: GitTrackerDatabase): RepositoryDao {
        return db.repositoryDao()
    }
}
