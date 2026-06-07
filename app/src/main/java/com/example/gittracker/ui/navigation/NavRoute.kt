package com.example.gittracker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface NavRoute {
    @Serializable
    data object RepoList : NavRoute

    @Serializable
    data class RepoDetail(val repoId: Long) : NavRoute
}
