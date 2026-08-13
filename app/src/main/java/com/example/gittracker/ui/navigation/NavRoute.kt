package com.example.gittracker.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute : NavKey {
    @Serializable
    data object RepoList : NavRoute

    @Serializable
    data class RepoDetail(val repoId: Long) : NavRoute

    @Serializable
    data object Settings : NavRoute
}
